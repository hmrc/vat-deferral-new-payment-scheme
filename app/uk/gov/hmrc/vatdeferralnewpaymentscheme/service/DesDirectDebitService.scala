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

package uk.gov.hmrc.vatdeferralnewpaymentscheme.service

import play.api.Logger
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.vatdeferralnewpaymentscheme.connectors.DesDirectDebitConnector
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.directdebit.{PaymentPlanReference, PaymentPlanRequest}

import javax.inject.Inject
import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}

class DesDirectDebitService @Inject()(desDirectDebitConnector: DesDirectDebitConnector, directDebitService: DirectDebitGenService)(
  implicit ec: ExecutionContext
) {

  val logger = Logger(getClass)

  def createPaymentPlan(
                         request: PaymentPlanRequest,
                         credentialId: String
                       ): Future[Either[UpstreamErrorResponse,PaymentPlanReference]] = {

    def createPaymentPlanWithRetry(request: PaymentPlanRequest, credentialId: String, acc: Int = 0): Future[Either[UpstreamErrorResponse,PaymentPlanReference]]  = {

      val ddiRef = directDebitService
        .createSeededDDIRef(credentialId, acc > 0)
        .fold(throw new RuntimeException("DDIRef cannot be generated"))(_.toString)

      val requestWithModDDI = request.copy(directDebitInstruction = request.directDebitInstruction.copy(ddiRefNumber = ddiRef))

      desDirectDebitConnector.createPaymentPlan(requestWithModDDI, credentialId).flatMap {

        case Right(p) => Future.successful(Right(p))
        case Left(p) if p.getMessage().contains(s"DDI already exists in core.") && acc < 3 => {
          logger.warn(s"DDI Payment plan failed: retrying with new DDI Ref for the number of times: ${acc + 1}")
          createPaymentPlanWithRetry(requestWithModDDI, credentialId, acc + 1)
        }
        case p => {
          logger.warn(s"DDI Payment plan failed")
          Future.successful(p)
        }
      }
    }

    createPaymentPlanWithRetry(request, credentialId)
  }
}