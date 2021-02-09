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

package uk.gov.hmrc.vatdeferralnewpaymentscheme.controllers

import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers.{status, _}
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.vatdeferralnewpaymentscheme.connectors.{DesDirectDebitConnector, DesTimeToPayArrangementConnector}
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.DirectDebitArrangementRequest

class DirectDebitArrangementControllerSpec extends BaseSpec {

  val fakeHeaders = FakeHeaders(
    Seq(
      "Content-type" -> "application/json"
    )
  )

  val fakeDirectDebitArrangementRequest = DirectDebitArrangementRequest(
    1,2,200.00,"123123","123123","foo"
  )

  val fakeBody: JsValue = Json.toJson(fakeDirectDebitArrangementRequest)

  "GET /" should {
    "return Created for happy path" in {
      val controller = testController(
        new FakeDesDirectDebitConnector(201),
        new FakeDesTimeToPayArrangementConnector(202)
      )
      val result = controller.post("9999999999").apply(FakeRequest("POST", "/direct-debit-arrangement/:vrn",fakeHeaders, fakeBody))
      status(result) shouldBe Status.CREATED
    }
    "also return Created for unhappy path" in {
      val controller = testController(
        new FakeDesDirectDebitConnector(201),
        new FakeDesTimeToPayArrangementConnector(4001)
      )
      val result = controller.post("9999999999").apply(FakeRequest("POST", "/direct-debit-arrangement/:vrn",fakeHeaders, fakeBody))
      status(result) shouldBe Status.CREATED
    }
  }

  def testController(
    ddConnector: DesDirectDebitConnector,
    ttpConnector: DesTimeToPayArrangementConnector
  ) = new DirectDebitArrangementController (
    appConfig,
    cc,
    ddConnector,
    ttpConnector,
    ppStore,
    ddService
  )

}
