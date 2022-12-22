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

package com.intel.analytics.bigdl.ppml.kms.frontend

import java.io.File
import java.security.{KeyStore, SecureRandom}
import java.util.concurrent.TimeUnit
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.{ConnectionContext, Http}
import akka.http.scaladsl.server.Directives.{complete, path, _}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.codahale.metrics.{MetricRegistry, Timer}
import com.intel.analytics.bigdl.orca.inference.EncryptSupportive
import com.intel.analytics.bigdl.serving.utils.Conventions
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.concurrent.Await
import com.intel.analytics.bigdl.dllib.utils.Log4Error
import sys.process._
import java.util.Base64
import javax.crypto.spec.SecretKeySpec
import javax.crypto.Cipher

object App extends Supportive {
  override val logger = LoggerFactory.getLogger(getClass)

  val name = "BigDL KMS Frontend"

  implicit val system = ActorSystem("bigdl-kms-frontend-system")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher
  implicit val timeout: Timeout = Timeout(100, TimeUnit.SECONDS)

  def main(args: Array[String]): Unit = {
      val arguments = timing("parse arguments")() {
        argumentsParser.parse(args, KMSFrontendArguments()) match {
          case Some(arguments) => logger.info(s"starting with $arguments"); arguments
          case None => argumentsParser.failure("miss args, please see the usage info"); null
        }
      }
      logger.info("Servable Manager Load Success!")
      val route = timing("initialize http route")() {
        path("") {
          timing("welcome")(overallRequestTimer) {
            complete("welcome to " + name)
          }
        } ~ (get & path("generatePrimaryKey") &
          extract(_.request.entity.contentType) & entity(as[String])) {
          (contentType, content) => {
           timing("generatePrimaryKey")(overallRequestTimer) {
            try{
                val params = content.split("&")
                val primaryKeyName = params(0).split("=")(1)
                val user = params(1).split("=")(1)
                val password = params(2).split("=")(1)
                val base64AES256Key:String = generateAESKey(256)
                login(user, password)
                addKeyToKeywhiz(user, primaryKeyName, base64AES256Key)
                complete(s"primaryKey [$primaryKeyName] is generated successfully!")
            } catch {
              case e: Exception =>
                e.printStackTrace()
                complete(500, e.getMessage + "\n please get a primary key like: " +
                  "primaryKeyName=a_primary_key_name&user=your_username&password=your_password")
            }
           }
          }
        }  ~ (get & path("generateDataKey") &
          extract(_.request.entity.contentType) & entity(as[String])) {
          (contentType, content) => {
           timing("generateDataKey")(overallRequestTimer) {
            try{
                val params = content.split("&")
                val primaryKeyName = params(0).split("=")(1)
                val dataKeyName = params(1).split("=")(1)
                val user = params(2).split("=")(1)
                val password = params(3).split("=")(1)
                login(user, password)
                val primaryKey:String = getKeyFromKeywhiz(user, primaryKeyName)
                val base64AES128Key:String = generateAESKey(128)
                val encryptedDataKey:String = encryptDataKey(primaryKey, base64AES128Key)
                addKeyToKeywhiz(user, dataKeyName, encryptedDataKey)
                complete(s"dataKey [$dataKeyName] is generated successfully!")
            } catch {
              case e: Exception =>
                e.printStackTrace()
                complete(500, e.getMessage + "\n please get a data key like: " +
                  "primaryKeyName=the_primary_key_name&dataKeyName=a_data_key_name_to_use" +
                  "&user=your_username&password=your_password")
            }
           }
          }
        } ~ (get & path("enroll") &
          extract(_.request.entity.contentType) & entity(as[String])) {
          (contentType, content) => {
           timing("enroll")(overallRequestTimer) {
            try {
              val params = content.split("&")
              val user = params(0).split("=")(1)
              val password = params(1).split("=")(1)
              //todo: send the enroll request to a keywhiz exposed k8s restAPI
              complete(s"user [$user] is enrolled successfully!")
            }
            catch {
              case e: Exception =>
                e.printStackTrace()
                complete(500, e.getMessage + "\n please enroll a user like: " +
                  "user=a_username_to_use&password=a_password_to_user")
            }
           }
          }
        } ~ (get & path("getDataKey") &
          extract(_.request.entity.contentType) & entity(as[String])) {
          (contentType, content) => {
           timing("getDataKey")(overallRequestTimer) {
            try {
                val params = content.split("&")
                val primaryKeyName = params(0).split("=")(1)
                val dataKeyName = params(1).split("=")(1)
                val user = params(2).split("=")(1)
                val password = params(3).split("=")(1)
                login(user, password)
                val primaryKey = getKeyFromKeywhiz(user, primaryKeyName)
                val base64DataKeyCiphertext = getKeyFromKeywhiz(user, dataKeyName)
                val base64DataKeyPlaintext = decryptedDataKey(primaryKey, dataKeyCiphertext)
                complete(base64DataKeyPlaintext)
            }
            catch {
              case e: Exception =>
                e.printStackTrace()
                complete(500, e.getMessage + "\n please get the data key like: " +
                  "primaryKeyName=the_primary_key_name&dataKeyName=the_data_key_name" +
                  "&user=your_username&password=your_password")
            }
           }
          }
        }
      }
      val serverContext = defineServerContext(arguments.httpsKeyStoreToken,
          arguments.httpsKeyStorePath)
      Http().bindAndHandle(route, arguments.interface, port = arguments.port,
          connectionContext = serverContext)
      logger.info(s"https started at https://${arguments.interface}:${arguments.port}")
  }

