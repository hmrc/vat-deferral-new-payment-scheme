/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.service

import javax.inject.Inject
import play.api.Logger
import uk.gov.hmrc.vatdeferralnewpaymentscheme.connectors.AmazonS3Connector
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.fileimport.{PaymentOnAccount, TimeToPay}
import uk.gov.hmrc.vatdeferralnewpaymentscheme.repo.{ImportFileRepo, PaymentOnAccountRepo, TimeToPayRepo}

import scala.concurrent.ExecutionContext.Implicits.global

class FileImportService @Inject()(amazonS3Connector: AmazonS3Connector, timeToPayRepo: TimeToPayRepo, paymentOnAccountRepo: PaymentOnAccountRepo, fileImportRepo: ImportFileRepo) {

  def importS3File() = {
    amazonS3Connector.listOfFiles().foreach(fileDetails => {

      fileImportRepo.lastModifiedDate(fileDetails.name).map {

        case Some(a) if !fileDetails.lastModifiedDate.after(a) => Logger.logger.debug(s"Do nothing: file:$a s3:${fileDetails.lastModifiedDate} is after: ${fileDetails.lastModifiedDate.after(a)} ")
        case _ => {

          val contentBytes = amazonS3Connector.objectContentBytes(fileDetails.name)
          val fileContents = contentBytes.map(_.toChar).mkString

          val paymentPlans = ParseFile(fileContents, fileDetails.name)

          Logger.logger.debug(s"count: ${paymentPlans.length}")

          paymentOnAccountRepo.deleteAll()
          paymentOnAccountRepo.addMany(paymentPlans.collect { case v: PaymentOnAccount => v })

          timeToPayRepo.deleteAll()
          timeToPayRepo.addMany(paymentPlans.collect { case v: TimeToPay => v })

          fileImportRepo.updateLastModifiedDate(fileDetails.name, fileDetails.lastModifiedDate)
        }
      }
    })
  }

  private def ParseFile(fileContents: String, filename: String): Array[Any] = {
    fileContents.split('\n').map {
      line => {
        Logger.logger.debug(s"Line $line")
        val lineSplit = line.split(',')
        val category: String = lineSplit(0)
        val vrn: String = lineSplit(1)

        if (category == "TTP") {
          TimeToPay(vrn, filename)
        }
        else if (category == "POA") {
          PaymentOnAccount(vrn, filename)
        }
      }
    }
  }
}