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
import scopt.OptionParser

import java.io.{BufferedOutputStream, BufferedInputStream};
import java.io.File;
import java.io.{FileInputStream, FileOutputStream};
import java.util.Base64

import com.intel.analytics.bigdl.ppml.attestation.generator._
import com.intel.analytics.bigdl.ppml.attestation.service._
import com.intel.analytics.bigdl.ppml.attestation.verifier._

/**
 * Simple Attestation Command Line tool for attestation service
 */
object AttestationCLI {
    def main(args: Array[String]): Unit = {
        var quote = Array[Byte]()
        val logger = LogManager.getLogger(getClass)
        case class CmdParams(appID: String = "test",
                             apiKey: String = "test",
                             asType: String = ATTESTATION_CONVENTION.MODE_EHSM_KMS,
                             asURL: String = "127.0.0.1:9000",
                             challenge: String = "",
                             policyID: String = "",
                             quoteType: String = "gramine",
                             httpsEnabled: Boolean = false,
                             userReport: String = "ppml")

        val cmdParser: OptionParser[CmdParams] = new OptionParser[CmdParams](
          "PPML Attestation Quote Generation Cmd tool") {
            opt[String]('i', "appID")
              .text("app id for this app")
              .action((x, c) => c.copy(appID = x))
            opt[String]('k', "apiKey")
              .text("app key for this app")
              .action((x, c) => c.copy(apiKey = x))
            opt[String]('u', "asURL")
              .text("attestation service url, default is 127.0.0.1:9000")
              .action((x, c) => c.copy(asURL = x))
            opt[String]('t', "asType")
              .text("attestation service type, default is EHSMKeyManagementService")
              .action((x, c) => c.copy(asType = x))
            opt[String]('c', "challenge")
              .text("challenge to attestation service, defaultly skip bi-attestation")
              .action((x, c) => c.copy(challenge = x))
            opt[String]('o', "policyID")
              .text("policyID of registered MREnclave and MRSigner, defaultly empty")
              .action((x, c) => c.copy(policyID = x))
            opt[String]('p', "userReport")
              .text("userReportDataPath, default is test")
              .action((x, c) => c.copy(userReport = x))
            opt[Boolean]('s', "httpsEnabled")
              .text("httpsEnabled")
              .action((x, c) => c.copy(httpsEnabled = x))
            opt[String]('O', "quoteType")
              .text("quoteType, default is gramine, occlum can be chose")
              .action((x, c) => c.copy(quoteType = x))
        }
        val params = cmdParser.parse(args, CmdParams()).get

        // Generate quote
        val userReportData = params.userReport
        val quoteGenerator = params.quoteType match {
          case "gramine" =>
            new GramineQuoteGeneratorImpl()
          case "occlum" =>
            new OcclumQuoteGeneratorImpl()
          case "TDX" =>
            new TDXQuoteGeneratorImpl()
          case _ => throw new AttestationRuntimeException("Wrong quote type")
        }
        quote = quoteGenerator.getQuote(userReportData.getBytes)

        // Attestation Client
        val as = params.asType match {
            case ATTESTATION_CONVENTION.MODE_EHSM_KMS =>
                new EHSMAttestationService(params.asURL.split(":")(0),
                    params.asURL.split(":")(1), params.appID, params.apiKey)
            case ATTESTATION_CONVENTION.MODE_BIGDL =>
                new BigDLAttestationService(params.asURL.split(":")(0),
                    params.asURL.split(":")(1), params.httpsEnabled)
            case ATTESTATION_CONVENTION.MODE_DUMMY =>
                new DummyAttestationService()
            case _ => throw new AttestationRuntimeException("Wrong Attestation service type")
        }

        val challengeString = params.challenge
        val debug = System.getenv("ATTESTATION_DEBUG")
        if (challengeString.length() > 0 && params.asType != ATTESTATION_CONVENTION.MODE_DUMMY) {
            val asQuote = params.asType match {
              case ATTESTATION_CONVENTION.MODE_EHSM_KMS =>
                Base64.getDecoder().decode(as.getQuoteFromServer(challengeString))
              case _ => throw new AttestationRuntimeException("Wrong Attestation service type")
            }
            val quoteVerifier = new SGXDCAPQuoteVerifierImpl()
            quoteVerifier.verifyQuote(asQuote)
        }

        val attResult = params.policyID match {
          case "" => as.attestWithServer(Base64.getEncoder.encodeToString(quote))
          case _ => as.attestWithServer(Base64.getEncoder.encodeToString(quote), params.policyID)
        }
        if (attResult._1) {
            System.out.println("Attestation Success!")
            // Bash success
            System.exit(0)
        } else if (debug == "true") {
          System.out.println("ERROR:Attestation Fail! In debug mode, continue.")
        }
        else {
            System.out.println("Attestation Fail! Application killed!")
            // bash fail
            System.exit(1)
        }
    }
}
