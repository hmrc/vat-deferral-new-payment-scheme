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

package uk.gov.hmrc.vatdeferralnewpaymentscheme.repo


import com.google.inject.{ImplementedBy, Inject, Singleton}
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import uk.gov.hmrc.vatdeferralnewpaymentscheme.repo.MongoPaymentPlanStore.PaymentPlan

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[MongoPaymentPlanStore])
trait PaymentPlanStore {
  def exists(vrn: String)(implicit hc: HeaderCarrier): Future[Boolean]
  def add(vrn: String)
}

@Singleton
class MongoPaymentPlanStore @Inject() (mongo: ReactiveMongoComponent)(implicit ec: ExecutionContext)
  extends ReactiveRepository[PaymentPlan, BSONObjectID] (
    collectionName = "paymentPlan",
    mongo          = mongo.mongoConnector.db,
    PaymentPlan.vrnFormat,
    ReactiveMongoFormats.objectIdFormats)
  with PaymentPlanStore {

  def exists(vrn: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    find("vrn" -> vrn).map(_.nonEmpty).recover{ case _ ⇒ false }
  }

  def add(vrn: String): Unit ={
    insert(PaymentPlan(vrn))
  }
}

object MongoPaymentPlanStore {

  private[repo] case class PaymentPlan(vrn: String)

  private[repo] object PaymentPlan {
    implicit val vrnFormat: Format[PaymentPlan] = Json.format[PaymentPlan]
  }
}