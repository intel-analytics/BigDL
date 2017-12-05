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
package com.intel.analytics.bigdl.nn.ops

import com.intel.analytics.bigdl.nn.SpatialConvolution
import com.intel.analytics.bigdl.nn.abstractnn.{AbstractModule, Activity, DataFormat}
import com.intel.analytics.bigdl.tensor.Tensor
import com.intel.analytics.bigdl.tensor.TensorNumericMath.TensorNumeric
import com.intel.analytics.bigdl.utils.Table
import com.intel.analytics.bigdl.utils.tf.loaders.Adapter

import scala.reflect.ClassTag

class DepthwiseConv2D[T: ClassTag](
  strideW: Int, strideH: Int,
  padW: Int, padH: Int,
  dataFormat: DataFormat
)(implicit ev: TensorNumeric[T]) extends Operation[Table, Tensor[T], T] {

  private var conv: SpatialConvolution[T] = _
  private var channelMultiplier = 0

  override def updateOutput(inputs: Table): Tensor[T] = {
    val input: Tensor[T] = inputs[Tensor[T]](1)
    val filter: Tensor[T] = inputs[Tensor[T]](2)
    val channelDim = if (dataFormat == DataFormat.NHWC) 4 else 2
    val kHDim = if (dataFormat == DataFormat.NHWC) 1 else 3
    val kWDim = if (dataFormat == DataFormat.NHWC) 2 else 4

    if (conv == null) {
      channelMultiplier = filter.size(4)
      conv = SpatialConvolution(
        nInputPlane = input.size(channelDim),
        nOutputPlane = channelMultiplier * input.size(channelDim),
        kernelH = filter.size(kHDim),
        kernelW = filter.size(kWDim),
        strideH = strideH,
        strideW = strideW,
        padH = padH,
        padW = padW,
        withBias = false,
        format = dataFormat
      )
      conv.weight.zero()
    }

    // Copy weight
    var in = 0
    while(in < input.size(channelDim)) {
      var out = 0
      while(out < channelMultiplier) {
        conv.weight.select(4, in + 1).select(4, in * channelMultiplier + out + 1)
          .copy(filter.select(3, in + 1).select(3, out + 1))
        out += 1
      }
      in += 1
    }

    output = conv.forward(input)
    output
  }
}

object DepthwiseConv2D {
  def apply[T: ClassTag](
    strideW: Int, strideH: Int,
    padW: Int, padH: Int,
    dataFormat: DataFormat = DataFormat.NHWC
  )(implicit ev: TensorNumeric[T]): DepthwiseConv2D[T] =
    new DepthwiseConv2D(strideW, strideH, padW, padH, dataFormat)
}
