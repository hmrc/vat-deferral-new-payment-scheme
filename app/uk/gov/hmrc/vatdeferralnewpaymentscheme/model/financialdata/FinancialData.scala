/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.model.financialdata

import play.api.libs.json.Json

case class FinancialData (
  idType: String,
  idNumber: String,
  regimeType: String,
  processingDate: String,
  financialTransactions: Seq[FinancialTransactions])

object FinancialData {
  implicit val format = Json.format[FinancialData]
}






