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

import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.vatdeferralnewpaymentscheme.connectors.DesTimeToPayArrangementConnector
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.arrangement.TimeToPayArrangementRequest

import scala.concurrent.Future

class FakeDesTimeToPayArrangementConnector(seed: Int) extends DesTimeToPayArrangementConnector {
  override def createArrangement(vrn: String, timeToPayArrangementRequest: TimeToPayArrangementRequest): Future[HttpResponse] = seed match {
    case 202 => Future.successful(HttpResponse.apply(202,""))
    case 4001 => Future.successful(HttpResponse.apply(400,"""{code:"INVALID_IDTYPE"}"""))
    case 4002 => Future.successful(HttpResponse.apply(400,"""{code:"INVALID_IDVALUE"}"""))
    case 4003 => Future.successful(HttpResponse.apply(400,"""{code:"INVALID_PAYLOAD"}"""))
    case 4004 => Future.successful(HttpResponse.apply(400,"""{code:"INVALID_CORRELATIONID"}"""))
    case 500 => Future.successful(HttpResponse.apply(500,""))
    case 503 => Future.successful(HttpResponse.apply(503,""))
  }
}
