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
package com.intel.analytics.bigdl.dataset

import com.intel.analytics.bigdl.tensor.TensorNumericMath.TensorNumeric
import com.intel.analytics.bigdl.tensor.{Storage, Tensor}
import com.intel.analytics.bigdl.utils.{T, Table}
import org.apache.commons.lang3.SerializationUtils

import scala.collection.Iterator
import scala.reflect.ClassTag

/**
 * Transform a data stream of type A to type B. It is usually used in data pre-process stage.
 * Different transformers can compose a pipeline. For example, if there're transformer1 from A to
 * B, transformer2 from B to C, and transformer3 from C to D, you can compose them into a bigger
 * transformer from A to D by transformer1 -> transformer2 -> transformer 3.
 *
 * The purpose of transformer is for code reuse. Many deep learning share many common data
 * pre-process steps. User needn't write them every time, but can reuse others work.
 *
 * Transformer can be used with RDD(rdd.mapPartition), iterator and DataSet.
 * @tparam A
 * @tparam B
 */
trait Transformer[A, B] extends Serializable {
  def apply(prev: Iterator[A]): Iterator[B]

  // scalastyle:off methodName
  // scalastyle:off noSpaceBeforeLeftBracket
  def -> [C](other: Transformer[B, C]): Transformer[A, C] = {
    new ChainedTransformer(this, other)
  }

  // scalastyle:on noSpaceBeforeLeftBracket
  // scalastyle:on methodName

  def cloneTransformer(): Transformer[A, B] = {
    SerializationUtils.clone(this)
  }
}

class ChainedTransformer[A, B, C](first: Transformer[A, B], last: Transformer[B, C])
  extends Transformer[A, C] {
  override def apply(prev: Iterator[A]): Iterator[C] = {
    last(first(prev))
  }
}

object Identity {
  def apply[A](): Identity[A] = new Identity[A]()
}

class Identity[A] extends Transformer[A, A] {
  override def apply(prev: Iterator[A]): Iterator[A] = {
    prev
  }
}

object SampleToBatch {
  def apply[T: ClassTag]
  (batchSize: Int,
   isTable: Boolean = false)
  (implicit ev: TensorNumeric[T]): SampleToBatch[T]
  = new SampleToBatch[T](batchSize, isTable)
}

class SampleToBatch[T: ClassTag]
  (totalBatch: Int, isTable: Boolean = false)
  (implicit ev: TensorNumeric[T])
  extends Transformer[Sample[T], MiniBatch[T]] {

  override def apply(prev: Iterator[Sample[T]]): Iterator[MiniBatch[T]] = {
    new Iterator[MiniBatch[T]] {
      private var featureTensor: Tensor[T] = _
      private var labelTensor: Tensor[T] = _
      private var featureTable: Table = _
      private var labelTable: Table = _
      private var featureData: Array[T] = null
      private var labelData: Array[T] = null
      private val batchSize = Utils.getBatchSize(totalBatch)
      private var featureSize: Array[Int] = null
      private var labelSize: Array[Int] = null
      private var oneFeatureLength: Int = 0
      private var oneLabelLength: Int = 0
      private var featureArrayOfTensor: Array[Tensor[T]] = null
      private var labelArrayOfTensor: Array[Tensor[T]] = null
      override def hasNext: Boolean = prev.hasNext

      if (isTable) {
        featureTable = T()
        labelTable = T()
        featureArrayOfTensor = new Array[Tensor[T]](batchSize)
        labelArrayOfTensor = new Array[Tensor[T]](batchSize)
        var i = 0
        while (i < batchSize) {
          featureArrayOfTensor(i) = Tensor[T]()
          labelArrayOfTensor(i) = Tensor[T]()
          i += 1
        }
      } else {
        featureTensor = Tensor[T]()
        labelTensor = Tensor[T]()
      }

      override def next(): MiniBatch[T] = {
        if (prev.hasNext) {
          if (isTable) {
            var i = 0
            while (i < batchSize && prev.hasNext) {
              val sample = prev.next()
              /*
              * Each Tensor in the Table should be independent
              * Thus the resizeAs function will only affect one sample
              */
              featureArrayOfTensor(i).resizeAs(sample.feature()).copy(sample.feature())
              labelArrayOfTensor(i).resizeAs(sample.label()).copy(sample.label())
              featureTable(i + 1) = featureArrayOfTensor(i)
              labelTable(i + 1) = labelArrayOfTensor(i)
              i += 1
            }
            var j = i + 1
            while (j <= featureTable.length) {
              featureTable.remove(j)
              j += 1
            }
            j = i + 1
            while (j <= labelTable.length) {
              labelTable.remove(j)
              j += 1
            }
            MiniBatch(featureTable, labelTable)
          } else {
            var i = 0
            while (i < batchSize && prev.hasNext) {
              val sample = prev.next()
              if (featureData == null) {
                oneFeatureLength = sample.feature().nElement()
                oneLabelLength = sample.label().nElement()
                /*
                * For example,
                * the featureSize of an image is: [3, 224, 224]
                * the labelSize of an image is: [1]
                *
                * the featureSize of a fixed sentence input is: [10, 1000]
                * the labelSize of a fixed sentence input is: [10]
                */
                featureSize = Array(1) ++ sample.feature().size
                labelSize = Array(1) ++ sample.label().size
                featureData = new Array[T](batchSize * oneFeatureLength)
                labelData = new Array[T](batchSize * oneLabelLength)
              }
              sample.extractLabel(labelData, i*oneLabelLength, oneLabelLength)
              sample.extractFeature(featureData, i*oneFeatureLength, oneFeatureLength)
              i += 1
            }
            featureSize(0) = i
            labelSize(0) = i
            featureTensor.set(Storage[T](featureData),
              storageOffset = 1, sizes = featureSize)
            labelTensor.set(Storage[T](labelData),
              storageOffset = 1, sizes = labelSize)
            MiniBatch(featureTensor, labelTensor)
          }
        } else {
          null
        }
      }
    }
  }
}
