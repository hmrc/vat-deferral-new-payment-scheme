/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.connectors

import javax.inject.Inject
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.financialdata.FinancialData
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.obligations.ObligationData

import scala.concurrent.ExecutionContext.Implicits.global

class DesConnector @Inject() (http: HttpClient, servicesConfig: ServicesConfig) {

  private def getConfig(key: String) = servicesConfig.getConfString(key, "")

  lazy val serviceURL = servicesConfig.baseUrl("des-service")
  lazy val environment: String = getConfig("des-service.environment")
  lazy val authorizationToken: String = s"Bearer ${getConfig("des-service.authorization-token")}"

  val headers = Seq("Authorization" -> authorizationToken, "Environment" -> environment)
  val headerCarrier = HeaderCarrier(extraHeaders = headers)

  def getObligations(vrn: String)(implicit hc: HeaderCarrier) = {
    val url: String = s"${serviceURL}/des/enterprise/obligation-data/vrn/$vrn/VATC?from=2016-04-06&to=2020-04-06&status=O"
    http.GET[ObligationData](url)(implicitly, headerCarrier, implicitly)
  }

  def getFinancialData(vrn: String)(implicit hc: HeaderCarrier) = {
    val url: String = s"${serviceURL}/des/enterprise/financial-data/VRN/$vrn/VATC?dateFrom=2020-03-20&dateTo=2020-06-30"
    http.GET[FinancialData](url)(implicitly, headerCarrier, implicitly)
  }
}