/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.model.directdebit

import play.api.libs.json.Json

case class DdiReference(ddiReferenceNo: String)

object DdiReference {
  implicit val format = Json.format[DdiReference]
}