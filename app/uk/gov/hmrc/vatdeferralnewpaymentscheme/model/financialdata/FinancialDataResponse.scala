/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.model.financialdata

import play.api.libs.json.Json

case class FinancialDataResponse(originalAmount: String, outstandingAmount: String)

object FinancialDataResponse {
  implicit val format = Json.format[FinancialDataResponse]
}
