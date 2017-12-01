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

package com.intel.analytics.bigdl.transform.vision.image.opencv

import java.io.File

import com.intel.analytics.bigdl.opencv.OpenCV
import com.intel.analytics.bigdl.transform.vision.image.util.BoundingBox
import com.intel.analytics.bigdl.utils.Engine
import org.apache.commons.io.FileUtils
import org.apache.spark.SparkContext
import org.opencv.imgcodecs.Imgcodecs
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

class OpenCVMatSpec extends FlatSpec with Matchers with BeforeAndAfter {
  val resource = getClass().getClassLoader().getResource("pascal/000025.jpg")

  "toFloatsPixels" should "work properly" in {
    val img = OpenCVMat.read(resource.getFile)
    val floats = new Array[Float](img.height() * img.width() * img.channels())
    val out = OpenCVMat.toFloatPixels(img, floats)
    out._2 should be(375)
    out._3 should be(500)

    // without buffer
    val out2 = OpenCVMat.toFloatPixels(img)
    out2._2 should be(375)
    out2._3 should be(500)

    out._1 should equal(out2._1)
  }

  "toBytesInPixels" should "work properly" in {
    val img = OpenCVMat.read(resource.getFile)
    val bytes = new Array[Byte](img.height() * img.width() * img.channels())
    val out = OpenCVMat.toBytePixels(img, bytes)
    out._2 should be(375)
    out._3 should be(500)

    // without buffer
    val out2 = OpenCVMat.toBytePixels(img)
    out2._2 should be(375)
    out2._3 should be(500)

    out._1 should equal(out2._1)
  }

  "fromImageBytes" should "work properly" in {
    OpenCV.isOpenCVLoaded
    val img = Imgcodecs.imread(resource.getFile)
    val bytes = FileUtils.readFileToByteArray(new File(resource.getFile))
    val mat = OpenCVMat.fromImageBytes(bytes)
    img.height() should be(mat.height())
    img.width() should be(mat.width())
    img.channels() should be(mat.channels())
    val bytes1 = OpenCVMat.toBytePixels(img)
    val bytes2 = OpenCVMat.toBytePixels(mat)
    bytes1._1 should equal(bytes2._1)
  }

  "imencode" should "work not affect pixels" in {
    val img = OpenCVMat.read(resource.getFile)
    val bytes = OpenCVMat.imencode(img)
    val mat = OpenCVMat.fromImageBytes(bytes)
    val bytes1 = OpenCVMat.toBytePixels(img)
    val bytes2 = OpenCVMat.toBytePixels(mat)
    bytes1._1 should equal(bytes2._1)
  }


  var sc: SparkContext = null
  before {
    val conf = Engine.createSparkConf().setAppName("ImageSpec").setMaster("local[2]")
    sc = new SparkContext(conf)
    Engine.init
  }

  after {
    if (null != sc) sc.stop()
  }

  "serialize" should "work properly" in {
    val img = OpenCVMat.read(resource.getFile)
    val shape = img.shape()
    val rdd = sc.parallelize(Array(img))
    val collect = rdd.collect()
    collect(0).shape() should be(shape)
  }

  "release" should "work properly" in {
    val img = OpenCVMat.read(resource.getFile)
    img.release()
    img.isReleased should be (true)
    img.shape() should be (0, 0, 0)
  }

  "drawBoundingBox" should "work properly" in {
    val img = OpenCVMat.read(resource.getFile)
    val boundingBox = BoundingBox(2.0f, 84.0f, 59.0f, 248.0f, false)
    val boundingBox2 = BoundingBox(68.0f, 115.0f, 233.0f, 279.0f, false)
    img.drawBoundingBox(boundingBox, "boundingBox")
    img.drawBoundingBox(boundingBox2, "boundingBox2")
    val tmpFile = java.io.File.createTempFile("module", ".jpg")
    Imgcodecs.imwrite(tmpFile.toString, img)
    println(tmpFile)
  }
}
