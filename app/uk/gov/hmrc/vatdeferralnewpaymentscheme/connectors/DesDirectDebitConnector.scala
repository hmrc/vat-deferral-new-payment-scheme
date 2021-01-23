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

package uk.gov.hmrc.vatdeferralnewpaymentscheme.connectors

import javax.inject.Inject
import play.api.Logger
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.directdebit.{PaymentPlanReference, PaymentPlanRequest}

import scala.concurrent.ExecutionContext

class DesDirectDebitConnector @Inject()(
  http: HttpClient,
  servicesConfig: ServicesConfig
)(implicit ec: ExecutionContext) {

  private def getConfig(key: String) = servicesConfig.getConfString(key, "")

  lazy val serviceURL = servicesConfig.baseUrl("des-directdebit-service")
  lazy val environment: String = getConfig("des-directdebit-service.environment")
  lazy val authorizationToken: String = s"Bearer ${getConfig("des-directdebit-service.authorization-token")}"

  val headers = Seq("Authorization" -> authorizationToken, "Environment" -> environment)
  implicit val headerCarrier = HeaderCarrier(extraHeaders = headers)

  def createPaymentPlan(request: PaymentPlanRequest, credentialId: String) = {
    val createPaymentPlanURL: String = s"$serviceURL/direct-debits/customers/$credentialId/instructions/payment-plans"
    http.POST[PaymentPlanRequest, PaymentPlanReference](createPaymentPlanURL, request)
  }
}