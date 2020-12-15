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
                               totalLiability:            BigDecimal) {

  def toPaymentPlan: PaymentPlan = {
    PaymentPlan(
      ppType,
      paymentReference,
      hodService,
      paymentCurrency,
      initialPaymentAmount.toString,
      initialPaymentStartDate,
      scheduledPaymentAmount.toString,
      scheduledPaymentStartDate,
      scheduledPaymentEndDate,
      scheduledPaymentFrequency,
      totalLiability.toString)
  }
}

object PaymentPlanDecimal {
  implicit val format = Json.format[PaymentPlanDecimal]
}