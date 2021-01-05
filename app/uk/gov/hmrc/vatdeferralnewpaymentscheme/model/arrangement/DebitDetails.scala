/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.model.arrangement
import play.api.libs.json.Json
case class DebitDetails(debitType: String, dueDate: String)

object DebitDetails {
  implicit val format = Json.format[DebitDetails]
}