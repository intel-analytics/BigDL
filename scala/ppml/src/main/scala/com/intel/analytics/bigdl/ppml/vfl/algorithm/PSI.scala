/*
 * Copyright 2021 The BigDL Authors.
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

package com.intel.analytics.bigdl.ppml.vfl

import java.util

import com.intel.analytics.bigdl.ppml.psi.HashingUtils
import com.intel.analytics.bigdl.ppml.vfl.utils.FLClientClosable
import org.apache.log4j.Logger
import scala.util.control.Breaks._

import scala.collection.JavaConverters._

class PSI() extends FLClientClosable {
  val logger = Logger.getLogger(getClass)
  var salt: String = null
  private var hashedKeyPairs: Map[String, String] = null
  def getHashedKeyPairs() = {
    hashedKeyPairs
  }
  private def uploadKeys(keys: Array[String]) = {
    val salt = getSalt
    logger.debug("Client get Salt=" + salt)
    val hashedKeys = HashingUtils.parallelToSHAHexString(keys, salt)
    hashedKeyPairs = hashedKeys.zip(keys).toMap
    // Hash(IDs, salt) into hashed IDs
    logger.debug("HashedIDs Size = " + hashedKeys.size)
    uploadSet(hashedKeys.toList.asJava)

  }

  def getSalt(): String = {
    salt = flClient.psiStub.getSalt()
    salt
  }
  def getSalt(name: String, clientNum: Int, secureCode: String): String = {
    salt = flClient.psiStub.getSalt(name, clientNum, secureCode)
    salt
  }

  def uploadSet(ids: util.List[String]): Unit = {
    val hashedIdArray = HashingUtils.parallelToSHAHexString(ids, salt)
    flClient.psiStub.uploadSet(hashedIdArray)
  }

  def downloadIntersection(max_try: Int = 20, retry: Long = 1000): util.List[String] = {
    var intersection: util.List[String] = null
    breakable {
      for (i <- 0 until max_try) {
        intersection = flClient.psiStub.downloadIntersection
        if (intersection == null) {
          logger.info(s"Got empty intersection, retry in $retry ms")
          Thread.sleep(retry)
        }
        else {
          logger.info("Intersection successful. Intersection's size is " + intersection.size + ".")
          break
        }
      }
    }
    intersection
  }

}
