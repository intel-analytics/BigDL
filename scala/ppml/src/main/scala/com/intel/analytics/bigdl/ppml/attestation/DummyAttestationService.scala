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

import java.math.BigInteger
import org.json.JSONObject

import scala.util.Random

class DummyAttestationService extends AttestationService {

    val logger = LogManager.getLogger(getClass)

    override def register(appID: String): String = "true"

    override def getPolicy(appID: String): String = "true"

    override def setPolicy(policy: JSONObject): String = "true"

    def getQuoteFromServer(): String = {
        "test"
    }

    override def attestWithServer(quote: String): (Boolean, String) = {
        timing("DummyAttestationService retrieveVerifyQuoteResult") {
            if (quote == null) {
                logger.error("Quote should be specified")
                throw new AttestationRuntimeException("Quote is null")
            }
            val nonce: String = "test"
            val response: JSONObject = new JSONObject()
            val number = new BigInteger(quote)
            val zero = new BigInteger("0")
            var verifyQuoteResult = true
            response.put("code", 200)
            response.put("message", "success")
            response.put("nonce", nonce)
            if(number.compareTo(zero) != 1) {
                verifyQuoteResult = false
            }
            response.put("result", verifyQuoteResult)
            val sign = (1 to 16).map(x => Random.nextInt(10)).mkString
            response.put("sign", sign)
            (verifyQuoteResult, response.toString)
        }
    }
}
