/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
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

package com.intel.analytics.bigdl.models.numerals_classification

import com.intel.analytics.bigdl.nn._
import com.intel.analytics.bigdl._
import com.intel.analytics.bigdl.numeric.NumericFloat

//import scala.reflect.ClassTag

object Numerals_Classification {
  def apply(classNum: Int): Module[Float] = {
    val model = Sequential()
    model.add(Linear(4, 40).setInitMethod(Xavier).setName("fc1"))
    model.add(ReLU().setName("relu1"))
    model.add(Linear(40, classNum).setInitMethod(Xavier).setName("fc2"))
    model.add(LogSoftMax())
    model
  }
}
