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

package com.intel.analytics.bigdl.transform.vision.image.augmentation

import com.intel.analytics.bigdl.tensor.Tensor
import com.intel.analytics.bigdl.transform.vision.image.{BytesToMat, ImageFrame, LocalImageFrame}
import com.intel.analytics.bigdl.utils.T
import org.scalatest.{FlatSpec, Matchers}

class CropSpec extends FlatSpec with Matchers {
  val resource = getClass.getClassLoader.getResource("pascal/")
  "centercrop" should "work properly" in {
    val data = ImageFrame.read(resource.getFile)
    val transformer = CenterCrop(50, 50)
    val transformed = transformer(data).asInstanceOf[LocalImageFrame]
    transformed.array(0).getHeight() should be (50)
    transformed.array(0).getWidth() should be (50)
  }

  "randomcrop" should "work properly" in {
    val data = ImageFrame.read(resource.getFile)
    val transformer = RandomCrop(50, 50)
    val transformed = transformer(data).asInstanceOf[LocalImageFrame]
    transformed.array(0).getHeight() should be (50)
    transformed.array(0).getWidth() should be (50)
  }

  "fixedCrop" should "work properly" in {
    val data = ImageFrame.read(resource.getFile)
    val transformer = FixedCrop(0, 0, 50, 50, false)
    val transformed = transformer(data).asInstanceOf[LocalImageFrame]
    transformed.array(0).getHeight() should be (50)
    transformed.array(0).getWidth() should be (50)
  }

  "fixedCrop normalized" should "work properly" in {
    val data = ImageFrame.read(resource.getFile)
    val transformer = FixedCrop(0, 0, 50 / 500f, 50 / 375f, true)
    val transformed = transformer(data).asInstanceOf[LocalImageFrame]
    transformed.array(0).getHeight() should be (50)
    transformed.array(0).getWidth() should be (50)
  }

  "fixedCrop clip" should "work properly" in {
    val data = ImageFrame.read(resource.getFile)
    val transformer = FixedCrop(0, 0, 600f, 700f, true)
    val transformed = transformer(data).asInstanceOf[LocalImageFrame]
    transformed.array(0).getHeight() should be (375)
    transformed.array(0).getWidth() should be (500)
  }

  "Detection Crop" should "work properly" in {
    val data = ImageFrame.read(resource.getFile)
    data.asInstanceOf[LocalImageFrame].array(0)("roi") = Tensor[Float](T(1, 1, 0.2, 0, 0, 0.5, 0.5))
    val transformer = DetectionCrop("roi")
    val transformed = transformer(data).asInstanceOf[LocalImageFrame]
    transformed.array(0).getHeight() should be (187)
    transformed.array(0).getWidth() should be (250)
  }
}
