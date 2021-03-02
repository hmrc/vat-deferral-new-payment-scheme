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

package uk.gov.hmrc.vatdeferralnewpaymentscheme

import java.time._

import play.api.libs.json.Writes
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext

package object controllers {

  def audit[T](
    auditType: String,
    result: T
  )(
    implicit headerCarrier: HeaderCarrier,
    auditConnector: AuditConnector,
    ec: ExecutionContext,
    writes: Writes[T]
  ): Unit = {
    import play.api.libs.json.Json
    auditConnector.sendExplicitAudit(
      auditType,
      Json.toJson(result)(writes)
    )
  }

  implicit class FirstPaymentDay(zdt: ZonedDateTime){

    // 2021 bank hols 2/4 5/4 3/5 31/5
    val bankHolidays = Seq(
      LocalDate.of(2021, 4, 2),
      LocalDate.of(2021, 4, 5),
      LocalDate.of(2021, 5, 3),
      LocalDate.of(2021, 5, 31)
    )

    def isWeekend: Boolean =
      Seq(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        .contains(zdt.getDayOfWeek)

    def isBankHoliday: Boolean =
      bankHolidays.contains(zdt.toLocalDate)

    def nonWorkingDay: Boolean = isWeekend || isBankHoliday

    @tailrec
    final def firstPaymentDate: ZonedDateTime = {
      val pdt = zdt.plusDays(7)
      pdt match {
        case dt if dt.nonWorkingDay =>
          zdt.plusDays(1).firstPaymentDate
        case _ =>
          pdt
      }
    }
  }
}
