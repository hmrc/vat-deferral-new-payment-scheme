/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.model.arrangement
import play.api.libs.json.Json
case class LetterAndControl(
   customerName: Option[String],
   salutation: Option[String],
   addressLine1: Option[String],
   addressLine2: Option[String],
   addressLine3: Option[String],
   addressLine4: Option[String],
   addressLine5: Option[String],
   postCode: Option[String],
   totalAll: Option[String],
   clmIndicateInt: Option[String],
   clmPymtString: Option[String],
   officeName1: Option[String],
   officeName2: Option[String],
   officePostcode: Option[String],
   officePhone: Option[String],
   officeFax: Option[String],
   officeOpeningHours: Option[String],
   template: Option[String],
   exceptionType: Option[String],
   exceptionReason: Option[String]
 )

object LetterAndControl {
  implicit val format = Json.format[LetterAndControl]
}