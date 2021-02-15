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

import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.vatdeferralnewpaymentscheme.connectors.DesDirectDebitConnector
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.directdebit.{DdiReference, PaymentPlanReference, PaymentPlanRequest, PpReference}

import scala.concurrent.Future

class FakeDesDirectDebitConnector(seed: Int) extends DesDirectDebitConnector {

  override def createPaymentPlan(request: PaymentPlanRequest, credentialId: String): Future[Either[UpstreamErrorResponse,PaymentPlanReference]] = seed match {
    case 201 =>
      Future.successful(Right(PaymentPlanReference("foo","bar", Seq(DdiReference("foo")), Seq(PpReference("bar")))))
    case 400 =>
      Future.successful(Left(UpstreamErrorResponse("foo", 400)))
    case 404 => ???
    case 500 => ???
    case 203 => ???
  }
}
