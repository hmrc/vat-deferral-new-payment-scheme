/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.service

import play.api.Logger
import uk.gov.hmrc.vatdeferralnewpaymentscheme.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentscheme.connectors.AmazonS3Connector
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.fileimport.TimeToPay
import uk.gov.hmrc.vatdeferralnewpaymentscheme.repo.{ImportFileRepo, LockRepository, PaymentOnAccountRepo, TimeToPayRepo}

import javax.inject.Inject
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

class FileImportService @Inject()(
   amazonS3Connector: AmazonS3Connector,
   timeToPayRepo: TimeToPayRepo,
   paymentOnAccountRepo: PaymentOnAccountRepo,
   fileImportRepo: ImportFileRepo,
   lockRepository: LockRepository,
   config: AppConfig
 )(implicit ec: ExecutionContext) {

  val logger = Logger(getClass)

  def importS3File(): Unit = {
    withLock(1)(importFile[TimeToPay](config.ttpFilename, { case x => ParseTTPString(x) }, { fc => timeToPayRepo.addMany(fc.toArray) }))
  }

  private def importFile[A](filename: String, func1: PartialFunction[String, A], bulkInsert: (Seq[A]) => Future[Unit]): Future[Unit] = {

    Logger.logger.debug(s"Import file triggered with parameters: filename:$filename, region:${config.region}, bucket:${config.bucket}")

    try {
      if (amazonS3Connector.exists(filename)) {
        Logger.logger.debug(s"File exists: filename:$filename")

        val s3Object = amazonS3Connector.getObject(filename)
        val s3FileLastModifiedDate = s3Object.getObjectMetadata.getLastModified

        fileImportRepo.lastModifiedDate(filename).map {
          x => {
            x match {
              case Some(date) if !s3FileLastModifiedDate.after(date) => Logger.logger.debug(s"Import not required: filename:$filename s3 last modified date: $s3FileLastModifiedDate: mongo last modified: $date ")
              case date => {
                Logger.logger.debug(s"Import required: filename:$filename s3 last modified date: $s3FileLastModifiedDate: mongo last modified: $date ")
                Logger.logger.debug(s"content length: ${s3Object.getObjectMetadata.getContentLength}")

                val downloadFileAndImport = amazonS3Connector.chunkFileDownload(filename, func1, bulkInsert)

                val renameCollection = {
                  timeToPayRepo.renameCollection().map {
                    case true => {
                      Logger.logger.debug("Updating last modified date")
                      fileImportRepo.updateLastModifiedDate(filename, s3FileLastModifiedDate)
                      Logger.logger.debug("Completed import")
                    }
                    case _ => throw new RuntimeException("Rename collection failed")
                  }
                }

                Await.result(downloadFileAndImport, Duration.Inf) // TODO: Remove this await
                Await.result(renameCollection, Duration.Inf)      // TODO: Remove this await
              }
            }
          }
        }
      }
      else {
        Logger.logger.warn(s"File does not exist: $filename")
        Future.successful[Unit]()
      }
    } catch {
      case e => {
        Logger.logger.error(s"File import error: $e")
        Future.successful[Unit]()
      }
    }
  }

  private def ParseTTPString(line: String): TimeToPay = {
    //  TODO: Discuss Validation
    if (line.startsWith("2") && line.length == 11) {
      TimeToPay(line.substring(2, 11))
    }
    else{
      logger.warn("Time to Pay String is invalid")
      TimeToPay("error") // TODO: Return an None
    }
  }

  private def withLock(id: Int)(f: => Future[Unit]): Future[Unit] = {
    lockRepository.lock(id).flatMap {
      gotLock =>
        if (gotLock) {
          f.flatMap {
            result =>
              lockRepository.release(id).map {
                _ => {
                  result
                }
              }
          }.recoverWith {
            case e =>
              lockRepository.release(id)
                .map { _ => throw e }
          }
        } else {
          Future.successful(())
        }
    }
  }
}