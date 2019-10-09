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

package com.intel.analytics.bigdl.dataset.segmentation

import com.intel.analytics.bigdl.tensor.Tensor
import com.intel.analytics.bigdl.transform.vision.image.ImageFeature
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

class ResizeSpec extends FlatSpec with Matchers with BeforeAndAfter {
  "resize for segmentation" should "be ok" in {
    val input = Tensor[Float](20, 30).rand()
    val imageFeature = ImageFeature()
    val resize = Resize(minSize = 10, maxSize = 40, resizeROI = false)
    val output = resize.transform(imageFeature)
  }
}
