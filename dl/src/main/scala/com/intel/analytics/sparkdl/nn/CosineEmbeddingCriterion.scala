/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intel.analytics.sparkdl.nn

import com.intel.analytics.sparkdl.tensor.Tensor
import com.intel.analytics.sparkdl.tensor.TensorNumericMath.TensorNumeric
import com.intel.analytics.sparkdl.utils.{T, Table}

import scala.reflect.ClassTag

class CosineEmbeddingCriterion[T: ClassTag](margin: Double = 0.0)
 (implicit ev: TensorNumeric[T]) extends Criterion[Table, T]{
  val sizeAverage = true
  val gradInput = T()
  @transient
  private var buffer: Tensor[T] = null
  @transient
  private var w1: Tensor[T] = null
  @transient
  private var w22: Tensor[T] = null
  @transient
  private var w: Tensor[T] = null
  @transient
  private val w32: Tensor[T] = null
  @transient
  private var _outputs: Tensor[T] = null
  @transient
  private var _idx: Tensor[T] = null

  override def updateOutput(input: Table, target: Table): T = {
    var input1 = input[Tensor[T]](1)
    var input2 = input[Tensor[T]](2)
    val _y = target[Tensor[T]](1)

    if (null == buffer) buffer = Tensor[T]()
    if (null == w1) w1 = Tensor[T]()
    if (null == w22) w22 = Tensor[T]()
    if (null == w) w = Tensor[T]()
    if (null == _outputs) _outputs = Tensor[T]()
    if (null == _idx) _idx = Tensor[T]()

    if (input1.dim() == 1) {
      input1 = input1.view(1, input1.nElement())
      input2 = input2.view(1, input2.nElement())
    }

    buffer.resizeAs(input1)cmul(input1, input2)
    w1.sum(buffer, 2)

    val epsilon = 1e-12
    buffer.cmul(input1, input1)
    w22.sum(buffer, 2).add(ev.fromType(epsilon))
    _outputs.resizeAs(w22).fill(ev.fromType(1))
    w22.cdiv(_outputs, w22)
    w.resizeAs(w22).copy(w22)

    buffer.cmul(input2, input2)
    w32.sum(buffer, 2).add(ev.fromType(epsilon))
    w32.cdiv(_outputs, w32)
    w.cmul(w32)
    w.sqrt()

    _outputs.cmul(w1, w)
    _outputs = _outputs.select(2, 1)

    _idx.resizeAs(_y).eq(_y, ev.fromType(-1))
    if (ev.toType[Double](_idx.sum()) > 0) {
      _outputs.maskedCopy(_idx, Tensor[T].maskedSelect(_idx, _outputs).add(ev.fromType(-margin)))
    }
    _idx.resizeAs(_y).eq(_y, ev.fromType(1))
    if (ev.toType[Double](_idx.sum()) > 0) {
      _outputs.maskedCopy(_idx, Tensor[T].resizeAs(_idx).maskedSelect(_idx, _outputs))
    }
    output = _outputs.sum()

    if (sizeAverage) {
      output = ev.divide(output, ev.fromType(_y.size(1)))
    }
    output
  }

  override def updateGradInput(input: Table, target: Table): Table = {
    var v1 = input[Tensor[T]](1)
    var v2 = input[Tensor[T]](2)
    val _y = target[Tensor[T]](1)
    var not_batch = false

    if (v1.dim() == 1) {
      v1 = v1.view(1, v1.nElement())
      v2 = v2.view(1, v2.nElement())
      not_batch = true
    }

    if (!gradInput.contains(1)) gradInput.insert(1, Tensor[T])
    if (!gradInput.contains(2)) gradInput.insert(2, Tensor[T])

    val gw1 = gradInput[Tensor[T]](1)
    val gw2 = gradInput[Tensor[T]](2)

    gw1.resizeAs(v1).copy(v2)
    gw2.resizeAs(v1).copy(v1)

    buffer.resizeAs(w1).cmul(w1, w22)
    gw1.addcmul(ev.fromType(-1), buffer.expandAs(v1), v1)
    gw1.cmul(w.expandAs(v1))

    buffer.resizeAs(w1).cmul(w1, w32)
    gw2.addcmul(ev.fromType(-1), buffer.expandAs(v1), v2)
    gw2.cmul(w.expandAs(v1))

    _idx.resizeAs(_y).le(_y, Tensor[T].resizeAs(_y).zero())
    _idx.view(_idx.nElement(), 1)
    _idx.resizeAs(gw1)

    val tmp = Tensor[T](ev.toType[Double](_idx.sum()).toInt).zero()
    gw1.maskedCopy(_idx, tmp)
    gw2.maskedCopy(_idx, Tensor[T](ev.toType[Double](_idx.sum()).toInt).zero())

    _idx.resizeAs(_y).eq(_y, ev.fromType(0))
    _idx.view(_idx.nElement(), 1)
    _idx.resizeAs(gw2)

    gw1.maskedCopy(_idx, Tensor[T](ev.toType[Double](_idx.sum()).toInt).zero())
    gw2.maskedCopy(_idx, Tensor[T](ev.toType[Double](_idx.sum()).toInt).zero())

    if (ev.toType[Double](_idx.sum()) > 0) {
      gw1.maskedCopy(_idx, Tensor[T].maskedSelect(_idx, gw1).mul(ev.fromType(-1)))
    }
    if (ev.toType[Double](_idx.sum()) > 0) {
      gw2.maskedCopy(_idx, Tensor[T].maskedSelect(_idx, gw2).mul(ev.fromType(-1)))
    }

    if (sizeAverage) {
      gw1.div(ev.fromType(_y.size(1)))
      gw2.div(ev.fromType(_y.size(1)))
    }

    if (not_batch) {
      gradInput[Tensor[T]](1).resize(gw1.size(2))
      gradInput[Tensor[T]](2).resize(gw2.size(2))
    }

    gradInput
  }

  override def toString(): String = {
    s"nn.CosineEmbeddingCriterion($margin)"
  }
}
