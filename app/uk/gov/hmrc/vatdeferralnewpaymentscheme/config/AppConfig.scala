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
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class AppConfig @Inject()(config: Configuration, servicesConfig: ServicesConfig) {

  val authBaseUrl: String = servicesConfig.baseUrl("auth")

  val auditingEnabled: Boolean = config.get[Boolean]("auditing.enabled")
  val graphiteHost: String     = config.get[String]("microservice.metrics.graphite.host")

  lazy val ddiRefNoGenMinValue: Int = config.get[Int]("ddiRefNoGenMinValue")
  lazy val ddiRefNoGenMaxValue: Int = config.get[Int]("ddiRefNoGenMaxValue")

  lazy val folderName: String = config.get[String]("s3.folderName")
  lazy val bucket: String = config.get[String]("s3.bucket")
  lazy val region: String = config.get[String]("s3.region")

  val ttpFilename: String = config.get[String]("microservice.services.schedulers.fileimport.ttpFilename")

  lazy val getObligationsPath: String = config.get[String]("microservice.services.des-service.getObligationsPath")
  lazy val getFinancialDataPath: String = config.get[String]("microservice.services.des-service.getFinancialDataPath")

  lazy val getVatCacheObligationsPath: String = config.get[String]("microservice.services.des-cache-service.getVatCacheObligationsPath")

  lazy val includedChargeReferences: Seq[String] = config.getOptional[Seq[String]]("financialDataApiFilter.includedChargeReferences").getOrElse(Seq.empty[String])
}
