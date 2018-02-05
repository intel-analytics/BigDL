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

import com.intel.analytics.bigdl.nn._
import com.intel.analytics.bigdl.nn.abstractnn.AbstractModule
import com.intel.analytics.bigdl.tensor.Tensor
import com.intel.analytics.bigdl.tensor.TensorNumericMath.TensorNumeric
import com.intel.analytics.bigdl.utils.{Shape, Table}

import scala.reflect.ClassTag

class Bidirectional[T: ClassTag](
   val layer: Recurrent[T],
   val mergeMode: String = "concat",
   var inputShape: Shape = null)(implicit ev: TensorNumeric[T])
  extends KerasLayer[Tensor[T], Tensor[T], T](KerasLayer.addBatch(inputShape)) {

  private val mode = mergeMode.toLowerCase()

  require(layer.returnSequences, "Bidirectional requires RNNs to return the full sequence")
  require(mode == "sum" || mode == "mul" || mode == "concat" || mode == "ave",
    s"Invalid merge mode: $mergeMode")

  override def computeOutputShape(inputShape: Shape): Shape = {
    val output = layer.computeOutputShape(inputShape)
    if (mode == "concat") {
      val outputArray = output.toSingle().toArray
      outputArray(outputArray.length-1) = outputArray(outputArray.length-1) *2
      Shape(outputArray)
    }
    else output
  }

  override def doBuild(inputShape: Shape): AbstractModule[Tensor[T], Tensor[T], T] = {
    val input = inputShape.toSingle().toArray
    val recurrent = layer.cell(input)
    val merge = mode match {
      case "concat" => JoinTable(input.length -1, input.length -1)
      case "sum" => CAddTable()
      case "mul" => CMulTable()
      case "ave" => CAveTable()
    }
    BiRecurrent(merge.asInstanceOf[AbstractModule[Table, Tensor[T], T]]).add(recurrent)
  }
}

object Bidirectional {
  def apply[@specialized(Float, Double) T: ClassTag](
    layer: Recurrent[T],
    mergeMode: String = "concat",
    inputShape: Shape = null)(implicit ev: TensorNumeric[T]): Bidirectional[T] = {
    new Bidirectional[T](layer, mergeMode, inputShape)
  }
}