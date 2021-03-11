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
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.directdebit.InstallmentsAvailable
import uk.gov.hmrc.vatdeferralnewpaymentscheme.service.InstallmentsService

import scala.concurrent.ExecutionContext

@Singleton
class InstallmentsController@Inject()(
  cc: ControllerComponents,
  installmentsService: InstallmentsService
)(
  implicit executionContext: ExecutionContext
) extends BackendController(cc) {

  def canPay(vrn: String, amount: String): Action[AnyContent] = Action.async {
    installmentsService.canPay(vrn, BigDecimal(amount)).map(x => Ok(Json.toJson(x)))
  }

  def installmentsAvailable(vrn: String, amount: String): Action[AnyContent] = Action.async {
    for {
      max <- installmentsService.installmentMonthsRemaining(vrn)
      min = installmentsService.minInstallments(max, BigDecimal(amount))
    } yield Ok(Json.toJson(InstallmentsAvailable(min, max.toInt)))
  }

}
