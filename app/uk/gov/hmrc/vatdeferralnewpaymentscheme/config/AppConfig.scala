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

import javax.inject.{Inject, Singleton}
import play.api.{ConfigLoader, Configuration}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import java.time.LocalDate

import com.typesafe.config.Config

@Singleton
class AppConfig @Inject()(config: Configuration, servicesConfig: ServicesConfig) {

  implicit val dateConfigLoader: ConfigLoader[LocalDate] = (config: Config, path: String) => {
    LocalDate.parse(config.getString(path))
  }

  val authBaseUrl: String = servicesConfig.baseUrl("auth")

  val auditingEnabled: Boolean = config.get[Boolean]("auditing.enabled")
  val useRandomDDIRefSeed: Boolean = config.get[Boolean]("useRandomDDIRefSeed")
  val graphiteHost: String = config.get[String]("microservice.metrics.graphite.host")

  lazy val ddiRefNoGenMinValue: Int = config.get[Int]("ddiRefNoGenMinValue")
  lazy val ddiRefNoGenMaxValue: Int = config.get[Int]("ddiRefNoGenMaxValue")

  lazy val bucket: String = config.get[String]("schedulers.fileImport.bucket")
  lazy val region: String = config.get[String]("schedulers.fileImport.region")

  val ttpEnabled: Boolean = config.getOptional[Boolean]("schedulers.fileImport.timeToPay.enabled").getOrElse(false)
  val ttpFilename: String = config.get[String]("schedulers.fileImport.timeToPay.filename")

  val poaEnabled: Boolean = config.getOptional[Boolean]("schedulers.fileImport.paymentOnAccount.enabled").getOrElse(false)
  val poaFilename: String = config.get[String]("schedulers.fileImport.paymentOnAccount.filename")

  val vmfEnabled: Boolean = config.getOptional[Boolean]("schedulers.fileImport.legacyMainframe.enabled").getOrElse(false)
  val vmfFilename: String = config.get[String]("schedulers.fileImport.legacyMainframe.filename")

  lazy val getObligationsPath: String = config.get[String]("microservice.services.des-service.getObligationsPath")
  lazy val obligationsDateRangeFrom: LocalDate = config.get[LocalDate]("microservice.services.des-service.obligationsDateRangeFrom")
  lazy val obligationsDateRangeTo: LocalDate = config.get[LocalDate]("microservice.services.des-service.obligationsDateRangeTo")

  lazy val getFinancialDataPath: String = config.get[String]("microservice.services.des-service.getFinancialDataPath")

  lazy val getVatCacheObligationsPath: String = config.get[String]("microservice.services.des-cache-service.getVatCacheObligationsPath")

  lazy val includedChargeReferences: Seq[String] = config.getOptional[Seq[String]]("financialDataApiFilter.includedChargeReferences").getOrElse(Seq.empty[String])

  lazy val poaUsersEnabled: Boolean = {
    if (!config.has("poaUsersEnabledFrom")) false
    else {
      val poa = config.getOptional[String]("poaUsersEnabledFrom").getOrElse("")
      if (poa.matches("^\\d{4}\\-(0[1-9]|1[012])\\-(0[1-9]|[12][0-9]|3[01])$")) !LocalDate.now().isBefore(LocalDate.parse(poa)) else false
    }
  }
}