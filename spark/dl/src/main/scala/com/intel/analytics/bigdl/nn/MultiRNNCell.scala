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

import com.intel.analytics.bigdl.nn.abstractnn.{AbstractModule, Activity, TensorModule}
import com.intel.analytics.bigdl.tensor.Tensor
import com.intel.analytics.bigdl.tensor.TensorNumericMath.TensorNumeric
import com.intel.analytics.bigdl.utils.serializer.{DataConverter, ModuleData, ModuleSerializable, ModuleSerializer}
import com.intel.analytics.bigdl.utils.{T, Table}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag

/**
 * Enable user stack multiple simple cells.
 */
class MultiRNNCell[T : ClassTag](val cells: Array[Cell[T]])(implicit ev: TensorNumeric[T])
  extends Cell[T](
    hiddensShape = cells.last.hiddensShape,
    regularizers = cells.flatMap(_.regularizers)) {
  // inputDim and hidDim must be the same with Recurrent
  private val inputDim = Recurrent.inputDim
  private val hidDim = Recurrent.hidDim

  override var preTopology: TensorModule[T] = null

  override var cell: AbstractModule[Activity, Activity, T] = buildModel()

  def buildModel(): Sequential[T] = {
    val seq = Sequential()
    cells.foreach{ cell =>
      if (cell.preTopology != null) {
        cell.includePreTopology = true
      }
      seq.add(cell)
    }
    seq
  }

  override def updateOutput(input: Table): Table = {
    val result = T()
    result(inputDim) = input(inputDim)
    val states = input(hidDim).asInstanceOf[Table]
    val outputStates = T()

    var i = 0
    while (i < cells.length) {
      result(hidDim) = states(i)
      cells(i).forward(result).toTable
      result(inputDim) = cells(i).output.toTable(inputDim)
      outputStates(i) = cells(i).output.toTable(hidDim)
      i += 1
    }

    result(hidDim) = outputStates
    this.output = result
    output
  }

  override def updateGradInput(input: Table, gradOutput: Table): Table = {
    var i = cells.length
    var error = T()
    error(inputDim) = gradOutput(inputDim)
    val states = input(hidDim).asInstanceOf[Table]
    val gradStates = gradOutput(hidDim).asInstanceOf[Table]
    val outputGradStates = T()

    val nextInput = T()
    while (i >= 1) {
      val input0: Tensor[T] = if (i > 1) {
        cells(i - 2).output.toTable(inputDim)
      } else input(inputDim)
      nextInput(inputDim) = input0

      nextInput(hidDim) = states(i - 1)
      error(hidDim) = gradStates(i - 1)
      error = cells(i - 1).updateGradInput(nextInput, error)
      outputGradStates(i - 1) = error(hidDim)
      i -= 1
    }

    this.gradInput = error
    gradInput(hidDim) = outputGradStates
    gradInput
  }

  override def accGradParameters(input: Table, gradOutput: Table): Unit = {
    var i = cells.length
    val error = T()
    error(inputDim) = gradOutput(inputDim)
    val states = input(hidDim).asInstanceOf[Table]
    val gradStates = gradOutput(hidDim).asInstanceOf[Table]

    val nextInput = T()
    while (i >= 1) {
      val input0: Tensor[T] = if (i > 1) {
        cells(i - 2).output.toTable(inputDim)
      } else input(inputDim)
      nextInput(inputDim) = input0

      nextInput(hidDim) = states(i - 1)
      error(hidDim) = gradStates(i - 1)
      cells(i - 1).accGradParameters(nextInput, error)
      error(inputDim) = cells(i - 1).gradInput.toTable(inputDim)
      i -= 1
    }
  }

  override def backward(input: Table, gradOutput: Table): Table = {
    var i = cells.length
    var error = T()
    error(inputDim) = gradOutput(inputDim)
    val states = input(hidDim).asInstanceOf[Table]
    val gradStates = gradOutput(hidDim).asInstanceOf[Table]
    val outputGradStates = T()

    val nextInput = T()
    while (i >= 1) {
      val input0: Tensor[T] = if (i > 1) {
        cells(i - 2).output.toTable(inputDim)
      } else input(inputDim)
      nextInput(inputDim) = input0

      nextInput(hidDim) = states(i - 1)
      error(hidDim) = gradStates(i - 1)
      error = cells(i - 1).backward(nextInput, error)
      outputGradStates(i - 1) = error(hidDim)
      i -= 1
    }

    this.gradInput = error
    gradInput(hidDim) = outputGradStates
    gradInput
  }

  override def zeroGradParameters(): Unit = {
    cells.foreach(_.zeroGradParameters())
  }

  override def reset(): Unit = {
    cells.foreach(_.reset())
  }
}

object MultiRNNCell {
  def apply[@specialized(Float, Double) T: ClassTag](cells: Array[Cell[T]]
    )(implicit ev: TensorNumeric[T]): MultiRNNCell[T] = {
    new MultiRNNCell[T](cells)
  }
}
