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
import reactivemongo.api.commands.MultiBulkWriteResult
import reactivemongo.play.json.JSONSerializationPack.Writer
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.ExecutionContext

trait BulkInsertFlow {

  val tempCollection: JSONCollection

  def insertFlow[A](
    implicit ec: ExecutionContext,
    writer: Writer[A]
  ): Flow[Seq[A], MultiBulkWriteResult, NotUsed] =
    Flow[Seq[A]]
      .mapAsyncUnordered(8)(docs => tempCollection.insert(ordered = false).many[A](docs))

}