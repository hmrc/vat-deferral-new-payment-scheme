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
                               initialPaymentAmount:      Option[BigDecimal],
                               initialPaymentStartDate:   Option[LocalDate],
                               scheduledPaymentAmount:    BigDecimal,
                               scheduledPaymentStartDate: LocalDate,
                               scheduledPaymentEndDate:   LocalDate,
                               scheduledPaymentFrequency: String,
                               balancingPaymentAmount:    BigDecimal,
                               balancingPaymentDate:      LocalDate,
                               totalLiability:            BigDecimal) {

  def toPaymentPlan: PaymentPlan = {
    PaymentPlan(
      ppType,
      paymentReference,
      hodService,
      paymentCurrency,
      initialPaymentAmount.fold[Option[String]](None)(amount => Some(amount.toString)),
      initialPaymentStartDate,
      scheduledPaymentAmount.toString,
      scheduledPaymentStartDate,
      scheduledPaymentEndDate,
      scheduledPaymentFrequency,
      balancingPaymentAmount.toString,
      balancingPaymentDate,
      totalLiability.toString)
  }
}

object PaymentPlanDecimal {
  implicit val format = Json.format[PaymentPlanDecimal]
}