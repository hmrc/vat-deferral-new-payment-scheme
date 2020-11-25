/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.model.financialdata

import play.api.libs.json.Json

case class Items (
  subItem: String,
  dueDate: String,
  amount: Int,
  clearingDate: String,
  clearingReason: String,
  outgoingPaymentMethod: String,
  paymentLock: String,
  clearingLock: String,
  interestLock: String,
  dunningLock: String,
  returnFlag: Boolean,
  paymentReference: String,
  paymentAmount: Int,
  paymentMethod: String,
  paymentLot: String,
  paymentLotItem: String,
  clearingSAPDocument: String,
  statisticalDocument: String,
  DDcollectionInProgress: Boolean,
  returnReason: String,
  promiseToPay: String)

object Items {
  implicit val format = Json.format[Items]
}