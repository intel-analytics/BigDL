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


package com.intel.analytics.bigdl.nn

import java.io.File
import java.util.UUID

import com.intel.analytics.bigdl.tensor.Tensor
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

class ScaleCalculatorSpec extends FlatSpec with Matchers with BeforeAndAfter {

  val modelPath: String = "myTestModel" + UUID.randomUUID().toString
  val weightPath: String = "myTestModelWeight" + UUID.randomUUID().toString

  "Calculating scales" should "work correct for BLAS Linear Module" in {

    val sampleMax = 999
    val inputSize = 120
    val outputSize = 1
    val inputTensor = make1DTensor(inputSize, sampleMax)

    // Global mask, null input
    val linear0 = Linear[Float](inputSize, outputSize)
    linear0.calcScales(null)
    linear0.output should be (null)
    linear0.getInputScales() should be (Array.empty)
    linear0.getOutputScales() should be (Array.empty)
    linear0.getWeightScales() should be (Array.empty)

    // Global mask, non-null input
    val linear1 = Linear[Float](inputSize, outputSize)
    linear1.calcScales(inputTensor)
    linear1.getInputScales() should be (Array(Array[Float](sampleMax)))
    linear1.getOutputScales().length should be (1)
    linear1.getOutputScales()(0).length should be (1)
    linear1.getWeightScales().length should be (1)
    linear1.getWeightScales()(0).length should be (1)

    // Single dimension mask, non-null input
    val linear2 = Linear[Float](inputSize, outputSize)
    linear2.setInputDimMask(Math.pow(2, 0).toInt)
    linear2.calcScales(inputTensor)
    linear2.getInputScales() should be (Array(Array[Float](sampleMax)))
    linear2.getOutputScales().length should be (1)
    linear2.getOutputScales()(0).length should be (1)
    linear2.getWeightScales().length should be (1)
    linear2.getWeightScales()(0).length should be (1)

  }


  "Calculating scales" should "work correct for BLAS Spatial Convolution Module" in {
    val inputSize = 1
    val outputSize = 1
    val sampleMax = 999
    var dimMaskIdx = 0
    val spatialConv = SpatialConvolution[Float](inputSize, outputSize, 1, 1)

    val inputTensor = make2DTensor().reshape(Array(inputSize, 3, 4))


    // Global mask, null input
    val spatialConv0 = SpatialConvolution[Float](inputSize, outputSize, 1, 1)
    spatialConv0.calcScales(inputTensor)
    spatialConv0.calcScales(null)
    spatialConv0.output should be (null)
    spatialConv0.getInputScales() should be (Array.empty)
    spatialConv0.getOutputScales() should be (Array.empty)
    spatialConv0.getWeightScales() should be (Array.empty)

    // Global mask, non-null input
    val spatialConv1 = SpatialConvolution[Float](inputSize, outputSize, 1, 1)
    spatialConv1.calcScales(inputTensor)
    spatialConv1.getInputScales() should be (Array(Array[Float](12)))
    spatialConv1.getOutputScales().length should be (1)
    spatialConv1.getOutputScales()(0).length should be (1)
    spatialConv1.getWeightScales().length should be (1)
    spatialConv1.getWeightScales()(0).length should be (1)

    // Single input dimension mask, non-null input
    dimMaskIdx = 1
    val spatialConv2 = SpatialConvolution[Float](inputSize, outputSize, 1, 1)
    spatialConv2.setInputDimMask(Math.pow(2, dimMaskIdx - 1).toInt)
    spatialConv2.calcScales(inputTensor)
    val inputScales2 = Array(Array(inputTensor.select(dimMaskIdx, 1).max()))
    spatialConv2.getInputScales() should be (inputScales2)

    dimMaskIdx = 2
    val spatialConv3 = SpatialConvolution[Float](inputSize, outputSize, 1, 1)
    spatialConv3.setInputDimMask(Math.pow(2, dimMaskIdx - 1).toInt)
    spatialConv3.calcScales(inputTensor)
    val inputScales3 = Array((1 to inputTensor.size(dimMaskIdx)).map(
      idx => inputTensor.select(dimMaskIdx, idx).max()
    ).toArray)
    spatialConv3.getInputScales() should be (inputScales3)

    dimMaskIdx = 3
    val spatialConv4 = SpatialConvolution[Float](inputSize, outputSize, 1, 1)
    spatialConv4.setInputDimMask(Math.pow(2, dimMaskIdx - 1).toInt)
    spatialConv4.calcScales(inputTensor)
    val inputScales4 = Array((1 to inputTensor.size(dimMaskIdx)).map(
      idx => inputTensor.select(dimMaskIdx, idx).max()
    ).toArray)
    spatialConv4.getInputScales() should be (inputScales4)

  }


