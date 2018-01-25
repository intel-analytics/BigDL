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

package com.intel.analytics.bigdl.keras.nn

import com.intel.analytics.bigdl.keras.KerasBaseSpec
import com.intel.analytics.bigdl.nn.keras.GaussianDropout
import com.intel.analytics.bigdl.nn.keras.{Sequential => KSequential}
import com.intel.analytics.bigdl.tensor.Tensor
import com.intel.analytics.bigdl.utils.Shape

class GaussianDropoutSpec extends KerasBaseSpec {

  "GaussianDropout" should "generate the correct outputShape" in {
    val seq = KSequential[Float]()
    val gaussiandropout = GaussianDropout[Float](0.5, inputShape = Shape(3))
    seq.add(gaussiandropout)
    gaussiandropout.getOutputShape().toSingle().toArray should be(Array(-1, 3))
  }

  "GaussianDropout forward and backward" should "work properly" in {
    val seq = KSequential[Float]()
    val input = Tensor[Float](Array(2, 28, 28, 1)).rand()
    val gaussiandropout = GaussianDropout[Float](0.6, inputShape = Shape(3))
    seq.add(gaussiandropout)
    val output = gaussiandropout.forward(input)
    val gradInput = gaussiandropout.backward(input, output)
  }
}
