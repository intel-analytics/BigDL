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

package com.intel.analytics.bigdl.nn

import com.intel.analytics.bigdl.nn.abstractnn.{AbstractModule, Activity}
import com.intel.analytics.bigdl.optim.Regularizer
import com.intel.analytics.bigdl.tensor.Tensor
import com.intel.analytics.bigdl.tensor.TensorNumericMath.TensorNumeric
import com.intel.analytics.bigdl.utils.{T, Table}

import scala.reflect.ClassTag

/**
 * Convolution Long Short Term Memory architecture with peephole.
 * Ref. A.: https://arxiv.org/abs/1506.04214 (blueprint for this module)
 * B. https://github.com/viorik/ConvLSTM
 *
 * @param inputSize the size of each input
 * @param hiddenSize Hidden unit size in the LSTM
 * @param kernelI Convolutional filter size to convolve input
 * @param kernelC Convolutional filter size to convolve cell
 * @param stride The step of the convolution
 * @param wRegularizer: instance of [[Regularizer]]
            (eg. L1 or L2 regularization), applied to the input weights matrices.
 * @param uRegularizer: instance [[Regularizer]]
            (eg. L1 or L2 regularization), applied to the recurrent weights matrices.
 * @param bRegularizer: instance of [[Regularizer]]
            applied to the bias.
 * @param withPeephole: whether use last cell status control a gate.
 */
class ConvLSTMPeephole[T : ClassTag] (
   val inputSize: Int,
   val hiddenSize: Int,
   val kernelI: Int,
   val kernelC: Int,
   val stride: Int,
   var wRegularizer: Regularizer[T] = null,
   var uRegularizer: Regularizer[T] = null,
   var bRegularizer: Regularizer[T] = null,
   val withPeephole: Boolean = true
)(implicit ev: TensorNumeric[T])
  extends Cell[T](
    hiddensShape = Array(hiddenSize, hiddenSize),
    regularizers = Array(wRegularizer, uRegularizer, bRegularizer)
  ) {
  var inputGate: Sequential[T] = _
  var forgetGate: Sequential[T] = _
  var outputGate: Sequential[T] = _
  var hiddenLayer: Sequential[T] = _
  var cellLayer: Sequential[T] = _
  override var cell: AbstractModule[Activity, Activity, T] = buildConvLSTM()

  def buildGate(): Sequential[T] = {
    val i2g = Sequential()
      .add(Contiguous())
      .add(SpatialConvolution(inputSize, hiddenSize, kernelI, kernelI,
      stride, stride, kernelI/2, kernelI/2, wRegularizer = wRegularizer,
        bRegularizer = bRegularizer))

    val h2g = SpatialConvolution(hiddenSize, hiddenSize, kernelC, kernelC,
      stride, stride, kernelC/2, kernelC/2, withBias = false,
      wRegularizer = uRegularizer)

    val gate = Sequential()
    if (withPeephole) {
      gate
        .add(ParallelTable()
          .add(i2g)
          .add(h2g)
          .add(CMul(Array(1, hiddenSize, 1, 1))))
    } else {
      gate.add(NarrowTable(1, 2))
      gate
        .add(ParallelTable()
          .add(i2g)
          .add(h2g))
    }

    gate.add(CAddTable())
      .add(Sigmoid())
  }

  def buildInputGate(): Sequential[T] = {
    inputGate = buildGate()
    inputGate
  }

  def buildForgetGate(): Sequential[T] = {
    forgetGate = buildGate()
    forgetGate
  }

  def buildOutputGate(): Sequential[T] = {
    outputGate = buildGate()
    outputGate
  }

  def buildHidden(): Sequential[T] = {
    val hidden = Sequential()
      .add(NarrowTable(1, 2))

    val i2h = Sequential()
      .add(Contiguous())
      .add(SpatialConvolution(inputSize, hiddenSize, kernelI, kernelI,
      stride, stride, kernelI/2, kernelI/2, wRegularizer = wRegularizer,
      bRegularizer = bRegularizer))
    val h2h = SpatialConvolution(hiddenSize, hiddenSize, kernelC, kernelC,
      stride, stride, kernelC/2, kernelC/2, withBias = false,
      wRegularizer = uRegularizer)

    hidden
      .add(ParallelTable()
        .add(i2h)
        .add(h2h))
      .add(CAddTable())
      .add(Tanh())

    this.hiddenLayer = hidden
    hidden
  }

  def buildCell(): Sequential[T] = {
    buildInputGate()
    buildForgetGate()
    buildHidden()

    val forgetLayer = Sequential()
      .add(ConcatTable()
        .add(forgetGate)
        .add(SelectTable(3)))
      .add(CMulTable())

    val inputLayer = Sequential()
      .add(ConcatTable()
        .add(inputGate)
        .add(hiddenLayer))
      .add(CMulTable())

    val cellLayer = Sequential()
      .add(ConcatTable()
        .add(forgetLayer)
        .add(inputLayer))
      .add(CAddTable())

    this.cellLayer = cellLayer
    cellLayer
  }

  def buildConvLSTM(): Sequential[T] = {
    buildCell()
    buildOutputGate()

    val convlstm = Sequential()
      .add(FlattenTable())
      .add(ConcatTable()
        .add(NarrowTable(1, 2))
        .add(cellLayer))
      .add(FlattenTable())

      .add(ConcatTable()
        .add(Sequential()
          .add(ConcatTable()
            .add(outputGate)
            .add(Sequential()
              .add(SelectTable(3))
              .add(Tanh())
            )
          )
          .add(CMulTable()))
        .add(SelectTable(3)))

      .add(ConcatTable()
        .add(SelectTable(1))
        .add(Identity()))

    cell = convlstm
    convlstm
  }

  override def canEqual(other: Any): Boolean = other.isInstanceOf[ConvLSTMPeephole[T]]

  override def equals(other: Any): Boolean = other match {
    case that: ConvLSTMPeephole[T] =>
      super.equals(that) &&
        (that canEqual this) &&
        inputSize == that.inputSize &&
        hiddenSize == that.hiddenSize &&
        kernelI == that.kernelI &&
        kernelC == that.kernelC &&
        stride == that.stride

    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(super.hashCode(), inputSize, hiddenSize, kernelI, kernelC, stride)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }

  override def reset(): Unit = {
    super.reset()
    cell.reset()
  }

  override def toString: String = s"ConvLSTMPeephole($inputSize, $hiddenSize," +
    s"$kernelI, $kernelC, $stride)"
}

object ConvLSTMPeephole {
  def apply[@specialized(Float, Double) T: ClassTag](
    inputSize: Int,
    hiddenSize: Int,
    kernelI: Int,
    kernelC: Int,
    stride: Int = 1,
    wRegularizer: Regularizer[T] = null,
    uRegularizer: Regularizer[T] = null,
    bRegularizer: Regularizer[T] = null,
    withPeephole: Boolean = true
  )
    (implicit ev: TensorNumeric[T]): ConvLSTMPeephole[T] = {
    new ConvLSTMPeephole[T](inputSize, hiddenSize, kernelI, kernelC, stride,
      wRegularizer, uRegularizer, bRegularizer, withPeephole)
  }
}

