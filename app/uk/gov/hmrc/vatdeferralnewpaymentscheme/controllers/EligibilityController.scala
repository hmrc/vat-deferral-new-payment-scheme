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
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.vatdeferralnewpaymentscheme.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentscheme.connectors.{DesCacheConnector, DesConnector}
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.eligibility.EligibilityResponse
import uk.gov.hmrc.vatdeferralnewpaymentscheme.repo.{PaymentOnAccountRepo, PaymentPlanStore, TimeToPayRepo}
import uk.gov.hmrc.vatdeferralnewpaymentscheme.service.FinancialDataService

import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class EligibilityController @Inject()(
  appConfig: AppConfig,
  cc: ControllerComponents,
  desConnector: DesConnector,
  desCacheConnector: DesCacheConnector,
  financialDataService: FinancialDataService,
  paymentOnAccountRepo: PaymentOnAccountRepo,
  timeToPayRepo: TimeToPayRepo,
  paymentPlanStore: PaymentPlanStore
)(
  implicit ec: ExecutionContext
) extends BackendController(cc) {

  def get(vrn: String): Action[AnyContent] = Action.async { implicit request =>

    for {
      paymentPlanExists      <- paymentPlanStore.exists(vrn)
      paymentOnAccountExists <- paymentOnAccountRepo.exists(vrn)
      timeToPayExists        <- timeToPayRepo.exists(vrn)
      obligations            <- desConnector.getObligations(vrn)
      obligationsCache       <- obligations.obligations match {
        case Nil => desCacheConnector.getVatCacheObligations(vrn)
        case _ => Future.successful(obligations)
      }
      financialData          <- financialDataService.getFinancialData(vrn)
    } yield {
      val eligibilityResponse =
        Json.toJson(
          EligibilityResponse(
            paymentPlanExists,
            paymentOnAccountExists,
            timeToPayExists,
            obligations.obligations.nonEmpty || obligationsCache.obligations.nonEmpty,
            financialData._1 > 0
          )
        ).toString()
      Ok(eligibilityResponse)
    }
  }

}

