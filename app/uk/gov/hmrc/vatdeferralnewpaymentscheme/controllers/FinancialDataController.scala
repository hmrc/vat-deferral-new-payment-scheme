/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.vatdeferralnewpaymentscheme.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.financialdata.FinancialDataResponse
import uk.gov.hmrc.vatdeferralnewpaymentscheme.service.FinancialDataService

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton()
class FinancialDataController @Inject()(http: HttpClient,
                                        appConfig: AppConfig,
                                        cc: ControllerComponents,
                                        financialDataService: FinancialDataService)
    extends BackendController(cc) {

  def get(vrn: String): Action[AnyContent] = Action.async { implicit request =>
    for {
      financialData <- financialDataService.getFinancialData(vrn)
    } yield {
      val fd = FinancialDataResponse(financialData._1.toString, financialData._2.toString)
      val financialDataResponse = Json.toJson(fd).toString()
      Ok(financialDataResponse)
    }
  }
}

