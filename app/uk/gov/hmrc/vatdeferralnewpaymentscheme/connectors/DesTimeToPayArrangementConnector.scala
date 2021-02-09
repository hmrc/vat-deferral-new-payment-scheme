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

import com.google.inject.ImplementedBy
import javax.inject.Inject
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.arrangement.TimeToPayArrangementRequest

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[DesTimeToPayArrangementConnectorImpl])
trait DesTimeToPayArrangementConnector {
  def createArrangement(
    vrn: String,
    timeToPayArrangementRequest: TimeToPayArrangementRequest
  ): Future[Either[UpstreamErrorResponse,HttpResponse]]
}

class DesTimeToPayArrangementConnectorImpl @Inject()(
  http: HttpClient,
  servicesConfig: ServicesConfig
)(
  implicit ec: ExecutionContext
) extends DesTimeToPayArrangementConnector {

  private def getConfig(key: String) = servicesConfig.getConfString(key, "")

  lazy val serviceURL: String = servicesConfig.baseUrl("des-arrangement-service")
  lazy val environment: String = getConfig("des-arrangement-service.environment")
  lazy val authorizationToken: String = s"Bearer ${getConfig("des-arrangement-service.authorization-token")}"

  val headers = Seq("Authorization" -> authorizationToken, "Environment" -> environment)
  implicit val headerCarrier: HeaderCarrier = HeaderCarrier(extraHeaders = headers)

  import uk.gov.hmrc.http.HttpReadsInstances._
  def createArrangement(
    vrn: String,
    timeToPayArrangementRequest:
    TimeToPayArrangementRequest
  ): Future[Either[UpstreamErrorResponse,HttpResponse]] = {
    val url: String = s"$serviceURL/time-to-pay/02.00.00/vrn/$vrn/arrangements"
    http.POST[TimeToPayArrangementRequest, Either[UpstreamErrorResponse,HttpResponse]](
      url,
      timeToPayArrangementRequest
    )
  }
}