  "Calculating scales" should "work correct for BLAS Sequential Module" in {

    var dimMaskIdx = 0
    def makeSequential(): Sequential[Float] = {
      val sequential = Sequential[Float]()
      sequential.add(Reshape(Array(1, 3, 4)))
        .add(SpatialConvolution[Float](1, 1, 2, 2).setName("conv1_5x5"))
        .add(Tanh())
        .add(SpatialMaxPooling(2, 2, 2, 2))
        .add(SpatialConvolution[Float](1, 1, 2, 2).setName("conv2_5x5"))
        .add(Tanh())
        .add(SpatialMaxPooling(2, 2, 2, 2))
        .add(Reshape(Array(1 * 4 * 4)))
        .add(Linear[Float](1 * 4 * 4, 2).setName("fc1"))
        .add(Tanh())
        .add(Linear[Float](2, 10).setName("fc2"))
        .add(LogSoftMax())
      sequential
    }

    val inputTensor = make2DTensor().reshape(Array(1, 3, 4))


    // Global mask, null input
    val sequential0 = makeSequential()
    sequential0.calcScales(null)
    sequential0.output should be (null)
    sequential0.getInputScales() should be (Array.empty)
    sequential0.getOutputScales() should be (Array.empty)
    sequential0.getWeightScales() should be (Array.empty)

    // Global mask, non-null input
    val sequential1 = makeSequential()
    sequential1.calcScales(inputTensor)
    sequential1.getOutputScales().length should be (1)
    sequential1.getOutputScales()(0).length should be (1)
    sequential1.getWeightScales().length should be (1)
    sequential1.getWeightScales()(0).length should be (1)
    val inputScales1 = Array(Array(inputTensor.max()))
    val outputScales1 = Array(Array(sequential1.output.toTensor[Float].max()))
    sequential1.getInputScales() should be (inputScales1)
    sequential1.getOutputScales() should be (outputScales1)

    // Single dimension mask at index 1, non-null input
    val sequential2 = makeSequential()
    sequential2.calcScales(inputTensor)

  }

  "Calculating scales" should "work correct for BLAS ConcatTable Module" in {

    val sampleMax = 999
    val numElem = 12
    val inputTensor = make1DTensor(numElem, sampleMax)

    val concatTable = new  ConcatTable[Float]().setName("concatTable")
    concatTable.add(Linear[Float](numElem, 1))
    concatTable.add(Linear[Float](numElem, 1))

    concatTable.calcScales(inputTensor)

    // Global mask
    concatTable.setInputDimMask(0)
    concatTable.setOutputDimMask(0)
    concatTable.setWeightDimMask(0)

    concatTable.calcScales(null)
    concatTable.output should be (null)
    concatTable.getInputScales() should be (Array.empty)
    concatTable.getOutputScales() should be (Array.empty)
    concatTable.getWeightScales() should be (Array.empty)

    concatTable.calcScales(inputTensor)
    concatTable.getInputScales() should be (Array(Array[Float](sampleMax)))
    concatTable.getOutputScales().length should be (1)
    concatTable.getOutputScales()(0).length should be (1)
    concatTable.getWeightScales().length should be (1)
    concatTable.getWeightScales()(0).length should be (1)

  }

  /**
    * tensor =
    * 01 10 03 12
    * 09 07 11 08
    * 05 02 06 04
    *
    * @param dim
    * @param max
    * @return
    */
  def make2DTensor(): Tensor[Float] = {
    val tensor = Tensor[Float](3, 4)
    tensor.setValue(1, 1, 1)
    tensor.setValue(1, 2, 10)
    tensor.setValue(1, 3, 3)
    tensor.setValue(1, 4, 12)
    tensor.setValue(2, 1, 9)
    tensor.setValue(2, 2, 7)
    tensor.setValue(2, 3, 11)
    tensor.setValue(2, 4, 8)
    tensor.setValue(3, 1, 5)
    tensor.setValue(3, 2, 2)
    tensor.setValue(3, 3, 6)
    tensor.setValue(3, 4, 4)

    tensor
  }

  def make1DTensor(n: Int, max: Float): Tensor[Float] = {
    val tensor = Tensor[Float](n)
    tensor.rand(0, 100)
    tensor.setValue(1, max)
    println(tensor.storage().size)
    tensor
  }


  def getScalesFromTensor(tensor: Tensor[Float], mask: Int): Array[Float] = {
    val dimSize = tensor.size(mask)

    (1 to dimSize).map(idx => {
      tensor.select(mask, idx).abs().max()
    }).toArray

  }

  after {
    new File(modelPath).delete()
    new File(weightPath).delete()
  }

}
