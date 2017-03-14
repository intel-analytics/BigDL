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

package com.intel.analytics.bigdl.nn

import com.intel.analytics.bigdl.nn.abstractnn.{TensorCriterion, TensorModule}
import com.intel.analytics.bigdl.tensor.Tensor
import com.intel.analytics.bigdl.tensor.TensorNumericMath.TensorNumeric
import org.scalatest.{FlatSpec, Matchers}
import com.intel.analytics.bigdl.utils.RandomGenerator._
import com.intel.analytics.bigdl.utils.{T, Table}

import scala.reflect.ClassTag

class CellUnit[T : ClassTag] ()
  (implicit ev: TensorNumeric[T])
  extends Cell[T] {

  val nHids = 3

  override def updateOutput(input: Table): Table = {
    T()
  }

  override def updateGradInput(input: Table, gradOutput: Table): Table = {
    T()
  }

  override def accGradParameters(input: Table, gradOutput: Table,
                                 scale: Double = 1.0): Unit = {}
}

@com.intel.analytics.bigdl.tags.Parallel
class CellSpec extends FlatSpec with Matchers {

  "A Cell" should "hidResize correctly" in {
    val cell = new CellUnit[Double]()
    val hidden = cell.hidResize(hidden = null, size1 = 5, size2 = 4)

    hidden.isInstanceOf[Table] should be (true)
    var i = 1
    while (i < hidden.toTable.length) {
      hidden.toTable(i).asInstanceOf[Tensor[Double]].size should be (Array(5, 4))
      i += 1
    }

    val hidden2 = T(T(Tensor[Double](3, 4)), Tensor[Double](4, 5), T(Tensor[Double](5, 6),
      Tensor[Double](3, 2)))
    cell.hidResize(hidden2, 5, 4)
    hidden2(1).asInstanceOf[Table](1).asInstanceOf[Tensor[Double]].size should be (Array(5, 4))
    hidden2(2).asInstanceOf[Tensor[Double]].size should be (Array(5, 4))
    val sample = hidden2(3).asInstanceOf[Table]
    sample(1).asInstanceOf[Tensor[Double]].size should be (Array(5, 4))
    sample(2).asInstanceOf[Tensor[Double]].size should be (Array(5, 4))
  }
}
