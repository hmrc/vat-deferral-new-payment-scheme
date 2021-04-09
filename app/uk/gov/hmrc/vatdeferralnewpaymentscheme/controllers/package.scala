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
import scala.language.implicitConversions

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

    val hmrcExcluded = Seq(
      LocalDate.of(2021, 3, 29),
      LocalDate.of(2021, 3, 30),
      LocalDate.of(2021, 3, 31),
      LocalDate.of(2021, 4, 29),
      LocalDate.of(2021, 4, 30),
      LocalDate.of(2021, 5, 27),
      LocalDate.of(2021, 5, 28)
    )

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

    def isHmrcExcluded: Boolean =
      hmrcExcluded.contains(zdt.toLocalDate)

    def isNonWorkingDay: Boolean = isWeekend || isBankHoliday

    def isUnacceptableDay: Boolean = isWeekend || isBankHoliday || isHmrcExcluded

    def firstPaymentDate: ZonedDateTime = firstPaymentDate()

    // N.b. gives us a date 5 working days from now unless that date is hmrcExcluded.
    // If 5 working days from now is hmrcExcluded gives us the next working day after that.
    @tailrec
    private final def firstPaymentDate(workingDayCount: Int = 0): ZonedDateTime = {
      (zdt, workingDayCount) match {
        case (d, i) if i >= 5 && !d.isUnacceptableDay => d
        case (d, i) => d.plusDays(1).firstPaymentDate(i + d)
      }
    }

    private implicit def workDayCountable(zdt: ZonedDateTime): Int =
      if (zdt.isNonWorkingDay) 0
      else 1

  }
}
