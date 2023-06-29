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

package com.intel.analytics.bigdl.dllib.nn

import com.intel.analytics.bigdl.dllib.tensor.Tensor
import org.scalatest.{FlatSpec, Matchers}
import com.intel.analytics.bigdl.dllib.utils.RandomGenerator._
import com.intel.analytics.bigdl.dllib.utils.Table
import com.intel.analytics.bigdl.dllib.utils.serializer.ModuleSerializationTest

import scala.util.Random
import java.security.SecureRandom

@com.intel.analytics.bigdl.tags.Parallel
class CosineSpec extends FlatSpec with Matchers {

  "A CosineSpec with scaleW" should "work correctly" in {
    val seed = 100
    RNG.setSeed(seed)

    val input = Tensor[Double](1).apply1(_ => new SecureRandom().nextDouble())
    val gradOutput = Tensor[Double](2).apply1(_ => new SecureRandom().nextDouble())

    val layer1 = new Cosine[Double](1, 2)
    val layer2 = new Cosine[Double](1, 2)
    val (weights, grad) = layer1.getParameters()
    val (w, g) = layer2.getParameters()
    w.copy(weights)
    layer2.setScaleW(2)

    val output1 = layer1.forward(input)
    val output2 = layer2.forward(input)
    val gradInput1 = layer1.backward(input, gradOutput)
    val gradInput2 = layer2.backward(input, gradOutput)

    output1 should be (output2)
    gradInput1 should be (gradInput2)

    layer2.gradWeight should be (layer1.gradWeight.mul(2))
  }
}

class CosineSerialTest extends ModuleSerializationTest {
  override def test(): Unit = {
    val cosine = Cosine[Float](5, 5).setName("cosine")
    val input = Tensor[Float](5).apply1(_ => new SecureRandom().nextFloat())
    runSerializationTest(cosine, input)
  }
}
