/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.model.directdebit

import play.api.libs.json.Json
import java.time.LocalDate

case class PaymentPlanDecimal(
                               ppType:                    String,
                               paymentReference:          String,
                               hodService:                String,
                               paymentCurrency:           String,
                               initialPaymentAmount:      BigDecimal,
                               initialPaymentStartDate:   LocalDate,
                               scheduledPaymentAmount:    BigDecimal,
                               scheduledPaymentStartDate: LocalDate,
                               scheduledPaymentEndDate:   LocalDate,
                               scheduledPaymentFrequency: String,
                               totalLiability:            BigDecimal)

object PaymentPlanDecimal {
  implicit val format = Json.format[PaymentPlanDecimal]
}