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

package com.intel.analytics.bigdl.transform.vision.image

import java.nio.file.{Files, Paths}

import com.intel.analytics.bigdl.tensor.{Storage, Tensor}
import com.intel.analytics.bigdl.transform.vision.image.augmentation._
import com.intel.analytics.bigdl.transform.vision.image.label.roi._
import org.scalatest.{FlatSpec, Matchers}

class FeatureTransformerSpec extends FlatSpec with Matchers {
  val resource = getClass.getClassLoader.getResource("pascal/")

  "Image Transformer with empty byte input" should "work properly" in {
    val img = Array[Byte]()
    val byteImage = ImageFeature(img)
    val imageFrame = new LocalImageFrame(Array(byteImage))
    val imgAug = BytesToMat() ->
      Resize(1, 1, -1) ->
      FixedCrop(-1, -1, -1, -1, normalized = false) ->
      MatToFloats(validHeight = 1, validWidth = 1)
    val out = imgAug(imageFrame)
    out.asInstanceOf[LocalImageFrame].array(0).floats().length should be(3)
    out.asInstanceOf[LocalImageFrame].array(0).isValid should be(false)
  }

  "Image Transformer with exception" should "work properly" in {
    val images = ImageFrame.read(resource.getFile)
    val imgAug = BytesToMat() ->
      FixedCrop(-1, -1, -1, -1, normalized = false) ->
      Resize(300, 300, -1) ->
      MatToFloats(validHeight = 300, validWidth = 300)
    val out = imgAug(images)
    out.asInstanceOf[LocalImageFrame].array(0).floats().length should be(3 * 300 * 300)
    out.asInstanceOf[LocalImageFrame].array(0).isValid should be(false)
  }

  "ImageAugmentation with label and random" should "work properly" in {
    val img = Files.readAllBytes(Paths.get(resource.getFile + "/000025.jpg"))
    val classes = Array(11.0, 11.0, 11.0, 16.0, 16.0, 16.0, 11.0, 16.0,
      16.0, 16.0, 16.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
      0.0, 0.0, 0.0, 0.0, 1.0).map(_.toFloat)
    val boxes = Array(2.0, 84.0, 59.0, 248.0,
      68.0, 115.0, 233.0, 279.0,
      64.0, 173.0, 377.0, 373.0,
      320.0, 2.0, 496.0, 375.0,
      221.0, 4.0, 341.0, 374.0,
      135.0, 14.0, 220.0, 148.0,
      69.0, 43.0, 156.0, 177.0,
      58.0, 54.0, 104.0, 139.0,
      279.0, 1.0, 331.0, 86.0,
      320.0, 22.0, 344.0, 96.0,
      337.0, 1.0, 390.0, 107.0).map(_.toFloat)
    val label = RoiLabel(Tensor(Storage(classes)).resize(2, 11),
      Tensor(Storage(boxes)).resize(11, 4))

    val feature = ImageFeature(img, label, resource.getFile)
    val imgAug = BytesToMat() ->
      RoiNormalize() ->
      ColorJitter() ->
      RandomTransformer(Expand() -> RoiProject(), 0.5) ->
      RandomSampler() ->
      Resize(300, 300, -1) ->
      RandomTransformer(HFlip() -> RoiHFlip(), 0.5) ->
      MatToFloats(validHeight = 300, validWidth = 300)

    val imageFrame = new LocalImageFrame(Array(feature))
    val out = imgAug(imageFrame)

    out.asInstanceOf[LocalImageFrame].array(0).isValid should be(true)
    out.asInstanceOf[LocalImageFrame].array(0).getOriginalHeight should be (375)
    out.asInstanceOf[LocalImageFrame].array(0).getOriginalWidth should be (500)
    out.asInstanceOf[LocalImageFrame].array(0).getHeight should be (300)
    out.asInstanceOf[LocalImageFrame].array(0).getWidth should be (300)
  }

  "ImageAugmentation" should "work properly" in {
    val imageFrame = ImageFrame.read(resource.getFile)
    val imgAug = BytesToMat() ->
      ColorJitter() ->
      Expand() ->
      Resize(300, 300, -1) ->
      HFlip() ->
      ChannelNormalize(123, 117, 104) ->
      MatToFloats(validHeight = 300, validWidth = 300)
    val out = imgAug(imageFrame)
    out.asInstanceOf[LocalImageFrame].array(0).isValid should be(true)
    out.asInstanceOf[LocalImageFrame].array(0).getOriginalHeight should be (375)
    out.asInstanceOf[LocalImageFrame].array(0).getOriginalWidth should be (500)
    out.asInstanceOf[LocalImageFrame].array(0).getHeight should be (300)
    out.asInstanceOf[LocalImageFrame].array(0).getWidth should be (300)
  }
}
