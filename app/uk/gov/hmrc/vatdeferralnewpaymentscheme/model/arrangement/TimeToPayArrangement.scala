/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.model.arrangement

import play.api.libs.json.Json

case class TimeToPayArrangementRequest(ttpArrangement: TtpArrangement, letterAndControl: Option[LetterAndControl])

object TimeToPayArrangementRequest {
  implicit val format = Json.format[TimeToPayArrangementRequest]
}
