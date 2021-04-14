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

package uk.gov.hmrc.vatdeferralnewpaymentscheme.model.directdebit

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.DirectDebitArrangementRequest

import java.time.LocalDate

class PaymentPlanSpec extends AnyWordSpec with Matchers {

  "Payment plan" when {
    "2 installments" should {
      "have no scheduled payments" in {
        val directDebitArrangementRequest = DirectDebitArrangementRequest(20, 2, 1000, "111111", "11111111", "company")
        val paymentPlan = PaymentPlan.apply("vrn", directDebitArrangementRequest, LocalDate.parse("2021-03-20"), LocalDate.parse("2021-04-20"))

        paymentPlan shouldBe PaymentPlan(
          "vrn",
          "500.00",
          LocalDate.parse("2021-03-20"),
          "500.00",
          LocalDate.parse("2021-04-20"),
          LocalDate.parse("2021-04-20"),
          "1000",
          LocalDate.parse("2021-04-20"),
          "500.00"
        )
      }
    }

    "3 installments" should {
      "start and end scheduled payments should be the same" in {
        val directDebitArrangementRequest = DirectDebitArrangementRequest(20, 3, 1000, "111111", "11111111", "company")
        val paymentPlan = PaymentPlan.apply("vrn", directDebitArrangementRequest, LocalDate.parse("2021-03-20"), LocalDate.parse("2021-05-20"))

        paymentPlan.scheduledPaymentStartDate shouldBe paymentPlan.scheduledPaymentStartDate

        paymentPlan shouldBe PaymentPlan(
          "vrn",
          "333.34",
          LocalDate.parse("2021-03-20"),
          "333.33",
          LocalDate.parse("2021-04-20"),
          LocalDate.parse("2021-04-20"),
          "1000",
          LocalDate.parse("2021-05-20"),
          "333.33"
        )
      }
    }

    "11 installments" should {
      "setup correctly" in {
        val directDebitArrangementRequest = DirectDebitArrangementRequest(20, 11, 1000, "111111", "11111111", "company")
        val paymentPlan = PaymentPlan.apply("vrn", directDebitArrangementRequest, LocalDate.parse("2021-03-20"), LocalDate.parse("2022-01-20"))

        paymentPlan shouldBe PaymentPlan(
          "vrn",
          "91.00",
          LocalDate.parse("2021-03-20"),
          "90.90",
          LocalDate.parse("2021-04-20"),
          LocalDate.parse("2021-12-20"),
          "1000",
          LocalDate.parse("2022-01-20"),
          "90.90"
          )
      }
    }

    "11 installments with different scheduled payment date" should {
      "first payment date is different from the scheduled payment dates" in {
        val directDebitArrangementRequest = DirectDebitArrangementRequest(5, 11, 1000, "111111", "11111111", "company")
        val paymentPlan = PaymentPlan.apply("vrn", directDebitArrangementRequest, LocalDate.parse("2021-03-20"), LocalDate.parse("2022-01-05"))

        paymentPlan.scheduledPaymentStartDate shouldBe paymentPlan.scheduledPaymentStartDate

        paymentPlan shouldBe PaymentPlan(
          "vrn",
          "91.00",
          LocalDate.parse("2021-03-20"),
          "90.90",
          LocalDate.parse("2021-04-05"),
          LocalDate.parse("2021-12-05"),
          "1000",
          LocalDate.parse("2022-01-05"),
          "90.90"
        )
      }
    }
  }
}