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

package com.intel.analytics.bigdl.nn.mkl

import com.intel.analytics.bigdl.mkl.MKL
import com.intel.analytics.bigdl.nn._
import com.intel.analytics.bigdl.tensor.TensorNumericMath.TensorNumeric
import com.intel.analytics.bigdl.tensor._
import com.intel.analytics.bigdl.utils.RandomGenerator._

import scala.language.implicitConversions
import scala.reflect.ClassTag

class SpatialConvolution[@specialized(Float, Double) T: ClassTag](
    val nInputPlane: Int,
    val nOutputPlane: Int,
    val kernelWidth: Int,
    val kernelHeight: Int,
    val strideWidth: Int = 1,
    val strideHeight: Int = 1,
    val padWidth: Int = 0,
    val padHeight: Int = 0,
    val groups: Int = 1,
    val propagateBack: Boolean = true,
    private var initMethod: InitializationMethod = Default
)(implicit ev: TensorNumeric[T])
    extends TensorModule[T] {

  require(nInputPlane % groups == 0, "Number of input channels should be multiples of group.")
  require(nOutputPlane % groups == 0, "Number of output channels should be multiples of group.")

  val weight: Tensor[T] =
    Tensor[T](groups, nOutputPlane / groups, nInputPlane / groups, kernelHeight, kernelWidth)
  var gradWeight = Tensor[T]().resizeAs(weight)
  var bias: Tensor[T] = Tensor[T](nOutputPlane)
  var gradBias = Tensor[T](nOutputPlane)

  reset()

  private var im2colTime = 0L
  private var col2imTime = 0L

  var classPtr = 0L
  private var firstPass = true
  private var useOpenMp = true

  override def getClassPtr(): Long = classPtr

  def getIm2ColTime(): Long = im2colTime
  def getCol2ImgTime(): Long = col2imTime

  def setInitMethod(initMethod: InitializationMethod): this.type = {
    this.initMethod = initMethod
    this
  }

  def setUseOpenMp(useIt : Boolean) : this.type = {
    useOpenMp = useIt
    this
  }

  override def reset(): Unit = {
    initMethod match {
      case Default =>
        val stdv = 1.0 / math.sqrt(kernelWidth * kernelHeight * nInputPlane)
        weight.apply1(_ => ev.fromType[Double](RNG.uniform(0, 1) * 2 * stdv - stdv))
        bias.apply1(_ => ev.fromType[Double](RNG.uniform(0, 1) * 2 * stdv - stdv))
      case Xavier =>
        val fanIn = nInputPlane * kernelHeight * kernelWidth
        val fanOut = nOutputPlane * kernelHeight * kernelWidth
        val stdv = math.sqrt(6.0 / (fanIn + fanOut))
        weight.apply1(_ => ev.fromType[Double](RNG.uniform(-stdv, stdv)))
        bias.fill(ev.fromType(0))
      case Constant =>
        weight.fill(ev.fromType(0.1))
        bias.fill(ev.fromType(0))
    }
  }

  override def updateOutput(input: Tensor[T]): Tensor[T] = {
    require(input.dim() == 3 || input.dim() == 4, "Only support 3D or 4D(batch mode) input")
    // TODO the requirement of contiguous input may be not necessary for MKL 2017.
    //      because it supports the api of groups convolution.
    require(input.isContiguous(), "input is not contiguous")

    // compute the output height and width
    def computeOut(input: Int, pad: Int, kernel: Int, stride: Int): Int = {
      (input + 2 * pad - kernel) / stride + 1
    }

    // +---------+-------+-------+
    // |         | 3-dim | 4-dim |
    // +=========+=======+=======+
    // | Number  | ?     | 1     |
    // +---------+-------+-------+
    // | Channel | 1     | 2     |
    // +---------+-------+-------+
    // | Height  | 2     | 3     |
    // +---------+-------+-------+
    // | Width   | 3     | 4     |
    // +---------+-------+-------+
    // Table: Index of 3-dim/4-dim input

    val inputWidth = input.size(input.dim())
    val inputHeight = input.size(input.dim() - 1)
    val inputChannel = input.size(input.dim() - 2)
    // TODO we may set input.size(input.dim() - 3) == 1 if input.dim() == 3
    val inputNumber = if (input.dim() == 3) 1 else input.size(input.dim() - 3)

    // output number is as same as input number
    val outputNumber = inputNumber
    val outputChannel = nOutputPlane
    val outputWidth =
      computeOut(inputWidth, padWidth, kernelWidth, strideWidth)
    val outputHeight =
      computeOut(inputHeight, padHeight, kernelHeight, strideHeight)

    require(outputWidth >= 1 && outputHeight >= 1, "output size is too small")
    if (input.dim() == 3) {
      output.resize(Array(outputChannel, outputHeight, outputWidth))
    } else {
      output.resize(Array(outputNumber, outputChannel, outputHeight, outputWidth))
    }

    // kernel number and bias number are as same as nOutputPlane
    val biasNumber = nOutputPlane
    val kernelNumber = nOutputPlane
    // TODO kernel channel equals to input channel now
    val kernelChannel = inputChannel

    val inputOffset = input.storageOffset() - 1
    val outputOffset = output.storageOffset() - 1
    val biasOffset = bias.storageOffset() - 1
    val kernelOffset = weight.storageOffset() - 1

    if (!MKL.isMKLLoaded) {
      println("UNLOADED MKL!!!!!!!!!!!!!!!")
    }

    implicit def bool2int(b: Boolean) = if (b) 1 else 0
    if (firstPass) {
      ev.getType() match {
        case "Double" =>
          classPtr = MKL.ConvolutionInitDouble(inputNumber,
                                               inputChannel,
                                               inputHeight,
                                               inputWidth,
                                               kernelNumber,
                                               kernelChannel,
                                               kernelHeight,
                                               kernelWidth,
                                               strideHeight,
                                               strideWidth,
                                               padHeight,
                                               padWidth,
                                               4,
                                               groups,
                                               this.getName())
          MKL.SetUseOpenMpDouble(classPtr, useOpenMp)
        case "Float" =>
          classPtr = MKL.ConvolutionInitFloat(inputNumber,
                                              inputChannel,
                                              inputHeight,
                                              inputWidth,
                                              kernelNumber,
                                              kernelChannel,
                                              kernelHeight,
                                              kernelWidth,
                                              strideHeight,
                                              strideWidth,
                                              padHeight,
                                              padWidth,
                                              4,
                                              groups,
                                              this.getName())
          MKL.SetUseOpenMpFloat(classPtr, useOpenMp)
        case _ =>
          throw new UnsupportedOperationException(s"Only Float supported")
      }
      firstPass = false
    }

    if (initForward) {
      this.updateMklOut()
      this.initForward = false
    }

    val start = System.nanoTime()
    ev.getType() match {
      case "Double" =>
        MKL.ConvolutionForwardDouble(input.storage().array().asInstanceOf[Array[Double]],
                                     inputOffset,
                                     output.storage().array().asInstanceOf[Array[Double]],
                                     outputOffset,
                                     weight.storage().array().asInstanceOf[Array[Double]],
                                     kernelOffset,
                                     bias.storage().array().asInstanceOf[Array[Double]],
                                     biasOffset,
                                     classPtr)
      case "Float" =>
        MKL.ConvolutionForwardFloat(input.storage().array().asInstanceOf[Array[Float]],
                                    inputOffset,
                                    output.storage().array().asInstanceOf[Array[Float]],
                                    outputOffset,
                                    weight.storage().array().asInstanceOf[Array[Float]],
                                    kernelOffset,
                                    bias.storage().array().asInstanceOf[Array[Float]],
                                    biasOffset,
                                    classPtr)

      case _ =>
        throw new UnsupportedOperationException(s"Only Float supported")
    }
    output
  }

  override def updateGradInput(input: Tensor[T], gradOutput: Tensor[T]): Tensor[T] = {
    require(input.nDimension() == 3 || input.nDimension() == 4, "Only support 3D or 4D input")
    require(nOutputPlane == (if (input.nDimension() == 3) gradOutput.size(1)
                             else gradOutput.size(2)),
            "Number of output features is not equal to nOutputPlane")
    require(input.isContiguous(), "input is not contiguous")
    require(gradInput.isContiguous(), "gradInput is not contiguous")
    gradInput.resizeAs(input)

    val gradInputOffset = gradInput.storageOffset() - 1
    val gradKernelOffset = gradWeight.storageOffset() - 1
    val gradOutputOffset = gradOutput.storageOffset() - 1
    val gradBiasOffset = gradBias.storageOffset() - 1

    // +---------+-------+-------+
    // |         | 3-dim | 4-dim |
    // +=========+=======+=======+
    // | Number  | ?     | 1     |
    // +---------+-------+-------+
    // | Channel | 1     | 2     |
    // +---------+-------+-------+
    // | Height  | 2     | 3     |
    // +---------+-------+-------+
    // | Width   | 3     | 4     |
    // +---------+-------+-------+
    // Table: Index of 3-dim/4-dim input

    val inputWidth = input.size(input.dim())
    val inputHeight = input.size(input.dim() - 1)
    val inputChannel = input.size(input.dim() - 2)
    // TODO we may set input.size(input.dim() - 3) == 1 if input.dim() == 3
    val inputNumber = if (input.dim() == 3) 1 else input.size(input.dim() - 3)

    val kernelNumber = nOutputPlane
    val kernelChannel = inputChannel

    val inputOffset = input.storageOffset() - 1
    val biasOffset = bias.storageOffset() - 1
    val kernelOffset = weight.storageOffset() - 1

    implicit def bool2int(b: Boolean) = if (b) 1 else 0
    val start = System.nanoTime()
    if (isNeedComputeBack()) {
      ev.getType() match {
        case "Double" =>
          MKL.ConvolutionBackwardDataDouble(
            input.storage().array().asInstanceOf[Array[Double]],
            inputOffset,
            gradOutput.storage().array().asInstanceOf[Array[Double]],
            gradOutputOffset,
            gradInput.storage().array().asInstanceOf[Array[Double]],
            gradInputOffset,
            weight.storage().array().asInstanceOf[Array[Double]],
            kernelOffset,
            bias.storage().array().asInstanceOf[Array[Double]],
            biasOffset,
            classPtr
          )
        case "Float" =>
          MKL.ConvolutionBackwardDataFloat(
            input.storage().array().asInstanceOf[Array[Float]],
            inputOffset,
            gradOutput.storage().array().asInstanceOf[Array[Float]],
            gradOutputOffset,
            gradInput.storage().array().asInstanceOf[Array[Float]],
            gradInputOffset,
            weight.storage().array().asInstanceOf[Array[Float]],
            kernelOffset,
            bias.storage().array().asInstanceOf[Array[Float]],
            biasOffset,
            classPtr
          )

        case _ =>
          throw new UnsupportedOperationException(s"Only Float/Double supported")
      }
    }
    ev.getType() match {
      case "Double" =>
        MKL.ConvolutionBackwardKernelDouble(
          input.storage().array().asInstanceOf[Array[Double]],
          inputOffset,
          gradOutput.storage().array().asInstanceOf[Array[Double]],
          gradOutputOffset,
          gradWeight.storage().array().asInstanceOf[Array[Double]],
          gradKernelOffset,
          weight.storage().array().asInstanceOf[Array[Double]],
          kernelOffset,
          bias.storage().array().asInstanceOf[Array[Double]],
          biasOffset,
          classPtr
        )
      case "Float" =>
        MKL.ConvolutionBackwardKernelFloat(
          input.storage().array().asInstanceOf[Array[Float]],
          inputOffset,
          gradOutput.storage().array().asInstanceOf[Array[Float]],
          gradOutputOffset,
          gradWeight.storage().array().asInstanceOf[Array[Float]],
          gradKernelOffset,
          weight.storage().array().asInstanceOf[Array[Float]],
          kernelOffset,
          bias.storage().array().asInstanceOf[Array[Float]],
          biasOffset,
          classPtr
        )
      case _ =>
        throw new UnsupportedOperationException(s"Only Float/Double supported")
    }
    ev.getType() match {
      case "Double" =>
        MKL.ConvolutionBackwardBiasDouble(
          input.storage().array().asInstanceOf[Array[Double]],
          inputOffset,
          gradOutput.storage().array().asInstanceOf[Array[Double]],
          gradOutputOffset,
          gradBias.storage().array().asInstanceOf[Array[Double]],
          gradBiasOffset,
          weight.storage().array().asInstanceOf[Array[Double]],
          kernelOffset,
          bias.storage().array().asInstanceOf[Array[Double]],
          biasOffset,
          classPtr
        )

      case "Float" =>
        MKL.ConvolutionBackwardBiasFloat(
          input.storage().array().asInstanceOf[Array[Float]],
          inputOffset,
          gradOutput.storage().array().asInstanceOf[Array[Float]],
          gradOutputOffset,
          gradBias.storage().array().asInstanceOf[Array[Float]],
          gradBiasOffset,
          weight.storage().array().asInstanceOf[Array[Float]],
          kernelOffset,
          bias.storage().array().asInstanceOf[Array[Float]],
          biasOffset,
          classPtr
        )

      case _ =>
        throw new UnsupportedOperationException(s"Only Float/Double supported")
    }
    if (initBackward) {
      updateMklGradInput()
      initBackward = false
    }

    gradInput
  }

  override def updateParameters(learningRate: T): Unit = {
    weight.map(gradWeight, (a, b) => ev.minus(a, ev.times(learningRate, b)))
    bias.map(gradBias, (a, b) => ev.minus(a, ev.times(learningRate, b)))
  }

  override def zeroGradParameters(): Unit = {
    gradWeight.zero()
    gradBias.zero()
  }

  override def parameters(): (Array[Tensor[T]], Array[Tensor[T]]) = {
    (Array(this.weight, this.bias), Array(this.gradWeight, this.gradBias))
  }

  override def equals(obj: Any): Boolean = {
    if (!super.equals(obj)) {
      return false
    }

    if (!obj.isInstanceOf[SpatialConvolution[T]]) { return false }
    val other = obj.asInstanceOf[SpatialConvolution[T]]
    if (this.eq(other)) { return true }

    nInputPlane == other.nInputPlane &&
    nOutputPlane == other.nOutputPlane &&
    kernelWidth == other.kernelWidth &&
    kernelHeight == other.kernelHeight &&
    strideWidth == other.strideWidth &&
    strideHeight == other.strideHeight &&
    padWidth == other.padWidth &&
    padHeight == other.padHeight &&
    weight == other.weight &&
    bias == other.bias &&
    gradWeight == other.gradWeight &&
    gradBias == other.gradBias
  }

  override def hashCode(): Int = {
    val seed = 37
    var hash = super.hashCode()
    hash = hash * seed + nInputPlane.hashCode()
    hash = hash * seed + nOutputPlane.hashCode()
    hash = hash * seed + kernelWidth.hashCode()
    hash = hash * seed + kernelHeight.hashCode()
    hash = hash * seed + strideWidth.hashCode()
    hash = hash * seed + strideHeight.hashCode()
    hash = hash * seed + padWidth.hashCode()
    hash = hash * seed + padWidth.hashCode()
    hash = hash * seed + weight.hashCode()
    hash = hash * seed + bias.hashCode()
    hash = hash * seed + gradWeight.hashCode()
    hash = hash * seed + gradBias.hashCode()

    hash
  }

  override def toString(): String = {
    s"""mkl.SpatialConvolution($nInputPlane -> $nOutputPlane,
       |$kernelWidth x $kernelHeight, $strideWidth, $strideHeight,
       |$padWidth, $padHeight)""".stripMargin.replaceAll("\n", " ")
  }

  // mkl-dnn's convolution_backward has done updateGradInput and accGradParameters,
  // so accGradParameters does nothing
  // override def updateGradInput(input: Tensor[T], gradOutput: Tensor[T]): Tensor[T] = {
  //   backward(input, gradOutput)
  // }

  override def accGradParameters(input: Tensor[T],
                                 gradOutput: Tensor[T],
                                 scale: Double = 1.0): Unit = {}
}
