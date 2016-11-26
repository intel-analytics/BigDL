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
package com.intel.analytics.bigdl.nn

import com.intel.analytics.bigdl.tensor.Tensor
import com.intel.analytics.bigdl.tensor.TensorNumericMath.TensorNumeric
import com.intel.analytics.bigdl.utils.Table

import scala.reflect.ClassTag

/**
 * Takes a table of Tensors and outputs the multiplication of all of them.
 */
class CMulTable[T: ClassTag]()(
  implicit ev: TensorNumeric[T]) extends Module[Table, Tensor[T], T]{
  override def updateOutput(input: Table): Tensor[T] = {
    output.resizeAs(input(1)).copy(input(1))
    var i = 2
    while (i <= input.length()) {
      output.cmul(input(i))
      i += 1
    }
    output
  }

  override def updateGradInput(input: Table, gradOutput: Tensor[T]) : Table = {
    var i = 1
    while (i <= input.length()) {
      if (!gradInput.contains(i)) gradInput.insert(i, Tensor[T]())
      gradInput[Tensor[T]](i).resizeAs(input(i)).copy(gradOutput)
      var j = 1
      while (j <= input.length()) {
        if (i != j) gradInput[Tensor[T]](i).cmul(input(j))
        j += 1
      }
      i += 1
    }
    gradInput
  }

  override def toString() : String = {
    "nn.CMulTable"
  }
}
