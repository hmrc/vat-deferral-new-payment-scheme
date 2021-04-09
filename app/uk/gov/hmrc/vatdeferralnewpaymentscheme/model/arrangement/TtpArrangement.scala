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

package uk.gov.hmrc.vatdeferralnewpaymentscheme.model.arrangement
import java.time.LocalDate

import play.api.libs.json.Json
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.DirectDebitArrangementRequest

case class TtpArrangement(
  startDate: String,
  endDate: String,
  firstPaymentDate: String,
  firstPaymentAmount: String,
  regularPaymentAmount: String,
  reviewDate: String,
  directDebit: Boolean,
  debitDetails: List[DebitDetails],
  regularPaymentFrequency: String = "Monthly",
  initials: String = "ZZZ",
  enforcementAction: String = "Other",
  note: String = "NA"
)

object TtpArrangement {

  def apply(
    startDate: LocalDate,
    endDate: LocalDate,
    ddar: DirectDebitArrangementRequest
  ): TtpArrangement = {

    val reviewDate = endDate.plusWeeks(3)
    val dd: Seq[DebitDetails] = (0 until ddar.numberOfPayments).map {
      month => {

        val installmentDate =
          if (month == 0) startDate
          else startDate.withDayOfMonth(ddar.paymentDay).plusMonths(month)

        DebitDetails("IN2", installmentDate.toString)
      }
    }

    TtpArrangement(
      LocalDate.now.toString,
      endDate.withDayOfMonth(ddar.paymentDay).toString,
      startDate.toString,
      ddar.firstPaymentAmount.toString,
      ddar.scheduledPaymentAmount.toString,
      reviewDate.toString,
      directDebit = true,
      dd.toList
    )
  }

  implicit val format = Json.format[TtpArrangement]
}