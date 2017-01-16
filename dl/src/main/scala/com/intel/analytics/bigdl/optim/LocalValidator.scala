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

package com.intel.analytics.bigdl.optim

import com.intel.analytics.bigdl.dataset.{LocalDataSet, MiniBatch}
import com.intel.analytics.bigdl._
import com.intel.analytics.bigdl.tensor.Tensor
import com.intel.analytics.bigdl.tensor.TensorNumericMath.TensorNumeric
import com.intel.analytics.bigdl.utils.{Engine, MklBlas, Table}
import org.apache.log4j.Logger

import scala.reflect.ClassTag

object LocalValidator {
  val logger = Logger.getLogger(getClass)
}

class LocalValidator[T: ClassTag] private[optim]
(model: Module[T], dataSet: LocalDataSet[MiniBatch[T]])
  (implicit ev: TensorNumeric[T])
  extends Validator[T, MiniBatch[T]](model, dataSet) {

  val logger = LocalValidator.logger
  private val coreNumber = Engine.coreNumber()

  private val subModelNumber = Engine.getEngineType match {
    case MklBlas => coreNumber
    case _ => throw new IllegalArgumentException
  }

  private val workingModels = (1 to subModelNumber).map(_ => model.cloneModule().evaluate()).toArray

  override def test(vMethods: Array[ValidationMethod[T]])
  : Array[(ValidationResult, ValidationMethod[T])] = {
    this.assertEngineInited()

    val dataIter = dataSet.data (train = false)
    var count = 0
    logger.info("model thread pool size is " + Engine.model.getPoolSize)
    dataIter.map(batch => {
      val (stackSize, extraSize) = (batch.size / subModelNumber,
        batch.size % subModelNumber)
      val parallelism = if (stackSize == 0) extraSize else subModelNumber
      val start = System.nanoTime()
      val result = Engine.default.invokeAndWait(
        (0 until parallelism).map(b =>
          () => {
            val offset = b * stackSize + math.min(b, extraSize)
            val length = stackSize + (if (b < extraSize) 1 else 0)
            val (input, target) = batch.narrow(1, offset + 1, length) match {
              case MiniBatch(a, b) => (a, b)
              case _ => throw new IllegalArgumentException("MiniBatch Arguments are Illegal!")
            }
            val output = workingModels(b).forward(input)
            vMethods.map(validation => {
              validation(output.asInstanceOf[Tensor[T]], target)
            })
          }
        )
      ).reduce((left, right) => {
        left.zip(right).map { case (l, r) =>
          l + r
        }
      })
      count += batch.size
      logger.info(s"[Validation] $count/${dataSet.size()} Throughput is ${
        batch.size / ((System.nanoTime() - start) / 1e9)
      } record / sec")
      result
    }).reduce((left, right) => {
      left.zip(right).map { case (l, r) =>
        l + r
      }
    }).zip(vMethods)
  }
}
