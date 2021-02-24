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

import uk.gov.hmrc.smartstub._
import uk.gov.hmrc.vatdeferralnewpaymentscheme.config.AppConfig

import java.time.{LocalDateTime, ZoneOffset}
import javax.inject.Inject

class DirectDebitGenService @Inject()(
  appConfig: AppConfig
) {

  def createSeededDDIRef(vrn: String, withDateTime: Boolean = false): Option[Int] = {
    val min = appConfig.ddiRefNoGenMinValue
    val max = appConfig.ddiRefNoGenMaxValue

    lazy val seed = if (withDateTime) vrn.hashCode.toLong + LocalDateTime.now.toEpochSecond(ZoneOffset.UTC) else vrn.hashCode.toLong

    val gen = DDIRefGen.genDDIRefNumber(min, max)
    //For QA we need to retrieve different DDIRef's from one UTR
    //This should never be enabled in production
    if (appConfig.useRandomDDIRefSeed)
      gen.sample
    else
      gen.seeded(seed)
  }
}