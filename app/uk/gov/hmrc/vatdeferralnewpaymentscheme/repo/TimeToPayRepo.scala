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

import akka.NotUsed
import akka.stream.scaladsl.Flow
import akka.stream.{Materializer, OverflowStrategy}
import cats.implicits._
import com.google.inject.{ImplementedBy, Inject, Singleton}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.MultiBulkWriteResult
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.JSONSerializationPack.Writer
import reactivemongo.play.json.collection.JSONCollection
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.fileimport.TimeToPay

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[MongoTimeToPayRepo])
trait TimeToPayRepo {
  def exists(vrn: String): Future[Boolean]
  def renameCollection(): Future[Boolean]
  def insertFlow[A](implicit writer: Writer[A]): Flow[Seq[A], MultiBulkWriteResult, NotUsed]
}

@Singleton
class MongoTimeToPayRepo @Inject() (
  reactiveMongoComponent: ReactiveMongoComponent
)(
  implicit ec: ExecutionContext,
  mat: Materializer
)
  extends ReactiveRepository[TimeToPay, BSONObjectID] (
    collectionName = "fileImportTimeToPay",
    mongo          = reactiveMongoComponent.mongoConnector.db,
    TimeToPay.format,
    ReactiveMongoFormats.objectIdFormats)
    with TimeToPayRepo {

  val tempCollection: JSONCollection =
    mongo()
      .collection[JSONCollection]("fileImportTimeToPayTemp")

  def insertFlow[A](implicit writer: Writer[A]): Flow[Seq[A], MultiBulkWriteResult, NotUsed]  =
    Flow[Seq[A]]
      .buffer(2000, OverflowStrategy.backpressure) // TODO check buffer behaviour, could be "lossy"
      .map(docs => tempCollection.insert(ordered = false).many[A](docs))
      .mapAsyncUnordered(8)(identity) // TODO probably this number should be below the mongo connection limit (10?)

  def renameCollection(): Future[Boolean] = {
    collection.db.connection.database("admin")
      .flatMap { adminDatabase =>
        logger.info(s"File Import: Renaming collection via main database, params: '${collection.db.name}' '${collection.name}' ")
        adminDatabase.renameCollection(collection.db.name, "fileImportTimeToPayTemp", collection.name, true)
      }.map { renameResult: BSONCollection =>
      logger.info(s"File Import: '${collection.name}' collection renamed operation finished, result: ${renameResult}")
      true
    }
  }

  def exists(vrn: String): Future[Boolean] = {
    find("vrn" -> vrn).map(_.nonEmpty).recover{ case _ ⇒ false }
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