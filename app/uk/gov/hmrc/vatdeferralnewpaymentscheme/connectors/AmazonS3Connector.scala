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

import com.amazonaws.services.s3.model.GetObjectRequest
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.amazonaws.util.IOUtils
import com.google.inject.Inject
import uk.gov.hmrc.vatdeferralnewpaymentscheme.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentscheme.model.fileimport.FileDetails

import scala.collection.JavaConverters._

class AmazonS3Connector @Inject()(config: AppConfig) {

  private lazy val s3client: AmazonS3 = {
    val builder = AmazonS3ClientBuilder
      .standard()
      .withPathStyleAccessEnabled(true)

    builder.withRegion(config.region)
    builder.build()
  }

  def listOfFiles(): List[FileDetails] = {
    s3client.listObjects(config.bucket).getObjectSummaries.asScala.map(summary => FileDetails(summary.getKey, summary.getLastModified)).toList
  }

  def objectContentBytes(filename: String, start: Long, end: Long) = {
    val request = new GetObjectRequest(config.bucket, filename).withRange(start, end)
    val objectContent = s3client.getObject(request).getObjectContent()
    val bytes = IOUtils.toByteArray(objectContent)
    objectContent.close()
    bytes
  }

  def getObject(filename: String) = s3client.getObject(config.bucket, filename)

  def exists(filename: String): Boolean = s3client.doesObjectExist(config.bucket, filename)
}

