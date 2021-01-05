/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.model.arrangement
import play.api.libs.json.Json
case class TtpArrangement(
   startDate: String,
   endDate: String,
   firstPaymentDate: String,
   firstPaymentAmount: String,
   regularPaymentAmount: String,
   regularPaymentFrequency: String,
   reviewDate: String,
   initials: String,
   enforcementAction: String,
   directDebit: Boolean,
   debitDetails: List[DebitDetails],
   note: String = ""
 )

object TtpArrangement {
  implicit val format = Json.format[TtpArrangement]
}