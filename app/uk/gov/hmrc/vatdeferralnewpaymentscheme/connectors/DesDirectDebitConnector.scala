/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.connectors

import javax.inject.Inject
import play.api.Logger
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.directdebit.{PaymentPlanReference, PaymentPlanRequest}

import scala.concurrent.ExecutionContext.Implicits.global

class DesDirectDebitConnector @Inject()(http: HttpClient, servicesConfig: ServicesConfig) {

  private def getConfig(key: String) = servicesConfig.getConfString(key, "")

  lazy val serviceURL = servicesConfig.baseUrl("des-directdebit-service")
  lazy val environment: String = getConfig("des-directdebit-service.environment")
  lazy val authorizationToken: String = s"Bearer ${getConfig("des-directdebit-service.authorization-token")}"

  val headers = Seq("Authorization" -> authorizationToken, "Environment" -> environment)
  val headerCarrier = HeaderCarrier(extraHeaders = headers)

  def createPaymentPlan(request: PaymentPlanRequest, credentialId: String) = {
    implicit val hc: HeaderCarrier = headerCarrier

    val createPaymentPlanURL: String = s"$serviceURL/direct-debits/customers/$credentialId/instructions/payment-plans"
    Logger.logger.debug("inside desDirectDebitApiConnector.createPaymentPlan paymentPlan request : " + request.toString)
    http.POST[PaymentPlanRequest, PaymentPlanReference](createPaymentPlanURL, request)
  }
}