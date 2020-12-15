/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.repo


import com.google.inject.{ImplementedBy, Inject, Singleton}
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.bson.{BSONDocument, BSONObjectID, document}
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
    find("vrn" -> vrn).map { res ⇒
      res.headOption.fold[Boolean](false)(_ ⇒ true)
    }.recover {
      case _ ⇒ {
        false
      }
    }
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