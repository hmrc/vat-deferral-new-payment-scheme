/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.vatdeferralnewpaymentscheme.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentscheme.connectors.DesConnector
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.eligibility.EligibilityResponse
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.financialdata.FinancialDataResponse
import uk.gov.hmrc.vatdeferralnewpaymentscheme.repo.PaymentPlanStore

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton()
class FinancialDataController @Inject()(http: HttpClient,
                                        appConfig: AppConfig,
                                        cc: ControllerComponents,
                                        desConnector: DesConnector)
    extends BackendController(cc) {

  def get(vrn: String): Action[AnyContent] = Action.async { implicit request =>
    for {
      financialData <- desConnector.getFinancialData(vrn)
    } yield {
      val financialDataResponse = Json.toJson(FinancialDataResponse(financialData.financialTransactions.head.originalAmount.toString, financialData.financialTransactions.head.outstandingAmount.toString)).toString()
      Ok(financialDataResponse)
    }
  }
}

