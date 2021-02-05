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

import java.util.Date

import cats.implicits._
import com.google.inject.{ImplementedBy, Inject, Singleton}
import play.api.Logger
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.fileimport.FileDetails

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[MongoImportFile])
trait ImportFileRepo {
  def lastModifiedDate(filename: String): Future[Option[Date]]
  def updateLastModifiedDate(filename: String, lastModifiedDate: Date)
}

@Singleton
class MongoImportFile @Inject() (mongo: ReactiveMongoComponent)(implicit ec: ExecutionContext)
  extends ReactiveRepository[FileDetails, BSONObjectID] (
    collectionName = "importFile",
    mongo          = mongo.mongoConnector.db,
    FileDetails.format,
    ReactiveMongoFormats.objectIdFormats)
  with ImportFileRepo {

  def lastModifiedDate(filename: String): Future[Option[Date]] = {
    find("name" -> filename).map(_.headOption.map{_.lastModifiedDate})
  }

  def updateLastModifiedDate(filename: String, lastModifiedDate: Date): Unit ={
    insert(FileDetails(filename, lastModifiedDate))
  }

  override def indexes: Seq[Index] = Seq(
    Index(
      name = "fileNameIndex".some,
      key = Seq( "name" -> IndexType.Ascending),
      background = true,
      unique = true
    )
  )
}