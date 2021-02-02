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

import com.amazonaws.services.s3.model.GetObjectTaggingRequest
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}

import javax.inject.Inject
import play.api.Logger
import play.api.Logger.logger
import uk.gov.hmrc.vatdeferralnewpaymentscheme.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentscheme.connectors.AmazonS3Connector
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.fileimport.{FileDetails, PaymentOnAccount, TimeToPay}
import uk.gov.hmrc.vatdeferralnewpaymentscheme.repo.{ImportFileRepo, PaymentOnAccountRepo, TimeToPayRepo}
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext

class FileImportService @Inject()(config: AppConfig)(implicit ec: ExecutionContext) {

  def importS3File(): Unit = {
    //importFile(config.ttpFilename, { fc => timeToPayRepo.addMany(ParseTTPFile(fc)) })

    logger.debug(s"importS3File triggered with parameters filename:${config.ttpFilename}, region:${config.region}, bucket:${config.bucket}")

    val s3client: AmazonS3 = {
      val builder = AmazonS3ClientBuilder
        .standard()
        .withPathStyleAccessEnabled(true)
      builder.withRegion(config.region)
      builder.build()
    }

    try {
      val response = s3client.listObjects(config.bucket).getObjectSummaries.asScala.map(summary => FileDetails(summary.getKey, summary.getLastModified)).toList
      // val response = s3client.getObject(config.bucket, config.ttpFilename)
      logger.debug(s"S3 Get Object ${response}")
    } catch {
      case e: Exception => {
        logger.error(s"S3 Exception: ${e}")
      }
    }
  }




  //    logger.debug(s"Get object: ${amazonS3Connector.getObject(config.ttpFilename)}")

//  private def importFile(filename: String, updateCollection: (String) => Unit) : Unit = {
//
//    if (amazonS3Connector.exists(filename)) {
//      val s3FileLastModifiedDate = amazonS3Connector.getObject(filename).getObjectMetadata.getLastModified
//
//      fileImportRepo.lastModifiedDate(filename).map {
//        case Some(date) if !s3FileLastModifiedDate.after(date) =>
//          Logger.logger.debug(s"filename:$filename is up to date s3:${s3FileLastModifiedDate} is after: ${s3FileLastModifiedDate.after(date)} ")
//        case _ => {
//          val contentBytes = amazonS3Connector.objectContentBytes(filename)
//          val fileContents = contentBytes.map(_.toChar).mkString
//          updateCollection(fileContents)
//          fileImportRepo.updateLastModifiedDate(filename, s3FileLastModifiedDate)
//        }
//      }
//    }
//    else logger.debug(s"File does not exist: $filename")
//  }
//
//  private def ParseTTPFile(fileContents: String): Array[TimeToPay] = {
//    fileContents.split('\n').drop(1).dropRight(1).map {
//      line => {
//        val lineSplit = line.split(',')
//        val vrn: String = lineSplit(1).replace("\r", "")
//        TimeToPay(vrn)
//      }
//    }
//  }
}