/*
 * Copyright 2021 HM Revenue & Customs
 *
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

  if(enabled) {
    logger.info(s"Initialising file import processing every $interval")
    actorSystem.scheduler.schedule(FiniteDuration(10, TimeUnit.SECONDS), interval) {
      fileImportService.importS3File()
    }
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
      .getOrElse(true)
}