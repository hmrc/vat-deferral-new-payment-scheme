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

package uk.gov.hmrc.vatdeferralnewpaymentscheme.auth

import com.google.inject.{ImplementedBy, Inject, Singleton}
import play.api.mvc._
import play.api.{Configuration, Environment}
import uk.gov.hmrc.auth.core.AuthProvider.{GovernmentGateway, PrivilegedApplication}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.{Name, ~}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.config.{AuthRedirects, ServicesConfig}

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[AuthImpl])
trait Auth extends AuthorisedFunctions with AuthRedirects with Results {
  def authorised(
    action: Request[AnyContent] => Future[Result]
   )
   (
     implicit ec: ExecutionContext,
     servicesConfig: ServicesConfig
   ): Action[AnyContent]
}

@Singleton
class AuthImpl @Inject() (
  val authConnector: AuthConnector,
  val env: Environment,
  val config: Configuration,
  defaultActionBuilder: DefaultActionBuilder) extends Auth {

  def authorised(action: Request[AnyContent] => Future[Result])
  (
    implicit ec: ExecutionContext,
    servicesConfig: ServicesConfig
  ): Action[AnyContent] = defaultActionBuilder.async { implicit request =>

    def authHeader: Option[String] =
      for {
        auth <- request.headers.get("Authorization")
      } yield {
        auth
      }

    authHeader match {
      case Some(authorization) =>
        implicit val hc: HeaderCarrier = HeaderCarrierConverter
          .fromHeadersAndSessionAndRequest(request.headers, Some(request.session), Some(request))
          .withExtraHeaders(("Authorization", authorization))

        authorised(AuthProviders(GovernmentGateway, PrivilegedApplication))
          .retrieve(Retrievals.allEnrolments and Retrievals.authorisedEnrolments) { _ => action(request)
          }.recoverWith {
          case _: NoActiveSession =>
            Future.successful(Unauthorized("No active session"))
          case _: InsufficientEnrolments =>
            Future.successful(Unauthorized("Insufficient Enrolments"))
        }
      case _ => Future.successful(BadRequest("Authorization header missing"))
    }
  }
}