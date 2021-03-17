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

import java.time.{LocalDate, Period}

import com.google.inject.ImplementedBy
import javax.inject.Inject

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[InstallmentsServiceImpl])
trait InstallmentsService {

  val finalPaymentDate: LocalDate = LocalDate.parse("2022-01-31")
  val monthlyPaymentLimit = BigDecimal(20000000)

  def installmentPeriodsAvailable(vrn: String): Future[Int]

  def canPay(vrn: String, amount: BigDecimal): Future[Boolean]

  def installmentMonthsBetween(a: LocalDate, b: LocalDate): Int = {
    val period = Period.between(
      a,
      b.plusDays(1) // n.b. the first date is inclusive, the second exclusive
    )
    period.toTotalMonths.toInt + scala.math.min(period.getDays, 1)
  }

  @tailrec
  final def minInstallments(installments: Int, amount: BigDecimal): Int = {
    if(installments == 0 || amount/installments > monthlyPaymentLimit) installments +1
    else minInstallments(installments -1, amount)
  }
}

class InstallmentsServiceImpl @Inject()(
  firstPaymentDateService: FirstPaymentDateService
)(
  implicit executionContext: ExecutionContext
) extends InstallmentsService {

  override def installmentPeriodsAvailable(vrn: String): Future[Int] =
    firstPaymentDateService.get(vrn).map { firstPaymentDate =>
      installmentMonthsBetween(firstPaymentDate.toLocalDate, finalPaymentDate)
    }

  override def canPay(vrn: String, amount: BigDecimal): Future[Boolean] =
    installmentPeriodsAvailable(vrn).map(x => amount/x <= monthlyPaymentLimit) // TODO need to know if this is < or <= e.g. is 20m okay

}
