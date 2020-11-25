/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.model.financialdata

import play.api.libs.json.Json

case class FinancialTransactions (
  chargeType: String,
  mainType: String,
  periodKey: String,
  periodKeyDescription: String,
  taxPeriodFrom: String,
  taxPeriodTo: String,
  businessPartner: String,
  contractAccountCategory: String,
  contractAccount: String,
  contractObjectType: String,
  contractObject: String,
  sapDocumentNumber: String,
  sapDocumentNumberItem: String,
  chargeReference: String,
  mainTransaction: String,
  subTransaction: String,
  originalAmount: Int,
  outstandingAmount: Int,
  clearedAmount: Int,
  accruedInterest: Int,
  items: Seq[Items])

object FinancialTransactions {
  implicit val format = Json.format[FinancialTransactions]
}
