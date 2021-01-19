/*
 * Copyright 2021 HM Revenue & Customs
 *
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

  lazy val folderName: String = config.get[String]("s3.folderName")
  lazy val awsAccessId: String = config.get[String]("s3.accessId")
  lazy val awsSecret: String = config.get[String]("s3.secret")
  lazy val bucket: String = config.get[String]("s3.bucket")
  lazy val region: String = config.get[String]("s3.region")

  lazy val getObligationsPath = config.get[String]("microservice.services.des-service.getObligationsPath")
  lazy val getFinancialDataPath = config.get[String]("microservice.services.des-service.getFinancialDataPath")
}
