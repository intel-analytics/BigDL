/*
 * Copyright 2018 Analytics Zoo Authors.
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

<<<<<<< HEAD:scala/orca/src/main/java/com/intel/analytics/bigdl/orca/inference/InferenceRuntimeException.java
package com.intel.analytics.bigdl.orca.inference;

public class InferenceRuntimeException extends RuntimeException {
	public InferenceRuntimeException(String msg) {
		super(msg);
	}

	public InferenceRuntimeException(String msg, Throwable cause) {
		super(msg, cause);
	}
=======
package com.intel.analytics.bigdl.dllib.keras.layers.internal

import com.intel.analytics.bigdl.dllib.tensor.Tensor
import com.intel.analytics.bigdl.dllib.keras.serializer.ModuleSerializationTest

class InternalERFSerialTest extends ModuleSerializationTest {
  override def test(): Unit = {
    val layer = new InternalERF[Float]()
    val input = Tensor[Float](2, 4, 4).rand
    runSerializationTest(layer, input)
  }
>>>>>>> upstream_bigdl-2.0:scala/dllib/src/test/scala/com/intel/analytics/bigdl/dllib/keras/layers/InternalERFSpec.scala
}
