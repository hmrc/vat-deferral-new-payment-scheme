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

package uk.gov.hmrc.vatdeferralnewpaymentscheme.connectors

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Framing, Sink, Source}
import akka.util.ByteString
import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.gilt.gfc.aws.s3.akka.S3DownloaderSource._
import uk.gov.hmrc.vatdeferralnewpaymentscheme.config.AppConfig

import javax.inject.Inject
import scala.concurrent.Future

class AmazonS3Connector @Inject()(config: AppConfig)(implicit system: ActorSystem) {

  implicit val ec = system.dispatcher
  implicit val materializer = akka.stream.ActorMaterializer()

  private lazy val s3client: AmazonS3 = {
    val builder = AmazonS3ClientBuilder
      .standard()
      .withPathStyleAccessEnabled(true)

    builder.withRegion(config.region)
    builder.build()
  }

  val splitter = Framing.delimiter(
    ByteString("\n"),
    maximumFrameLength = 1024,
    allowTruncation = true
  )

  def chunkFileDownload[A](filename: String, func1: PartialFunction[String, A], func2: Seq[A] => Future[Unit]): Future[Unit] = {
    val chunkSize = 1024 * 1024 // 1 Mb chunks to request from S3
    val memoryBufferSize = 128 * 1024 // 128 Kb buffer

    val source = Source.s3ChunkedDownload(
      s3client,
      config.bucket,
      filename,
      chunkSize,
      memoryBufferSize
    ).via(splitter)
      .map(_.utf8String.trim)
      .collect(func1)
      .grouped(10000)

    source.runWith(Sink.foldAsync() {
      case (acc, x) => func2(x)
    })
  }

  def getObject(filename: String): S3Object = s3client.getObject(config.bucket, filename)

  def exists(filename: String): Boolean = s3client.doesObjectExist(config.bucket, filename)
}