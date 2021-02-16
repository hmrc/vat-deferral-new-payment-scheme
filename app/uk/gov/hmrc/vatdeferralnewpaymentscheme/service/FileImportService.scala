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
import reactivemongo.play.json.JSONSerializationPack.Writer
import uk.gov.hmrc.vatdeferralnewpaymentscheme.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentscheme.connectors.AmazonS3Connector
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.fileimport.{FileImportParser, VatMainframe, PaymentOnAccount, TimeToPay}
import uk.gov.hmrc.vatdeferralnewpaymentscheme.repo._

import java.util.Date
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FileImportService @Inject()(
   timeToPayRepo: TimeToPayRepo,
   paymentOnAccountRepo: PaymentOnAccountRepo,
   fileImportRepo: ImportFileRepo,
   vatMainframeRepo: VatMainframeRepo,
   lockRepository: LockRepository,
   config: AppConfig
 )(implicit ec: ExecutionContext) {

  val logger = Logger(getClass)

  def importS3File(): Unit = {
    if (config.ttpEnabled) importFile[TimeToPay](config.ttpFilename, timeToPayRepo, TimeToPay, 1) else logger.info("File Import: TimeToPay File import is disabled")
    if (config.poaEnabled) importFile[PaymentOnAccount](config.poaFilename, paymentOnAccountRepo, PaymentOnAccount, 2) else logger.info("File Import: PaymentOnAccount File import is disabled")
    if (config.vmfEnabled) importFile[VatMainframe](config.vmfFilename, vatMainframeRepo, VatMainframe, 3) else logger.info("File Import: VatMainframe File import is disabled")
  }

  def importFile[A](
    filename: String,
    baseFileImportRepo: BaseFileImportRepo,
    fileImportParser: FileImportParser[A],
    lockId: Int
  )(implicit writer: Writer[A]): Future[Unit] = {

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

                withLock(lockId) {

                  logger.info(s"File Import: filename: $filename: Import required: s3 file last modified date: $s3FileLastModifiedDate: mongo last modified: $date: content length: ${s3Object.getObjectMetadata.getContentLength}")

                  amazonS3Connector
                    .chunkFileDownload(
                      filename,
                      { case x => fileImportParser.parse(x) },
                      baseFileImportRepo.insertFlow,
                      afterImport(s3FileLastModifiedDate, filename, baseFileImportRepo),
                      fileImportParser.filter)
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
      case e:Throwable => {
        logger.error(s"File Import: filename: $filename: File import error: $e")
        Future.successful[Unit]()
      }
    }
  }

  private def afterImport(
     s3FileLastModifiedDate: Date,
     filename: String,
     baseFileImportRepo: BaseFileImportRepo
   ): Future[Unit] = {
    for {
      _ <- baseFileImportRepo.renameCollection()
      _ <- fileImportRepo.updateLastModifiedDate(filename, s3FileLastModifiedDate)
    } yield ()
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