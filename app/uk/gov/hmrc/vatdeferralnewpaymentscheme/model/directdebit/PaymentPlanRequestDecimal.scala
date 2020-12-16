/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.model.directdebit

import play.api.libs.json.Json

case class PaymentPlanRequestDecimal(
                                      requestingService:      String,
                                      submissionDateTime:     String,
                                      knownFact:              Seq[KnownFact],
                                      directDebitInstruction: DirectDebitInstructionRequest,
                                      paymentPlan:            PaymentPlanDecimal,
                                      printFlag:              Boolean)

object PaymentPlanRequestDecimal {
  implicit val format = Json.format[PaymentPlanRequestDecimal]
}