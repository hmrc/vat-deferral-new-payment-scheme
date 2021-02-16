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

import play.api.Logger
import play.api.libs.json.Json
import shapeless.syntax.typeable._

case class VatMainframe(
  vrn: String,
  deferredCharges: BigDecimal,
  payments: BigDecimal
) {
  def outstandingExists: Boolean =
    deferredCharges - payments > 0
}

object VatMainframe  extends FileImportParser[VatMainframe] {
  implicit val format = Json.format[VatMainframe]

  val logger = Logger(getClass)

  def parse(line: String): VatMainframe = {
    // Characters 1 – 9 = VRN
    // Characters 10 – 21 = Deferred Charges (Chars 10 – 18 the pounds, Char 19 the decimal point, Chars 20-21 the pence)
    // Characters 22 – 33 = Payments (Chars 22-30 the pounds, Char 31 the decimal point, chars 32-33 the pence)
    try {
      VatMainframe(
        line.substring(0, 9),
        BigDecimal(line.substring(9, 21)).setScale(2),
        BigDecimal(line.substring(21, 33)).setScale(2))
    }
    catch {
      case e:Throwable => {
        logger.warn("File Import: VatMainframe String is invalid")
        VatMainframe("error", BigDecimal(0), BigDecimal(0))
      }
    }
  }

  def filter[A](item: A): Boolean = {
    item.cast[VatMainframe]
      .fold(
        throw new RuntimeException("FileImport: unable to cast item as VatMainframe")
      )(x => x.vrn != "error")
  }
}