  def login(user:String, password:String): Unit = {
    s"keywhiz.cli --user $user --password $password login" !!
  }

  def generateAESKey(keysize:Int): String = {
    s"key.provider gen-aes --keysize $keysize" !!
  }

  def addKeyToKeywhiz(user:String, keyName:String, keyContent:String): Unit = {
    val command:String = s"keywhiz.cli  --user $user " +
                         s"add secret --name $keyName" +
                         s"--json {\"_key\":\"$keyContent\"}  < salt"
    command !!
  }

  def getKeyFromKeywhiz(user:String, keyName:String): String = {
    s"keywhiz.cli --user $user get --name $keyName" !!
  }

  def dataKeyCryptoCodec(base64PrimaryKeyPlaintext:String,
                     base64DataKey:String,
                     om: Int): String = {
      bytePrimaryKeyPlaintext = base64PrimaryKeyPlaintext.getBytes
      encryptionKeySpec = new SecretKeySpec(bytePrimaryKeyPlaintext, "AES")
      cipher = Cipher.getInstance("AES")
      cipher.init(om, encryptionKeySpec)
      val byteDataKey = Base64.getDecoder().decode(base64DataKey.getBytes)
      val byteDataKeyOperated = cipher.doFinal(byteDataKey)
      val base64DataKeyOperated = Base64.getEncoder.encodeToString(byteDataKeyOperated)
      base64DataKeyOperated
  }

  def encryptDataKey(base64PrimaryKeyPlaintext:String,
                     base64DataKeyPlaintext:String): String = {
      dataKeyCryptoCodec(base64PrimaryKeyPlaintext,
                         base64DataKeyPlaintext,
                         Cipher.ENCRYPT_MODE)
  }

  def decryptDataKey(base64PrimaryKeyPlaintext:String,
                     base64DataKeyCiphertext:String): String = {
      dataKeyCryptoCodec(base64PrimaryKeyPlaintext,
                         base64DataKeyCiphertext,
                         Cipher.DECRYPT_MODE)
  }

  val metrics = new MetricRegistry
  val overallRequestTimer = metrics.timer("bigdl.kms.frontend.request.overall")
  val argumentsParser = new scopt.OptionParser[FrontEndAppArguments]("BigDL KMS Frontend") {
    head("BigDL KMS Frontend")
    opt[String]('i', "interface")
      .action((x, c) => c.copy(interface = x))
      .text("network interface of frontend")
    opt[Int]('p', "port")
      .action((x, c) => c.copy(port = x))
      .text("https port of frontend")
    opt[String]('h', "keywhizHost")
      .action((x, c) => c.copy(keywhizHost = x))
      .text("host of keywhiz")
    opt[Int]('r', "keywhizPort")
      .action((x, c) => c.copy(keywhizPort = x))
      .text("port of keywhiz")
    opt[Int]('l', "parallelism")
      .action((x, c) => c.copy(parallelism = x))
      .text("parallelism of frontend")
    opt[Boolean]('e', "tokenBucketEnabled")
      .action((x, c) => c.copy(tokenBucketEnabled = x))
      .text("Token Bucket Enabled or not")
    opt[Int]('k', "tokensPerSecond")
      .action((x, c) => c.copy(tokensPerSecond = x))
      .text("tokens per second")
    opt[Int]('a', "tokenAcquireTimeout")
      .action((x, c) => c.copy(tokenAcquireTimeout = x))
      .text("token acquire timeout")
    opt[String]('p', "httpsKeyStorePath")
      .action((x, c) => c.copy(httpsKeyStorePath = x))
      .text("https keyStore path")
    opt[String]('w', "httpsKeyStoreToken")
      .action((x, c) => c.copy(httpsKeyStoreToken = x))
      .text("https keyStore token")
    opt[String]('p', "keywhizTrustStorePath")
      .action((x, c) => c.copy(keywhizTrustStorePath = x))
      .text("keywhiz trustStore path")
    opt[String]('w', "keywhizTrustStoreToken")
      .action((x, c) => c.copy(keywhizTrustStoreToken = x))
      .text("rediss trustStore password")
  }

  def defineServerContext(httpsKeyStoreToken: String,
                          httpsKeyStorePath: String): ConnectionContext = {
    val token = httpsKeyStoreToken.toCharArray

    val keyStore = KeyStore.getInstance("PKCS12")
    val keystoreInputStream = new File(httpsKeyStorePath).toURI().toURL().openStream()
    Log4Error.invalidOperationError(keystoreInputStream != null, "Keystore required!")
    keyStore.load(keystoreInputStream, token)

    val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(keyStore, token)

    val trustManagerFactory = TrustManagerFactory.getInstance("SunX509")
    trustManagerFactory.init(keyStore)

    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(keyManagerFactory.getKeyManagers,
      trustManagerFactory.getTrustManagers, new SecureRandom)

    ConnectionContext.https(sslContext)
  }
}

case class KMSFrontendArguments(
                                 interface: String = "0.0.0.0",
                                 port: Int = 9876,
                                 keywhizHost: String = "keywhiz-service",
                                 keywhizPort: Int = 4444,
                                 parallelism: Int = 1000,
                                 tokenBucketEnabled: Boolean = false,
                                 tokensPerSecond: Int = 100,
                                 tokenAcquireTimeout: Int = 100,
                                 httpsKeyStorePath: String = null,
                                 httpsKeyStoreToken: String = null,
                                 keywhizTrustStorePath: String = null,
                                 keywhizTrustStoreToken: String = null
                               )

