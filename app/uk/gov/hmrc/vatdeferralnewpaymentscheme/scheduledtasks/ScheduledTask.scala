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
import com.google.inject.AbstractModule
import javax.inject.{Inject, Singleton}
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.vatdeferralnewpaymentscheme.connectors.AmazonS3Connector
import uk.gov.hmrc.vatdeferralnewpaymentscheme.repo.{FileImportRepo, PaymentOnAccountRepo, TimeToPayRepo}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class FileImportScheduler @Inject() (actorSystem: ActorSystem, amazonS3Connector: AmazonS3Connector, timeToPayRepo: TimeToPayRepo, paymentOnAccountRepo: PaymentOnAccountRepo, fileImportRepo: FileImportRepo) {

  val logger = Logger(getClass)

  actorSystem.scheduler.schedule(FiniteDuration(2, TimeUnit.SECONDS), 10.seconds) {

    amazonS3Connector.listOfFiles().foreach(fileDetails => {

      logger.info(s"Filename: ${fileDetails}")

      fileImportRepo.lastModifiedDate(fileDetails.name).map{

          case Some(a) if !fileDetails.lastModifiedDate.after(a) => Logger.logger.debug(s"Do nothing: file:$a s3:${fileDetails.lastModifiedDate} is after: ${fileDetails.lastModifiedDate.after(a)} ")
          case _ => {

            val contentBytes = amazonS3Connector.objectContentBytes(fileDetails.name)
            val fileContents = contentBytes.map(_.toChar).mkString

            val paymentPlans = ParseFile(fileContents)

            Logger.logger.debug(s"count: ${paymentPlans.length}")

            paymentOnAccountRepo.deleteAll()
            paymentOnAccountRepo.addMany(paymentPlans.collect { case v: POA => v.vrn }, fileDetails.name)

            timeToPayRepo.deleteAll()
            timeToPayRepo.addMany(paymentPlans.collect { case v: TTP => v.vrn }, fileDetails.name)

            fileImportRepo.updateLastModifiedDate(fileDetails.name, fileDetails.lastModifiedDate)
          }
      }
    })
  }

  private def ParseFile(fileContents: String): Array[Any] = {
    fileContents.split('\n').map {
      line => {
        Logger.logger.debug(s"Line $line")
        val lineSplit = line.split(',')
        val category: String = lineSplit(0)
        val vrn: String = lineSplit(1)

        if (category == "TTP") {
          TTP(vrn)
        }
        else if (category == "POA") {
          POA(vrn)
        }
      }
    }
  }
}

@Singleton
class FileImportSchedulerModule(environment: Environment, val runModeConfiguration: Configuration) extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[FileImportScheduler]).asEagerSingleton()
  }
}

case class TTP(vrn: String)
case class POA(vrn: String)