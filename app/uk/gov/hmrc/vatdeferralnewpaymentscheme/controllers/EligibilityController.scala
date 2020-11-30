/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.vatdeferralnewpaymentscheme.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentscheme.repo.PaymentPlanStore
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.eligibility.EligibilityResponse
import play.api.libs.json.Json
import uk.gov.hmrc.vatdeferralnewpaymentscheme.connectors.DesConnector
import uk.gov.hmrc.vatdeferralnewpaymentscheme.service.FinancialDataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton()
class EligibilityController @Inject()(http: HttpClient,
      appConfig: AppConfig,
      cc: ControllerComponents,
      paymentPlanStore: PaymentPlanStore,
      desConnector: DesConnector,
      financialDataService: FinancialDataService)
    extends BackendController(cc) {

  def get(vrn: String): Action[AnyContent] = Action.async { implicit request =>

    for {
      paymentPlanExists <- paymentPlanStore.exists(vrn)
      obligations <- desConnector.getObligations(vrn)
      financialData <- financialDataService.getFinancialData(vrn)
    } yield {
      val eligibilityResponse = Json.toJson(EligibilityResponse(paymentPlanExists, obligations.obligations.size > 0, financialData._1 > 0)).toString()
      Ok(eligibilityResponse)
    }
  }
}

