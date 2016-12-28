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

package com.intel.analytics.bigdl.dataset.text

import com.intel.analytics.bigdl.dataset.{Sample, Transformer}

import scala.collection.Iterator
import com.intel.analytics.bigdl.utils.RandomGenerator.RNG

object LabeledSentenceToSample {
  def apply(vocabLength: Int, vocabNumber: Int, batchMode: Boolean)
  : LabeledSentenceToSample =
    new LabeledSentenceToSample(vocabLength, vocabNumber, batchMode)
}

class LabeledSentenceToSample(vocabLength: Int, vocabNumber: Int, batchMode: Boolean)
  extends Transformer[LabeledSentence, Sample] {
  private val buffer = new Sample()
  private var arrayBuffer: Array[Float] = null
  private var labelBuffer: Array[Float] = null

  override def apply(prev: Iterator[LabeledSentence]): Iterator[Sample] = {
    prev.map(other => {
      val wordLength = batchMode match {
        case true => vocabNumber
        case _ => other.length
      }
      if (arrayBuffer == null || arrayBuffer.length < wordLength * vocabLength) {
        arrayBuffer = new Array[Float](wordLength*vocabLength)
      }
      var i = 0
      while (i < other.length) {
        arrayBuffer(i*vocabLength + other.getData(i).toInt) = 1.0f
        i += 1
      }
      while (i < wordLength) {
        val index = (RNG.uniform(0.0, 1.0) * vocabLength).toInt
        arrayBuffer(i*vocabLength + index) = 1.0f
        i += 1
      }
      if (labelBuffer == null || labelBuffer.length < wordLength) {
        labelBuffer = new Array[Float](wordLength)
      }
      i = 0
      while (i < other.length) {
        labelBuffer(i) = other.label()(i) + 1.0f
        i += 1
      }
      while (i < wordLength) {
        val index = (RNG.uniform(0.0, 1.0) * vocabLength).toInt
        labelBuffer(i) = index.toFloat + 1.0f
        i += 1
      }
      buffer.copy(arrayBuffer, labelBuffer,
        Array(wordLength, vocabLength), Array(wordLength))
    })
  }
}
