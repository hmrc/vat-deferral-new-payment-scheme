/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentscheme.repo

import com.google.inject.{ImplementedBy, Inject, Singleton}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.fileimport.TimeToPay

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[MongoTimeToPayRepo])
trait TimeToPayRepo {
  def addMany(timeToPay: Array[TimeToPay])
  def deleteAll()
  def exists(vrn: String): Future[Boolean]
}

@Singleton
class MongoTimeToPayRepo @Inject() (mongo: ReactiveMongoComponent)(implicit ec: ExecutionContext)
  extends ReactiveRepository[TimeToPay, BSONObjectID] (
    collectionName = "fileImportTimeToPay",
    mongo          = mongo.mongoConnector.db,
    TimeToPay.format,
    ReactiveMongoFormats.objectIdFormats)
  with TimeToPayRepo {

  def addMany(timeToPay: Array[TimeToPay]): Unit = {
    bulkInsert(timeToPay)
  }

  def deleteAll(): Unit ={
    removeAll()
  }

  def exists(vrn: String): Future[Boolean] = {
    find("vrn" -> vrn).map(_.nonEmpty).recover{ case _ ⇒ false }
  }
}