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

import com.intel.analytics.bigdl.nn.SpatialMaxPooling
import com.intel.analytics.bigdl.nn.abstractnn.{AbstractModule, Activity, DataFormat}
import com.intel.analytics.bigdl.tensor.Tensor
import com.intel.analytics.bigdl.tensor.TensorNumericMath.TensorNumeric
import com.intel.analytics.bigdl.utils.Shape

import scala.reflect.ClassTag

class MaxPooling2D[T: ClassTag] (
   val poolSize: Array[Int] = Array(2, 2),
   val strides: Option[Array[Int]] = None,
   val borderMode: String = "valid",
   val format: DataFormat = DataFormat.NCHW,
   var inputShape: Shape = null)(implicit ev: TensorNumeric[T])
  extends KerasLayer[Tensor[T], Tensor[T], T](KerasLayer.addBatch(inputShape)) {

  require(borderMode == "valid" || borderMode == "same", s"Invalid border mode for " +
    s"MaxPooling2D: $borderMode")

  private val stridesValue = if (strides.nonEmpty) strides.get else poolSize

  override def doBuild(inputShape: Shape): AbstractModule[Tensor[T], Tensor[T], T] = {
    val pads = KerasUtils.getPadsFromBorderMode(borderMode)
    val layer = SpatialMaxPooling(
      kW = poolSize(1),
      kH = poolSize(0),
      dW = stridesValue(1),
      dH = stridesValue(0),
      padW = pads._2,
      padH = pads._1,
      format = format
    )
    layer.asInstanceOf[AbstractModule[Tensor[T], Tensor[T], T]]
  }
}

object MaxPooling2D {
  def apply[@specialized(Float, Double) T: ClassTag](
    poolSize: (Int, Int) = (2, 2),
    strides: Option[(Int, Int)] = None,
    borderMode: String = "valid",
    format: DataFormat = DataFormat.NCHW,
    inputShape: Shape = null)
    (implicit ev: TensorNumeric[T]): MaxPooling2D[T] = {
    val stridesValue = if (strides.nonEmpty) Some(Array(strides.get._1, strides.get._2))
                       else None
    new MaxPooling2D[T](Array(poolSize._1, poolSize._2),
      stridesValue, borderMode, format, inputShape)
  }
}
