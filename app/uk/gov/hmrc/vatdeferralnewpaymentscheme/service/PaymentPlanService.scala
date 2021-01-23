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

import com.google.inject.Inject
import uk.gov.hmrc.vatdeferralnewpaymentscheme.repo.{PaymentOnAccountRepo, PaymentPlanStore, TimeToPayRepo}

import scala.concurrent.{ExecutionContext, Future}

class PaymentPlanService @Inject()(
  paymentPlanStore: PaymentPlanStore,
  paymentOnAccountRepo: PaymentOnAccountRepo,
  timeToPayRepo: TimeToPayRepo
)(implicit ec: ExecutionContext) {

  def exists(vrn: String): Future[Boolean] = {
    paymentPlanStore.exists(vrn)
      .flatMap(ppExists => if (ppExists) Future.successful(ppExists) else paymentOnAccountRepo.exists(vrn))
      .flatMap(poaExists => if (poaExists) Future.successful(poaExists) else timeToPayRepo.exists(vrn))
  }
}