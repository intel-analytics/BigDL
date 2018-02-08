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

import com.intel.analytics.bigdl.nn.abstractnn.{AbstractModule, DataFormat}
import com.intel.analytics.bigdl.nn.{InitializationMethod, SpatialDilatedConvolution, Xavier, Zeros}
import com.intel.analytics.bigdl.optim.Regularizer
import com.intel.analytics.bigdl.tensor.Tensor
import com.intel.analytics.bigdl.tensor.TensorNumericMath.TensorNumeric
import com.intel.analytics.bigdl.utils.Shape

import scala.reflect.ClassTag

class AtrousConvolution2D[T: ClassTag](
   val nbFilter: Int,
   val nbRow: Int,
   val nbCol: Int,
   val init: InitializationMethod = Xavier,
   val activation: AbstractModule[Tensor[T], Tensor[T], T] = null,
   val subsample: Array[Int] = Array(1, 1),
   val atrousRate: Array[Int] = Array(1, 1),
   val dimOrdering: DataFormat = DataFormat.NCHW,
   var wRegularizer: Regularizer[T] = null,
   var bRegularizer: Regularizer[T] = null,
   val inputShape: Shape = null)(implicit ev: TensorNumeric[T])
  extends KerasLayer[Tensor[T], Tensor[T], T](KerasLayer.addBatch(inputShape)) {

  require(dimOrdering == DataFormat.NCHW, s"AtrousConvolution2D currently only supports " +
    s"format NCHW, but got format $dimOrdering.")
  require(subsample.length == 2,
    s"For AtrousConvolution2D, subsample should be of length 2 but got length ${subsample.length}")
  require(atrousRate.length == 2, s"For AtrousConvolution2D, " +
    s"atrousRate should be of length 2 but got length ${atrousRate.length}")

  override def doBuild(inputShape: Shape): AbstractModule[Tensor[T], Tensor[T], T] = {
    val input = inputShape.toSingle().toArray
    val layer = SpatialDilatedConvolution(
      nInputPlane = input(1),
      nOutputPlane = nbFilter,
      kW = nbCol,
      kH = nbRow,
      dW = subsample(1),
      dH = subsample(0),
      dilationW = atrousRate(1),
      dilationH = atrousRate(0),
      wRegularizer = wRegularizer,
      bRegularizer = bRegularizer)
    layer.setInitMethod(weightInitMethod = init, biasInitMethod = Zeros)
    KerasLayer.fuse(layer, activation,
      inputShape).asInstanceOf[AbstractModule[Tensor[T], Tensor[T], T]]
  }
}

object AtrousConvolution2D {
  def apply[@specialized(Float, Double) T: ClassTag](
    nbFilter: Int,
    nbRow: Int,
    nbCol: Int,
    init: String = "glorot_uniform",
    activation: String = null,
    subsample: (Int, Int) = (1, 1),
    atrousRate: (Int, Int) = (1, 1),
    wRegularizer: Regularizer[T] = null,
    bRegularizer: Regularizer[T] = null,
    dimOrdering: String = "th",
    inputShape: Shape = null)(implicit ev: TensorNumeric[T]): AtrousConvolution2D[T] = {
    new AtrousConvolution2D[T](nbFilter, nbRow, nbCol, KerasUtils.getInitMethod(init),
      KerasUtils.getActivation(activation),
      Array(subsample._1, subsample._2), Array(atrousRate._1, atrousRate._2),
      KerasUtils.toBigDLFormat(dimOrdering), wRegularizer, bRegularizer, inputShape)
  }
}
