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
package com.intel.analytics.bigdl.nn.ops

import com.intel.analytics.bigdl.tensor.TensorNumericMath.TensorNumeric
import com.intel.analytics.bigdl.nn.Sigmoid
import com.intel.analytics.bigdl.tensor.Tensor
import com.intel.analytics.bigdl.utils.Table

import scala.reflect.ClassTag

class SigmoidGrad[T: ClassTag](implicit ev: TensorNumeric[T])
  extends Operation[Table, Tensor[T], T]{

  private val module = Sigmoid[T]()
  override def updateOutput(input: Table): Tensor[T] = {
    val (y, grads) = (input[Tensor[T]](1), input[Tensor[T]](2))

    output = module.updateGradInputInternal(y, grads).toTensor[T]
    output
  }
}

object SigmoidGrad {
  def apply[T: ClassTag]()(implicit ev: TensorNumeric[T]): SigmoidGrad[T] = new SigmoidGrad()
}