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
package com.intel.analytics.bigdl.utils.caffe

import caffe.Caffe.EltwiseParameter.EltwiseOp
import caffe.Caffe._
import caffe.Caffe.PoolingParameter.PoolMethod
import com.google.protobuf.GeneratedMessage
import com.intel.analytics.bigdl.nn.Graph._
import com.intel.analytics.bigdl.nn._
import com.intel.analytics.bigdl.nn.abstractnn.{AbstractModule, Activity}
import com.intel.analytics.bigdl.tensor.Tensor
import com.intel.analytics.bigdl.tensor.TensorNumericMath.TensorNumeric
import com.intel.analytics.bigdl.utils.Node

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag

abstract class Converter[T: ClassTag](implicit ev: TensorNumeric[T]) {

  private var customizedLayer : Map[String, AbstractModule[Activity, Activity, T]] = _

  def setCustomizedLayer(map : Map[String, AbstractModule[Activity, Activity, T]]) : this.type = {
    customizedLayer = map
    this
  }
  /**
    * Support customized layer mapping implemented by user for specific type
   */
  private def fitCustomizedLayer(layerType : String, layerName : String) : ModuleNode[T] = {
    if (customizedLayer !=null && customizedLayer.contains(layerType)) {
      return customizedLayer(layerType).setName(layerName).apply()
    }
    throw new Exception(s"$layerType is not supported in BigDL fow now")
  }

  def convertLayerFromCaffe(layer : GeneratedMessage) : ModuleNode[T] = {
    val layerName = getLayerName(layer)
    val layerType = getLayerType(layer).toUpperCase
    val module = layerType match {
      case "CONVOLUTION" => fromCaffeConvolution(layer)
      case "RELU" => fromCaffeReLU(layer)
      case "LRN" => fromCaffeLRN(layer)
      case "POOLING" => fromCaffePooling(layer)
      case "INNERPRODUCT" => fromCaffeInnerProduct(layer)
      case "INNER_PRODUCT" => fromCaffeInnerProduct(layer)
      case "DROPOUT" => fromCaffeDropout(layer)
      case "SOFTMAX_LOSS" => fromCaffeSoftmax(layer)
      case "SOFTMAX" => fromCaffeSoftmax(layer)
      case "TANH" => fromCaffeTanh(layer)
      case "SIGMOID" => fromCaffeSigmoid(layer)
      case "SIGMOIDCROSSENTROPYLOSS" => fromCaffeSigmoid(layer)
      case "ABSVAL" => fromCaffeAbsVal(layer)
      case "BATCHNORM" => fromCaffeBatchNormalization(layer)
      case "CONCAT" => fromCaffeConcat(layer)
      case "ELU" => fromCaffeELU(layer)
      case "FLATTEN" => fromCaffeFlatten(layer)
      case "LOG" => fromCaffeLog(layer)
      case "POWER" => fromCaffePower(layer)
      case "PRELU" => fromCaffePreLU(layer)
      case "RECURRENT" => fromCaffeRecurrent(layer)
      case "RNN" => fromCaffeRecurrent(layer)
      case "RESHAPE" => fromCaffeReshape(layer)
      case "SCALE" => fromCaffeScale(layer)
      case "BIAS" => fromCaffeBias(layer)
      case "THRESHOLD" => fromCaffeThreshold(layer)
      case "Exp" => fromCaffeExp(layer)
      case "SLICE" => fromCaffeSlice(layer)
      case "TILE" => fromCaffeTile(layer)
      case "ELTWISE" => fromCaffeEltwise(layer)
      case "INPUT" => null
      case "DATA" => null
      case _ => fitCustomizedLayer(layerType, layerName)
    }
    module
  }

  protected def fromCaffeReLU(layer : GeneratedMessage) : ModuleNode[T] = {
    val layerName = getLayerName(layer)
    new ReLU(true).setName(layerName).apply()
  }

  private def fromCaffeLRN(layer : GeneratedMessage) : ModuleNode[T] = {
    val layerName = getLayerName(layer)
    val param = getLRNParam(layer).get
    val localSize = param.getLocalSize
    val alpha = param.getAlpha
    val belta = param.getBeta
    val k = param.getK
    new SpatialCrossMapLRN[T](localSize, alpha, belta, k).setName(layerName).apply()
  }

