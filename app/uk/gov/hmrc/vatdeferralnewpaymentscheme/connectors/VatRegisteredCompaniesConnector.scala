/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.connectors

import javax.inject.Inject
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.vatregisteredcompanies.LookupResponse

import scala.concurrent.ExecutionContext.Implicits.global


class VatRegisteredCompaniesConnector @Inject()(
                                                 http: HttpClient,
                                                 servicesConfig: ServicesConfig
                                               ) {

  lazy val url: String = s"${servicesConfig.baseUrl("vat-registered-companies")}/vat-registered-companies"

  def lookup(vrn: String)(implicit hc: HeaderCarrier) = {
    http.GET[LookupResponse](url = s"$url/lookup/$vrn").map(Some(_))
  }
}