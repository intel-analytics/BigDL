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

package com.intel.analytics.bigdl.utils

import org.apache.spark.SparkContext
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

class EngineSpec extends FlatSpec with Matchers with BeforeAndAfter {
  var sc : SparkContext = null
  before {
    Engine.reset
  }

  after {
    Engine.reset
    if (sc != null) {
      sc.stop()
    }
    Engine.reset
  }

  "Engine" should "be inited correct under no spark environment" in {
    Engine.setLocalMode
    Engine.init
    Engine.nodeNumber should be(1)
    Engine.coreNumber should be(1)
  }

  "Engine" should "be inited correct under spark local environment" in {
    TestUtils.sparkLocalEnv(core = 4) {
      val conf = Engine.createSparkConf().setAppName("EngineSpecTest").setMaster("local[4]")
      sc = new SparkContext(conf)
      Engine.init
      Engine.nodeNumber should be(1)
      Engine.coreNumber should be(4)
    }
  }

  "Engine" should "be inited with correct value under spark local environment" in {
    TestUtils.sparkLocalEnv(core = 4) {
      Engine.init(1, 4, true)
      Engine.nodeNumber should be(1)
      Engine.coreNumber should be(4)
    }
  }

  "Engine" should "be inited correct under spark standalone environment" in {
    TestUtils.sparkStandaloneEnv(totalCore = 24, core = 4) {
      val conf = Engine.createSparkConf().setAppName("EngineSpecTest").setMaster("local[4]")
      sc = new SparkContext(conf)
      Engine.init
      Engine.nodeNumber should be(6)
      Engine.coreNumber should be(4)
    }
  }

  "Engine" should "be inited correct under spark yarn environment" in {
    TestUtils.sparkYarnEnv(executors = 6, core = 4) {
      val conf = Engine.createSparkConf().setAppName("EngineSpecTest").setMaster("local[4]")
      sc = new SparkContext(conf)
      Engine.init
      Engine.nodeNumber should be(6)
      Engine.coreNumber should be(4)
    }
  }

  "Engine" should "be inited correct under spark mesos environment" in {
    TestUtils.sparkMesosEnv(totalCore = 24, core = 4) {
      val conf = Engine.createSparkConf().setAppName("EngineSpecTest").setMaster("local[4]")
      sc = new SparkContext(conf)
      Engine.init
      Engine.nodeNumber should be(6)
      Engine.coreNumber should be(4)
    }
  }

  "sparkExecutorAndCore" should "parse local[*]" in {
    System.setProperty("spark.master", "local[*]")
    val (nExecutor, executorCore) = Engine.sparkExecutorAndCore(true).get
    nExecutor should be(1)
    System.clearProperty("spark.master")
  }

  "readConf" should "be right" in {
    val conf = Engine.readConf
    val target = Map(
      "spark.executorEnv.DL_ENGINE_TYPE" -> "mklblas",
      "spark.executorEnv.MKL_DISABLE_FAST_MM" -> "1",
      "spark.executorEnv.KMP_BLOCKTIME" -> "0",
      "spark.executorEnv.OMP_WAIT_POLICY" -> "passive",
      "spark.executorEnv.OMP_NUM_THREADS" -> "1",
      "spark.shuffle.reduceLocality.enabled" -> "false",
      "spark.shuffle.blockTransferService" -> "nio",
      "spark.scheduler.minRegisteredResourcesRatio" -> "1.0"
    )
    conf.length should be(target.keys.size)
    conf.foreach(s => {
      s._2 should be(target(s._1))
    })
  }
}
