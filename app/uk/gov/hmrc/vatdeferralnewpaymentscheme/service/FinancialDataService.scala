/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.service

import javax.inject.Inject
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.vatdeferralnewpaymentscheme.connectors.DesConnector

import scala.concurrent.ExecutionContext.Implicits.global

class FinancialDataService @Inject()(http: HttpClient, servicesConfig: ServicesConfig, desConnector: DesConnector) {

  def getFinancialData(vrn: String)(implicit hc: HeaderCarrier) = {
    for {
      financialData <- desConnector.getFinancialData(vrn)
    } yield {
      val originalAmount = financialData.financialTransactions.map{a => a.originalAmount}.sum
      val outstandingAmount = financialData.financialTransactions.map{a => a.outstandingAmount}.sum
      (originalAmount, outstandingAmount)
    }
  }
}