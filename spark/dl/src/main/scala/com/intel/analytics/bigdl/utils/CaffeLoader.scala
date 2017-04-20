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

package com.intel.analytics.bigdl.utils

import java.io.{File, FileInputStream, InputStreamReader}

import scala.collection.JavaConverters._
import caffe.Caffe
import caffe.Caffe.PoolingParameter.PoolMethod
import caffe.Caffe._
import com.google.protobuf.{CodedInputStream, TextFormat}
import com.intel.analytics.bigdl.Module
import com.intel.analytics.bigdl.nn.Graph.ModuleNode
import com.intel.analytics.bigdl.nn._
import com.intel.analytics.bigdl.tensor.Tensor
import com.intel.analytics.bigdl.tensor.TensorNumericMath.TensorNumeric
import org.apache.log4j.Logger

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag

/**
 * load caffe model parameters to defined bigdl model
 * @param prototxtPath caffe model define prototxt path
 * @param modelPath    caffe serialized model path
 * @param matchAll     if match all modules with parameters
 * @tparam T type
 */
class CaffeLoader[T: ClassTag](prototxtPath: String, modelPath: String,
  matchAll: Boolean = true
)(implicit ev: TensorNumeric[T]) {

  val logger = Logger.getLogger(getClass)

  private var netparam: Caffe.NetParameter = _
  private var name2LayerV1: Map[String, V1LayerParameter] = _
  private var name2LayerV2: Map[String, LayerParameter] = _

  private def loadCaffe(prototxtPath: String, modelPath: String): Unit = {
    if (name2LayerV2 == null) {
      netparam = loadBinary(prototxtPath, modelPath)
      name2LayerV2 = Map[String, LayerParameter]()
      name2LayerV1 = Map[String, V1LayerParameter]()
      import scala.collection.JavaConverters._
      // V1LayerParameter
      netparam.getLayersList.asScala.foreach(layer => name2LayerV1 += (layer.getName -> layer))
      // V2LayerParameter
      netparam.getLayerList.asScala.foreach(layer => name2LayerV2 += (layer.getName -> layer))
    }
  }

  private def loadBinary(prototxtPath: String, modelPath: String): Caffe.NetParameter = {
    val f = new java.io.File(prototxtPath)
    require(f.exists(), prototxtPath + " does not exists")
    val reader = new InputStreamReader(new FileInputStream(f), "ASCII")
    val builder = NetParameter.newBuilder
    TextFormat.merge(reader, builder)
    logger.info(s"start loading caffe model from $modelPath")
    val cis = CodedInputStream.newInstance(new FileInputStream(modelPath))
    cis.setSizeLimit(Integer.MAX_VALUE)
    builder.mergeFrom(cis)
    logger.info("load caffe model done")
    builder.build()
  }

  private def getBlob(name: String, ind: Int): Option[Caffe.BlobProto] = {
    if (name2LayerV2.contains(name) && name2LayerV2(name).getBlobsCount > ind) {
      Some(name2LayerV2(name).getBlobs(ind))
    } else if (name2LayerV1.contains(name) && name2LayerV1(name).getBlobsCount > ind) {
      Some(name2LayerV1(name).getBlobs(ind))
    } else {
      None
    }
  }

  private def loadParameters(name: String, params: Table): Unit = {
    logger.info(s"load parameters for $name ...")
    val caffeWeight = getBlob(name, 0)
    if (caffeWeight.isDefined) {
      require(params.contains("weight"), s"$name should contain weight")
      val caffeWeightData = caffeWeight.get.getDataList
      val weight = params[Tensor[T]]("weight")
      require(params != null && weight.nElement() == caffeWeightData.size(),
        s"weight element number is not equal between caffe layer and bigdl module $name, " +
          s"data shape in caffe is ${ caffeWeight.get.getShape() }," +
          s" while data shape in bigdl is ${ weight.size().mkString(",") }")
      var i = 0
      val weightData = weight.storage().array()
      var offset = weight.storageOffset() - 1
      while (i < caffeWeightData.size()) {
        weightData(offset) = ev.fromType[Float](caffeWeightData.get(i))
        offset += 1
        i += 1
      }
    }

    val caffeBias = getBlob(name, 1)
    if (caffeBias.isDefined) {
      require(params.contains("bias"), s"$name should contain bias")
      val caffeBiasList = caffeBias.get.getDataList
      val bias = params[Tensor[T]]("bias")
      require(bias.nElement() == caffeBiasList.size(),
        s"bias element number is not equal between caffe layer and bigdl module $name, " +
          s"data shape in caffe is ${ caffeBias.get.getShape() }," +
          s" while data shape in bigdl is ${ bias.size().mkString(",") }")
      var i = 0
      val biasData = bias.storage().array()
      var offset = bias.storageOffset() - 1
      while (i < caffeBiasList.size()) {
        biasData(offset) = ev.fromType[Float](caffeBiasList.get(i))
        offset += 1
        i += 1
      }
    }
  }

  /**
   * copy caffe parameters to module
   * if matchAll, throw an exception if some layers are not mapped
   * @param model the model defined in big-dl
   * @return
   */
  private def copyParameters(model: Module[T]): Module[T] = {
    loadCaffe(prototxtPath, modelPath)
    val parameterTable = model.getParametersTable()

    parameterTable.foreach {
      case (name: String, params: Table) =>
        copyParameter(name, params)
    }
    model
  }

  private def copyParameter(name: String, params: Table): Unit = {
    if (params == null || (!params.contains("weight") && !params.contains("bias"))) return
    if (!name2LayerV2.contains(name) && !name2LayerV1.contains(name)) {
      if (matchAll) throw new Exception(s"module $name cannot map a layer in caffe model")
      logger.info(s"$name uses initialized parameters")
      return
    }
    loadParameters(name, params)
  }

  /**
   * Load caffe model from prototxt file and binary pre-trained model and converted
   * to BigDL graph module
   * Pre-defined module structure is not needed, it will be created dynamically
   */
  def createCaffeModel(): Module[T] = {
    loadCaffe(prototxtPath, modelPath)
    val caffeTypedLayers = getCaffeTypedList
    val layers = convert(caffeTypedLayers)
    val inputs = layers.filter(layer => layer.prevNodes.size == 0).toArray
    val outputs = layers.filter(layer => layer.nextNodes.size == 0).toArray
    val module = Graph(inputs, outputs)
    copyParameters(module)
    module
  }

  private def convert(caffeTypedLayers : ArrayBuffer[Node[String]]):
        ArrayBuffer[ModuleNode[T]] = {
    val layers = new ArrayBuffer[ModuleNode[T]]()
    val layersMap = new mutable.HashMap[String, ModuleNode[T]]()
    caffeTypedLayers.foreach(layer => {
      var converted : ModuleNode[T] = convertCaffeLayer(layer)
      val layerName = layer.element
      if (converted != null) {
        val dependencies = layer.prevNodes
        dependencies.foreach(dependency => {
          val dependencyKey = dependency.element
          if (layersMap.contains(dependency.element)) layersMap(dependency.element) -> converted
        })
        while (converted.nextNodes.size != 0) {
          layers.append(converted)
          converted = converted.nextNodes(0)
        }
        layers.append(converted)
        layersMap(layerName) = converted
      }
    })
    return layers
  }

  private def convertCaffeLayer(node : Node[String]): ModuleNode[T] = {
    val layerName = node.element
    val layerType = getLayerType(layerName).get
    val module = layerType match {
      case "Convolution" => fromCaffeConvolution(layerName)
      case "ReLU" => fromCaffeReLU(layerName)
      case "LRN" => fromCaffeLRN(layerName)
      case "Pooling" => fromCaffePooling(layerName)
      case "InnerProduct" => fromCaffeInnerProduct(layerName)
      case "Dropout" => fromCaffeDropout(layerName)
      case "SOFTMAX_LOSS" => fromCaffeSoftmax(layerName)
      case "TanH" => fromCaffeTanh(layerName)
      case "SigMoid" => fromCaffeSigmoid(layerName)
      case "AbsVal" => fromCaffeAbsVal(layerName)
      case "BatchNorm" => fromCaffeBatchNormalization(layerName)
      case "Input" => null
      case _ => throw new Exception(s"$layerType is not supported in BigDL fow now")
    }
    module
  }

  private def fromCaffeBatchNormalization(layerName : String) : ModuleNode[T] = {
    val param = getBatchNormParam(layerName).get
    val eps = param.getEps
    BatchNormalization[T](3, eps).apply()
  }
  private def fromCaffeAbsVal(layerName : String) : ModuleNode[T] = {
    new Abs[T]().setName(layerName).apply()
  }
  private def fromCaffeSigmoid(layerName : String) : ModuleNode[T] = {
    new Sigmoid[T]().setName(layerName).apply()
  }

  private def fromCaffeTanh(layerName : String) : ModuleNode[T] = {
    new Tanh[T]().setName(layerName).apply()
  }

  private def fromCaffeSoftmax(layerName : String) : ModuleNode[T] = {
    new LogSoftMax().setName(layerName).apply()
  }
  private def fromCaffeDropout(layerName : String) : ModuleNode[T] = {
    val param = getDropoutParam(layerName).get
    val initP = param.getDropoutRatio
    new Dropout[T](initP).setName(layerName).apply()
  }
  private def fromCaffeInnerProduct(layerName : String) : ModuleNode[T] = {
    val param = getInnerProductParam(layerName).get
    val weightBlob = getBlob(layerName, 0).get
    var nInputPlane = 0
    if (weightBlob.hasShape) {
      nInputPlane = weightBlob.getShape.getDim(1).toInt
    }
    else {
      nInputPlane = weightBlob.getWidth
    }
    val nOutputPlane = param.getNumOutput
    val linear = Linear[T](nInputPlane, nOutputPlane).setName(layerName)
    val node = linear.apply()
    if(nInputPlane != nOutputPlane) {
      // Construct a view layer in between
      val view = View[T](nInputPlane).apply()
      view -> node
      view
    } else {
      node
    }
  }

  private def fromCaffePooling(layerName : String): ModuleNode[T] = {
    val param = getPoolingParam(layerName).get
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
    val pooling = poolingType match {
      case PoolMethod.MAX => SpatialMaxPooling[T](kw, kh, dw, dh, pw, ph).
        setName(layerName).apply()
      case PoolMethod.AVE => SpatialAveragePooling[T](kw, kh, dw, dh, pw, ph).
        setName(layerName).apply()
      case _ => null
    }
    pooling
  }

  private def fromCaffeLRN(layerName : String) : ModuleNode[T] = {
    val param = getLRNParam(layerName).get
    val localSize = param.getLocalSize
    val alpha = param.getAlpha
    val belta = param.getBeta
    val k = param.getK
    new SpatialCrossMapLRN[T](localSize, alpha, belta, k).setName(layerName).apply()
  }

  private def fromCaffeReLU(layerName : String) : ModuleNode[T] = {
    new ReLU(true).setName(layerName).apply()
  }

  private def fromCaffeConvolution(layerName : String) : ModuleNode[T] = {
    val param = getConvolutionParam(layerName).get
    val weightBlob = getBlob(layerName, 0).get
    val group = if (param.getGroup == 0)  1 else param.getGroup
    val nInputPlane = weightBlob.getChannels * group
    val nOutPlane = weightBlob.getNum
    var kw = param.getKernelW
    var kh = param.getKernelH
    var dw = param.getStrideW
    var dh = param.getStrideH
    if (kw ==0 || kh == 0) {
      kw = param.getKernelSize(0)
      kh = kw
    }
    if (dw == 0 || dh == 0) {
      if (param.getStrideList.size() != 0) {
        dw = param.getStride(0)
        dh = dw
      } else {
        // use default values if not found
        dw = 1
        dh = 1
      }

    }
    var pw = param.getPadW
    var ph = param.getPadH
    if (pw == 0 || ph == 0) {
      if (param.getPadList.size() != 0) {
        pw = param.getPad(0)
        ph = pw
      }
    }
    new SpatialConvolution[T](nInputPlane, nOutPlane, kw, kh, dw, dh, pw, ph, group)
      .setName(layerName).apply()
  }

  private def getBatchNormParam(name: String): Option[BatchNormParameter] = {
    if (name2LayerV2.contains(name)) {
      Some(name2LayerV2(name).getBatchNormParam)
    } else {
      None
    }
  }

  private def getDropoutParam(name: String): Option[DropoutParameter] = {
    if (name2LayerV2.contains(name)) {
      Some(name2LayerV2(name).getDropoutParam)
    } else if (name2LayerV1.contains(name)) {
      Some(name2LayerV1(name).getDropoutParam)
    } else {
      None
    }
  }

  private def getInnerProductParam(name: String): Option[InnerProductParameter] = {
    if (name2LayerV2.contains(name)) {
      Some(name2LayerV2(name).getInnerProductParam)
    } else if (name2LayerV1.contains(name)) {
      Some(name2LayerV1(name).getInnerProductParam)
    } else {
      None
    }
  }

  private def getPoolingParam(name: String): Option[PoolingParameter] = {
    if (name2LayerV2.contains(name)) {
      Some(name2LayerV2(name).getPoolingParam)
    } else if (name2LayerV1.contains(name)) {
      Some(name2LayerV1(name).getPoolingParam)
    } else {
      None
    }
  }

  private def getLRNParam(name: String): Option[LRNParameter] = {
    if (name2LayerV2.contains(name)) {
      Some(name2LayerV2(name).getLrnParam)
    } else if (name2LayerV1.contains(name)) {
      Some(name2LayerV1(name).getLrnParam)
    } else {
      None
    }
  }

  private def getConvolutionParam(name: String): Option[ConvolutionParameter] = {
    if (name2LayerV2.contains(name)) {
      Some(name2LayerV2(name).getConvolutionParam)
    } else if (name2LayerV1.contains(name)) {
      Some(name2LayerV1(name).getConvolutionParam)
    } else {
      None
    }
  }

  private def getLayerType(name: String): Option[String] = {
    if (name2LayerV2.contains(name)) {
      Some(name2LayerV2(name).getType)
    } else if (name2LayerV1.contains(name)) {
      Some(name2LayerV1(name).getType.toString)
    } else {
      None
    }
  }

  private def getCaffeTypedList() : ArrayBuffer[Node[String]] = {
    val list = ArrayBuffer[Node[String]]()
    val layersMap = new mutable.HashMap[String, Node[String]]()
    val top2LayerMap = new mutable.HashMap[String, String]()
    netparam.getLayersList.asScala.foreach(layer => {
      val name = layer.getName
      val node = new Node(name)
      list.append(node)
      layersMap(name) = node
      val dependencies = layer.getBottomList.asScala
      dependencies.foreach(dependency => {
        val dependentNode = layersMap(top2LayerMap(dependency))
        dependentNode -> node
      })
      val outputs = layer.getTopList.asScala
      outputs.foreach(output => {
        top2LayerMap(output) = name
      })
    })
    return list
  }
}

object CaffeLoader {

  def load[T: ClassTag](model: Module[T],
    defPath: String, modelPath: String, matchAll: Boolean = true)
    (implicit ev: TensorNumeric[T]): Module[T] = {
    val caffeLoader = new CaffeLoader[T](defPath, modelPath, matchAll)
    caffeLoader.copyParameters(model)
  }

  def loadDynamic[T: ClassTag](defPath: String, modelPath: String, matchAll: Boolean = true)
                       (implicit ev: TensorNumeric[T]): Module[T] = {
    val caffeLoader = new CaffeLoader[T](defPath, modelPath, matchAll)
    caffeLoader.createCaffeModel()
  }
}
