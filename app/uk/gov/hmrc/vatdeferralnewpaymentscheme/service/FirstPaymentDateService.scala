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

import java.time.{LocalDateTime, ZoneId, ZonedDateTime}

import com.google.inject.ImplementedBy
import javax.inject.Inject
import uk.gov.hmrc.vatdeferralnewpaymentscheme.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentscheme.controllers._
import uk.gov.hmrc.vatdeferralnewpaymentscheme.repo.PaymentOnAccountRepo

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[FirstPaymentDateServiceImpl])
trait FirstPaymentDateService {

  def get(vrn: String): Future[ZonedDateTime]
  def now: ZonedDateTime
  val zoneId: ZoneId = ZoneId.of("Europe/London")
  val firstPoaDate: ZonedDateTime =
    ZonedDateTime.of(
      LocalDateTime.of(
        2021,
        3,
        24,
        0,
        0,
        0
      ),
      zoneId
    )
}

class FirstPaymentDateServiceImpl @Inject()(
  paymentOnAccountRepo: PaymentOnAccountRepo,
  appConfig: AppConfig
)(
    implicit executionContext: ExecutionContext
) extends FirstPaymentDateService {

  def now: ZonedDateTime =
    ZonedDateTime
      .now
      .withZoneSameInstant(zoneId)

  override def get(vrn: String): Future[ZonedDateTime] =
    paymentOnAccountRepo.exists(vrn).map { isPoa =>
      if (appConfig.poaUsersEnabled && isPoa && firstPoaDate.isAfter(now.firstPaymentDate)) {
        firstPoaDate
      }
      else now.firstPaymentDate
    }

}
