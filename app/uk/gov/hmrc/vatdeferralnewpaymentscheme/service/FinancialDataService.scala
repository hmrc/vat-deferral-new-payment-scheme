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

import cats.Monoid.combineAll
import cats.implicits._
import javax.inject.Inject
import uk.gov.hmrc.vatdeferralnewpaymentscheme.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentscheme.connectors.DesConnector
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.financialdata.TransactionPair

import scala.concurrent.{ExecutionContext, Future}

class FinancialDataService @Inject()(
  desConnector: DesConnector,
  appConfig: AppConfig
)(implicit ec: ExecutionContext) {

  // TODO consider extracting to config
  val allowedPeriodKeys =
    List("20AB", "20AC", "20AD", "19B4", "20C1", "20A1")
  val allowedTransactionPairs =
    List(
      TransactionPair(Some("4730"),Some("1174")),
      TransactionPair(Some("4731"),Some("1174")),
      TransactionPair(Some("4732"),Some("1174")),
      TransactionPair(Some("4733"),Some("1174")),
      TransactionPair(Some("4700"),Some("1174"))
    )

  def getFinancialData(vrn: String): Future[(BigDecimal, BigDecimal)] = {

    for {
      financialData <- desConnector.getFinancialData(vrn)
    } yield {

      combineAll(financialData.financialTransactions
        .filter { ft =>
          (ft.periodKey, ft.transactionPair, ft.originalAmount, ft.outstandingAmount) match {
            case (Some(pk), tp@TransactionPair(Some(_), Some(_)), Some(origA), Some(outStA)) =>
              allowedPeriodKeys.contains(pk) &&
                allowedTransactionPairs.contains(tp) &&
                origA + outStA > 0
            case _ => false
          }
        }.map { ft =>
        (ft.originalAmount.getOrElse(BigDecimal(0)), ft.outstandingAmount.getOrElse(BigDecimal(0)))
      })
    }
  }
}