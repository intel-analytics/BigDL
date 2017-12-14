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

import com.intel.analytics.bigdl.nn.LocallyConnected2D
import com.intel.analytics.bigdl.nn.abstractnn.DataFormat
import com.intel.analytics.bigdl.tensor.Tensor
import com.intel.analytics.bigdl.utils.RandomGenerator


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


class LocallyConnected2DSpec extends KerasBaseSpec {

  "LocallyConnected1D NCHW Float" should "be ok" in {
    ifskipTest()
    val kerasCode =
      """
        |input_tensor = Input(shape=[3,6,2])
        |input = np.array([[[[1,2], [2,3], [3,4],[4,5],[5,6],[6,7]],
        | [[2,3], [3,4],[4,5],[5,6],[6,7], [1,2]],
        | [[1,2], [2,3], [3,4],[4,5],[6,7],[5,6]]]])
        |output_tensor = LocallyConnected2D(4,(2, 1),
        |data_format='channels_first', input_shape=(3,6,2))(input_tensor)
        |model = Model(input=input_tensor, output=output_tensor)
      """.stripMargin
    val locallyConnected1d =
      LocallyConnected2D[Float](3, 2, 6, 4, 1, 2)
    val a = locallyConnected1d.parameters()


    val wc = (data: Array[Tensor[Float]]) => {

      val out = new Array[Tensor[Float]](data.length)
      val d1l: Int = data(0).size(1)
      val d2l: Int = data(0).size(2)
      val d3l: Int = data(0).size(3)

      out(0) = Tensor(d1l, d3l, d2l)

      val page: Int = d2l * d3l
      for (i <- 0 to d1l * d2l * d3l - 1) {
        val d1 = i / page + 1
        val d2 = (i % page) / (d3l) + 1
        val d3 = (i % page) % d3l + 1
        val v = data(0).valueAt(d1, d2, d3)
        out(0).setValue(d1, d3, d2, v)
      }

      if (data.length > 1) {
        out(1) = data(1)
      }
      out
    }

    checkOutputAndGrad(locallyConnected1d, kerasCode, wc)

  }

  "LocallyConnected1D NHWC Float" should "be ok" in {
    ifskipTest()
    val kerasCode =
      """
        |input_tensor = Input(shape=[3,6,2])
        |input = np.array([[[[1,2], [2,3], [3,4],[4,5],[5,6],[6,7]],
        | [[2,3], [3,4],[4,5],[5,6],[6,7], [1,2]],
        | [[1,2], [2,3], [3,4],[4,5],[6,7],[5,6]]]])
        |output_tensor = LocallyConnected2D(3,(2, 1),
        |input_shape=(3,6,2))(input_tensor)
        |model = Model(input=input_tensor, output=output_tensor)
      """.stripMargin
    val locallyConnected1d =
      LocallyConnected2D[Float](2, 6, 3, 3, 1, 2, format = DataFormat.NHWC)
    val a = locallyConnected1d.parameters()


    val wc = (data: Array[Tensor[Float]]) => {

      val out = new Array[Tensor[Float]](data.length)
      val d1l: Int = data(0).size(1)
      val d2l: Int = data(0).size(2)
      val d3l: Int = data(0).size(3)

      out(0) = Tensor(d1l, d3l, d2l)

      val page: Int = d2l * d3l
      for (i <- 0 to d1l * d2l * d3l - 1) {
        val d1 = i / page + 1
        val d2 = (i % page) / (d3l) + 1
        val d3 = (i % page) % d3l + 1
        val v = data(0).valueAt(d1, d2, d3)
        out(0).setValue(d1, d3, d2, v)
      }

      if (data.length > 1) {
        out(1) = data(1)
      }
      out
    }

    checkOutputAndGrad(locallyConnected1d, kerasCode, wc)

  }

  "a" should "a" in {
    import com.intel.analytics.bigdl.numeric.NumericFloat
    val seed = 100
    val nInputPlane = 2
    val inputWidth = 5
    val inputHeight = 6
    val nOutputPlane = 3
    val kernelW = 2
    val kernelH = 2
    val layer =
      LocallyConnected2D(nInputPlane, inputWidth, inputHeight, nOutputPlane, kernelW, kernelH)
    RandomGenerator.RNG.setSeed(seed)

    val input = Tensor(2, 6, 5).rand()
    val gradOutput = Tensor(3, 5, 4).rand()

    val output = layer.updateOutput(input)
    println(output)

    val gradInput = layer.updateGradInput(input, gradOutput)
    println(gradInput)
  }
}
