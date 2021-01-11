/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.model.financialdata

import play.api.libs.json.Json

case class FinancialTransactions (
  chargeType: String,
  mainType: String,
  periodKey: Option[String],
  periodKeyDescription: Option[String],
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
  originalAmount: BigDecimal,
  outstandingAmount: BigDecimal,
  clearedAmount: Option[BigDecimal],
  accruedInterest: Option[BigDecimal],
  items: Seq[Items])

object FinancialTransactions {
  implicit val format = Json.format[FinancialTransactions]
}
