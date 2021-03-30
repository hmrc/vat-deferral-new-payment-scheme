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

import com.google.inject.Inject

import javax.inject.Singleton
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.vatdeferralnewpaymentscheme.service.FirstPaymentDateService
import uk.gov.hmrc.vatdeferralnewpaymentscheme.auth.Auth
import uk.gov.hmrc.vatdeferralnewpaymentscheme.config.AppConfig

import scala.concurrent.ExecutionContext

@Singleton
class FirstPaymentDateController @Inject()(
  cc: ControllerComponents,
  firstPaymentDateService: FirstPaymentDateService,
  auth: Auth
)(
  implicit executionContext: ExecutionContext, val appConfig: AppConfig, val serviceConfig: ServicesConfig
) extends BackendController(cc) {

  def get(vrn: String): Action[AnyContent] = auth.authorised { implicit request =>
    firstPaymentDateService.get(vrn).map(x => Ok(Json.toJson(x)))
  }
}
