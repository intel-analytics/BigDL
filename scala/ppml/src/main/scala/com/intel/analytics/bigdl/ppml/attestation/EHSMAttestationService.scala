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

import com.intel.analytics.bigdl.dllib.utils.Log4Error
import com.intel.analytics.bigdl.ppml.utils.EHSMParams
import com.intel.analytics.bigdl.ppml.utils.HTTPSUtil.postRequest
import org.apache.logging.log4j.LogManager
import org.json.JSONObject
import javax.net.ssl.SSLContext
import org.apache.http.conn.ssl.AllowAllHostnameVerifier
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.ssl.SSLContextBuilder
import org.apache.http.ssl.SSLContexts
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate
import javax.net.ssl.TrustManager
import org.apache.http.util.EntityUtils
import java.security.SecureRandom

/**
 * Attestation Service provided by ehsm
 * @param kmsServerIP ehsm IP
 * @param kmsServerPort ehsm port
 * @param ehsmAPPID application ID
 * @param ehsmAPPKEY application Key
 */
class EHSMAttestationService(kmsServerIP: String, kmsServerPort: String,
                             ehsmAPPID: String, ehsmAPPKEY: String)
  extends AttestationService {

  val logger = LogManager.getLogger(getClass)

  val sslConSocFactory = {
    val sslContext: SSLContext = SSLContext.getInstance("SSL")
    val trustManager: TrustManager = new X509TrustManager() {
            override def checkClientTrusted(chain: Array[X509Certificate], authType: String): Unit = {}
            override def checkServerTrusted(chain: Array[X509Certificate], authType: String): Unit = {}
            override def getAcceptedIssuers(): Array[X509Certificate] = Array.empty
    }
    sslContext.init(null, Array(trustManager), new SecureRandom())
    new SSLConnectionSocketFactory(sslContext, new AllowAllHostnameVerifier())
  }

  // Quote
  val PAYLOAD_QUOTE = "quote"
  val PAYLOAD_NONCE = "nonce"

  val ACTION_VERIFY_QUOTE = "VerifyQuote"
  // Respone keys
  val RES_RESULT = "result"
  val RES_SIGN = "sign"

  override def register(appID: String): String = "true"

  override def getPolicy(appID: String): String = "true"

  override def setPolicy(policy: JSONObject): String = "true"

  def getQuoteFromServer(): String = {
    // TODO Get qutoe from ehsm
    "test"
  }

  override def attestWithServer(quote: String): (Boolean, String) = {
    // TODO nonce
    val nonce: String = "test"
    if (quote == null) {
      Log4Error.invalidInputError(false,
        "Quote should be specified")
    }
    val action: String = ACTION_VERIFY_QUOTE
    val currentTime = System.currentTimeMillis() // ms
    val timestamp = s"$currentTime"
    val ehsmParams = new EHSMParams(ehsmAPPID, ehsmAPPKEY, timestamp)
    ehsmParams.addPayloadElement(PAYLOAD_QUOTE, quote)
    ehsmParams.addPayloadElement(PAYLOAD_NONCE, nonce)

    val postResult: JSONObject = timing("EHSMAttestationService request for VerifyQuote") {
      val postString: String = ehsmParams.getPostJSONString()
      postRequest(constructUrl(action), sslConSocFactory, postString)
    }
    // Check sign with nonce
    val sign = postResult.getString(RES_SIGN)
    val verifyQuoteResult = postResult.getBoolean(RES_RESULT)
    (verifyQuoteResult, postResult.toString)
  }

  private def constructUrl(action: String): String = {
    s"https://$kmsServerIP:$kmsServerPort/ehsm?Action=$action"
  }
}
