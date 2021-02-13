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

import akka.actor.ActorSystem
import java.util.Date

import akka.NotUsed
import akka.stream.scaladsl.Flow
import javax.inject.Inject
import play.api.Logger
import reactivemongo.api.commands.MultiBulkWriteResult
import uk.gov.hmrc.vatdeferralnewpaymentscheme.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentscheme.connectors.AmazonS3Connector
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.fileimport.TimeToPay
import uk.gov.hmrc.vatdeferralnewpaymentscheme.repo.{ImportFileRepo, LockRepository, PaymentOnAccountRepo, TimeToPayRepo}

import scala.concurrent.{ExecutionContext, Future}

class FileImportService @Inject()(
   timeToPayRepo: TimeToPayRepo,
   paymentOnAccountRepo: PaymentOnAccountRepo,
   fileImportRepo: ImportFileRepo,
   lockRepository: LockRepository,
   config: AppConfig
 )(implicit ec: ExecutionContext, system: ActorSystem) {

  val logger = Logger(getClass)

  def importS3File(): Unit = {
      importFile[TimeToPay](
        config.ttpFilename,
        { case x => ParseTTPString(x) },
        { timeToPayRepo.insertFlow}
      )
  }

  def afterImport(
    s3FileLastModifiedDate: Date,
    filename: String
  ): Future[Unit] = {
    for {
      _ <- timeToPayRepo.renameCollection()
      _ <- fileImportRepo.updateLastModifiedDate(filename, s3FileLastModifiedDate)
    } yield ()
  }

  private def importFile[A](
    filename: String,
    lineToItem: PartialFunction[String, A],
    mongoBulkInsertFlow: Flow[Seq[A], MultiBulkWriteResult, NotUsed]
  ): Future[Unit] = {

    logger.info(s"File Import: filename: $filename: Import file triggered with parameters: region:${config.region}, bucket:${config.bucket}")

    val amazonS3Connector = new AmazonS3Connector(config)

    try {
      if (amazonS3Connector.exists(filename)) {
        logger.info(s"filename: $filename: Exists")

        val s3Object = amazonS3Connector.getObject(filename)
        val s3FileLastModifiedDate: Date = s3Object.getObjectMetadata.getLastModified

        fileImportRepo.lastModifiedDate(filename).map {
          x => {
            x match {
              case Some(date) if !s3FileLastModifiedDate.after(date) => logger.info(s"filename: $filename: Import not required: s3 file last modified date: $s3FileLastModifiedDate: mongo last modified: $date ")
              case date => {

                withLock(1) {

                  logger.info(s"File Import: filename: $filename: Import required: s3 file last modified date: $s3FileLastModifiedDate: mongo last modified: $date: content length: ${s3Object.getObjectMetadata.getContentLength}")

                  amazonS3Connector
                    .chunkFileDownload(
                      filename,
                      lineToItem,
                      mongoBulkInsertFlow,
                      afterImport(s3FileLastModifiedDate, filename),
                      ttpFilter
                    )
                }
              }
            }
          }
        }
      }
      else {
        logger.warn(s"File Import: filename: $filename: File does not exist")
        Future.successful[Unit]()
      }
    } catch {
      case e => {
        logger.error(s"File Import: filename: $filename: File import error: $e")
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
      logger.info("File Import: Time to Pay String is invalid")
      TimeToPay("error") // TODO: Return an None
    }
  }

  def ttpFilter[A](item: A): Boolean = {
    import shapeless.syntax.typeable._
    item.cast[TimeToPay]
      .fold(
        throw new RuntimeException("FileImport: unable to cast item as TimeToPay")
      )(ttp => ttp.vrn != "error")
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