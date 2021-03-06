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
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, NotFoundException}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.vatdeferralnewpaymentscheme.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.obligations.{ObligationData, Obligations}

import scala.concurrent.{ExecutionContext, Future}

class DesCacheConnector @Inject() (
  http: HttpClient,
  servicesConfig: ServicesConfig,
  appConfig: AppConfig
)(implicit ec: ExecutionContext) {

  val logger = Logger(getClass)

  private def getConfig(key: String) = servicesConfig.getConfString(key, "")

  lazy val serviceURL: String = servicesConfig.baseUrl("des-cache-service")

  lazy val credentials: String = s"Bearer ${getConfig("des-cache-service.credentials")}"
  lazy val environment: String = getConfig("des-cache-service.environment")

  val headers = Seq("Authorization" -> credentials, "Environment" -> environment)
  implicit val headerCarrier: HeaderCarrier = HeaderCarrier(extraHeaders = headers)

  def getVatCacheObligations(vrn: String): Future[ObligationData] = {
    val url: String = s"$serviceURL/${appConfig.getVatCacheObligationsPath.replace("$vrn", vrn)}"
    http.GET[ObligationData](url) recover {
      case _: NotFoundException =>
        logger.info("No CacheObligations found")
        ObligationData(List.empty[Obligations])
    }
  }
}
