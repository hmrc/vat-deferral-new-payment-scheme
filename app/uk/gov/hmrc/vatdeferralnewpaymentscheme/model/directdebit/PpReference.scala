/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.model.directdebit

import play.api.libs.json.Json

case class PpReference(ppReferenceNo: String)

object PpReference {
  implicit val format = Json.format[PpReference]
}