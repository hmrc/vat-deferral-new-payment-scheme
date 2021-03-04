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

package uk.gov.hmrc.vatdeferralnewpaymentscheme.config

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import java.time.LocalDate

class AppConfigSpec extends AnyWordSpec with Matchers {

  def appConfig(date: String) = {
    val env           = Environment.simple()
    val configuration = Configuration.load(env, Map("poaUsersEnabledFrom" -> date))
    val serviceConfig = new ServicesConfig(configuration)
    new AppConfig(configuration, serviceConfig)
  }

  "Payment on account users" should {
    "be enabled" when {
        "poaUsersEnabledFrom config setting is equal to today" in {
        appConfig(LocalDate.now.toString).poaUsersEnabled shouldBe true
      }

      "poaUsersEnabledFrom config setting is after to today" in {
        appConfig(LocalDate.now.minusDays(1).toString).poaUsersEnabled shouldBe true
      }
    }

    "not be enabled" when {
      "poaUsersEnabledFrom config setting is before today" in {
        appConfig(LocalDate.now.plusDays(1).toString).poaUsersEnabled shouldBe false
      }

      "poaUsersEnabledFrom config setting is empty" in {
        appConfig("").poaUsersEnabled shouldBe false
      }

      "poaUsersEnabledFrom config setting is not set" in {
        appConfig(null).poaUsersEnabled shouldBe false
      }

      "poaUsersEnabledFrom config setting is not valid" in {
        appConfig("asdfsadf").poaUsersEnabled shouldBe false
      }
    }
  }
}
