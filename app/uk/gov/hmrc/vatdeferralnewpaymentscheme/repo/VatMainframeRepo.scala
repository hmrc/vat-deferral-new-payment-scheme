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
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.fileimport.VatMainframe

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[MongoVatMainframeRepo])
trait VatMainframeRepo extends BaseFileImportRepo {
  def findOne(vrn: String): Future[Option[VatMainframe]]
}

@Singleton
class MongoVatMainframeRepo @Inject() (
  reactiveMongoComponent: ReactiveMongoComponent
)(
  implicit ec: ExecutionContext
)
  extends ReactiveRepository[VatMainframe, BSONObjectID] (
    collectionName = "fileImportVatMainframe",
    mongo          = reactiveMongoComponent.mongoConnector.db,
    VatMainframe.format,
    ReactiveMongoFormats.objectIdFormats)
    with VatMainframeRepo {

  val tempCollection: JSONCollection =
    mongo()
      .collection[JSONCollection]("fileImportVatMainframeTemp")

  def renameCollection(): Future[Boolean] = {
    collection.db.connection.database("admin")
      .flatMap { adminDatabase =>
        logger.info(s"File Import: Renaming collection via main database, params: '${collection.db.name}' '${collection.name}' ")
        adminDatabase.renameCollection(collection.db.name, "fileImportVatMainframeTemp", collection.name, true)
      }.map { renameResult: BSONCollection =>
      logger.info(s"File Import: '${collection.name}' collection renamed operation finished, result: ${renameResult}")
      true
    }
  }

  def findOne(vrn: String): Future[Option[VatMainframe]] = {
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