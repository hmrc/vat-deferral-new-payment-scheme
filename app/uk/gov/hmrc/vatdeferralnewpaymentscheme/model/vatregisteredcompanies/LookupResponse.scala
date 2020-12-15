/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.model.vatregisteredcompanies

import play.api.libs.json.{Json, OFormat}

case class LookupResponse(target: Option[VatRegisteredCompany])

object LookupResponse {
  implicit val lookupResponseFormat: OFormat[LookupResponse] = Json.format[LookupResponse]
}
