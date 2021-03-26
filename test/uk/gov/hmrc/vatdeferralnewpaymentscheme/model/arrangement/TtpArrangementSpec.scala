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

package uk.gov.hmrc.vatdeferralnewpaymentscheme.model.arrangement

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.DirectDebitArrangementRequest

import java.time.LocalDate

class TtpArrangementSpec extends AnyWordSpec with Matchers {

  "Time to pay arrangement" when {
    {
      "object is initialised" should {
        "set the direct debit list up correctly" in {

          val directDebitArrangementRequest = DirectDebitArrangementRequest(5, 11, 1000, "111111", "11111111", "company")
          val ttpArrangement = TtpArrangement(LocalDate.parse("2021-03-20"), LocalDate.parse("2022-01-05"), directDebitArrangementRequest)

          ttpArrangement shouldBe TtpArrangement(
            LocalDate.now.toString,
            "2022-01-05",
            "2021-03-20",
            "91.00",
            "90.90",
            "2022-01-26",
            true,
            List[DebitDetails](
              DebitDetails("IN2", "2021-03-20"),
              DebitDetails("IN2", "2021-04-05"),
              DebitDetails("IN2", "2021-05-05"),
              DebitDetails("IN2", "2021-06-05"),
              DebitDetails("IN2", "2021-07-05"),
              DebitDetails("IN2", "2021-08-05"),
              DebitDetails("IN2", "2021-09-05"),
              DebitDetails("IN2", "2021-10-05"),
              DebitDetails("IN2", "2021-11-05"),
              DebitDetails("IN2", "2021-12-05"),
              DebitDetails("IN2", "2022-01-05"))
          )
        }
      }
    }
  }
}