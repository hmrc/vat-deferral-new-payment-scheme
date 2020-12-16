/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.model.directdebit

import play.api.libs.json.Json

case class KnownFact(service: String, value: String)

object KnownFact {
  implicit val format = Json.format[KnownFact]
}