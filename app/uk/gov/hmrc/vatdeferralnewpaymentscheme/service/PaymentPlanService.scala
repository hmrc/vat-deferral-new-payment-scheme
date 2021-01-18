/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.service

import com.google.inject.Inject
import uk.gov.hmrc.vatdeferralnewpaymentscheme.repo.{PaymentOnAccountRepo, PaymentPlanStore, TimeToPayRepo}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PaymentPlanService @Inject()(paymentPlanStore: PaymentPlanStore, paymentOnAccountRepo: PaymentOnAccountRepo, timeToPayRepo: TimeToPayRepo) {

  def exists(vrn: String): Future[Boolean] = {
    paymentPlanStore.exists(vrn)
      .flatMap(ppExists => if (ppExists) Future.successful(ppExists) else paymentOnAccountRepo.exists(vrn))
      .flatMap(poaExists => if (poaExists) Future.successful(poaExists) else timeToPayRepo.exists(vrn))
  }
}