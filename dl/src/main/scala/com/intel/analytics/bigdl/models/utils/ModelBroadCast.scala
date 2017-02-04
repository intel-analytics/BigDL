/*
 * Licensed to Intel Corporation under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Intel Corporation licenses this file to You under the Apache License, Version 2.0
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

package com.intel.analytics.bigdl.models.utils

import com.intel.analytics.bigdl.Module
import com.intel.analytics.bigdl.tensor.TensorNumericMath.TensorNumeric
import com.intel.analytics.bigdl.tensor.{Storage, Tensor}
import org.apache.spark.SparkContext
import org.apache.spark.broadcast.Broadcast

import scala.reflect.ClassTag

class ModelBroadCast[T: ClassTag](implicit ev: TensorNumeric[T]) extends Serializable {

  private var broadcastModel: Broadcast[Module[T]] = _
  private var broadcastParameters: Broadcast[Array[Tensor[T]]] = _

  def broadcast(sc: SparkContext, model: Module[T]): this.type = {
    val bcModel = model.cloneModule()
    val weightsBias = getWeightBias(bcModel.parameters())
    broadcastModel = sc.broadcast(bcModel)
    broadcastParameters = sc.broadcast(weightsBias)
    this
  }

  def cloneModel(): Module[T] = {
    val localModel = broadcastModel.value.cloneModule()
    putWeightBias(broadcastParameters.value, localModel)
    localModel
  }


  private def getWeightBias(parameters: (Array[Tensor[T]], Array[Tensor[T]]))
  : Array[Tensor[T]] = {
    var i = 0
    val weightsBias = new Array[Tensor[T]](parameters._1.length)
    while (i < parameters._1.length) {
      if (parameters._1(i) != null) {
        val wb = parameters._1(i)
        weightsBias(i) = Tensor[T](Storage(wb.storage().array()),
          wb.storageOffset(), wb.size(), wb.stride())
        wb.set()
        parameters._2(i).set()
      }
      i += 1
    }
    weightsBias
  }

  private def putWeightBias(broadcastWeightBias: Array[Tensor[T]],
    localModel: Module[T]): Unit = {
    val localWeightBias = localModel.parameters()._1
    var i = 0
    while (i < localWeightBias.length) {
      if (localWeightBias(i) != null) {
        localWeightBias(i).set(broadcastWeightBias(i))
      }
      i += 1
    }
  }
}


object ModelBroadCast {
  def apply[@specialized(Float, Double) T: ClassTag]()(implicit ev: TensorNumeric[T])
  : ModelBroadCast[T] = new ModelBroadCast
}
