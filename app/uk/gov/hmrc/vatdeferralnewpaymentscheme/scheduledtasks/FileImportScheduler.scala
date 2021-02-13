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

package uk.gov.hmrc.vatdeferralnewpaymentscheme.scheduledtasks

import akka.actor.ActorSystem
import com.google.inject.{AbstractModule, Provides}
import play.api.inject.DefaultApplicationLifecycle
import play.api.{Configuration, Environment, Logger}
import play.modules.reactivemongo.ReactiveMongoComponentImpl
import uk.gov.hmrc.vatdeferralnewpaymentscheme.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentscheme.repo.{DefaultLockRepository, MongoImportFile, MongoPaymentOnAccountRepo, MongoTimeToPayRepo}
import uk.gov.hmrc.vatdeferralnewpaymentscheme.service.FileImportService

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.duration._

class FileImportScheduler @Inject() (
  actorSystem: ActorSystem,
  @Named("payloadInterval") interval: FiniteDuration,
  @Named("fileImportEnabled") enabled: Boolean,
  configuration: Configuration,
  environment: Environment,
  appConfig: AppConfig)(implicit system: ActorSystem) {

  val logger = Logger(getClass)
  implicit val ec = system.dispatcher
  implicit val materializer = akka.stream.ActorMaterializer()

  if (enabled) {
    logger.info(s"File Import: Initialising file import processing every $interval")
    actorSystem.scheduler.schedule(FiniteDuration(5, TimeUnit.MINUTES), interval) {

      val lifecycle = new DefaultApplicationLifecycle()
      val mongo = new ReactiveMongoComponentImpl(configuration, environment, lifecycle)
      val ttpRepo = new MongoTimeToPayRepo(new ReactiveMongoComponentImpl(configuration, environment, lifecycle))
      val poaRepo = new MongoPaymentOnAccountRepo(mongo)
      val fileImportRepo = new MongoImportFile(mongo)
      val locksRepo = new DefaultLockRepository(mongo, configuration)

      val fileImportService = new FileImportService(ttpRepo, poaRepo, fileImportRepo, locksRepo, appConfig)

      fileImportService.importS3File()

    }
  } else {
    logger.info("File Import: File import is disabled")
  }
}

@Singleton
class FileImportSchedulerModule(environment: Environment, val runModeConfiguration: Configuration) extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[FileImportScheduler]).asEagerSingleton()
  }

  @Provides
  @Named("payloadInterval")
  def interval(): FiniteDuration =
    new FiniteDuration(
      runModeConfiguration
        .getOptional[Int]("microservice.services.schedulers.fileimport.interval.seconds")
        .getOrElse(300)
        .toLong,
      TimeUnit.SECONDS
    )

  @Provides
  @Named("fileImportEnabled")
  def enabled(): Boolean =
    runModeConfiguration
      .getOptional[Boolean]("microservice.services.schedulers.fileimport.enabled")
      .getOrElse(false)
}