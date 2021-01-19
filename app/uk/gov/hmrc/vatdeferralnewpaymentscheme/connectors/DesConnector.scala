/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.connectors

import javax.inject.Inject
import play.api.Logger
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, NotFoundException}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.vatdeferralnewpaymentscheme.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.financialdata.FinancialData
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.obligations.{ObligationData, Obligations}

import scala.concurrent.ExecutionContext.Implicits.global

class DesConnector @Inject() (http: HttpClient, servicesConfig: ServicesConfig, appConfig: AppConfig) {

  private def getConfig(key: String) = servicesConfig.getConfString(key, "")

  lazy val serviceURL = servicesConfig.baseUrl("des-service")
  lazy val environment: String = getConfig("des-service.environment")
  lazy val authorizationToken: String = s"Bearer ${getConfig("des-service.authorization-token")}"

  val headers = Seq("Authorization" -> authorizationToken, "Environment" -> environment)
  implicit val headerCarrier = HeaderCarrier(extraHeaders = headers)

  def getObligations(vrn: String) = {
    val url: String = s"$serviceURL/${appConfig.getObligationsPath.replace("$vrn", vrn)}"
    Logger.logger.debug(s"url: $url")
    http.GET[ObligationData](url) recover {
      case _: NotFoundException => ObligationData(List.empty[Obligations])
    }
  }

  def getFinancialData(vrn: String)= {
    val url: String = s"$serviceURL/${appConfig.getFinancialDataPath.replace("$vrn", vrn)}"
    http.GET[FinancialData](url)
  }
}