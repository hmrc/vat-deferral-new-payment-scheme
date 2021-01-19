/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.controllers

import javax.inject.{Inject, Singleton}
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
        Ok(PaymentPlan(vrn))
      else
        NotFound("")
    }
    }
  }
}