  private def fromCaffePooling(layer : GeneratedMessage): ModuleNode[T] = {
    val layerName = getLayerName(layer)
    val param = getPoolingParam(layer).get
    var kw = param.getKernelW
    var kh = param.getKernelH
    var dw = param.getStrideW
    var dh = param.getStrideH
    var pw = param.getPadW
    var ph = param.getPadH
    if (kw ==0 || kh == 0) {
      kw = param.getKernelSize
      kh = kw
    }
    if (dw == 0 || dh == 0) {
      dw = param.getStride
      dh = dw
    }
    if (pw == 0 || ph == 0) {
      pw = param.getPad
      ph = pw
    }
    val poolingType = param.getPool
    // caffe use ceil model
    val pooling = poolingType match {
      case PoolMethod.MAX => SpatialMaxPooling[T](kw, kh, dw, dh, pw, ph).ceil().
        setName(layerName).apply()
      case PoolMethod.AVE => SpatialAveragePooling[T](kw, kh, dw, dh, pw, ph).ceil().
        setName(layerName).apply()
      case _ => null
    }
    pooling
  }

  private def fromCaffeDropout(layer : GeneratedMessage) : ModuleNode[T] = {
    val param = getDropoutParam(layer).get
    val layerName = getLayerName(layer)
    val initP = param.getDropoutRatio
    new Dropout[T](initP).setName(layerName).apply()
  }

  private def fromCaffeSoftmax(layer : GeneratedMessage) : ModuleNode[T] = {
    val layerName = getLayerName(layer)
    new LogSoftMax().setName(layerName).apply()
  }

  private def fromCaffeTanh(layer : GeneratedMessage) : ModuleNode[T] = {
    val layerName = getLayerName(layer)
    new Tanh[T]().setName(layerName).apply()
  }

  private def fromCaffeSigmoid(layer : GeneratedMessage) : ModuleNode[T] = {
    val layerName = getLayerName(layer)
    new Sigmoid[T]().setName(layerName).apply()
  }

  private def fromCaffeAbsVal(layer : GeneratedMessage) : ModuleNode[T] = {
    val layerName = getLayerName(layer)
    new Abs[T]().setName(layerName).apply()
  }

  private def fromCaffeConcat(layer : GeneratedMessage) : ModuleNode[T] = {
    val layerName = getLayerName(layer)
    val param = getConcatParam(layer)
    val dim = param.get.getAxis
    JoinTable[T](dim + 1, 0).setName(layerName).apply()
  }

  private def fromCaffeFlatten(layer : GeneratedMessage) : ModuleNode[T] = {
    val layerName = getLayerName(layer)
    FlattenTable[T].setName(layerName).apply()
  }

  private def fromCaffeLog(layer : GeneratedMessage) : ModuleNode[T] = {
    val layerName = getLayerName(layer)
    Log[T]().setName(layerName).apply()
  }

  private def fromCaffePower(layer : GeneratedMessage) : ModuleNode[T] = {
    val layerName = getLayerName(layer)
    val param = getPowerParam(layer).get
    val power = param.getPower
    var scale = 1.0
    var shift = 0.0
    if (param.hasScale) scale = param.getScale
    if (param.hasShift) shift = param.getShift
    Power[T](power, scale, shift).apply()
  }

  private def fromCaffePreLU(layer : GeneratedMessage) : ModuleNode[T] = {
    val layerName = getLayerName(layer)
    PReLU[T]().setName(layerName).apply()
  }

  private def fromCaffeRecurrent(layer : GeneratedMessage) : ModuleNode[T] = {
    val layerName = getLayerName(layer)
    Recurrent[T]().setName(layerName).apply()
  }

  private def fromCaffeThreshold(layer : GeneratedMessage) : ModuleNode[T] = {
    val param = getThresholdParam(layer).get
    var threshold = 1e-6
    if (param.hasThreshold) {
      threshold = param.getThreshold
    }
    Threshold[T](threshold).setName(getLayerName(layer)).apply()
  }

