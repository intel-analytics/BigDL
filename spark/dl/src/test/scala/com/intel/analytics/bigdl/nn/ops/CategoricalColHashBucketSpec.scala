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

import com.intel.analytics.bigdl.tensor.Tensor
import com.intel.analytics.bigdl.utils.{T, Table}
import org.scalatest.{FlatSpec, Matchers}

class CategoricalColHashBucketSpec extends FlatSpec with Matchers {

  "CategoricalColHashBucket operation single value feature column" should "work correctly" in {
    val input = T(
      Tensor[Int](T(T(1), T(2), T(3)))
    )
    val indices = Array(Array(0, 1, 2), Array(0, 0, 0))
    val values = Array(5.0, 53.0, 77.0)
    val shape = Array(3, 1)
    val expectOutput = Tensor.sparse(
      indices, values, shape
    )
    val output = CategoricalColHashBucket[Double](hashBucketSize = 100, transType = 1)
      .forward(input)
    output should be(expectOutput)
  }

  "CategoricalColHashBucket operation multi value feature column" should "work correctly" in {
    val input = T(
      Tensor[String](T(T("1,2"), T("2"), T("1,3,2")))
    )
    val indices = Array(Array(0, 0, 1, 2, 2, 2), Array(0, 1, 0, 0, 1, 2))
    val values = Array(5.0, 53.0, 53.0, 5.0, 77.0, 53.0)
    val shape = Array(3, 3)
    val expectOutput = Tensor.dense(Tensor.sparse(
      indices, values, shape
    ))
    val output = CategoricalColHashBucket[Double](hashBucketSize = 100, transType = 0)
      .forward(input)
    output should be(expectOutput)
  }
}
