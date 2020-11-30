/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.model.eligibility

import play.api.libs.json.Json

case class EligibilityResponse(paymentPlanExists: Boolean, existingObligations: Boolean, outstandingBalance: Boolean)

object EligibilityResponse {
  implicit val format = Json.format[EligibilityResponse]
}