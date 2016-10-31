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

import com.intel.analytics.sparkdl.tensor.{DenseTensorApply, Tensor, TensorFunc6}
import com.intel.analytics.sparkdl.tensor.TensorNumericMath.TensorNumeric

import scala.reflect.ClassTag

class LogSigmoid[T: ClassTag] (implicit ev: TensorNumeric[T])
  extends TensorModule[T] {
  val buffer = Tensor()

  override def updateOutput(input: Tensor[T]): Tensor[T] = {
    output.resizeAs(input)
    buffer.resizeAs(input)

    val func = new TensorFunc6[T] {
      override def apply(data1: Array[T], offset1: Int, data2: Array[T], offset2: Int,
        data3: Array[T], offset3: Int): Unit = {
        val z = ev.exp(ev.negative(data2(offset2)))
        data3(offset3) = z
        data1(offset1) = ev.negative(
          ev.log(ev.plus(ev.fromType[Int](1), z)))
      }
    }
    DenseTensorApply.apply3[T](output, input, buffer, func)

    output
  }

  override def updateGradInput(input: Tensor[T], gradOutput: Tensor[T]): Tensor[T] = {
    require(input.isSameSizeAs(gradOutput), "input and gradOutput should have the same size")
    gradInput
      .resizeAs(buffer)

    val func = new TensorFunc6[T] {
      override def apply(data1: Array[T], offset1: Int, data2: Array[T], offset2: Int,
        data3: Array[T], offset3: Int): Unit = {
        val z = data3(offset3)
        data1(offset1) = ev.divide(
          ev.times(data2(offset2), z), ev.plus(ev.fromType[Int](1), z))
      }
    }
    DenseTensorApply.apply3[T](gradInput, gradOutput, buffer, func)

    gradInput
  }

  override def toString(): String = {
    s"nn.LogSigmoid"
  }
}
