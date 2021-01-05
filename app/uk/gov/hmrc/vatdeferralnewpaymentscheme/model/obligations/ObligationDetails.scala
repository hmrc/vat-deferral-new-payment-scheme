/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.model.obligations

import play.api.libs.json.Json

case class ObligationDetails (status: String,
                              inboundCorrespondenceFromDate: String,
                              inboundCorrespondenceToDate: String,
                              inboundCorrespondenceDateReceived: Option[String],
                              inboundCorrespondenceDueDate: String,
                              periodKey: String)

object ObligationDetails {
  implicit val format = Json.format[ObligationDetails]
}