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

package uk.gov.hmrc.vatdeferralnewpaymentscheme.model.directdebit

import play.api.libs.json.Json
import java.time.LocalDate

import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.DirectDebitArrangementRequest

case class PaymentPlan(
  paymentReference:          String,
  initialPaymentAmount:      String,
  initialPaymentStartDate:   LocalDate,
  scheduledPaymentAmount:    String,
  scheduledPaymentStartDate: LocalDate,
  scheduledPaymentEndDate:   LocalDate,
  totalLiability:            String,
  balancingPaymentDate:      LocalDate,
  balancingPaymentAmount:    String,
  paymentCurrency:           String = "GBP",
  hodService:                String = "VNPS",
  ppType:                    String = "Time to Pay",
  scheduledPaymentFrequency: String = "Calendar Monthly"
)

object PaymentPlan {

  def apply(
    vrn: String,
    ddar: DirectDebitArrangementRequest,
    startDate: LocalDate,
    endDate: LocalDate
  ): PaymentPlan = PaymentPlan(
    paymentReference          = vrn,
    initialPaymentAmount      = ddar.firstPaymentAmount.toString,
    initialPaymentStartDate   = startDate,
    scheduledPaymentAmount    = ddar.scheduledPaymentAmount.toString,
    scheduledPaymentStartDate = if (ddar.numberOfPayments > 2)
                                  startDate.withDayOfMonth(ddar.paymentDay).plusMonths(1)
                                else
                                  endDate.withDayOfMonth(ddar.paymentDay),
    scheduledPaymentEndDate   = if (ddar.numberOfPayments > 2)
                                  endDate.withDayOfMonth(ddar.paymentDay).minusMonths(1)
                                else
                                  endDate.withDayOfMonth(ddar.paymentDay),
    totalLiability            = ddar.totalAmountToPay.toString,
    balancingPaymentDate      = endDate.withDayOfMonth(ddar.paymentDay),
    balancingPaymentAmount    = ddar.scheduledPaymentAmount.toString
  )

  implicit val format = Json.format[PaymentPlan]
}