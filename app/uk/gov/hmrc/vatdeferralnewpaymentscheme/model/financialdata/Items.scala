/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.model.financialdata

import play.api.libs.json.Json

case class Items (
  subItem: String,
  dueDate: String,
  amount: BigDecimal,
  clearingDate: Option[String],
  clearingReason: Option[String],
  outgoingPaymentMethod: Option[String],
  paymentLock: Option[String],
  clearingLock: Option[String],
  interestLock: Option[String],
  dunningLock: Option[String],
  returnFlag: Option[Boolean],
  paymentReference: Option[String],
  paymentAmount: Option[BigDecimal],
  paymentMethod: Option[String],
  paymentLot: Option[String],
  paymentLotItem: Option[String],
  clearingSAPDocument: Option[String],
  statisticalDocument: Option[String],
  DDcollectionInProgress: Option[Boolean],
  returnReason: Option[String],
  promiseToPay: Option[String])

object Items {
  implicit val format = Json.format[Items]
}