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
import org.joda.time.{DateTime, Seconds}

import javax.inject.Inject
import play.api.Logger
import uk.gov.hmrc.vatdeferralnewpaymentscheme.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentscheme.connectors.AmazonS3Connector
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.fileimport.{PaymentOnAccount, TimeToPay}
import uk.gov.hmrc.vatdeferralnewpaymentscheme.repo.{ImportFileRepo, PaymentOnAccountRepo, TimeToPayRepo}

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

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

  private def importFile(filename: String, bulkInsert: (String) => Future[Boolean]): Unit = {

    val skipInitialBytesFrom = 7
    val skipInitialBytesTo = 18
    val bytesPerLine = 13
    val numberOfLinesToRead = 100000
    val byteToRead = bytesPerLine * numberOfLinesToRead

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
          Logger.logger.debug(s"content length: ${s3Object.getObjectMetadata.getContentLength}")
          val contentLength = s3Object.getObjectMetadata.getContentLength

          val start = DateTime.now

          def readFileContents(from: Long, to: Long): Unit = {

            if (from > contentLength) {
              Logger.logger.debug("Rename collection")
              timeToPayRepo.renameCollection().map {
                case true => fileImportRepo.updateLastModifiedDate(filename, s3FileLastModifiedDate)
                case _ => throw new RuntimeException("Rename collection failed")
              }
              Logger.logger.debug(s"It took ${Seconds.secondsBetween(start, DateTime.now())} to import all records from file")
            }

            val contentBytes = amazonS3Connector.objectContentBytes(filename, from, to)
            val fileContents = contentBytes.map(_.toChar).mkString

            Logger.logger.debug(s"Line: from: $from, to: $to")

            bulkInsert(fileContents).map {
              case true => {
                Logger.logger.debug("Read next chunk")
                readFileContents(to + 1, to + byteToRead)
              }
              case _ => {
                Logger.logger.debug("Throw exception")
                throw new RuntimeException("failed to do bulk insert")
              }
            }
          }

          readFileContents(skipInitialBytesFrom, skipInitialBytesTo + byteToRead)
        }
      }
    }
    else Logger.logger.debug(s"File does not exist: $filename")
  }

  private def ParseTTPFile(fileContents: String): Array[TimeToPay] = {

    val lst = ListBuffer[TimeToPay]()

    fileContents.split("\\r?\\n").foreach {
      line => {
        println(line)
        if(line.startsWith("2") && line.length == 11) {
          lst.append(TimeToPay(line.substring(2, 11)))
        }
      }
    }

    lst.toArray
  }
}