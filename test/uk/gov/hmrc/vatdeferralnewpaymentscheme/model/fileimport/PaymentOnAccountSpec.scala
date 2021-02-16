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

package uk.gov.hmrc.vatdeferralnewpaymentscheme.model.fileimport

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class PaymentOnAccountSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with MockitoSugar {
  "PaymentOnAccount" should {
    "parse line correctly" in {

      val line = "2,100000000"
      val paymentOnAccount = PaymentOnAccount.parse(line)

      paymentOnAccount.vrn shouldBe "100000000"
    }

    "return error instance 1" in {

      val line = "1,TTP"
      val paymentOnAccount = PaymentOnAccount.parse(line)

      paymentOnAccount.vrn shouldBe "error"
    }

    "return error instance 2" in {

      val line = "3,000004"
      val paymentOnAccount = PaymentOnAccount.parse(line)

      paymentOnAccount.vrn shouldBe "error"
    }
  }
}
