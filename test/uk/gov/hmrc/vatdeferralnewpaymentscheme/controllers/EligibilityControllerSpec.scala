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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.Assertion
import play.api.mvc.{ControllerComponents, Result}
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.{Upstream4xxResponse, UpstreamErrorResponse}
import uk.gov.hmrc.vatdeferralnewpaymentscheme.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentscheme.connectors.{DesCacheConnector, DesConnector}
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.eligibility.EligibilityResponse
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.fileimport
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.fileimport.{PaymentOnAccount, VatMainframe}
import uk.gov.hmrc.vatdeferralnewpaymentscheme.repo.{PaymentOnAccountRepo, PaymentPlanStore, TimeToPayRepo, VatMainframeRepo}
import uk.gov.hmrc.vatdeferralnewpaymentscheme.service.{DesObligationsService, FinancialDataService}

import scala.concurrent.Future

class EligibilityControllerSpec extends BaseSpec {

  "GET /eligibility/:vrn" should {
    // TODO - remove these
    when(paymentOnAccountRepo.findOne(any())).thenReturn(Future.successful(Option.empty[PaymentOnAccount]))
    when(vatMainframeRepo.findOne(any())).thenReturn(Future.successful(Option.empty[VatMainframe]))

    "return ineligible for existing payment plan" in {
      when(paymentPlanStore.exists(any())).thenReturn(Future.successful(true))
      val result = testController.get(any())(fakeGet)
      status(result) shouldBe OK
      resultEq(result, EligibilityResponse(Some(true),None,None,Some(false),None))
    }

    "return ineligible for existing time to pay" in {
      when(paymentPlanStore.exists(any())).thenReturn(Future.successful(false))
      when(timeToPayRepo.exists(any())).thenReturn(Future.successful(true))
      val result = testController.get(any())(fakeGet)
      status(result) shouldBe OK
      resultEq(result, EligibilityResponse(None,None,Some(true),Some(false),None))
    }

    "return eligible for payment on account with outstanding & no obligations" in {
      when(paymentPlanStore.exists(any())).thenReturn(Future.successful(false))
      when(timeToPayRepo.exists(any())).thenReturn(Future.successful(false))
      when(paymentOnAccountRepo.findOne(any())).thenReturn(Future.successful(Some(PaymentOnAccount("foo",Some(200.00)))))
      when(desObligationsService.getObligationsFromDes(any())).thenReturn(Future.successful(false))
      val result = testController.get(any())(fakeGet)
      status(result) shouldBe OK
      resultEq(result, EligibilityResponse(None,None,None,Some(false),Some(true)))
    }

    "return ineligible for payment on account without outstanding & no obligations" in {
      when(paymentPlanStore.exists(any())).thenReturn(Future.successful(false))
      when(timeToPayRepo.exists(any())).thenReturn(Future.successful(false))
      when(paymentOnAccountRepo.findOne(any())).thenReturn(Future.successful(Some(PaymentOnAccount("foo",Some(0.00)))))
      when(desObligationsService.getObligationsFromDes(any())).thenReturn(Future.successful(false))
      val result = testController.get(any())(fakeGet)
      status(result) shouldBe OK
      resultEq(result, EligibilityResponse(None,None,None,Some(false),None))
    }

    "return ineligible for payment on account with outstanding & obligations" in {
      when(paymentPlanStore.exists(any())).thenReturn(Future.successful(false))
      when(timeToPayRepo.exists(any())).thenReturn(Future.successful(false))
      when(paymentOnAccountRepo.findOne(any())).thenReturn(Future.successful(Some(PaymentOnAccount("foo",Some(200.00)))))
      when(desObligationsService.getObligationsFromDes(any())).thenReturn(Future.successful(true))
      val result = testController.get(any())(fakeGet)
      status(result) shouldBe OK
      resultEq(result, EligibilityResponse(None,None,None,Some(true),Some(true)))
    }

    "return eligible for vatmainframe with outstanding & no obligations" in {
      when(paymentPlanStore.exists(any())).thenReturn(Future.successful(false))
      when(timeToPayRepo.exists(any())).thenReturn(Future.successful(false))
      when(paymentOnAccountRepo.findOne(any())).thenReturn(Future.successful(None))
      when(vatMainframeRepo.findOne(any())).thenReturn(Future.successful(Some(fileimport.VatMainframe("foo",200.00, 0.0))))
      when(desObligationsService.getCacheObligationsFromDes(any())).thenReturn(Future.successful(false))
      val result = testController.get(any())(fakeGet)
      status(result) shouldBe OK
      resultEq(result, EligibilityResponse(None,None,None,Some(false),Some(true)))
    }

    "return ineligible for vatmainframe without outstanding & no obligations" in {
      when(paymentPlanStore.exists(any())).thenReturn(Future.successful(false))
      when(timeToPayRepo.exists(any())).thenReturn(Future.successful(false))
      when(paymentOnAccountRepo.findOne(any())).thenReturn(Future.successful(None))
      when(vatMainframeRepo.findOne(any())).thenReturn(Future.successful(Some(fileimport.VatMainframe("foo",200.00, 200.0))))
      when(desObligationsService.getCacheObligationsFromDes(any())).thenReturn(Future.successful(false))
      val result = testController.get(any())(fakeGet)
      status(result) shouldBe OK
      resultEq(result, EligibilityResponse(None,None,None,Some(false),None))
    }

    "return ineligible for vatmainframe with outstanding & obligations" in {
      when(paymentPlanStore.exists(any())).thenReturn(Future.successful(false))
      when(timeToPayRepo.exists(any())).thenReturn(Future.successful(false))
      when(paymentOnAccountRepo.findOne(any())).thenReturn(Future.successful(None))
      when(vatMainframeRepo.findOne(any())).thenReturn(Future.successful(Some(fileimport.VatMainframe("foo",200.00, 0.0))))
      when(desObligationsService.getCacheObligationsFromDes(any())).thenReturn(Future.successful(true))
      val result = testController.get(any())(fakeGet)
      status(result) shouldBe OK
      resultEq(result, EligibilityResponse(None,None,None,Some(true),Some(true)))
    }

//    "failover and return eligible for poa when etmp returns 403 NOT_FOUND_BPKEY" in {
//      when(paymentPlanStore.exists(any())).thenReturn(Future.successful(false))
//      when(timeToPayRepo.exists(any())).thenReturn(Future.successful(false))
//      when(paymentOnAccountRepo.findOne(any())).thenReturn(Future.successful(Some(PaymentOnAccount("foo",Some(200.00)))))
//      when(desObligationsService.getObligationsFromDes(any())).thenThrow(UpstreamErrorResponse("NOT_FOUND_BPKEY", 403))
//      when(desObligationsService.getCacheObligationsFromDes(any())).thenReturn(Future.successful(false))
//      val result = testController.get(any())(fakeGet)
//      status(result) shouldBe OK
//      resultEq(result, EligibilityResponse(None,None,None,Some(true),Some(true)))
//    }

  }

  def resultEq(result: Future[Result], er: EligibilityResponse): Assertion =
    Json.fromJson[EligibilityResponse](Json.parse(contentAsString(result))).get shouldBe er

  def testController = new  EligibilityController(
    appConfig: AppConfig,
    cc: ControllerComponents,
    desConnector: DesConnector,
    desObligationsService: DesObligationsService,
    financialDataService: FinancialDataService,
    paymentOnAccountRepo: PaymentOnAccountRepo,
    timeToPayRepo: TimeToPayRepo,
    vatMainframeRepo: VatMainframeRepo,
    paymentPlanStore: PaymentPlanStore,
    cacheConnector: DesCacheConnector
  )
}
