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


package com.intel.analytics.bigdl.nn.onnx

import scala.reflect.ClassTag
import com.intel.analytics.bigdl.nn
import com.intel.analytics.bigdl.tensor.TensorNumericMath.TensorNumeric


case class BatchNormalization[T: ClassTag](
  numFeatures: Int, // number of output channels, BigDL requires.
  epsilon: Float,
  momentum: Float
)


object BatchNormalization {
  def apply[T: ClassTag](
    numFeatures: Int, // number of input features, BigDL requires.
    epsilon: Float = 1e-05.toFloat,
    momentum: Float = 0.9.toFloat
  )(implicit ev: TensorNumeric[T]): nn.SpatialBatchNormalization[T] = {
   new nn.SpatialBatchNormalization(nOutput = numFeatures, eps = epsilon, momentum = momentum)
  }

}
