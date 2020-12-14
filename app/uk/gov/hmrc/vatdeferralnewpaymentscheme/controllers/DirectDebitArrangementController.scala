/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.controllers

import java.time.LocalDate

import javax.inject.{Inject, Singleton}
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.vatdeferralnewpaymentscheme.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentscheme.connectors.{DesDirectDebitConnector, DesTimeToPayArrangementConnector}
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.DirectDebitArrangementRequest
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.arrangement.{DebitDetails, LetterAndControl, TimeToPayArrangementRequest, TtpArrangement}
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.directdebit._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.math.BigDecimal.RoundingMode
import scala.concurrent.Future

@Singleton()
class DirectDebitArrangementController @Inject()(appConfig: AppConfig, cc: ControllerComponents, desDirectDebitConnector: DesDirectDebitConnector, desTimeToPayArrangementConnector: DesTimeToPayArrangementConnector)
  extends BackendController(cc) {

  def post(vrn: String) = Action.async(parse.json) { implicit request =>
    withJsonBody[DirectDebitArrangementRequest] {
      ddar => {

        val totalAmountToPay = ddar.totalAmountToPay
        val numberOfPayments = ddar.numberOfPayments

        val scheduledPaymentAmount = (totalAmountToPay / numberOfPayments).setScale(2, RoundingMode.DOWN)
        val balancingPaymentAmount = scheduledPaymentAmount + (totalAmountToPay - (scheduledPaymentAmount * numberOfPayments))

        // TODO: Get the rules from the business
        val startDate = LocalDate.now.withDayOfMonth(ddar.paymentDay)
        val endDate = startDate.plusMonths(numberOfPayments - 1)

        val directDebitInstructionRequest = DirectDebitInstructionRequest(Some(ddar.sortCode), Some(ddar.accountNumber), Some(ddar.accountName), false, None)
        val paymentPlan = PaymentPlan(
          "Time to Pay",
          vrn, // TODO: Verify payment reference
          "VNPS",
          "GBP",
          None,
          None,
          scheduledPaymentAmount.toString,
          startDate,
          endDate,
          "Calendar Monthly",
          balancingPaymentAmount.toString,
          endDate,
          totalAmountToPay.toString)

        val paymentPlanRequest = PaymentPlanRequest(
          "VDNPS",
          LocalDate.now().toString,
          Seq(KnownFact("VRN", vrn)),
          directDebitInstructionRequest,
          paymentPlan,
          false
        )

        val reviewDate = endDate.plusWeeks(3)

        val dd: Seq[DebitDetails] = (0 to numberOfPayments - 1).map {
          month => {
            DebitDetails("IN2", startDate.plusMonths(month).toString)
          }
        }

        val ttpArrangement = TtpArrangement(
          startDate.toString,
          endDate.toString,
          startDate.toString,
          balancingPaymentAmount.toString, // TODO: Hack in the robotics to make initial payment the final payment
          scheduledPaymentAmount.toString, // TODO: Final monthly amount
          "Monthly",
          reviewDate.toString,
          "ZZZ", // TODO: What should the initials be
          "Other", // TODO: What should the enforcement type be
          true,
          dd.toList, // TODO: What are the debit details
          "" // TODO: What notes should be pass
        )

        // val letterAndControl = LetterAndControl() // TODO: Get address from VAT chcker api
        val arrangement = TimeToPayArrangementRequest(ttpArrangement, None)

        desDirectDebitConnector.createPaymentPlan(paymentPlanRequest, vrn).map(
          _ => {
            desTimeToPayArrangementConnector.createArrangement(vrn, arrangement)
              .flatMap{_ => Future.successful(Ok("hello"))}
          }
        ).flatMap(b => b)
      }
    }
  }
}