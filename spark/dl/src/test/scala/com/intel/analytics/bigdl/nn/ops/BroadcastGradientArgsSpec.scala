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
package com.intel.analytics.bigdl.nn.ops

import com.intel.analytics.bigdl.tensor.Tensor
import com.intel.analytics.bigdl.utils.T
import org.scalatest.{FlatSpec, Matchers}

class BroadcastGradientArgsSpec extends FlatSpec with Matchers {
  "BroadcastGradientArgs operation Float" should "works correctly" in {
    import com.intel.analytics.bigdl.numeric.NumericInt
    val input =
      T(
        Tensor(T(1, 2, 3)),
        Tensor(T(2, 2, 1))
      )

    val expectOutput = Tensor(T(3, 2, 4))

    val output = BroadcastGradientArgs().forward(input)
    output should be(expectOutput)
  }
}
