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
import com.intel.analytics.bigdl.nn.abstractnn._
import com.intel.analytics.bigdl.nn.VolumetricMaxPooling
import com.intel.analytics.bigdl.nn.{Sequential => TSequential}
import com.intel.analytics.bigdl.tensor.Tensor
import com.intel.analytics.bigdl.tensor.TensorNumericMath.TensorNumeric
import com.intel.analytics.bigdl.utils.Shape

import scala.reflect.ClassTag

/**
  * Global Max pooling operation for 3D data.
  * Only support dim_ordering='th' now
  */
class GlobalMaxPooling3D[T: ClassTag](
   inputShape: Shape = null)(implicit ev: TensorNumeric[T])
  extends GlobalPooling3D[T](inputShape) {

  override def doBuild(inputShape: Shape): AbstractModule[Tensor[T], Tensor[T], T] = {
    val input = inputShape.toSingle().toArray
    val model = TSequential[T]()
    val layer = VolumetricMaxPooling(
      kT = input(2),
      kW = input(4),
      kH = input(3),
      dT = 1,
      dW = 1,
      dH = 1,
      padT = 0,
      padW = 0,
      padH = 0)
    model.add(layer)
    model.add(Squeeze(5))
    model.add(Squeeze(4))
    model.add(Squeeze(3))
    model.asInstanceOf[AbstractModule[Tensor[T], Tensor[T], T]]
  }
}

object GlobalMaxPooling3D {
  def apply[@specialized(Float, Double) T: ClassTag](
    inputShape: Shape = null)(implicit ev: TensorNumeric[T]) : GlobalMaxPooling3D[T] = {
    new GlobalMaxPooling3D[T](inputShape)
  }
}
