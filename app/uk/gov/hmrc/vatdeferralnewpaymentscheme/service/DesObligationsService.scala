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

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import javax.inject.Inject
import uk.gov.hmrc.vatdeferralnewpaymentscheme.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentscheme.connectors.{DesCacheConnector, DesConnector}

import scala.concurrent.{ExecutionContext, Future}

class DesObligationsService @Inject()(
  desConnector: DesConnector,
  desCacheConnector: DesCacheConnector,
  appConfig: AppConfig
)(implicit ec: ExecutionContext) {

//  2017-02-07 to 2021-02-06

  def isWithinDateRange(dueDate: LocalDate): Boolean = {
    (dueDate.isAfter(appConfig.obligationsDateRangeFrom) || dueDate.isEqual(appConfig.obligationsDateRangeFrom)) &&
      (dueDate.isBefore(appConfig.obligationsDateRangeTo) || dueDate.isEqual(appConfig.obligationsDateRangeTo))
  }

  def getObligationsFromDes(vrn: String): Future[Boolean] = {
      desConnector.getObligations(vrn).map{ getObl =>
        getObl.obligations.exists { obl =>
          obl.obligationDetails.exists(od =>
            isWithinDateRange(LocalDate.parse(od.inboundCorrespondenceDueDate))
          )
        }
      }
  }

  def getCacheObligationsFromDes(vrn: String): Future[Boolean] = {
    desCacheConnector.getVatCacheObligations(vrn).map{ getObl =>
      getObl.obligations.exists { obl =>
        obl.obligationDetails.exists(od =>
          isWithinDateRange(LocalDate.parse(od.inboundCorrespondenceDueDate))
        )
      }
    }
  }

}
