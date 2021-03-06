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
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.vatdeferralnewpaymentscheme.auth.Auth
import uk.gov.hmrc.vatdeferralnewpaymentscheme.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.financialdata.FinancialDataResponse
import uk.gov.hmrc.vatdeferralnewpaymentscheme.repo.{PaymentOnAccountRepo, VatMainframeRepo}
import uk.gov.hmrc.vatdeferralnewpaymentscheme.service.FinancialDataService

import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class FinancialDataController @Inject()(
  appConfig: AppConfig,
  cc: ControllerComponents,
  financialDataService: FinancialDataService,
  vatMainframeRepo: VatMainframeRepo,
  poaRepo: PaymentOnAccountRepo,
  auth: Auth
)(
  implicit ec: ExecutionContext,
  val servicesConfig: ServicesConfig
) extends BackendController(cc) {

  val logger = Logger(getClass)

  def get(vrn: String): Action[AnyContent] = auth.authorised { implicit request =>
    for {
      poa <- poaRepo.findOne(vrn)
      vmf <- vatMainframeRepo.findOne(vrn)
      financialData <- (poa, vmf) match {
        case (Some(p), _) if appConfig.poaUsersEnabled => Future.successful((None, p.outstandingAmount.getOrElse(BigDecimal(0))))
        case (_, Some(v)) => Future.successful((Some(v.deferredCharges), v.deferredCharges - v.payments))
        case _ => financialDataService.getFinancialData(vrn).map { a=>(Some(a._1), a._2) }
      }
    } yield {
      val fd = FinancialDataResponse(financialData._1, financialData._2)
      val financialDataResponse = Json.toJson(fd).toString()

      logger.info("FinancialDataResponse was retrieved successfully")
      Ok(financialDataResponse)
    }
  }
}

