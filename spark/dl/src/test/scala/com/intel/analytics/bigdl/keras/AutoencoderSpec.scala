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

package com.intel.analytics.bigdl.keras

import com.intel.analytics.bigdl.models.autoencoder.Autoencoder
import com.intel.analytics.bigdl.tensor.Tensor
import com.intel.analytics.bigdl.utils.RandomGenerator

class AutoencoderSpec extends KerasBaseSpec {

  "Autoencoder sequential" should "generate the correct outputShape" in {
    val autoencoder = Autoencoder.keras(classNum = 32)
    autoencoder.getOutputShape().toSingle().toArray should be (Array(-1, 784))
  }

  "Autoencoder graph" should "generate the correct outputShape" in {
    val autoencoder = Autoencoder.kerasGraph(classNum = 32)
    autoencoder.getOutputShape().toSingle().toArray should be (Array(-1, 784))
  }

  "Autoencoder Sequential Keras-Style definition" should
    "be the same as Torch-Style definition" in {
    RandomGenerator.RNG.setSeed(1000)
    val kmodel = Autoencoder.keras(classNum = 32)
    RandomGenerator.RNG.setSeed(1000)
    val tmodel = Autoencoder(classNum = 32)
    val input = Tensor[Float](Array(32, 28, 28)).rand()
    compareModels(kmodel, tmodel, input)
  }

  "Autoencoder Graph Keras-Style definition" should
    "be the same as Torch-Style definition" in {
    val kmodel = Autoencoder.kerasGraph(classNum = 32)
    val tmodel = Autoencoder.graph(classNum = 32)
    val input = Tensor[Float](Array(32, 28, 28)).rand()
    compareModels(kmodel, tmodel, input)
  }

  "Autoencoder sequential definition" should "be the same as graph definition" in {
    RandomGenerator.RNG.setSeed(1000)
    val kseq = Autoencoder.keras(classNum = 32)
    RandomGenerator.RNG.setSeed(1000)
    val kgraph = Autoencoder.kerasGraph(classNum = 32)
    val input = Tensor[Float](Array(2, 28, 28, 1)).rand()
    compareModels(kseq, kgraph, input)
  }

}
