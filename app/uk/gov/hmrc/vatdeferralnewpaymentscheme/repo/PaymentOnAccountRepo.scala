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

import cats.implicits._
import com.google.inject.{ImplementedBy, Inject, Singleton}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.collection.JSONCollection
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.fileimport.PaymentOnAccount

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[MongoPaymentOnAccountRepo])
trait PaymentOnAccountRepo extends BaseFileImportRepo  {
  def exists(vrn: String): Future[Boolean]
  def findOne(vrn: String): Future[Option[PaymentOnAccount]]
}

@Singleton
class MongoPaymentOnAccountRepo @Inject() (reactiveMongoComponent: ReactiveMongoComponent)(implicit ec: ExecutionContext)
  extends ReactiveRepository[PaymentOnAccount, BSONObjectID] (
    collectionName = "fileImportPaymentOnAccount",
    mongo          = reactiveMongoComponent.mongoConnector.db,
    PaymentOnAccount.format,
    ReactiveMongoFormats.objectIdFormats)
  with PaymentOnAccountRepo {

  val tempCollection: JSONCollection =
    mongo()
      .collection[JSONCollection]("paymentOnAccountTemp")

  def renameCollection(): Future[Boolean] = {
    collection.db.connection.database("admin")
      .flatMap { adminDatabase =>
        logger.info(s"File Import: Renaming collection via main database, params: '${collection.db.name}' '${collection.name}' ")
        adminDatabase.renameCollection(collection.db.name, "paymentOnAccountTemp", collection.name, true)
      }.map { renameResult: BSONCollection =>
      logger.info(s"File Import: '${collection.name}' collection renamed operation finished, result: ${renameResult}")
      true
    }
  }

  def exists(vrn: String): Future[Boolean] = {
    find("vrn" -> vrn).map(_.nonEmpty).recover{ case _ ⇒ false }
  }

  def findOne(vrn: String): Future[Option[PaymentOnAccount]] = {
    find("vrn" -> vrn).map(_.headOption) // TODO consider replacing headOption
  }

  override def indexes: Seq[Index] = Seq(
    Index(
      name = "vrnIndex".some,
      key = Seq( "vrn" -> IndexType.Ascending),
      background = true,
      unique = true
    )
  )

}