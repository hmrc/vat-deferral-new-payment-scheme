/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.model.obligations

import play.api.libs.json.Json

object Obligations {
  implicit val format = Json.format[Obligations]
}

case class Obligations (identification: Identification, obligationDetails: List[ObligationDetails])