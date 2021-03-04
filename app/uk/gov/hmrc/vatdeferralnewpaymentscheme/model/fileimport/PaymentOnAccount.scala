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

package uk.gov.hmrc.vatdeferralnewpaymentscheme.model.fileimport

import cats.implicits._
import play.api.Logger
import play.api.libs.json.Json
import shapeless.syntax.typeable._

case class PaymentOnAccount(vrn: String, outstandingAmount: Option[BigDecimal]){
  def outstandingExists: Boolean =
    outstandingAmount.getOrElse(BigDecimal(0)) > 0
}

object PaymentOnAccount extends FileImportParser[PaymentOnAccount]  {
  implicit val format = Json.format[PaymentOnAccount]

  val logger = Logger(getClass)

  def parse(line: String): PaymentOnAccount = {
    //  TODO: Discuss Validation
    try {
      val lineFields: Array[String] = line.split(",")
      PaymentOnAccount(lineFields(0).trim, Some(BigDecimal(lineFields(1).trim)))
    } catch {
      case _:Throwable => {
        logger.warn("File Import: PaymentOnAccount String is invalid")
        PaymentOnAccount("error", none[BigDecimal])
      }
    }
  }

  def filter[A](item: A): Boolean = {
    item.cast[PaymentOnAccount]
      .fold(
        throw new RuntimeException("FileImport: unable to cast item as PaymentOnAccount")
      )(x => x.vrn != "error")
  }
}