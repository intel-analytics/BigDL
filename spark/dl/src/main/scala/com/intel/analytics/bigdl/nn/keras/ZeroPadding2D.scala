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

package com.intel.analytics.bigdl.nn.keras

import com.intel.analytics.bigdl.nn.Padding
import com.intel.analytics.bigdl.nn.{Sequential => TSequential}
import com.intel.analytics.bigdl.nn.abstractnn.{AbstractModule, DataFormat}
import com.intel.analytics.bigdl.tensor.Tensor
import com.intel.analytics.bigdl.tensor.TensorNumericMath.TensorNumeric
import com.intel.analytics.bigdl.utils.Shape

import scala.reflect.ClassTag

class ZeroPadding2D[T: ClassTag](
   val padding: (Int, Int, Int, Int) = (1, 1, 1, 1), // (top_pad, bottom_pad, left_pad, right_pad)
   val format: DataFormat = DataFormat.NCHW,
   var inputShape: Shape = null)(implicit ev: TensorNumeric[T])
  extends KerasLayer[Tensor[T], Tensor[T], T](KerasLayer.addBatch(inputShape)) {

  override def computeOutputShape(inputShape: Shape): Shape = {
    val input = inputShape.toSingle().toArray
    require(input.length == 4,
      s"ZeroPadding2D requires 4D input, but got input dim ${input.length}")
    format match {
      case DataFormat.NCHW =>
        Shape(input(0), input(1),
          input(2) + padding._1 + padding._2, input(3) + padding._3 + padding._4)
      case DataFormat.NHWC =>
        Shape(input(0), input(1) + padding._1 + padding._2,
          input(2) + padding._3 + padding._4, input(3))
    }
  }

  override def doBuild(inputShape: Shape): AbstractModule[Tensor[T], Tensor[T], T] = {
    val input = inputShape.toSingle().toArray
    val nInputDim = input.length -1
    val (dim1, dim2) = format match {
      case DataFormat.NCHW => (2, 3)
      case DataFormat.NHWC => (1, 2)
    }
    val model = TSequential[T]()
    val pad1 = Padding(dim1, -padding._1, nInputDim)
    val pad2 = Padding(dim1, padding._2, nInputDim)
    val pad3 = Padding(dim2, -padding._3, nInputDim)
    val pad4 = Padding(dim2, padding._4, nInputDim)
    model.add(pad1)
    model.add(pad2)
    model.add(pad3)
    model.add(pad4)
    model.asInstanceOf[AbstractModule[Tensor[T], Tensor[T], T]]
  }
}

object ZeroPadding2D {
  def apply[@specialized(Float, Double) T: ClassTag](
    padding: (Int, Int) = (1, 1),
    format: DataFormat = DataFormat.NCHW,
    inputShape: Shape = null)(implicit ev: TensorNumeric[T]): ZeroPadding2D[T] = {
    new ZeroPadding2D[T]((padding._1, padding._1, padding._2, padding._2),
      format, inputShape)
  }
}
