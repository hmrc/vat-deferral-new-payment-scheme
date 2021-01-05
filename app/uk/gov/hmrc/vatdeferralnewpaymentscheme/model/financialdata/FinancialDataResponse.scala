/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.model.financialdata

import play.api.libs.json.Json

case class FinancialDataResponse(originalAmount: String, outstandingAmount: String)

object FinancialDataResponse {
  implicit val format = Json.format[FinancialDataResponse]
}
