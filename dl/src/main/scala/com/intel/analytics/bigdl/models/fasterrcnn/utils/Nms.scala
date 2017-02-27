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

package com.intel.analytics.bigdl.models.fasterrcnn.utils

import com.intel.analytics.bigdl.tensor.Tensor

/**
 * Non-Maximum Suppression (nms) for Object Detection
 * The goal of nms is to solve the problem that groups of several detections near the real location,
 * ideally obtaining only one detection per object
 */
class Nms extends Serializable {

  @transient private var areas: Array[Float] = _
  @transient private var sortedScores: Tensor[Float] = _
  @transient private var sortedInds: Tensor[Float] = _
  @transient private var sortIndBuffer: Array[Int] = _
  @transient private var suppressed: Array[Int] = _

  private def init(size: Int): Unit = {
    if (suppressed == null || suppressed.length < size) {
      suppressed = new Array[Int](size)
      sortIndBuffer = new Array[Int](size)
      areas = new Array[Float](size)
    } else {
      var i = 0
      while (i < size) {
        suppressed(i) = 0
        i += 1
      }
    }
    if (sortedScores == null) {
      sortedScores = Tensor[Float]
      sortedInds = Tensor[Float]
    }
  }

  /**
   * 1. first sort the scores from highest to lowest and get indices
   * 2. for the bbox of first index,
   * get the overlap between this box and the remaining bboxes
   * put the first index to result buffer
   * 3. update the indices by keeping those bboxes with overlap less than thresh
   * 4. repeat 2 and 3 until the indices are empty
   * @param scores  score tensor
   * @param boxes box tensor, with the size N*4
   * @param thresh  overlap thresh
   * @param indices buffer to store indices after nms
   * @return the length of indices after nms
   */
  def nms(scores: Tensor[Float], boxes: Tensor[Float], thresh: Float,
    indices: Array[Int]): Int = {
    if (scores.nElement() == 0) return 0
    require(indices.length >= scores.nElement() && boxes.size(2) == 4)

    init(scores.nElement())
    val boxArray = boxes.storage().array()
    val offset = boxes.storageOffset() - 1
    val rowLength = boxes.stride(1)
    getAreas(boxArray, offset, rowLength, boxes.size(1), areas)
    // indices start from 0
    val orderLength = getSortedScoreInds(scores, sortIndBuffer)
    var indexLenth = 0
    var i = 0
    var curInd = 0

    while (i < orderLength) {
      curInd = sortIndBuffer(i)
      if (suppressed(curInd) != 1) {
        indices(indexLenth) = curInd + 1
        indexLenth += 1
        var k = i + 1
        while (k < orderLength) {
          if (suppressed(sortIndBuffer(k)) != 1 &&
            isOverlapRatioGtThresh(boxArray, offset, rowLength,
              areas, curInd, sortIndBuffer(k), thresh)) {
            suppressed(sortIndBuffer(k)) = 1
          }
          k += 1
        }
      }

      i += 1
    }
    indexLenth
  }

  private def getSortedScoreInds(scores: Tensor[Float], resultBuffer: Array[Int]): Int = {
    // note that when the score is the same,
    // the order of the indices are different in python and here
    scores.topk(scores.nElement(), dim = 1, increase = false, result = sortedScores,
      indices = sortedInds
    )
    var i = 0
    while (i < scores.nElement()) {
      sortIndBuffer(i) = sortedInds.valueAt(i + 1).toInt - 1
      i += 1
    }
    scores.nElement()
  }

  private def getAreas(boxesArr: Array[Float], offset: Int, rowLength: Int, total: Int,
    areas: Array[Float]): Array[Float] = {
    var i = 0
    while (i < total) {
      val x1 = boxesArr(offset + rowLength * i)
      val y1 = boxesArr(offset + 1 + rowLength * i)
      val x2 = boxesArr(offset + 2 + rowLength * i)
      val y2 = boxesArr(offset + 3 + rowLength * i)
      areas(i) = (x2 - x1 + 1) * (y2 - y1 + 1)
      i += 1
    }
    areas
  }

  private def isOverlapRatioGtThresh(boxArr: Array[Float], offset: Int, rowLength: Int,
    areas: Array[Float], ind: Int, ind2: Int, thresh: Float): Boolean = {
    val b1x1 = boxArr(offset + 2 + rowLength * ind2)
    val b1x2 = boxArr(offset + rowLength * ind2)
    val b2x1 = boxArr(offset + 2 + rowLength * ind)
    val b2x2 = boxArr(offset + rowLength * ind)
    val w = math.min(b1x1, b2x1) -
      math.max(b1x2, b2x2) + 1
    if (w < 0) return false

    val b1y1 = boxArr(offset + 3 + rowLength * ind2)
    val b1y2 = boxArr(offset + 1 + rowLength * ind2)
    val b2y1 = boxArr(offset + 3 + rowLength * ind)
    val b2y2 = boxArr(offset + 1 + rowLength * ind)
    val h = math.min(b1y1, b2y1) - math.max(b1y2, b2y2) + 1
    if (h < 0) return false

    val overlap = w * h
    overlap / ((areas(ind2) + areas(ind)) - overlap) > thresh
  }
}
