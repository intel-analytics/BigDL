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


package com.intel.analytics.bigdl.ppml.attestation.service

import com.intel.analytics.bigdl.dllib.utils.Log4Error
import com.intel.analytics.bigdl.ppml.utils.EHSMParams
import com.intel.analytics.bigdl.ppml.utils.HTTPUtil._
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

import scala.util.Random

import com.intel.analytics.bigdl.ppml.attestation._

/**
 * Attestation Service provided by BigDL
 * @param attestationServerIP ehsm IP
 * @param attestationServerPort ehsm port
 */
class BigDLAttestationService(attestationServerIP: String, attestationServerPort: String)
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

  val ACTION_VERIFY_QUOTE = "verifyQuote"
  // Respone keys
  val RES_RESULT = "result"

  override def register(appID: String): String = "true"

  override def getPolicy(appID: String): String = "true"

  override def setPolicy(policy: JSONObject): String = "true"

  def getQuoteFromServer(challenge: String): String = {
    val userReportData = new Array[Byte](16)
    Random.nextBytes(userReportData)
    new String(userReportData)
  }

  override def attestWithServer(quote: String): (Boolean, String) = {
    if (quote == null) {
      Log4Error.invalidInputError(false,
        "Quote should be specified")
    }
    val action: String = ACTION_VERIFY_QUOTE

    val postResult: JSONObject = timing("BigDLAttestationService request for VerifyQuote") {
      val postString: String = "{\"quote\": \"" + quote + "\"}"
      System.out.println(postString)
      System.out.println(constructUrl(action))
      var response: String = retrieveResponse(constructUrl(action), postString)
      if (response != null && response.startsWith("\ufeff")) {
        response = response.substring(1)
      }
      System.out.println(response)
      new JSONObject(response)
    }
    (true, postResult.getString(RES_RESULT))
  }

  override def attestWithServer(quote: String, policyID: String): (Boolean, String) = {
    attestWithServer(quote)
  }

  private def constructUrl(action: String): String = {
    s"http://$attestationServerIP:$attestationServerPort/$action"
  }
}