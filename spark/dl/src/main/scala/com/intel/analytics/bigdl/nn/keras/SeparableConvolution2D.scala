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

import com.intel.analytics.bigdl.nn.{InitializationMethod, SpatialSeperableConvolution, Xavier}
import com.intel.analytics.bigdl.nn.abstractnn._
import com.intel.analytics.bigdl.optim.Regularizer
import com.intel.analytics.bigdl.tensor.Tensor
import com.intel.analytics.bigdl.tensor.TensorNumericMath.TensorNumeric
import com.intel.analytics.bigdl.utils.Shape
import com.intel.analytics.bigdl.nn.abstractnn.{AbstractModule, DataFormat}

import scala.reflect.ClassTag

/**
  * Separable convolution operator for 2D inputs.
  * Separable convolutions consist in first performing a depthwise spatial convolution (which acts
  * on each input channel separately) followed by a pointwise convolution which mixes together the
  * resulting output channels. The  depthMultiplier argument controls how many output channels are
  * generated per input channel in the depthwise step.
  *
  * When using this layer as the first layer in a model, you need to provide the argument
  * inputShape (a Single Shape, does not include the batch dimension),
  * e.g. inputShape=(3, 128, 128) for 128x128 RGB pictures.
  *
  * @param nbFilter Number of convolution filters to use.
  * @param nbRow Number of rows in the convolution kernel.
  * @param nbCol Number of columns in the convolution kernel.
  * @param init Initialization method for the weights of the layer. Default is Xavier.
  *             You can also pass in corresponding string representations such as 'glorot_uniform'
  *             or 'normal', etc. for simple init methods in the factory method.
  * @param activation activation function to use.Default is null.
  *                   You can also pass in corresponding string representations such as 'relu'
  *                   or 'sigmoid', etc. for simple activations in the factory method.
  * @param borderMode Either 'valid' or 'same'. Default is 'valid'.
  * @param subsample Int array of length 2. The step of the convolution in the height and
  *                  width dimension. Also called strides elsewhere. Default is (1, 1).
  * @param depthMultiplier how many output channel to use per input channel for the depthwise convolution step.
  * @param depthwiseRegularizer An instance of [[Regularizer]], (eg. L1 or L2 regularization),
  *                     applied to the depthwise weights matrices. Default is null.
  * @param pointwiseRegularizer An instance of [[Regularizer]], (eg. L1 or L2 regularization),
  *                     applied to the pointwise weights matrices. Default is null.
  * @param bRegularizer An instance of [[Regularizer]], applied to the bias. Default is null.
  * @param format Format of the input data. Either DataFormat.NCHW or DataFormat.NHWC. Default is NCHW.
  * @param bias Whether to include a bias (i.e. make the layer affine rather than linear).
  *             Default is true.
  * @tparam T The numeric type of parameter(e.g. weight, bias). Only support float/double now
  */
class SeparableConvolution2D[T: ClassTag](
   val nbFilter: Int,
   val nbCol: Int,
   val nbRow: Int,
   val init: InitializationMethod = Xavier,
   val activation: TensorModule[T] = null,
   val borderMode: String = "valid",
   val subsample: Array[Int] = Array(1, 1),
   val depthMultiplier: Int = 1,
   var depthwiseRegularizer: Regularizer[T] = null,
   var pointwiseRegularizer: Regularizer[T] = null,
   var bRegularizer: Regularizer[T] = null,
   val format: DataFormat = DataFormat.NCHW,
   val bias: Boolean = true,
   var inputShape: Shape = null)(implicit ev: TensorNumeric[T])
  extends KerasLayer[Tensor[T], Tensor[T], T](KerasLayer.addBatch(inputShape)) {

  require(borderMode.toLowerCase() == "valid" || borderMode.toLowerCase() == "same",
    s"$borderMode is not supported")

  override def doBuild(inputShape: Shape): AbstractModule[Tensor[T], Tensor[T], T] = {
    val input = inputShape.toSingle().toArray
    val stackSize = if (format == DataFormat.NCHW) input(1) else input(3)
    val pad = KerasUtils.getPadsFromBorderMode(borderMode)
    val layer = SpatialSeperableConvolution(
      nInputChannel = stackSize,
      nOutputChannel = nbFilter,
      depthMultiplier = depthMultiplier,
      kW = nbCol,
      kH = nbRow,
      sW = subsample(1),
      sH = subsample(0),
      pW = pad._2,
      pH = pad._1,
      hasBias = bias,
      dataFormat = format,
      wRegularizer = depthwiseRegularizer,
      bRegularizer = bRegularizer,
      pRegularizer = pointwiseRegularizer)
    KerasLayer.fuse(layer, activation,
      inputShape).asInstanceOf[AbstractModule[Tensor[T], Tensor[T], T]]
  }
}

object SeparableConvolution2D {
  def apply[@specialized(Float, Double) T: ClassTag](
    nbFilter: Int,
    nbCol: Int,
    nbRow: Int,
    init: InitializationMethod = Xavier,
    activation: String = null,
    borderMode: String = "valid",
    subsample: (Int, Int) = (1, 1),
    depthMultiplier: Int = 1,
    depthwiseRegularizer: Regularizer[T] = null,
    pointwiseRegularizer: Regularizer[T] = null,
    bRegularizer: Regularizer[T] = null,
    format: DataFormat = DataFormat.NCHW,
    bias: Boolean = true,
    inputShape: Shape = null)(implicit ev: TensorNumeric[T]) : SeparableConvolution2D[T] = {
    new SeparableConvolution2D[T](
      nbFilter, nbCol, nbRow, init, KerasUtils.getActivation(activation),
      borderMode, Array(subsample._1, subsample._2), depthMultiplier, depthwiseRegularizer,
      pointwiseRegularizer, bRegularizer, format, bias, inputShape)
  }
}
