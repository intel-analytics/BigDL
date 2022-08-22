/*
 * Copyright 2016 The BigDL Authors.
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

package com.intel.analytics.bigdl.ppml.attestation

import org.apache.logging.log4j.LogManager

abstract class QuoteVerifier {

  val logger = LogManager.getLogger(getClass)

  def verifyQuote(quote: Array[Byte]): Int = {

    try {
      val verifyQuoteResult = Attestation.sdkVerifyQuote(quote)
      return verifyQuoteResult
    } catch {
      case e: Exception =>
        logger.error(s"Failed to verify quote, ${e}")
        throw new AttestationRuntimeException("Failed " +
          "to verify quote", e)
    }

    throw new AttestationRuntimeException("Unexpected workflow when verifying Quote!")
  }

  def verifyQuote(quote: Array[Byte], policy: Policy): Int = {
    1
  }

}
