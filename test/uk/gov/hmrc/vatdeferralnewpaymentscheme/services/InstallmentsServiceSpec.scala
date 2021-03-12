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

package uk.gov.hmrc.vatdeferralnewpaymentscheme.services

import java.time.LocalDate

import uk.gov.hmrc.vatdeferralnewpaymentscheme.controllers.BaseSpec
import uk.gov.hmrc.vatdeferralnewpaymentscheme.service.InstallmentsServiceImpl

class InstallmentsServiceSpec extends BaseSpec {

  def service: InstallmentsServiceImpl = new InstallmentsServiceImpl(firstPaymentDateService)

  val vrn = "1000000000"
  val twentyMillion: BigDecimal = 20000000

  "installmentsMonthsBetween" should {
    "return 11 for firstPaymentDate of 24th of March 2021" in {
      val result = service.installmentMonthsBetween(LocalDate.parse("2021-03-24"), service.finalPaymentDate)
      result shouldBe 11
    }

    "return 10 for firstPaymentDate of 25th of March 2021" in {
      val result = service.installmentMonthsBetween(LocalDate.parse("2021-03-25"), service.finalPaymentDate)
      result shouldBe 10
    }

    "return 10 for firstPaymentDate of 1st of March 2021" in {
      val result = service.installmentMonthsBetween(LocalDate.parse("2021-03-01"), service.finalPaymentDate)
      result shouldBe 11
    }

    "return 1 for firstPaymentDate of 23rd of Jan 2022" in {
      val result = service.installmentMonthsBetween(LocalDate.parse("2022-01-23"), service.finalPaymentDate)
      result shouldBe 1
    }
  }

  "minInstallments" should {
    s"return 11 installments for ${twentyMillion * 11}" in {
      val result = service.minInstallments(11, twentyMillion * 11)
      result shouldBe 11
    }

    "return 1 installments for 200" in {
      val result = service.minInstallments(11, 200)
      result shouldBe 1
    }

    s"return 6 installments for ${twentyMillion * 6}" in {
      val result = service.minInstallments(11, twentyMillion * 6)
      result shouldBe 6
    }

    s"return 7 installments for ${twentyMillion * 6 + 50}" in {
      val result = service.minInstallments(11, twentyMillion * 6 + 50)
      result shouldBe 7
    }
  }
}
