/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.model.directdebit

import play.api.libs.json.Json

case class PaymentPlanReference(
                                 processingDate:         String,
                                 acknowledgementId:      String,
                                 directDebitInstruction: Seq[DdiReference],
                                 paymentPlan:            Seq[PpReference])

object PaymentPlanReference {
  implicit val format = Json.format[PaymentPlanReference]
}