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

package uk.gov.hmrc.vatdeferralnewpaymentscheme.controllers

import akka.stream.Materializer
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.{Configuration, Environment}
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.vatdeferralnewpaymentscheme.config.AppConfig
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.vatdeferralnewpaymentscheme.auth.{Auth, FakeAuth}
import uk.gov.hmrc.vatdeferralnewpaymentscheme.connectors.{DesCacheConnector, DesConnector}
import uk.gov.hmrc.vatdeferralnewpaymentscheme.repo.{PaymentOnAccountRepo, PaymentPlanStore, TimeToPayRepo, VatMainframeRepo}
import uk.gov.hmrc.vatdeferralnewpaymentscheme.service._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class BaseSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with MockitoSugar {

  val fakeGet       = FakeRequest("GET","/")
  val fakePost   = FakeRequest("POST", "/")
  val env           = Environment.simple()
  val configuration = Configuration.load(env, Map("poaUsersEnabledFrom" -> "2021-03-08"))
  implicit val serviceConfig = new ServicesConfig(configuration)
  val appConfig     = new AppConfig(configuration, serviceConfig)
  val cc            = Helpers.stubControllerComponents()

  implicit val materializer: Materializer = app.materializer
  implicit val executionContext = app.injector.instanceOf[ExecutionContext]
  implicit val auditConnector   = mock[AuditConnector]

  val ddService = app.injector.instanceOf[DirectDebitGenService]
  val firstPaymentDateService = app.injector.instanceOf[FirstPaymentDateService]
  val installmentsService = app.injector.instanceOf[InstallmentsService]

  val auth = new FakeAuth(cc)

  lazy val paymentPlanStore = mock[PaymentPlanStore]
  lazy val paymentOnAccountRepo = mock[PaymentOnAccountRepo]
  lazy val timeToPayRepo = mock[TimeToPayRepo]
  lazy val vatMainframeRepo = mock[VatMainframeRepo]
  lazy val desConnector = mock[DesConnector]
  lazy val desObligationsService =  mock[DesObligationsService]
  lazy val financialDataService = mock[FinancialDataService]
  lazy val cacheConnector = mock[DesCacheConnector]

  def getConfig(key: String) = serviceConfig.getConfString(key, "")
  lazy val desEnvironment: String = getConfig("des-arrangement-service.environment")
  lazy val authorizationToken: String = s"Bearer ${getConfig("des-arrangement-service.authorization-token")}"

  implicit val defaultTimeout: FiniteDuration = 5 seconds
  def await[A](future: Future[A])(implicit timeout: Duration): A = Await.result(future, timeout)
}