  private def fromCaffeExp(layer : GeneratedMessage) : ModuleNode[T] = {
    val layerName = getLayerName(layer)
    Exp[T]().setName(layerName).apply()
  }

  private def fromCaffeSlice(layer : GeneratedMessage) : ModuleNode[T] = {
    val param = getSliceParam(layer)
    val layerName = getLayerName(layer)
    val axis = param.get.getAxis
    SplitTable[T](axis).setName(layerName).apply()
  }

  private def fromCaffeEltwise(layer : GeneratedMessage) : ModuleNode[T] = {
    val param = getEltWiseParam(layer).get
    val layerName = getLayerName(layer)
    val opsType = param.getOperation
    val coeff2 = param.getCoeff(1)
    val ops = opsType match {
      case EltwiseOp.PROD => CMaxTable[T]().setName(layerName).apply()
      case EltwiseOp.MAX => CMaxTable[T]().setName(layerName).apply()
      case EltwiseOp.SUM =>
        if (coeff2 < 0) {
          CAddTable[T]().setName(layerName).apply()
        } else {
          CSubTable[T]().setName(layerName).apply()
        }
      case _ => null
    }
    ops
  }

  protected def fromCaffeBatchNormalization(layer : GeneratedMessage) : ModuleNode[T]

  protected def fromCaffeConvolution(layer : GeneratedMessage) : ModuleNode[T]

  protected def fromCaffeInnerProduct(layer : GeneratedMessage) : ModuleNode[T]

  protected def fromCaffeELU(layer : GeneratedMessage) : ModuleNode[T]

  protected def fromCaffeReshape(layer : GeneratedMessage) : ModuleNode[T]

  protected def fromCaffeScale(layer : GeneratedMessage) : ModuleNode[T]

  protected def fromCaffeBias(layer : GeneratedMessage) : ModuleNode[T]

  protected def fromCaffeTile(layer : GeneratedMessage) : ModuleNode[T]

  protected def getLayerType(layer : GeneratedMessage) : String

  protected def getLayerName(layer : GeneratedMessage) : String

  protected def getConvolutionParam(layer : GeneratedMessage): Option[ConvolutionParameter]

  protected def getLRNParam(layer : GeneratedMessage): Option[LRNParameter]

  protected def getPoolingParam(layer : GeneratedMessage): Option[PoolingParameter]

  protected def getInnerProductParam(layer : GeneratedMessage): Option[InnerProductParameter]

  protected def getDropoutParam(layer : GeneratedMessage): Option[DropoutParameter]

  protected def getConcatParam(layer : GeneratedMessage) : Option[ConcatParameter]

  protected def getPowerParam(layer : GeneratedMessage) : Option[PowerParameter]

  protected def getThresholdParam(layer : GeneratedMessage): Option[ThresholdParameter]

  protected def getSliceParam(layer : GeneratedMessage): Option[SliceParameter]

  protected def getEltWiseParam(layer : GeneratedMessage): Option[EltwiseParameter]

  def toCaffe(moduleNode : ModuleNode[T],
              bottoms : ArrayBuffer[String]) : GeneratedMessage = {
    val model = moduleNode.element match {
      case convolution : SpatialConvolution[T] => toCaffeConvolution(moduleNode, bottoms)
      case  relu : ReLU[T] => toCaffeRelu(moduleNode, bottoms)
      case lrn : SpatialCrossMapLRN[T] => toCaffeLRN(moduleNode, bottoms)
      case maxPooling : SpatialMaxPooling[T] => toCaffeMaxPooling(moduleNode, bottoms)
      case avgPooling : SpatialAveragePooling[T] => toCaffeAvePooling(moduleNode, bottoms)
      case linear : Linear[T] => toCaffeInnerProduct(moduleNode, bottoms)
      case dropout : Dropout[T] => toCaffeDropOut(moduleNode, bottoms)
      case logSoftMax : LogSoftMax[T] => toCaffeLogSoftMax(moduleNode, bottoms)
      case tanh : Tanh[T] => toCaffeTanh(moduleNode, bottoms)
    }
    model
  }

  protected def toCaffeConvolution(moduleNode : ModuleNode[T],
                                   bottoms : ArrayBuffer[String]): GeneratedMessage

