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

import com.intel.analytics.bigdl._
import com.intel.analytics.bigdl.nn.abstractnn.{AbstractModule, Activity}
import com.intel.analytics.bigdl.tensor.Tensor
import com.intel.analytics.bigdl.tensor.TensorNumericMath.TensorNumeric
import com.intel.analytics.bigdl.utils.{T, Table}

import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag

/**
 * [[Recurrent]] module is a container of rnn cells
 * Different types of rnn cells can be added using add() function
 */
class Recurrent[T : ClassTag]()
  (implicit ev: TensorNumeric[T]) extends Container[Tensor[T], Tensor[T], T] {

  private var hidden: Activity = null
  private var gradHidden: Activity = null
  private var hiddenShape: Array[Int] = null
  private val currentInput = T()
  private val currentGradOutput = T()
  private val _input = T()
  private val batchDim = 1
  private val timeDim = 2
  private val inputDim = 1
  private val hidDim = 2
  private var cellAppendStart = 0
  private var (batchSize, times) = (0, 0)
  private val dropouts: ArrayBuffer[Array[Dropout[T]]] =
    new ArrayBuffer[Array[Dropout[T]]]

  /**
   * copy type
   * @param typeId type id
   */
  sealed abstract class SetType(typeId: Int)
    extends Serializable

  /**
   *  define some dataset type
   */
  object SetType {
    case object Output extends SetType(0)
    case object GradInput extends SetType(1)
  }

  override def add(module: AbstractModule[_ <: Activity, _ <: Activity, T]): Recurrent.this.type = {
    require(module.isInstanceOf[Cell[T]],
      "Recurrent: contained module should be Cell type")
    modules += module.asInstanceOf[Cell[T]]
    this
  }

  // list of cell modules cloned from added modules
  private val cells: ArrayBuffer[Cell[T]]
  = ArrayBuffer[Cell[T]]()

  /**
   * Clone N models; N depends on the time dimension of the input
   * @param times
   * @param batchSize
   * @param hiddenSize
   */
  private def extend(times: Int, batchSize: Int, hiddenSize: Int): Unit = {
    if (hidden == null) {
      require(modules != null && modules.length == 1,
        "Recurrent extend: should contain only one cell")

      cells.clear()
      cells += modules.head.asInstanceOf[Cell[T]]
      val cell = cells.head

      // The cell will help initialize or resize the hidden variable.
      hidden = cell.hidResize(hidden = null, size = batchSize)

      /*
       * Since the gradHidden is only used as an empty Tensor or Table during
       * backward operations. We can reuse the hidden variable by pointing the
       * gradHidden to it.
       */
      gradHidden = hidden
    } else {
      cells.head.hidResize(hidden = hidden, size = batchSize)
      gradHidden = hidden
    }
    var t = cells.length
    cellAppendStart = t
    if (t < times) {
      val cloneCell = cells.head.cloneModule()
      cloneCell.parameters()._1.map(_.set())
      cloneCell.parameters()._2.map(_.set())
      while (t < times) {
        cells += cloneCell.cloneModule()
          .asInstanceOf[Cell[T]]
        t += 1
      }
      share(cells)
    }
  }

  /**
   * set the cells' output and gradInput to recurrent's output and gradInput
   * to decrease the copy expense.
   * @param setType
   */
  private def set(setType: SetType): Unit = {
    if (setType == SetType.Output) {
      if (cells.head.output.toTable(inputDim)
          .asInstanceOf[Tensor[T]]
          .storage.eq(output.select(timeDim, 1).storage)) {
        var t = cellAppendStart + 1
        while (t <= times) {
          cells(t - 1).output.toTable(inputDim)
            .asInstanceOf[Tensor[T]]
            .set(output.select(timeDim, t))
          t += 1
        }
      } else {
        var t = 1
        while (t <= times) {
          output.select(timeDim, t).copy(cells(t - 1).output.toTable(inputDim))
          t += 1
        }
        cells.head.output.toTable(inputDim)
          .asInstanceOf[Tensor[T]]
          .set(output.select(timeDim, 1))
        set(setType)
      }
    } else if (setType == SetType.GradInput) {
      if (cells.head.gradInput.toTable(inputDim)
          .asInstanceOf[Tensor[T]]
          .storage.eq(gradInput.select(timeDim, 1).storage)) {
        var t = cellAppendStart + 1
        while (t <= times) {
          cells(t - 1).gradInput.toTable(inputDim)
            .asInstanceOf[Tensor[T]]
            .set(gradInput.select(timeDim, t))
          t += 1
        }
      } else {
        var t = 1
        while (t <= times) {
          gradInput.select(timeDim, t).copy(cells(t - 1).gradInput.toTable(inputDim))
          t += 1
        }
        cells.head.gradInput.toTable(inputDim)
          .asInstanceOf[Tensor[T]]
          .set(gradInput.select(timeDim, 1))
        set(setType)
      }
    }
  }

  /**
   * Sharing weights, bias, gradWeights across all the cells in time dim
   * @param cells
   */
  def share(cells: ArrayBuffer[Cell[T]]): Unit = {
    val params = cells.head.parameters()
    cells.foreach(c => {
      if (!c.parameters().eq(params)) {
        var i = 0
        while (i < c.parameters()._1.length) {
          c.parameters()._1(i).set(params._1(i))
          i += 1
        }
        i = 0
        while (i < c.parameters()._2.length) {
          c.parameters()._2(i).set(params._2(i))
          i += 1
        }

        dropouts.append(findDropouts(c))
      }
    })

    val stepLength = dropouts.length
    for (i <- dropouts.head.indices) {
      val head = dropouts.head(i)
      val noise = head.noise
      for (j <- 1 until stepLength) {
        val current = dropouts(j)(i)
        current.noise = noise
        current.isResampling = false
      }
    }
  }

  def findDropouts(cell: Cell[T]): Array[Dropout[T]] = {
    var result: Array[Dropout[T]] = null
    cell.cell match {
      case container: Container[_, _, T] =>
        result = container
          .findModules("Dropout")
          .toArray
          .map(_.asInstanceOf[Dropout[T]])
      case _ =>
    }

    result
  }

  override def updateOutput(input: Tensor[T]): Tensor[T] = {
    require(input.dim == 3,
      "Recurrent: input should be a 3D Tensor, e.g [batch, times, nDim], " +
        s"current input.dim = ${input.dim}")

    batchSize = input.size(batchDim)
    times = input.size(timeDim)

    val hiddenSize = modules.last.asInstanceOf[Cell[T]].hiddensShape(0)
    output.resize(batchSize, times, hiddenSize)

    // Clone N modules along the sequence dimension.
    extend(times, batchSize, hiddenSize)

    /**
     * currentInput forms a T() type. It contains two elements, hidden and input.
     * Each time it will feed the cell with T(hidden, input) (or T(input, hidden) depends on
     * your hidDim and inputDim), and the cell will give a table output containing two
     * identical elements T(output, output). One of the elements from the cell output is
     * the updated hidden. Thus the currentInput will update its hidden element with this output.
     */
    currentInput(hidDim) = hidden
    var i = 1
    while (i <= times) {
      currentInput(inputDim) = input.select(timeDim, i)
      cells(i - 1).updateOutput(currentInput)
      currentInput(hidDim) = cells(i - 1).output.toTable(hidDim)
      i += 1
    }
    set(SetType.Output)
    output
  }

  override def accGradParameters(input: Tensor[T], gradOutput: Tensor[T],
                                 scale: Double = 1.0): Unit = {
    currentGradOutput(hidDim) = gradHidden
    /**
     * Since we clone module along the time dimension, the output of each
     * iteration have been recorded by the cloned modules. Thus, we can
     * reuse these outputs during the backward operations by copying the
     * outputs to _input variable.
     *
     * The output of Cell(i-1) should be one of the elements fed to the inputs
     * of Cell(i)
     * The first module in the cells array accepts zero hidden parameter.
     */
    var i = times
    while (i >= 1) {
      currentGradOutput(inputDim) = gradOutput.select(timeDim, i)
      _input(hidDim) = if (i > 1) cells(i - 2).output.toTable(hidDim)
        else hidden
      _input(inputDim) = input.select(timeDim, i)
      if (i == 1) {
        cells(i - 1).regluarized(true)
      } else {
        cells(i - 1).regluarized(false)
      }
      cells(i - 1).accGradParameters(_input, currentGradOutput, scale)
      currentGradOutput(hidDim) = cells(i - 1).gradInput.toTable(hidDim)
      i -= 1
    }
  }

  override def updateGradInput(input: Tensor[T], gradOutput: Tensor[T]): Tensor[T] = {
    gradInput.resizeAs(input)
    currentGradOutput(hidDim) = gradHidden
    var i = times
    while (i >= 1) {
      currentGradOutput(inputDim) = gradOutput.select(timeDim, i)
      _input(hidDim) = if (i > 1) cells(i - 2).output.toTable(hidDim)
        else hidden
      _input(inputDim) = input.select(timeDim, i)
      cells(i - 1).updateGradInput(_input, currentGradOutput)
      currentGradOutput(hidDim) = cells(i - 1).gradInput.toTable(hidDim)
      i -= 1
    }
    set(SetType.GradInput)
    gradInput
  }

  override def clearState() : this.type = {
    super.clearState()
    hidden = null
    gradHidden = null
    hiddenShape = null
    currentInput.clear()
    currentGradOutput.clear()
    _input.clear()
    cells.clear()
    this
  }

  override def reset(): Unit = {
    require(modules != null && modules.length == 1,
      "Recurrent extend: should contain only one cell")
    require(modules.head.isInstanceOf[Cell[T]],
      "Recurrent: should contain module with Cell type")

    modules.foreach(_.reset())
    cells.clear()
  }

  override def canEqual(other: Any): Boolean = other.isInstanceOf[Recurrent[T]]

  override def equals(other: Any): Boolean = other match {
    case that: Recurrent[T] =>
      super.equals(that) &&
        (that canEqual this) &&
        cells == that.cells
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(super.hashCode(), cells)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

object Recurrent {
  def apply[@specialized(Float, Double) T: ClassTag](
    hiddenSize: Int = 3)
    (implicit ev: TensorNumeric[T]) : Recurrent[T] = {
    new Recurrent[T]()
  }
}
