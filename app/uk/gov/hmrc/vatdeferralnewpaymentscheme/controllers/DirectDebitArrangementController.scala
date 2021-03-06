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

import play.api.Logger
import play.api.libs.json.{JsValue, Reads}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.vatdeferralnewpaymentscheme.auth.Auth
import uk.gov.hmrc.vatdeferralnewpaymentscheme.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentscheme.connectors.DesTimeToPayArrangementConnector
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.arrangement._
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.directdebit._
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.{DirectDebitArrangementRequest, TtpArrangementAuditWrapper}
import uk.gov.hmrc.vatdeferralnewpaymentscheme.repo.PaymentPlanStore
import uk.gov.hmrc.vatdeferralnewpaymentscheme.service.{DesDirectDebitService, DirectDebitGenService, FirstPaymentDateService, InstallmentsService}
import java.time.format.DateTimeFormatter
import java.util.ServiceConfigurationError

import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class DirectDebitArrangementController @Inject()(
  appConfig: AppConfig,
  cc: ControllerComponents,
  desDirectDebitService: DesDirectDebitService,
  desTimeToPayArrangementConnector: DesTimeToPayArrangementConnector,
  paymentPlanStore: PaymentPlanStore,
  directDebitService: DirectDebitGenService,
  firstPaymentDateService: FirstPaymentDateService,
  installmentsService: InstallmentsService,
  auth: Auth
)(
  implicit ec: ExecutionContext,
  auditConnector: AuditConnector,
  servicesConfig: ServicesConfig
)
  extends BackendController(cc) {

  val logger = Logger(this.getClass)

  def post(vrn: String): Action[JsValue] = auth.authorisedWithJson(parse.json) { implicit request =>
    withJsonBody[DirectDebitArrangementRequest] {
      ddar => {

        val totalAll = ddar.totalAmountToPay.setScale(2)
        val letterAndControl = LetterAndControl(totalAll = totalAll.toString)

        for {
          startDate <- firstPaymentDateService.get(vrn).map(_.toLocalDate)
          endDate = startDate.plusMonths(ddar.numberOfPayments - 1)
          maxInstallments <- installmentsService.installmentPeriodsAvailable(vrn)
          _ = if (maxInstallments < ddar.numberOfPayments) {
                 logger.warn(s"someone has too many installments: maxInstallment $maxInstallments, requested: ${ddar.numberOfPayments}")
                 auditConnector.sendExplicitAudit(
                   "CreatePaymentPlanFailure",
                   Map(
                     "maxInstallments" -> maxInstallments,
                     "requested" -> ddar.numberOfPayments
                   )
                 )
                throw new IllegalArgumentException(s"Too many installments requested; available: $maxInstallments, requested: ${ddar.numberOfPayments} ")
              }
          ppr = PaymentPlanRequest(
                  vrn,
                  ddar,
                  startDate,
                  endDate,
                  firstPaymentDateService.now.format(DateTimeFormatter.ISO_INSTANT),
                  ddiRef(vrn: String)
                )
          ttpa = TtpArrangement(startDate, endDate, ddar)
          arrangement = TimeToPayArrangementRequest(ttpa, letterAndControl)
          a <- desDirectDebitService.createPaymentPlan(ppr, vrn)
          _ = if (a.isRight) paymentPlanStore.add(vrn)
          b <- if (a.isRight) desTimeToPayArrangementConnector.createArrangement(vrn, arrangement)
               else Future.successful(Left(UpstreamErrorResponse("fake error", 418)))
        } yield {
          (a,b) match {
            case (Right(ppr:PaymentPlanReference), Right(y)) if y.status == 202 =>
              audit[PaymentPlanReference](
                "CreatePaymentPlanSuccess",
                ppr
              )
              audit[TtpArrangementAuditWrapper](
                "CreateArrangementSuccess",
                TtpArrangementAuditWrapper(
                  vrn,
                  ttpa,
                  letterAndControl
                )
              )
              logger.info("createPaymentPlan and createArrangement has been successful")
              Created
            case (Right(ppr:PaymentPlanReference), Left(e)) =>
              logger.warn(s"unable to set up time to pay arrangement for $vrn, error response: ${e.message}")
              audit[PaymentPlanReference](
                "CreatePaymentPlanSuccess",
                ppr
              )
              audit[TtpArrangementAuditWrapper](
                "CreateArrangementFailure",
                TtpArrangementAuditWrapper(
                  vrn,
                  ttpa,
                  letterAndControl
                )
              )
              // n.b. we fail silently as there is a manual intervention to fix user state
              Created
            case (Left(UpstreamErrorResponse(message, status, _, _)), _) =>
              logger.warn(s"$status unable to set up direct debit payment plan, not setting up arrangement: $message")
              auditConnector.sendExplicitAudit(
                "CreatePaymentPlanFailure",
                Map(
                  "status" -> status.toString,
                  "message" -> message
                )
              )
              audit[TtpArrangementAuditWrapper](
                "CreateArrangementFailure",
                TtpArrangementAuditWrapper(
                  vrn,
                  ttpa,
                  letterAndControl
                )
              )
              NotAcceptable
          }
        }
      }
    }
  }

  def ddiRef(vrn: String) : String = directDebitService
    .createSeededDDIRef(vrn)
    .fold(throw new RuntimeException("DDIRef cannot be generated"))(_.toString)
}