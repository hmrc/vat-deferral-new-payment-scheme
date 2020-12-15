/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.model.vatregisteredcompanies

import play.api.libs.json.{Json, OFormat}

case class VatRegisteredCompany(
  name: String,
  vatNumber: String,
  address: Address
)

object VatRegisteredCompany {
  implicit val vatRegisteredCompanyFormat: OFormat[VatRegisteredCompany] =
    Json.format[VatRegisteredCompany]
}
