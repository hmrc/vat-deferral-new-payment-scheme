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
  implicit val headerCarrier = HeaderCarrier(extraHeaders = headers)

  def getObligations(vrn: String) = {
    val url: String = s"${serviceURL}/enterprise/obligation-data/vrn/$vrn/VATC?from=2016-04-06&to=2020-04-06&status=O"
    http.GET[ObligationData](url)
  }

  def getFinancialData(vrn: String)= {
    val url: String = s"${serviceURL}/enterprise/financial-data/VRN/$vrn/VATC?dateFrom=2020-03-20&dateTo=2020-06-30"
    http.GET[FinancialData](url)
  }
}