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
package com.intel.analytics.bigdl.utils.serializer

import com.intel.analytics.bigdl.nn._

import scala.collection.JavaConverters._
import scala.reflect.runtime.universe
import com.intel.analytics.bigdl.nn.abstractnn.{AbstractModule, Activity}
import com.intel.analytics.bigdl.tensor.{Tensor, TensorNumericMath}
import com.intel.analytics.bigdl.tensor.TensorNumericMath.TensorNumeric
import serialization.Model.{AttrValue, BigDLModule}

import scala.collection.mutable
import scala.reflect.ClassTag

object ModuleSerializer extends ModuleSerializable{

  val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)

  private val moduleMaps = new mutable.HashMap[String, Class[_]]()
  private val classMaps = new mutable.HashMap[Class[_], String]()
  private val deserializerMaps = new mutable.HashMap[String, ModuleSerializable]()
  private val serializerMaps = new mutable.HashMap[Class[_], ModuleSerializable]()

  // generic type definition for type matching

  var tensorNumericType : universe.Type = null
  var tensorType : universe.Type = null
  var regularizerType : universe.Type = null
  var abstractModuleType : universe.Type = null
  var moduleType : universe.Type = null

  init

  override def loadModule[T: ClassTag](model : BigDLModule)
    (implicit ev: TensorNumeric[T]) : ModuleData[T] = {

    val evidence = scala.reflect.classTag[T]
    val modelAttributes = model.getAttrMap
    val moduleType = model.getModuleType
    val cls = ModuleSerializer.getModuleClsByType(moduleType)
    val constructors = cls.getConstructors()
    require(constructors.length == 1, "only support one constructor")
    val constructor = constructors(0)
    val constructorFullParams = getCostructorFullParams(cls)
    val args = new Array[Object](constructorFullParams(0).size + constructorFullParams(1).size)
    var i = 0;
    constructorFullParams.foreach(map => {
      map.foreach(param => {
        val name = param.name.decodedName.toString
        val ptype = param.typeSignature
        if (ptype.toString == "scala.reflect.ClassTag[T]") {
          args(i) = evidence
        } else if (ptype.toString ==
          tensorNumericType.toString) {
          args(i) = ev
        } else {
          require(modelAttributes.containsKey(name), s"$name value cannot be found")
          val attribute = modelAttributes.get(name)
          val value = DataConverter.getAttributeValue(attribute)
          args(i) = value
        }
        i+= 1
      })
    })

    val module = constructor.newInstance(args : _*).
      asInstanceOf[AbstractModule[Activity, Activity, T]]
    createBigDLModule(model, module)
  }

  override def serializeModule[T: ClassTag](module : ModuleData[T])
                                           (implicit ev: TensorNumeric[T]) : BigDLModule = {
    val bigDLModelBuilder = BigDLModule.newBuilder
    val cls = module.module.getClass
    val moduleType = getModuleTypeByCls(cls)
    bigDLModelBuilder.setModuleType(moduleType)
    val constructors = cls.getConstructors()
    require(constructors.length == 1, "only support one constructor")
    val constructor = constructors(0)
    val fullParams = getCostructorFullParams(cls)
    val clsTag = scala.reflect.classTag[T]
    val constructorParams = fullParams(0)
    constructorParams.foreach(param => {
      val paramName = param.name.decodedName.toString
      var ptype = param.typeSignature
      val attrBuilder = AttrValue.newBuilder
      val field = cls.getDeclaredField(paramName)
      field.setAccessible(true)
      val fieldValue = field.get(module.module)
      DataConverter.setAttributeValue(attrBuilder, fieldValue, ptype)
      bigDLModelBuilder.putAttr(paramName, attrBuilder.build)
    })
    copyFromBigDL(module, bigDLModelBuilder)
    createSerializeBigDLModule(bigDLModelBuilder, module)
  }


  def serialize[T: ClassTag](bigDLModule : ModuleData[T])
                            (implicit ev: TensorNumeric[T])
    : BigDLModule = {
    val module = bigDLModule.module
    val cls = module.getClass
    serializerMaps(cls).serializeModule(bigDLModule)
  }

  def load[T: ClassTag](model: BigDLModule)
                       (implicit ev: TensorNumeric[T]) : ModuleData[T] = {
    deserializerMaps(model.getModuleType).loadModule(model)
  }



  def registerModule(moduleType : String, moduleCls : Class[_],
    serializer : ModuleSerializable) : Unit = {
    moduleMaps(moduleType) = moduleCls
    classMaps(moduleCls) = moduleType
    serializerMaps(moduleCls) = serializer
    deserializerMaps(moduleType) = serializer
  }

  def getModuleClsByType(moduleType : String) : Class[_] = {
    require(moduleMaps.contains(moduleType), s"$moduleType is not supported")
    moduleMaps(moduleType)
  }

  def getModuleTypeByCls(cls : Class[_]) : String = {
    require(classMaps.contains(cls), s"$cls is not supported")
    classMaps(cls)
  }

  def getCostructorFullParams[T : ClassTag](cls : Class[_]) : List[List[universe.Symbol]] = {

    val clsSymbol = runtimeMirror.classSymbol(cls)
    val cm = runtimeMirror.reflectClass(clsSymbol)
    // to make it compatible with both 2.11 and 2.10
    val ctorC = clsSymbol.toType.declaration(universe.nme.CONSTRUCTOR).asMethod
    val ctorm = cm.reflectConstructor(ctorC)
    ctorm.symbol.paramss
  }

  private def init() : Unit = {
    registerAllModules
    initializeDeclaredTypes
  }

  private def registerAllModules : Unit = {
    registerModule("Abs", Class.forName("com.intel.analytics.bigdl.nn.Abs"), Abs)
    registerModule("Add", Class.forName("com.intel.analytics.bigdl.nn.Add"), Add)
    registerModule("AddConstant", Class.forName("com.intel.analytics.bigdl.nn.AddConstant"),
      AddConstant)
    registerModule("BatchNormalization",
      Class.forName("com.intel.analytics.bigdl.nn.BatchNormalization"), BatchNormalization)
    registerModule("Bilinear", Class.forName("com.intel.analytics.bigdl.nn.Bilinear"), Bilinear)
    // Place holder for birecurrent
    registerModule("Bottle", Class.forName("com.intel.analytics.bigdl.nn.Bottle"), Bottle)
    registerModule("CAdd", Class.forName("com.intel.analytics.bigdl.nn.CAdd"), CAdd)
    registerModule("CAddTable", Class.forName("com.intel.analytics.bigdl.nn.CAddTable"), CAddTable)
    registerModule("CDivTable", Class.forName("com.intel.analytics.bigdl.nn.CDivTable"), CDivTable)
    registerModule("Clamp", Class.forName("com.intel.analytics.bigdl.nn.Clamp"), Clamp)
    registerModule("CMaxTable", Class.forName("com.intel.analytics.bigdl.nn.CMaxTable"), CMaxTable)
    registerModule("CMinTable", Class.forName("com.intel.analytics.bigdl.nn.CMinTable"), CMinTable)
    registerModule("CMul", Class.forName("com.intel.analytics.bigdl.nn.CMul"), CMul)
    registerModule("CMulTable", Class.forName("com.intel.analytics.bigdl.nn.CMulTable"), CMulTable)
    registerModule("Concat", Class.forName("com.intel.analytics.bigdl.nn.Concat"), Concat)
    registerModule("ConcatTable", Class.forName("com.intel.analytics.bigdl.nn.ConcatTable"),
      ConcatTable)
    registerModule("Contiguous", Class.forName("com.intel.analytics.bigdl.nn.Contiguous"),
      Contiguous)
    registerModule("Cosine", Class.forName("com.intel.analytics.bigdl.nn.Cosine"),
      Cosine)
    registerModule("CosineDistance", Class.forName("com.intel.analytics.bigdl.nn.CosineDistance"),
      CosineDistance)
    registerModule("CSubTable", Class.forName("com.intel.analytics.bigdl.nn.CSubTable"),
      CSubTable)
    registerModule("DotProduct", Class.forName("com.intel.analytics.bigdl.nn.DotProduct"),
      DotProduct)
    registerModule("Dropout", Class.forName("com.intel.analytics.bigdl.nn.Dropout"),
      Dropout)
    registerModule("Echo", Class.forName("com.intel.analytics.bigdl.nn.Echo"),
      Echo)
    registerModule("ELU", Class.forName("com.intel.analytics.bigdl.nn.ELU"),
      ELU)
    registerModule("Euclidean", Class.forName("com.intel.analytics.bigdl.nn.Euclidean"),
      Euclidean)
    registerModule("Exp", Class.forName("com.intel.analytics.bigdl.nn.Exp"),
      Exp)
    registerModule("FlattenTable", Class.forName("com.intel.analytics.bigdl.nn.FlattenTable"),
      FlattenTable)
    registerModule("GradientReversal", Class.forName
    ("com.intel.analytics.bigdl.nn.GradientReversal"), GradientReversal)
    // palce holder for Graph
    registerModule("GRU", Class.forName("com.intel.analytics.bigdl.nn.GRU"), GRU)
    registerModule("HardShrink", Class.forName("com.intel.analytics.bigdl.nn.HardShrink"),
      HardShrink)
    registerModule("HardTanh", Class.forName("com.intel.analytics.bigdl.nn.HardTanh"), HardTanh)
    registerModule("Identity", Class.forName("com.intel.analytics.bigdl.nn.Identity"), Identity)
    registerModule("Index", Class.forName("com.intel.analytics.bigdl.nn.Index"), Index)
    registerModule("InferReshape", Class.forName("com.intel.analytics.bigdl.nn.InferReshape"),
      InferReshape)
    registerModule("JoinTable", Class.forName("com.intel.analytics.bigdl.nn.JoinTable"), JoinTable)
    registerModule("L1Penalty", Class.forName("com.intel.analytics.bigdl.nn.L1Penalty"), L1Penalty)
    registerModule("LeakyReLU", Class.forName("com.intel.analytics.bigdl.nn.LeakyReLU"), LeakyReLU)
    registerModule("Linear", Class.forName("com.intel.analytics.bigdl.nn.Linear"), Linear)
    registerModule("Log", Class.forName("com.intel.analytics.bigdl.nn.Log"), Log)
    registerModule("LogSigmoid", Class.forName("com.intel.analytics.bigdl.nn.LogSigmoid"),
      LogSigmoid)
    registerModule("LogSoftMax", Class.forName("com.intel.analytics.bigdl.nn.LogSoftMax"),
      LogSoftMax)
    registerModule("LookupTable", Class.forName("com.intel.analytics.bigdl.nn.LookupTable"),
      LookupTable)
    registerModule("LSTM", Class.forName("com.intel.analytics.bigdl.nn.LSTM"),
      LSTM)

  }

  private def initializeDeclaredTypes() : Unit = {

    val tensorNumericCls = Class.
      forName("com.intel.analytics.bigdl.tensor.TensorNumericMath$TensorNumeric")
    tensorNumericType = runtimeMirror.
      classSymbol(tensorNumericCls).selfType

    val tensorCls = Class.forName("com.intel.analytics.bigdl.tensor.Tensor")
    tensorType = runtimeMirror.
      classSymbol(tensorCls).selfType

    val regularizerCls = Class.forName("com.intel.analytics.bigdl.optim.Regularizer")
    regularizerType = runtimeMirror.
      classSymbol(regularizerCls).selfType

    val abstractModuleCls = Class.forName("com.intel.analytics.bigdl.nn.abstractnn.AbstractModule")
    abstractModuleType = runtimeMirror.classSymbol(abstractModuleCls).selfType

  }
}

