/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.model.directdebit

import play.api.libs.json.Json

case class PaymentPlanRequest(
                               requestingService:      String,
                               submissionDateTime:     String,
                               knownFact:              Seq[KnownFact],
                               directDebitInstruction: DirectDebitInstructionRequest,
                               paymentPlan:            PaymentPlan,
                               printFlag:              Boolean)

object PaymentPlanRequest {
  implicit val format = Json.format[PaymentPlanRequest]
}