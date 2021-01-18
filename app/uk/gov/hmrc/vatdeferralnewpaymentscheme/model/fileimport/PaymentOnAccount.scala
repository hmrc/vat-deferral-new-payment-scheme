/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.model.fileimport

import play.api.libs.json.Json

case class PaymentOnAccount(vrn: String, filename: String)

object PaymentOnAccount {
  implicit val format = Json.format[PaymentOnAccount]
}