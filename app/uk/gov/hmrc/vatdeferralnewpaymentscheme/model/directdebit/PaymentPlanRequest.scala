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

import java.time.LocalDate

import play.api.libs.json.Json
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.DirectDebitArrangementRequest

case class PaymentPlanRequest(
  submissionDateTime:     String,
  knownFact:              Seq[KnownFact],
  directDebitInstruction: DirectDebitInstructionRequest,
  paymentPlan:            PaymentPlan,
  requestingService:      String = "VDNPS",
  printFlag:              Boolean = false
)

object PaymentPlanRequest {

  def apply(
    vrn: String,
    ddar: DirectDebitArrangementRequest,
    startDate: LocalDate,
    endDate: LocalDate,
    submissionDateTime: String,
    ddiReference: String,
  ): PaymentPlanRequest =
    PaymentPlanRequest(
      submissionDateTime,
      Seq(KnownFact("VRN", vrn)),
      DirectDebitInstructionRequest(vrn, ddar, ddiReference),
      PaymentPlan(vrn, ddar, startDate, endDate),
    )

  implicit val format = Json.format[PaymentPlanRequest]
}