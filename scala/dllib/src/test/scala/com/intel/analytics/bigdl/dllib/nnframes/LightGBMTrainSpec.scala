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

package com.intel.analytics.bigdl.dllib.nnframes

import com.intel.analytics.bigdl.dllib.keras.ZooSpecHelper
import com.intel.analytics.bigdl.dllib.utils.{Engine, TestUtils}
import org.apache.spark.SparkContext
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.sql.{SQLContext, SparkSession}
import com.microsoft.azure.synapse.ml.lightgbm.{LightGBMClassifier => MLightGBMClassifier}
import org.apache.spark.SparkConf


class LightGBMTrainSpec extends ZooSpecHelper {
  var sc : SparkContext = _
  var sqlContext : SQLContext = _

  override def doBefore(): Unit = {
    val conf = new SparkConf().setAppName("Test lightGBM").setMaster("local[1]")
    sc = SparkContext.getOrCreate(conf)
    sqlContext = new SQLContext(sc)
  }

  override def doAfter(): Unit = {
    if (sc != null) {
      sc.stop()
    }
  }

  "LightGBMClassifer train" should "work" in {
    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._
    Engine.init
    val df = Seq(
      (1.0, 2.0, 3.0, 4.0, 1),
      (1.0, 3.0, 8.0, 2.0, 0)
    ).toDF("f1", "f2", "f3", "f4", "label")
    val vectorAssembler = new VectorAssembler()
      .setInputCols(Array("f1", "f2", "f3", "f4"))
      .setOutputCol("features")
    val assembledDf = vectorAssembler.transform(df).select("features", "label").cache()
    if (spark.version.substring(0, 3).toDouble >= 3.1) {
      val lightGBMclassifier = new LightGBMClassifier()
      val classifier = new MLightGBMClassifier()
      val model1 = lightGBMclassifier.fit(assembledDf)
      val model = classifier.fit(assembledDf)
      val res1 = model1.transform(assembledDf)
      TestUtils.conditionFailTest(res1.count() == 2)
    }
  }

  "LightGBMClassifer save" should "work" in {
    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._
    Engine.init
    val df = Seq(
      (1.0, 2.0, 3.0, 4.0, 1),
      (1.0, 3.0, 8.0, 2.0, 0)
    ).toDF("f1", "f2", "f3", "f4", "label")
    val vectorAssembler = new VectorAssembler()
      .setInputCols(Array("f1", "f2", "f3", "f4"))
      .setOutputCol("features")
    val assembledDf = vectorAssembler.transform(df).select("features", "label").cache()

    if (spark.version.substring(0, 3).toDouble >= 3.1) {
      val lightGBMclassifier = new LightGBMClassifier()
      val model = lightGBMclassifier.fit(assembledDf)
      model.saveNativeModel("/tmp/lightgbm/classifier1")
      val model2 = LightGBMClassifierModel.loadNativeModel("/tmp/lightgbm/classifier1")
      val res2 = model2.transform(assembledDf)
      TestUtils.conditionFailTest(res2.count() == 2)
    }
  }

  "LightGBMRegressor train" should "work" in {
    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._
    Engine.init
    val df = Seq(
      (1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 1.0f, 2.0f, 4.0f, 8.0f, 3.0f, 116.3668f),
      (1.0f, 3.0f, 8.0f, 6.0f, 5.0f, 9.0f, 5.0f, 6.0f, 7.0f, 4.0f, 116.367f),
      (2.0f, 1.0f, 5.0f, 7.0f, 6.0f, 7.0f, 4.0f, 1.0f, 2.0f, 3.0f, 116.367f),
      (2.0f, 1.0f, 4.0f, 3.0f, 6.0f, 1.0f, 3.0f, 2.0f, 1.0f, 3.0f, 116.3668f)
    ).toDF("f1", "f2", "f3", "f4", "f5", "f6", "f7", "f8", "f9", "f10", "label")
    val vectorAssembler = new VectorAssembler()
      .setInputCols(Array("f1", "f2", "f3", "f4", "f5", "f6", "f7", "f8", "f9", "f10"))
      .setOutputCol("features")
    val assembledDf = vectorAssembler.transform(df).select("features", "label").cache()

    if (spark.version.substring(0, 3).toDouble >= 3.1) {
      val lightGBMRegressor = new LightGBMRegressor()
      val regressorModel0 = lightGBMRegressor.fit(assembledDf)
      val y0 = regressorModel0.transform(assembledDf)
      regressorModel0.saveNativeModel("/tmp/test")
      val model = LightGBMRegressorModel.loadNativeModel("/tmp/test")
      val y0_0 = model.transform(assembledDf)
      TestUtils.conditionFailTest(y0.count() == 4)
      TestUtils.conditionFailTest(y0_0.count() == 4)
    }
  }
}

