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

import com.intel.analytics.bigdl.nn.abstractnn._
import com.intel.analytics.bigdl.tensor.Tensor
import com.intel.analytics.bigdl.tensor.TensorNumericMath.TensorNumeric
import com.intel.analytics.bigdl.utils.Shape

import scala.reflect.ClassTag

class SpatialDropout3D[T: ClassTag](val p: Double = 0.5,
                                    val dim_ordering: DataFormat = DataFormat.NCHW,
                                    var inputShape: Shape = null
                                   )(implicit ev: TensorNumeric[T])
  extends KerasLayer[Tensor[T], Tensor[T], T](KerasLayer.addBatch(inputShape)) {

  override def doBuild(inputShape: Shape): AbstractModule[Tensor[T], Tensor[T], T] = {
    val layer = com.intel.analytics.bigdl.nn.SpatialDropout3D(
      initP = p,
      format = dim_ordering
    )
    layer.asInstanceOf[AbstractModule[Tensor[T], Tensor[T], T]]
  }
}


object SpatialDropout3D {
  def apply[@specialized(Float, Double) T: ClassTag](
    p: Double = 0.5,
    dim_ordering: DataFormat = DataFormat.NCHW,
    inputShape: Shape = null
    )(implicit ev: TensorNumeric[T]) : SpatialDropout3D[T] = {
    new SpatialDropout3D[T](
      p,
      dim_ordering,
      inputShape)
  }
}
