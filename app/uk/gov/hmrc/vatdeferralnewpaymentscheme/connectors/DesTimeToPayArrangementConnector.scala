/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.connectors

import javax.inject.Inject
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.arrangement.TimeToPayArrangementRequest

import scala.concurrent.ExecutionContext.Implicits.global

class DesTimeToPayArrangementConnector @Inject()(http: HttpClient, servicesConfig: ServicesConfig) {

  private def getConfig(key: String) = servicesConfig.getConfString(key, "")

  lazy val serviceURL = servicesConfig.baseUrl("des-arrangement-service")
  lazy val environment: String = getConfig("des-arrangement-service.environment")
  lazy val authorizationToken: String = s"Bearer ${getConfig("des-arrangement-service.authorization-token")}"

  val headers = Seq("Authorization" -> authorizationToken, "Environment" -> environment)
  val headerCarrier = HeaderCarrier(extraHeaders = headers)

  def createArrangement(vrn: String, timeToPayArrangementRequest: TimeToPayArrangementRequest)(implicit hc: HeaderCarrier) = {
    val url: String = s"${serviceURL}/time-to-pay/2.0.0/vrn/$vrn/arrangements"
    http.POST[TimeToPayArrangementRequest, HttpResponse](url, timeToPayArrangementRequest)(implicitly, implicitly, headerCarrier, implicitly)
  }
}