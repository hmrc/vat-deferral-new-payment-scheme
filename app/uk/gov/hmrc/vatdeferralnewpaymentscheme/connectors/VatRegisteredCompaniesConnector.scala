/*
 * Copyright 2021 HM Revenue & Customs
 *
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
    http.GET[VatRegisteredCompany](url = s"$url/lookup/$vrn")
  }
}