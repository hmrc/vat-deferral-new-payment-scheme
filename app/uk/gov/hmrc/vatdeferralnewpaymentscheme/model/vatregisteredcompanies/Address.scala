/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.model.vatregisteredcompanies

import play.api.libs.json.{Json, OFormat}

case class Address(
  line1: String,
  line2: Option[String],
  line3: Option[String],
  line4: Option[String],
  line5: Option[String],
  postcode: Option[String],
  countryCode: String)

object Address {
  implicit val addressFormat: OFormat[Address] =
    Json.format[Address]
}
