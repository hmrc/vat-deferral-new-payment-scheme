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

import akka.stream.scaladsl.StreamConverters
import com.amazonaws.util.IOUtils

import javax.inject.Inject
import play.api.Logger
import uk.gov.hmrc.vatdeferralnewpaymentscheme.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentscheme.connectors.AmazonS3Connector
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.fileimport.{PaymentOnAccount, TimeToPay}
import uk.gov.hmrc.vatdeferralnewpaymentscheme.repo.{ImportFileRepo, PaymentOnAccountRepo, TimeToPayRepo}

import scala.concurrent.ExecutionContext

class FileImportService @Inject()(
  amazonS3Connector: AmazonS3Connector,
  timeToPayRepo: TimeToPayRepo,
  paymentOnAccountRepo: PaymentOnAccountRepo,
  fileImportRepo: ImportFileRepo,
  config: AppConfig
)(implicit ec: ExecutionContext) {


  def importS3File(): Unit = {
    importFile(config.ttpFilename, { fc => timeToPayRepo.addMany(ParseTTPFile(fc)) })
  }

  private def importFile(filename: String, updateCollection: (String) => Unit): Unit = {

    Logger.logger.debug(s"Import file triggered with parameters: filename:$filename, region:${config.region}, bucket:${config.bucket}")

    if (amazonS3Connector.exists(filename)) {
      Logger.logger.debug(s"File exists: filename:$filename")

      val s3Object = amazonS3Connector.getObject(filename)
      val s3FileLastModifiedDate = s3Object.getObjectMetadata.getLastModified

      fileImportRepo.lastModifiedDate(filename).map {
        case Some(date) if !s3FileLastModifiedDate.after(date) =>
          Logger.logger.debug(s"Import not required: filename:$filename s3 last modified date: ${s3FileLastModifiedDate}: mongo last modified: ${date} ")
        case date => {
          Logger.logger.debug(s"Import required: filename:$filename s3 last modified date: ${s3FileLastModifiedDate}: mongo last modified: ${date} ")

          val objectContent = {
            val bytes = IOUtils.toByteArray(s3Object.getObjectContent)
            s3Object.close()
            bytes
          }

          val contentBytes = objectContent
          val fileContents = contentBytes.map(_.toChar).mkString
          updateCollection(fileContents)
          fileImportRepo.updateLastModifiedDate(filename, s3FileLastModifiedDate)
        }
      }
    }
    else Logger.logger.debug(s"File does not exist: $filename")
  }

  private def ParseTTPFile(fileContents: String): Array[TimeToPay] = {
    fileContents.split('\n').drop(1).dropRight(1).map {
      line => {
        val lineSplit = line.split(',')
        val vrn: String = lineSplit(1).replace("\r", "")
        TimeToPay(vrn)
      }
    }
  }
}