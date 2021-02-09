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

import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.vatdeferralnewpaymentscheme.connectors.DesTimeToPayArrangementConnector
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.arrangement.TimeToPayArrangementRequest

import scala.concurrent.Future

class FakeDesTimeToPayArrangementConnector(seed: Int) extends DesTimeToPayArrangementConnector {
  override def createArrangement(
    vrn: String,
    timeToPayArrangementRequest: TimeToPayArrangementRequest
  ): Future[Either[UpstreamErrorResponse,HttpResponse]] = seed match {
    case 202 =>
      Future.successful(
        Right(HttpResponse.apply(202,""))
      )
    case 4001 =>
      Future.successful(
        Left(
          UpstreamErrorResponse("""{code:"INVALID_IDTYPE"}""",400)
        )
      )
    case 4002 =>
      Future.successful(
        Left(
          UpstreamErrorResponse("""{code:"INVALID_IDVALUE"}""", 400)
        )
      )
    case 4003 =>
      Future.successful(
        Left(
          UpstreamErrorResponse("""{code:"INVALID_PAYLOAD"}""",400)
        )
      )
    case 4004 =>
      Future.successful(
        Left(
          UpstreamErrorResponse("""{code:"INVALID_CORRELATIONID"}""",400)
        )
      )
    case 500 =>
      Future.successful(
        Left(
          UpstreamErrorResponse("SERVER_ERROR",500,500)
        )
      )
    case 503 =>
      Future.successful(
        Left(
          UpstreamErrorResponse("SERVICE_UNAVAILABLE",503,503)
        )
      )
  }
}