  protected def toCaffeRelu(moduleNode : ModuleNode[T],
                            bottoms : ArrayBuffer[String]): GeneratedMessage

  protected def toCaffeLRN(moduleNode : ModuleNode[T],
                            bottoms : ArrayBuffer[String]): GeneratedMessage

  protected def toCaffeMaxPooling(moduleNode : ModuleNode[T],
                           bottoms : ArrayBuffer[String]): GeneratedMessage

  protected def toCaffeAvePooling(moduleNode : ModuleNode[T],
                                  bottoms : ArrayBuffer[String]): GeneratedMessage

  protected def toCaffeInnerProduct(moduleNode : ModuleNode[T],
                                  bottoms : ArrayBuffer[String]): GeneratedMessage

  protected def toCaffeDropOut(moduleNode : ModuleNode[T],
                                    bottoms : ArrayBuffer[String]): GeneratedMessage

  protected def toCaffeLogSoftMax(moduleNode : ModuleNode[T],
                               bottoms : ArrayBuffer[String]): GeneratedMessage

  protected def toCaffeTanh(moduleNode : ModuleNode[T],
                                  bottoms : ArrayBuffer[String]): GeneratedMessage

  protected def toCaffeConvolutionParam(module : AbstractModule[Activity, Tensor[T], T])
    : mutable.HashMap[String, Int] = {
    var map = new mutable.HashMap[String, Int]()
    val layer = classOf[SpatialConvolution[T]].cast(module)
    val nInputPlane = layer.nGroup
    val nOutputPlane = layer.nOutputPlane
    val kernelW = layer.kernelW
    val kernelH = layer.kernelH
    val strideW = layer.strideW
    val strideH = layer.strideH
    val padW = layer.padW
    val padH = layer.padH
    val ngroup = layer.nGroup
    map("nInputPlane") = nInputPlane
    map("nOutputPlane") = nOutputPlane
    map("kernelW") = kernelW
    map("kernelH") = kernelH
    map("strideW") = strideW
    map("strideH") = strideH
    map("padW") = padW
    map("padH") = padH
    map("ngroup") = ngroup
    map
  }

  protected def toCaffeLRNParam(module : AbstractModule[Activity, Tensor[T], T])
    : (Int, Double, Double, Double) = {
    val layer = classOf[SpatialCrossMapLRN[T]].cast(module)
    (layer.size, layer.alpha, layer.beta, layer.k)
  }

  protected def toCaffeMaxPoolingParam(module : AbstractModule[Activity, Tensor[T], T])
    : PoolingParameter = {
    val layer = classOf[SpatialMaxPooling[T]].cast(module)
    val poolingParameter = PoolingParameter.newBuilder()
    poolingParameter.setKernelW(layer.kW)
    poolingParameter.setKernelH(layer.kH)
    poolingParameter.setStrideW(layer.dW)
    poolingParameter.setStrideH(layer.dH)
    poolingParameter.setPadW(layer.padW)
    poolingParameter.setPadH(layer.padH)
    poolingParameter.setPool(PoolMethod.MAX)
    poolingParameter.build
  }

  protected def toCaffeAvgPoolingParam(module : AbstractModule[Activity, Tensor[T], T])
    : PoolingParameter = {
    val layer = classOf[SpatialAveragePooling[T]].cast(module)
    val poolingParameter = PoolingParameter.newBuilder()
    poolingParameter.setKernelW(layer.kW)
    poolingParameter.setKernelH(layer.kH)
    poolingParameter.setStrideW(layer.dW)
    poolingParameter.setStrideH(layer.dH)
    poolingParameter.setPadW(layer.padW)
    poolingParameter.setPadH(layer.padH)
    poolingParameter.setPool(PoolMethod.AVE)
    poolingParameter.build
  }

  protected def toCaffeInnerProductParam(module : AbstractModule[Activity, Tensor[T], T])
    : (Int, Int, Boolean) = {
    val layer = classOf[Linear[T]].cast(module)
    (layer.inputSize, layer.outputSize, layer.withBias)
  }

  protected def toCaffeDropOutParam(module : AbstractModule[Activity, Tensor[T], T]) : Double = {
    val layer = classOf[Dropout[T]].cast(module)
    layer.initP
  }
}
