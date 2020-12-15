/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.model.arrangement
import play.api.libs.json.Json
case class LetterAndControl(
   customerName: Option[String] = None,
   salutation: Option[String] = None,
   addressLine1: Option[String] = None,
   addressLine2: Option[String] = None,
   addressLine3: Option[String] = None,
   addressLine4: Option[String] = None,
   addressLine5: Option[String] = None,
   postCode: Option[String] = None,
   totalAll: Option[String] = None,
   clmIndicateInt: Option[String] = None,
   clmPymtString: Option[String] = None,
   officeName1: Option[String] = None,
   officeName2: Option[String] = None,
   officePostcode: Option[String] = None,
   officePhone: Option[String] = None,
   officeFax: Option[String] = None,
   officeOpeningHours: Option[String] = None,
   template: Option[String] = None,
   exceptionType: Option[String] = None,
   exceptionReason: Option[String] = None
 )

object LetterAndControl {
  implicit val format = Json.format[LetterAndControl]
}