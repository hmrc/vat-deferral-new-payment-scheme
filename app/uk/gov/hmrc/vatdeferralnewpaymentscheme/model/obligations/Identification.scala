/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.model.obligations

import play.api.libs.json.Json

case class Identification (incomeSourceType: String, referenceNumber: String, referenceType: String)

object Identification {
  implicit val format = Json.format[Identification]
}