/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.model.directdebit

import play.api.libs.json.Json

case class DirectDebitInstructionRequest(
                                          sortCode:        Option[String],
                                          accountNumber:   Option[String],
                                          accountName:     Option[String],
                                          paperAuddisFlag: Boolean,
                                          ddiRefNumber:    Option[String]) {

  def copyWithDdiRef(ddiRefrence: String): DirectDebitInstructionRequest = copy(ddiRefNumber = Some(ddiRefrence))
}

object DirectDebitInstructionRequest {
  implicit val format = Json.format[DirectDebitInstructionRequest]
}