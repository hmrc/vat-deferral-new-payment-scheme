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
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.fileimport.PaymentOnAccount

import scala.concurrent.ExecutionContext

@ImplementedBy(classOf[MongoPaymentOnAccountRepo])
trait PaymentOnAccountRepo {
  def addMany(vrn: Array[String], filename: String)
  def deleteAll()
}

@Singleton
class MongoPaymentOnAccountRepo @Inject() (mongo: ReactiveMongoComponent)(implicit ec: ExecutionContext)
  extends ReactiveRepository[PaymentOnAccount, BSONObjectID] (
    collectionName = "fileImportPaymentOnAccount",
    mongo          = mongo.mongoConnector.db,
    PaymentOnAccount.format,
    ReactiveMongoFormats.objectIdFormats)
  with PaymentOnAccountRepo {

  def addMany(vrn: Array[String], filename: String): Unit = {
    bulkInsert(vrn.map(x => PaymentOnAccount(x, filename)))
  }

  def deleteAll(): Unit ={
    removeAll()
  }
}