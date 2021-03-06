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
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.vatdeferralnewpaymentscheme.auth.Auth
import uk.gov.hmrc.vatdeferralnewpaymentscheme.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.eligibility._
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.fileimport.{PaymentOnAccount, VatMainframe}
import uk.gov.hmrc.vatdeferralnewpaymentscheme.repo.{PaymentOnAccountRepo, PaymentPlanStore, TimeToPayRepo, VatMainframeRepo}
import uk.gov.hmrc.vatdeferralnewpaymentscheme.service.{DesObligationsService, FinancialDataService}

import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class EligibilityController @Inject()(
  appConfig: AppConfig,
  cc: ControllerComponents,
  desObligationsService: DesObligationsService,
  financialDataService: FinancialDataService,
  paymentOnAccountRepo: PaymentOnAccountRepo,
  timeToPayRepo: TimeToPayRepo,
  vatMainframeRepo: VatMainframeRepo,
  paymentPlanStore: PaymentPlanStore,
  auth: Auth
)(
  implicit ec: ExecutionContext,
  val serviceConfig: ServicesConfig
) extends BackendController(cc) {

  val logger = Logger(getClass)
  val nof: Future[Boolean] = Future.successful(false)

  def poaObligations(vrn: String): Future[Boolean] = {
    desObligationsService.getObligationsFromDes(vrn).flatMap {
      case Right(isObl) => Future.successful(isObl)
      case e@Left(UpstreamErrorResponse(msg, 403, _, _)) if msg.contains("NOT_FOUND_BPKEY") =>
        logger.info(s"Got error from getObligationsFromDes$e")
        logger.info(s"Trying getCacheObligationsFromDes")
        desObligationsService.getCacheObligationsFromDes(vrn)
      case Left(e) =>
        logger.warn(s"Unexpected error $e")
        throw e
    }
  }

  def extractObligations(vrn: String) : Future[Boolean] = {
    desObligationsService.getObligationsFromDes(vrn).map {
      case Right(isObl) => isObl
      case Left(e) =>
        logger.warn(s"Unexpected error from extract of etmp $e")
        throw e
    }
  }

  def get(vrn: String): Action[AnyContent] = auth.authorised { _ =>
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
           else if (poa.nonEmpty) poaObligations(vrn)
           else if (vmf.isEmpty) extractObligations(vrn)
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

