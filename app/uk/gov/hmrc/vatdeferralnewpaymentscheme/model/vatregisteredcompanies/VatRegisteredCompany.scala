/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.model.vatregisteredcompanies

import play.api.libs.functional.syntax._
import play.api.libs.json.{Reads, __}

case class VatRegisteredCompany(
  name: String,
  vatNumber: String,
  address: Address
)

object VatRegisteredCompany {
  implicit val reads: Reads[VatRegisteredCompany] = (
        (__ \ "target" \ "name").read[String] and
        (__ \ "target" \ "vatNumber").read[String] and
        (__ \ "target" \ "address").read[Address]
    )(VatRegisteredCompany.apply _)
}
