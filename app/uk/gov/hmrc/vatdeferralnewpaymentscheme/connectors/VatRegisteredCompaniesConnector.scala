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

package uk.gov.hmrc.vatdeferralnewpaymentscheme.connectors

import javax.inject.Inject
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.vatregisteredcompanies.VatRegisteredCompany

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class VatRegisteredCompaniesConnector @Inject()(
                                                 http: HttpClient,
                                                 servicesConfig: ServicesConfig
                                               ) {

  lazy val url: String = s"${servicesConfig.baseUrl("vat-registered-companies")}/vat-registered-companies"

  def lookup(vrn: String)(implicit hc: HeaderCarrier): Future[VatRegisteredCompany] = {
    http.GET[VatRegisteredCompany](url = s"$url/lookup/$vrn").map{
      res => println("Noooooo");res
    }.recover{
      case e: uk.gov.hmrc.http.JsValidationException => throw new MissingCheckResponseException
    }
  }
  class MissingCheckResponseException extends RuntimeException("no CheckResponse from CheckEoriNumberConnector")
}