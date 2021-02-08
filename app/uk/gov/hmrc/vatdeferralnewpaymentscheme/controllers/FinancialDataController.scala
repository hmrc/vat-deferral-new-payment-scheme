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

package uk.gov.hmrc.vatdeferralnewpaymentscheme.controllers

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.vatdeferralnewpaymentscheme.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.financialdata.FinancialDataResponse
import uk.gov.hmrc.vatdeferralnewpaymentscheme.service.FinancialDataService

import scala.concurrent.ExecutionContext

@Singleton()
class FinancialDataController @Inject()(
  http: HttpClient,
  appConfig: AppConfig,
  cc: ControllerComponents,
  financialDataService: FinancialDataService
)(implicit ec: ExecutionContext) extends BackendController(cc) {

  val logger = Logger(getClass)

  def get(vrn: String): Action[AnyContent] = Action.async { implicit request =>
    for {
      financialData <- financialDataService.getFinancialData(vrn)
    } yield {
      val fd = FinancialDataResponse(financialData._1.toString, financialData._2.toString)
      val financialDataResponse = Json.toJson(fd).toString()

      logger.info("FinancialDataResponse was retrieved successfully")
      Ok(financialDataResponse)
    }
  }
}

