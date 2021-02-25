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

package uk.gov.hmrc.vatdeferralnewpaymentscheme

import org.joda.time.LocalDate
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class FirstPaymentDateSpec extends AnyWordSpec with Matchers {

  def firstPaymentDate(date: LocalDate): LocalDate = {
    date match {
      case dt if dt.getDayOfWeek == 6 =>
        dt.plusDays(9)
      case dt if dt.getDayOfWeek == 7 =>
        dt.plusDays(8)
      case dt =>
        dt.plusDays(7)
    }
  }

  "First payment day" when {
    "Today is Monday" should {
      "be the following Monday" in {
        assertDates("2021-02-22", "2021-03-01")
      }
    }

    "Today is Tuesday" should {
      "be the following Tuesday" in {
        assertDates("2021-02-23", "2021-03-02")
      }
    }

    "Today is Wednesday" should {
      "be the following Wednesday" in {
        assertDates("2021-02-24", "2021-03-03")
      }
    }

    "Today is Thursday" should {
      "be the following Thursday" in {
        assertDates("2021-02-25", "2021-03-04")
      }
    }

    "Today is Friday" should {
      "be the following Friday" in {
        assertDates("2021-02-26", "2021-03-05")
      }
    }

    "Today is Saturday" should {
      "be a week Monday" in {
        assertDates("2021-02-27", "2021-03-08")
      }
    }

    "Today is Sunday" should {
      "be a week Monday" in {
        assertDates("2021-02-28", "2021-03-08")
      }
    }

    // TODO: Add extra days for bank holidays


    def assertDates(today: String, expected: String) = {
      val today1 = LocalDate.parse(today)
      val expectedFirstPaymentDate = LocalDate.parse(expected)
      firstPaymentDate(today1) shouldBe expectedFirstPaymentDate
    }
  }
}
