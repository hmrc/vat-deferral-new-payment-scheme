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

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import com.google.inject.{AbstractModule, Provides}
import javax.inject.{Inject, Named, Singleton}
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.vatdeferralnewpaymentscheme.service.FileImportService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class FileImportScheduler @Inject() (
  actorSystem: ActorSystem,
  @Named("payloadInterval") interval: FiniteDuration,
  @Named("fileImportEnabled") enabled: Boolean,
  fileImportService: FileImportService) {

  val logger = Logger(getClass)

  if (enabled) {
    logger.info(s"File Import: Initialising file import processing every $interval")
    actorSystem.scheduler.schedule(FiniteDuration(5, TimeUnit.MINUTES), interval) {
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
        .getOrElse(900)
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