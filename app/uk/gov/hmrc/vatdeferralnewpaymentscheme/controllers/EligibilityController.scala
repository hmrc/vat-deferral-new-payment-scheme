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
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.vatdeferralnewpaymentscheme.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentscheme.connectors.{DesCacheConnector, DesConnector}
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.eligibility._
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.fileimport.{PaymentOnAccount, VatMainframe}
import uk.gov.hmrc.vatdeferralnewpaymentscheme.repo.{PaymentOnAccountRepo, PaymentPlanStore, TimeToPayRepo, VatMainframeRepo}
import uk.gov.hmrc.vatdeferralnewpaymentscheme.service.{DesObligationsService, FinancialDataService}

import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class EligibilityController @Inject()(
  appConfig: AppConfig,
  cc: ControllerComponents,
  desConnector: DesConnector,
  desObligationsService: DesObligationsService,
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

  def tryPoaObligations(vrn: String): Future[Boolean] = {
    try {
      desObligationsService.getObligationsFromDes(vrn)
    } catch {
      case e@UpstreamErrorResponse(msg, 403, _, _) if msg.contains("NOT_FOUND_BPKEY") =>
        logger.info(s"Got error from getObligationsFromDes$e")
        logger.info(s"Trying getCacheObligationsFromDes")
        desObligationsService.getCacheObligationsFromDes(vrn)
      case e:Exception =>
        logger.warn(s"Unexpected error $e")
        throw e
    }
  }

  def get(vrn: String): Action[AnyContent] = Action.async {
    (for {
      a <- paymentPlanStore.exists(vrn)
      c <- if (a) nof else timeToPayRepo.exists(vrn)
      poa <- if (a || c) Future.successful(Option.empty[PaymentOnAccount]) else paymentOnAccountRepo.findOne(vrn)
      vmf <- if (a || c) Future.successful(Option.empty[VatMainframe]) else vatMainframeRepo.findOne(vrn)
      d <- if (a || c ) nof
           else if(poa.nonEmpty) Future.successful(poa.fold(false)(_.outstandingExists))
           else if(vmf.nonEmpty) Future.successful(vmf.fold(false)(_.outstandingExists))
           else financialDataService.getFinancialData(vrn).map(x => (x._1 + x._2) > 0)
      e <- if (!d) nof
           else if (poa.nonEmpty) tryPoaObligations(vrn)
           else if (vmf.isEmpty) desObligationsService.getObligationsFromDes(vrn)
           else desObligationsService.getCacheObligationsFromDes(vrn)
    } yield EligibilityResponse(a, false, c, Some(e), d)).map { result =>
      logger.info("EligibilityResponse was retrieved successfully")
      Ok(Json.toJson(result).toString)
    }
  }

  implicit def toOpt(b: Boolean):Option[Boolean] = b match {
    case true => true.some
    case _ => none[Boolean]
  }
}

