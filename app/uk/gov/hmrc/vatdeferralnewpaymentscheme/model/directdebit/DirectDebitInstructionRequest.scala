/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.model.directdebit

import play.api.libs.json.Json

case class DirectDebitInstructionRequest(
  sortCode:        String,
  accountNumber:   String,
  accountName:     String,
  paperAuddisFlag: Boolean,
  ddiRefNumber:    String)

object DirectDebitInstructionRequest {
  implicit val format = Json.format[DirectDebitInstructionRequest]
}