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

import javax.inject.Inject
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatdeferralnewpaymentscheme.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentscheme.connectors.DesConnector

import scala.concurrent.ExecutionContext.Implicits.global

class FinancialDataService @Inject()(desConnector: DesConnector, appConfig: AppConfig) {

  def getFinancialData(vrn: String)(implicit hc: HeaderCarrier) = {
    for {
      financialData <- desConnector.getFinancialData(vrn)
    } yield {

      val chargeReferences = appConfig.includedChargeReferences

      financialData.financialTransactions
        .filter { ft =>
          (ft.chargeReference, ft.originalAmount) match {
            case (Some(chargeRef), Some(_)) => chargeReferences.contains(chargeRef)
            case _ => false
          }
        }
        .map { ft => (ft.originalAmount.getOrElse(BigDecimal(0)), ft.outstandingAmount.getOrElse(BigDecimal(0))) }
        .reduceOption((x, y) => {
          (x._1 + y._1, x._2 + y._2)
        }).getOrElse((BigDecimal(0), BigDecimal(0)))
    }
  }
}