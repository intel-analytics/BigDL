/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intel.analytics.bigdl.models.lenet

import java.nio.file.Paths

import com.intel.analytics.bigdl._
import com.intel.analytics.bigdl.dataset.DataSet
import com.intel.analytics.bigdl.dataset.image.{GreyImgToBatch, GreyImgNormalizer, SampleToGreyImg}
import com.intel.analytics.bigdl.nn.{Module, ClassNLLCriterion}
import com.intel.analytics.bigdl.nn.abstractnn.AbstractModule
import com.intel.analytics.bigdl.optim.DataSet
import com.intel.analytics.bigdl.optim._
import com.intel.analytics.bigdl.utils.{Engine, T}
import org.apache.spark.{SparkConf, SparkContext}
import com.intel.analytics.bigdl.tensor.TensorNumericMath.TensorNumeric._


object Train {

  import Utils._

  def main(args: Array[String]): Unit = {
    trainParser.parse(args, new TrainParams()).map(param => {
      val trainData = Paths.get(param.folder, "/train-images.idx3-ubyte")
      val trainLabel = Paths.get(param.folder, "/train-labels.idx1-ubyte")
      val validationData = Paths.get(param.folder, "/t10k-images.idx3-ubyte")
      val validationLabel = Paths.get(param.folder, "/t10k-labels.idx1-ubyte")

      val trainSet = DataSet.array(load(trainData, trainLabel))
        .transform(SampleToGreyImg(28, 28))
      val normalizer = GreyImgNormalizer(trainSet)

      val model = if (param.modelSnapshot.isDefined) {
        Module.load[Float](param.modelSnapshot.get)
      } else {
        LeNet5(classNum = 10)
      }

      val state = if (param.stateSnapshot.isDefined) {
        T.load(param.stateSnapshot.get)
      } else {
        T(
          "learningRate" -> param.learningRate
        )
      }

      Engine.setCoreNumber(param.coreNumber)
      val optimizer = new LocalOptimizer[Float](
        model = model,
        dataset = trainSet.transform(normalizer).transform(GreyImgToBatch(param.batchSize)),
        criterion = new ClassNLLCriterion[Float]()
      )
      if (param.cache.isDefined) {
        optimizer.setCache(param.cache.get, Trigger.everyEpoch)
      }

      val validationSet = DataSet.array(load(validationData, validationLabel))
        .transform(SampleToGreyImg(28, 28))
      val normalizerVal = GreyImgNormalizer(validationSet)
      val valSet = validationSet.transform(normalizerVal)
        .transform(GreyImgToBatch(param.batchSize))

      optimizer
        .setValidation(Trigger.everyEpoch, valSet, Array(new Top1Accuracy[Float]))
        .setState(state)
        .setEndWhen(Trigger.maxEpoch(param.maxEpoch))
        .optimize()
    })
  }
}
