/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.model.obligations

import play.api.libs.json.Json

case class ObligationData (obligations: List[Obligations])

object ObligationData {
  implicit val format = Json.format[ObligationData]
}











