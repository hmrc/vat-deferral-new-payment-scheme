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

import cats.implicits._
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.vatdeferralnewpaymentscheme.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentscheme.connectors.{DesCacheConnector, DesConnector}
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.eligibility.EligibilityResponse
import uk.gov.hmrc.vatdeferralnewpaymentscheme.repo.{PaymentOnAccountRepo, PaymentPlanStore, TimeToPayRepo, VatMainframeRepo}
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
  vatMainframeRepo: VatMainframeRepo,
  paymentPlanStore: PaymentPlanStore,
  cacheConnector: DesCacheConnector
)(
  implicit ec: ExecutionContext
) extends BackendController(cc) {

  val logger = Logger(getClass)
  val nof: Future[Boolean] = Future.successful(false)
  val not: Future[Boolean] = Future.successful(true)

  def get(vrn: String): Action[AnyContent] = Action.async {
    (for {
      a <- paymentPlanStore.exists(vrn)
      b <- if (a) nof else paymentOnAccountRepo.exists(vrn)
      c <- if (a || b) nof else timeToPayRepo.exists(vrn)
      vmf <- vatMainframeRepo.findOne(vrn)
      d <- if (a || b || c ) nof
           else if(vmf.isEmpty) financialDataService.getFinancialData(vrn).map(x => (x._1 + x._2) > 0)
           else Future.successful(vmf.fold(false)(_.outstandingExists))
      e <- if (!d) nof
           else if (vmf.isEmpty) desConnector.getObligations(vrn).map(_.obligations.nonEmpty)
           else cacheConnector.getVatCacheObligations(vrn).map(_.obligations.nonEmpty)
    } yield EligibilityResponse(a, b, c, Some(e), d)).map { result =>
      logger.info("EligibilityResponse was retrieved successfully")
      Ok(Json.toJson(result).toString)
    }
  }

  implicit def toOpt(b: Boolean):Option[Boolean] = b match {
    case true => true.some
    case _ => none[Boolean]
  }
}

