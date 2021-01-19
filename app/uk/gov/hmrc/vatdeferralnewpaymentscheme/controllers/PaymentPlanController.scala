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
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.fileimport.PaymentPlan
import uk.gov.hmrc.vatdeferralnewpaymentscheme.service.PaymentPlanService

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton()
class PaymentPlanController @Inject()(
      appConfig: AppConfig,
      cc: ControllerComponents,
      paymentPlanService: PaymentPlanService)
    extends BackendController(cc) {

  def get(vrn: String): Action[AnyContent] = Action.async { implicit request =>
    paymentPlanService.exists(vrn).map { exists => {
      if (exists)
        Ok(Json.toJson(PaymentPlan(vrn)))
      else
        NotFound("")
    }
    }
  }
}