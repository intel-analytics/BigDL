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

package com.intel.analytics.bigdl.tensor

import java.util

import scala.reflect.ClassTag

private[tensor] class MklStorage[@specialized(Double, Float) T: ClassTag](
    private[tensor] var values: Array[T])
    extends Storage[T] {

  override def apply(index: Int): T = values(index)

  override def update(index: Int, value: T): Unit = values(index) = value

  override def length(): Int = values.length

  override def iterator: Iterator[T] = values.iterator

  override def array(): Array[T] = values

  override def copy(source: Storage[T], offset: Int, sourceOffset: Int,
                    length: Int): this.type = {
    this
  }

  override def resize(size: Long): this.type = {
    this
  }

  override def fill(value: T, offset: Int, length: Int): this.type = {
    this
  }

  override def set(other: Storage[T]): this.type = {
    this
  }
}
