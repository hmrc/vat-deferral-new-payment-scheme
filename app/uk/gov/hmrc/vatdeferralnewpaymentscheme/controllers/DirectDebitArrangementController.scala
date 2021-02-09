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
import java.time.{LocalDate, LocalDateTime, ZoneId, ZonedDateTime}

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.vatdeferralnewpaymentscheme.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentscheme.connectors.{DesDirectDebitConnector, DesTimeToPayArrangementConnector}
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.DirectDebitArrangementRequest
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.arrangement._
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.directdebit._
import uk.gov.hmrc.vatdeferralnewpaymentscheme.repo.PaymentPlanStore
import uk.gov.hmrc.vatdeferralnewpaymentscheme.service.DirectDebitGenService

import scala.concurrent.ExecutionContext
import scala.math.BigDecimal.RoundingMode

@Singleton()
class DirectDebitArrangementController @Inject()(
  appConfig: AppConfig,
  cc: ControllerComponents,
  desDirectDebitConnector: DesDirectDebitConnector,
  desTimeToPayArrangementConnector: DesTimeToPayArrangementConnector,
  paymentPlanStore: PaymentPlanStore,
  directDebitService: DirectDebitGenService
)(
  implicit ec: ExecutionContext,
  auditConnector: AuditConnector
)
  extends BackendController(cc) {

  val logger = Logger(this.getClass)

  private lazy val now: ZonedDateTime = ZonedDateTime.now.withZoneSameInstant(ZoneId.of("Europe/London"))

  def firstPaymentDate: ZonedDateTime = { // TODO: Refactor to pass in from FE
    val serviceStart: ZonedDateTime =
      ZonedDateTime.of(
        LocalDateTime.of(2021,2,15,1,1,1),
        ZoneId.of("Europe/London")
      )
    val today = if (now.isAfter(serviceStart)) now else serviceStart
    today match {
      case d if d.getDayOfMonth >= 15 && d.getDayOfMonth <= 22 && d.getMonthValue == 2 =>
        d.withDayOfMonth(3).withMonth(3)
      case d if d.plusDays(5).getDayOfWeek.getValue <= 5 =>
        d.plusDays(5)
      case d if d.plusDays(5).getDayOfWeek.getValue == 6 =>
        d.plusDays(7)
      case d if d.plusDays(5).getDayOfWeek.getValue == 7 =>
        d.plusDays(6)
    }
  }

  def post(vrn: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[DirectDebitArrangementRequest] {
      ddar => {

        val totalAmountToPay = ddar.totalAmountToPay
        val numberOfPayments = ddar.numberOfPayments

        val scheduledPaymentAmount = (totalAmountToPay / numberOfPayments).setScale(2, RoundingMode.DOWN)
        val firstPaymentAmount = scheduledPaymentAmount + (totalAmountToPay - (scheduledPaymentAmount * numberOfPayments))

        val startDate = firstPaymentDate.toLocalDate // TODO: Review toLocalDate
        val endDate =   firstPaymentDate.toLocalDate.plusMonths(numberOfPayments - 1) // TODO: Review toLocalDate

        val directDebitInstructionRequest = DirectDebitInstructionRequest(
          ddar.sortCode,
          ddar.accountNumber,
          ddar.accountName,
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

        for {
          a <- desDirectDebitConnector.createPaymentPlan(paymentPlanRequest, vrn)
          arrangement = TimeToPayArrangementRequest(ttpArrangement)
          b <- desTimeToPayArrangementConnector.createArrangement(vrn, arrangement)
        } yield {
          (a,b) match {
            case (Right(ppr:PaymentPlanReference), Right(y)) if y.status == 202 =>
              paymentPlanStore.add(vrn)
              audit[PaymentPlanReference]("CreatePaymentPlanSuccess", ppr)
              audit[TtpArrangement]("CreateArrangementSuccess", ttpArrangement)
              logger.info("createPaymentPlan and createArrangement has been successful")
              Created
            case (Right(ppr:PaymentPlanReference), Left(e)) =>
              logger.warn(s"unable to set up time to pay arrangement for $vrn, error response: ${e.message}")
              audit[PaymentPlanReference]("CreatePaymentPlanSuccess", ppr)
              audit[TtpArrangementAuditWrapper](
                "CreateArrangementFailure",
                TtpArrangementAuditWrapper(vrn,ttpArrangement)
              )
              // n.b. we fail silently as there is a manual intervention to fix user state
              Created
            case (Left(UpstreamErrorResponse(message, status, _, _)), _) =>
              logger.warn(s"$status unable to set up direct debit payment plan & arrangement: $message")
              auditConnector.sendExplicitAudit(
                "CreatePaymentPlanFailure",
                Map(
                  "status" -> status.toString,
                  "message" -> message
                )
              )
              audit[TtpArrangementAuditWrapper](
                "CreateArrangementFailure",
                TtpArrangementAuditWrapper(vrn,ttpArrangement)
              )
              Created
          }
        }
      }
    }
  }
}