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

class VatMainframeSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with MockitoSugar {
  "VatMainframe" should {
    "parse line correctly" in {

      val line = "888914062000040371.63000002123.45"
      val vatMainframe = VatMainframe.parse(line)

      vatMainframe.vrn shouldBe "888914062"
      vatMainframe.deferredCharges shouldBe BigDecimal("40371.63")
      vatMainframe.payments shouldBe BigDecimal("2123.45")
    }

    "parse line correctly with zero pence" in {

      val line = "888914062000040371.63000002123.00"
      val vatMainframe = VatMainframe.parse(line)

      vatMainframe.vrn shouldBe "888914062"
      vatMainframe.deferredCharges shouldBe BigDecimal("40371.63")
      vatMainframe.payments shouldBe BigDecimal("2123.00")
    }

    "return error instance" in {

      val line = "xxxxx"
      val vatMainframe = VatMainframe.parse(line)

      vatMainframe.vrn shouldBe "error"
      vatMainframe.deferredCharges shouldBe BigDecimal(0)
      vatMainframe.payments shouldBe BigDecimal(0)
    }
  }
}
