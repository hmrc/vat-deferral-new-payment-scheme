/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.model.directdebit

import play.api.libs.json.Json
import java.time.LocalDate

case class PaymentPlan(
                        ppType:                    String,
                        paymentReference:          String,
                        hodService:                String,
                        paymentCurrency:           String,
                        initialPaymentAmount:      String,
                        initialPaymentStartDate:   LocalDate,
                        scheduledPaymentAmount:    String,
                        scheduledPaymentStartDate: LocalDate,
                        scheduledPaymentEndDate:   LocalDate,
                        scheduledPaymentFrequency: String,
                        totalLiability:            String)

object PaymentPlan {
  implicit val format = Json.format[PaymentPlan]
}