/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.model.arrangement
import play.api.libs.json.Json
case class LetterAndControl(
   salutation: String,
   customerName: String,
   addressLine1: String,
   addressLine2: Option[String],
   addressLine3: Option[String],
   addressLine4: Option[String],
   addressLine5: Option[String],
   postCode: Option[String],
   totalAll: String
 )

object LetterAndControl {
  implicit val format = Json.format[LetterAndControl]
}