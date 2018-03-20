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

package com.intel.analytics.bigdl.models.lenet

import com.intel.analytics.bigdl._
import com.intel.analytics.bigdl.numeric.NumericFloat
import com.intel.analytics.bigdl.nn._

object LeNet5 {
  def apply(classNum: Int): Module[Float] = {
    val model = Sequential()
    model.add(Reshape(Array(1, 28, 28)))
      .add(SpatialConvolution(1, 6, 5, 5).setName("conv1_5x5"))
      .add(Tanh())
      .add(SpatialMaxPooling(2, 2, 2, 2))
      .add(Tanh())
      .add(SpatialConvolution(6, 12, 5, 5).setName("conv2_5x5"))
      .add(SpatialMaxPooling(2, 2, 2, 2))
      .add(Reshape(Array(12 * 4 * 4)))
      .add(Linear(12 * 4 * 4, 100).setName("fc1"))
      .add(Tanh())
      .add(Linear(100, classNum).setName("fc2"))
      .add(LogSoftMax())
  }

  def graph(classNum: Int): Module[Float] = {
    val input = Reshape(Array(1, 28, 28)).inputs()
    val conv1 = SpatialConvolution(1, 6, 5, 5).setName("conv1_5x5").inputs(input)
    val tanh1 = Tanh().inputs(conv1)
    val pool1 = SpatialMaxPooling(2, 2, 2, 2).inputs(tanh1)
    val tanh2 = Tanh().inputs(pool1)
    val conv2 = SpatialConvolution(6, 12, 5, 5).setName("conv2_5x5").inputs(tanh2)
    val pool2 = SpatialMaxPooling(2, 2, 2, 2).inputs(conv2)
    val reshape = Reshape(Array(12 * 4 * 4)).inputs(pool2)
    val fc1 = Linear(12 * 4 * 4, 100).setName("fc1").inputs(reshape)
    val tanh3 = Tanh().inputs(fc1)
    val fc2 = Linear(100, classNum).setName("fc2").inputs(tanh3)
    val output = LogSoftMax().inputs(fc2)

    Graph(input, output)
  }

  def keras(classNum: Int): nn.keras.Sequential[Float] = {
    import com.intel.analytics.bigdl.nn.keras._
    import com.intel.analytics.bigdl.utils.Shape

    val model = Sequential()
    model.add(Reshape(Array(1, 28, 28), inputShape = Shape(28, 28, 1)))
    model.add(Convolution2D(6, 5, 5, activation = "tanh").setName("conv1_5x5"))
    model.add(MaxPooling2D())
    model.add(Activation("tanh"))
    model.add(Convolution2D(12, 5, 5).setName("conv2_5x5"))
    model.add(MaxPooling2D())
    model.add(Reshape(Array(12 * 4 * 4)))
    model.add(Dense(100, activation = "tanh").setName("fc1"))
    model.add(Dense(classNum, activation = "softmax").setName("fc2"))
  }

  def kerasGraph(classNum: Int): nn.keras.Model[Float] = {
    import com.intel.analytics.bigdl.nn.keras._
    import com.intel.analytics.bigdl.utils.Shape

    val input = Input(inputShape = Shape(28, 28, 1))
    val reshape1 = Reshape(Array(1, 28, 28)).inputs(input)
    val conv1 = Convolution2D(6, 5, 5, activation = "tanh").setName("conv1_5x5").inputs(reshape1)
    val pool1 = MaxPooling2D().inputs(conv1)
    val tanh = Activation("tanh").inputs(pool1)
    val conv2 = Convolution2D(12, 5, 5).setName("conv2_5x5").inputs(tanh)
    val pool2 = MaxPooling2D().inputs(conv2)
    val reshape2 = Reshape(Array(12 * 4 * 4)).inputs(pool2)
    val fc1 = Dense(100, activation = "tanh").setName("fc1").inputs(reshape2)
    val fc2 = Dense(classNum, activation = "softmax").setName("fc2").inputs(fc1)
    Model(input, fc2)
  }
}
