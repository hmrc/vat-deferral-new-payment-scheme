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

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZoneId, ZonedDateTime}

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.vatdeferralnewpaymentscheme.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentscheme.connectors.DesTimeToPayArrangementConnector
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.arrangement._
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.directdebit._
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.{DirectDebitArrangementRequest, TtpArrangementAuditWrapper}
import uk.gov.hmrc.vatdeferralnewpaymentscheme.repo.PaymentPlanStore
import uk.gov.hmrc.vatdeferralnewpaymentscheme.service.{DesDirectDebitService, DirectDebitGenService}

import scala.concurrent.{ExecutionContext, Future}
import scala.math.BigDecimal.RoundingMode

@Singleton()
class DirectDebitArrangementController @Inject()(
  appConfig: AppConfig,
  cc: ControllerComponents,
  desDirectDebitService: DesDirectDebitService,
  desTimeToPayArrangementConnector: DesTimeToPayArrangementConnector,
  paymentPlanStore: PaymentPlanStore,
  directDebitService: DirectDebitGenService
)(
  implicit ec: ExecutionContext,
  auditConnector: AuditConnector
)
  extends BackendController(cc) {

  val logger = Logger(this.getClass)

  private def now: ZonedDateTime = ZonedDateTime.now.withZoneSameInstant(ZoneId.of("Europe/London"))

  def fixAccountName(accountName: String): String = {
    if (accountName.take(40).matches("^[0-9a-zA-Z &@()!:,+`\\-\\'\\.\\/^]{1,40}$")) {
      accountName.take(40)
    } else "NA"
  }

  def post(vrn: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[DirectDebitArrangementRequest] {
      ddar => {

        val totalAmountToPay = ddar.totalAmountToPay
        val numberOfPayments = ddar.numberOfPayments

        val scheduledPaymentAmount = (totalAmountToPay / numberOfPayments).setScale(2, RoundingMode.DOWN)
        val firstPaymentAmount = scheduledPaymentAmount + (totalAmountToPay - (scheduledPaymentAmount * numberOfPayments))

        val startDate = now.firstPaymentDate.toLocalDate // TODO: Review toLocalDate
        val endDate =   now.firstPaymentDate.toLocalDate.plusMonths(numberOfPayments - 1) // TODO: Review toLocalDate

        val directDebitInstructionRequest = DirectDebitInstructionRequest(
          ddar.sortCode,
          ddar.accountNumber,
          fixAccountName(ddar.accountName),
          paperAuddisFlag = false,
          directDebitService
            .createSeededDDIRef(vrn)
            .fold(throw new RuntimeException("DDIRef cannot be generated"))(_.toString)
        )

        val paymentPlan = PaymentPlan(
          "Time to Pay",
          vrn,
          "VNPS",
          "GBP",
          firstPaymentAmount.toString,
          startDate,
          scheduledPaymentAmount.toString,
          startDate.withDayOfMonth(ddar.paymentDay).plusMonths(1),
          endDate.withDayOfMonth(ddar.paymentDay).minusMonths(1),
          "Calendar Monthly",
          totalAmountToPay.toString,
          endDate.withDayOfMonth(ddar.paymentDay),
          scheduledPaymentAmount.toString
        )

        val paymentPlanRequest = PaymentPlanRequest(
          "VDNPS",
          now.format(DateTimeFormatter.ISO_INSTANT),
          Seq(KnownFact("VRN", vrn)),
          directDebitInstructionRequest,
          paymentPlan,
          printFlag = false
        )

        val reviewDate = endDate.plusWeeks(3)

        val dd: Seq[DebitDetails] = (0 until numberOfPayments).map {
          month => {
            DebitDetails("IN2", startDate.withDayOfMonth(ddar.paymentDay).plusMonths(month).toString)
          }
        }

        val ttpArrangement = TtpArrangement(
          LocalDate.now.toString,
          endDate.withDayOfMonth(ddar.paymentDay).toString,
          startDate.toString,
          firstPaymentAmount.toString,
          scheduledPaymentAmount.toString,
          "Monthly",
          reviewDate.toString,
          "ZZZ",
          "Other",
          directDebit = true,
          dd.toList)

        val totalAll = ddar.totalAmountToPay.setScale(2)
        val letterAndControl = LetterAndControl(totalAll = totalAll.toString)

        for {
          a <- desDirectDebitService.createPaymentPlan(paymentPlanRequest, vrn)
          arrangement = TimeToPayArrangementRequest(ttpArrangement, letterAndControl)
          b <- if (a.isRight) desTimeToPayArrangementConnector.createArrangement(vrn, arrangement)
               else Future.successful(Left(UpstreamErrorResponse("fake error", 418)))
        } yield {
          (a,b) match {
            case (Right(ppr:PaymentPlanReference), Right(y)) if y.status == 202 =>
              paymentPlanStore.add(vrn)
              audit[PaymentPlanReference](
                "CreatePaymentPlanSuccess",
                ppr
              )
              audit[TtpArrangementAuditWrapper](
                "CreateArrangementSuccess",
                TtpArrangementAuditWrapper(
                  vrn,
                  ttpArrangement,
                  letterAndControl
                )
              )
              logger.info("createPaymentPlan and createArrangement has been successful")
              Created
            case (Right(ppr:PaymentPlanReference), Left(e)) =>
              paymentPlanStore.add(vrn)
              logger.warn(s"unable to set up time to pay arrangement for $vrn, error response: ${e.message}")
              audit[PaymentPlanReference](
                "CreatePaymentPlanSuccess",
                ppr
              )
              audit[TtpArrangementAuditWrapper](
                "CreateArrangementFailure",
                TtpArrangementAuditWrapper(
                  vrn,
                  ttpArrangement,
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
                  ttpArrangement,
                  letterAndControl
                )
              )
              NotAcceptable
          }
        }
      }
    }
  }
}