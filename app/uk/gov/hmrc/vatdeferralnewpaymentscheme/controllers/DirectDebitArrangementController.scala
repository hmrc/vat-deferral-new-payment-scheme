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
import uk.gov.hmrc.vatdeferralnewpaymentscheme.connectors.{DesDirectDebitConnector, DesTimeToPayArrangementConnector, VatRegisteredCompaniesConnector}
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.DirectDebitArrangementRequest
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.arrangement.{DebitDetails, LetterAndControl, TimeToPayArrangementRequest, TtpArrangement}
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.directdebit._
import uk.gov.hmrc.vatdeferralnewpaymentscheme.repo.PaymentPlanStore

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.math.BigDecimal.RoundingMode

@Singleton()
class DirectDebitArrangementController @Inject()(
  appConfig: AppConfig,
  cc: ControllerComponents,
  desDirectDebitConnector: DesDirectDebitConnector,
  desTimeToPayArrangementConnector: DesTimeToPayArrangementConnector,
  vatRegisteredCompaniesConnector: VatRegisteredCompaniesConnector,
  paymentPlanStore: PaymentPlanStore)
  extends BackendController(cc) {

  def post(vrn: String) = Action.async(parse.json) { implicit request =>
    withJsonBody[DirectDebitArrangementRequest] {
      ddar => {

        val totalAmountToPay = ddar.totalAmountToPay
        val numberOfPayments = ddar.numberOfPayments

        val scheduledPaymentAmount = (totalAmountToPay / numberOfPayments).setScale(2, RoundingMode.DOWN)
        val firstPaymentAmount = scheduledPaymentAmount + (totalAmountToPay - (scheduledPaymentAmount * numberOfPayments))

        // TODO: Get the rules from the business
        val startDate = LocalDate.now.withDayOfMonth(ddar.paymentDay)
        val endDate = startDate.plusMonths(numberOfPayments - 1)

        val directDebitInstructionRequest = DirectDebitInstructionRequest(Some(ddar.sortCode), Some(ddar.accountNumber), Some(ddar.accountName), false, None)
        val paymentPlan = PaymentPlan(
          "Time to Pay",
          vrn, // TODO: Verify payment reference
          "VNPS",
          "GBP",
          firstPaymentAmount.toString,
          startDate,
          scheduledPaymentAmount.toString,
          startDate.plusMonths(1),
          endDate,
          "Calendar Monthly",
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
          LocalDate.now.toString,
          endDate.toString,
          startDate.toString,
          firstPaymentAmount.toString,
          scheduledPaymentAmount.toString,
          "Monthly",
          reviewDate.toString,
          "ZZZ",
          "Other",
          true,
          dd.toList)

        vatRegisteredCompaniesConnector.lookup(vrn)
          .map(a => a.getOrElse(throw new RuntimeException("Response does not exist")))
          .map(b => b.target.getOrElse(throw new RuntimeException("Target does not exist")))
          .map(c => {
            val letterAndControl = LetterAndControl(
              customerName = Some(c.name),
              addressLine1 = Some(c.address.line1),
              addressLine2 = c.address.line2,
              addressLine3 = c.address.line3,
              addressLine4 = c.address.line4,
              addressLine5 = c.address.line5,
              postCode = c.address.postcode,
              salutation = Some("Dear Sir or Madam"),
              totalAll = Some(totalAmountToPay.toString))

            val arrangement = TimeToPayArrangementRequest(ttpArrangement, Some(letterAndControl))

            desDirectDebitConnector.createPaymentPlan(paymentPlanRequest, vrn).map(
              _ => desTimeToPayArrangementConnector.createArrangement(vrn, arrangement).flatMap {
                _ => {
                  paymentPlanStore.add(vrn)
                  Future.successful(Created(""))
                }
              }
            )

          }).flatMap(b => b.flatMap(c => c))
      }
    }
  }
}