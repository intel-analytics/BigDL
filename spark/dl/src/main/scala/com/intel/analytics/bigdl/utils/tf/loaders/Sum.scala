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
package com.intel.analytics.bigdl.utils.tf.loaders

import java.nio.ByteOrder

import com.intel.analytics.bigdl.Module
import com.intel.analytics.bigdl.nn.{Sequential, Sum => SumOp}
import com.intel.analytics.bigdl.tensor.Tensor
import com.intel.analytics.bigdl.tensor.TensorNumericMath.TensorNumeric
import org.tensorflow.framework.{DataType, NodeDef}

import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag

class Sum extends TensorflowOpsLoader {

  import Utils._

  override def build[T: ClassTag](nodeDef: NodeDef, byteOrder: ByteOrder)
                                 (implicit ev: TensorNumeric[T]): Module[T] = {
    Adapter[T](Array(2), tensorArrays => {
      val attr = nodeDef.getAttrMap

      val squeeze = !getBoolean(attr, "keep_dims")
      val dims = tensorArrays(0).asInstanceOf[Tensor[Int]]
      val dim = ArrayBuffer[Int]()
      val sum = Sequential[T]()
      for (i <- 1 to dims.size(1)) {
        dim += dims.valueAt(i) + 1
      }

      val dataType = getType(attr, "T")
      dataType match {
        case DataType.DT_INT8 =>
          dim.foreach(i => sum.add(new SumOp[T, Int](i, squeeze = squeeze)))
        case DataType.DT_INT16 =>
          dim.foreach(i => sum.add(new SumOp[T, Int](i, squeeze = squeeze)))
        case DataType.DT_UINT8 =>
          dim.foreach(i => sum.add(new SumOp[T, Int](i, squeeze = squeeze)))
        case DataType.DT_UINT16 =>
          dim.foreach(i => sum.add(new SumOp[T, Int](i, squeeze = squeeze)))
        case DataType.DT_INT32 =>
          dim.foreach(i => sum.add(new SumOp[T, Int](i, squeeze = squeeze)))
        case DataType.DT_INT64 =>
          dim.foreach(i => sum.add(new SumOp[T, Long](i, squeeze = squeeze)))
        case DataType.DT_FLOAT =>
          dim.foreach(i => sum.add(new SumOp[T, Float](i, squeeze = squeeze)))
        case DataType.DT_DOUBLE =>
          dim.foreach(i => sum.add(new SumOp[T, Double](i, squeeze = squeeze)))
      }
      sum
    })
  }
}