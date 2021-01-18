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

        val startDate = LocalDate.now.withDayOfMonth(ddar.paymentDay)
        val endDate = startDate.plusMonths(numberOfPayments - 1)

        val directDebitInstructionRequest = DirectDebitInstructionRequest(ddar.sortCode, ddar.accountNumber, ddar.accountName, false, "") //TODO: calculate ddiRef
        val paymentPlan = PaymentPlan(
          "Time to Pay",
          vrn,
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

        val ttpArrangement: TtpArrangement = TtpArrangement(
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

        // TODO fix: this fails silently e.g. if the VAT Number is unknown
        val foo = for {
          c <- vatRegisteredCompaniesConnector.lookup(vrn)
          letterAndControl = LetterAndControl(
            "Dear Sir or Madam", // TODO: Welsh translation
            c.name,
            c.address.line1,
            c.address.line2,
            c.address.line3,
            c.address.line4,
            c.address.line5,
            c.address.postcode,
            totalAmountToPay.toString)
          _ <- Future.successful(println(s"BBBBBB${vatRegisteredCompaniesConnector.lookup(vrn)}"))
          _ <-  Future.successful(println(s"CCCCC$c"))
          arrangement = TimeToPayArrangementRequest(ttpArrangement, Some(letterAndControl))
          _ <- desDirectDebitConnector.createPaymentPlan(paymentPlanRequest, vrn)
          _ <- desTimeToPayArrangementConnector.createArrangement(vrn, arrangement)
        } yield {
          paymentPlanStore.add(vrn)
          Created("")
        }
        foo
      }
    }
  }
}