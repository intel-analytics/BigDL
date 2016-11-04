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

package com.intel.analytics.sparkdl.nn

import com.intel.analytics.sparkdl.tensor.TensorNumericMath.TensorNumeric
import com.intel.analytics.sparkdl.tensor.Tensor
import com.intel.analytics.sparkdl.utils.Activities
import org.apache.commons.lang3.SerializationUtils

import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._
import com.intel.analytics.sparkdl.mkl.MKL


abstract class TensorModule[@specialized(Float, Double) T: ClassTag]
  (implicit ev: TensorNumeric[T]) extends Module[Tensor[T], Tensor[T], T]

abstract class Module[A <: Activities: ClassTag, B <: Activities: ClassTag,
  @specialized(Float, Double) T: ClassTag](
  implicit ev: TensorNumeric[T]) extends Serializable {
  var output: B = Activities[B, T]().asInstanceOf[B]
  var gradInput: A = Activities[A, T]().asInstanceOf[A]

  var gradWeight: Tensor[T] = null
  var gradBias: Tensor[T] = null
  var gradient: (Tensor[T], Tensor[T]) = (gradWeight, gradBias)

  private var name : String = null

  def setName(name : String) : this.type = {
    this.name = name
    this
  }

  def getName() : String = {
    if (this.name == null) this.getClass.getName else this.name
  }

  private var needComputeBack = true

  def setNeedComputeBack(need: Boolean): this.type = {
    needComputeBack = need
    this
  }

  def isNeedComputeBack(): Boolean = {
    needComputeBack
  }

  // list of sub modules
  val modules: ArrayBuffer[Module[Activities, Activities, T]]
    = ArrayBuffer[Module[Activities, Activities, T]]()

  protected var train: Boolean = true

  protected var forwardTime = 0L

  protected var backwardTime = 0L

  def getTimes(): Array[(Module[_ <: Activities, _ <: Activities, T], Long, Long)] = {
    Array((this, forwardTime, backwardTime))
  }

  def resetTimes(): Unit = {
    forwardTime = 0
    backwardTime = 0
  }

  final def forward(input: A): B = {
    val before = System.nanoTime()
    val result = updateOutput(input)
    forwardTime += System.nanoTime() - before
    result
  }

  def backward(input: A, gradOutput: B): A = {
    val before = System.nanoTime()
    val result = updateGradInput(input, gradOutput)
    accGradParameters(input, gradOutput)
    backwardTime += System.nanoTime() - before
    result
  }

  def updateOutput(input: A): B = {
    this.output = input.asInstanceOf[B]
    output
  }

  def updateOutput(input: A, flag: Int): B = {
    this.output = input.asInstanceOf[B]
    output
  }

  def updateGradInput(input: A, gradOutput: B): A

  def accGradParameters(input: A, gradOutput: B, scale: Double = 1.0): Unit = {}

  def zeroGradParameters(): Unit = {}

  def updateParameters(learningRate: T): Unit = {}

  def getParameters(): (Tensor[T], Tensor[T]) = {
    val (weightParameters, gradParameters) = this.parameters()
    (Module.flatten[T](weightParameters), Module.flatten[T](gradParameters))
  }

  /**
   * @return (Array of weights, Array of grad)
   */
  def parameters(): (Array[Tensor[T]], Array[Tensor[T]]) = null

  def training(): this.type = {
    train = true
    this
  }

  /**
   * Find a module by given a parameter offset
   *
   * @param paramOffset parameter offset in the (weight, grad) vector returned by the
   *                    getParamter function
   * @param indexes     ignore it
   * @return module ref, offset(ignore), indexes from the current module
   */
  def findModel(
    paramOffset: Int,
    indexes: Array[Int] = Array()):
  (Module[_ <: Activities, _ <: Activities, T], Int, Array[Int]) = (this, paramOffset, indexes)

  def evaluate(): this.type = {
    train = false
    this
  }

  final def isTraining(): Boolean = {
    this.train
  }

  def reset(): Unit = {}

  protected var line = "\n"

  def setLine(line: String): this.type = {
    this.line = line
    this
  }

  override def equals(obj: Any): Boolean = {
    if (obj == null) {
      return false
    }
    if (!obj.isInstanceOf[Module[_ <: Activities, _ <: Activities, T]]) {
      return false
    }
    val other = obj.asInstanceOf[Module[_ <: Activities, _ <: Activities, T]]
    if (this.eq(other)) {
      return true
    }
    if (output != other.output) {
      return false
    }
    if (gradInput != other.gradInput) {
      return false
    }
    if (gradWeight == null) {
      if (other.gradWeight != null) {
        return false
      }
    } else {
      if (gradWeight != other.gradWeight) {
        return false
      }
    }
    if (gradBias == null) {
      if (other.gradBias != null) {
        return false
      }
    } else {
      if (gradBias != other.gradBias) {
        return false
      }
    }

    true
  }

  override def hashCode() : Int = {
    val seed = 37
    var hash = 1
    if (output != null) {
      hash = hash * seed + this.output.hashCode()
    }
    if (gradInput != null) {
      hash = hash * seed + this.gradInput.hashCode()
    }
    if (gradWeight != null) {
      hash = hash * seed + this.gradWeight.hashCode()
    }
    if (gradBias != null) {
      hash = hash * seed + this.gradBias.hashCode()
    }

    hash
  }

  def cloneModule(): Module[A, B, T] = {
    SerializationUtils.clone(this)
  }

  // Support for mkl init.
  def getClassPtr() : Long = {0L}
  def getInputPtr() : Long = getClassPtr()
  def getOutputPtr() : Long = getClassPtr()
  var hasSet = false
  def initMkl(prevPtr: Long) : Unit = {
//    println("I WANT TO SET THE PREV LAYOUT IN MODULE")
//    if (prevPtr != 0 && this.getClassPtr() != 0 &&
//        prevPtr != this.getClassPtr()) {
//      ev.getType() match {
//        case "Double" =>
//          MKL.SetPrevDouble(prevPtr, this.getClassPtr())
//        case "Float" =>
//          MKL.SetPrevFloat(prevPtr, this.getClassPtr())
//        case _ =>
//          throw new UnsupportedOperationException(s"Only Float/Double support")
//      }
//    }
  }

  var isPrevMkl = false
  var isNextMKl = false

  private var prevPtr = 0L
  private var nextPtr = 0L

  def setPrevPtr(ptr : Long) : Unit = { prevPtr = ptr }
  def setNextPtr(ptr : Long) : Unit = { nextPtr = ptr }
  def getPrevPtr() : Long = prevPtr
  def getNextPtr() : Long = nextPtr

  var initForward = true
  var initBackward = true

  def updateMklOut(): Unit = {
//     If the layer uses mkl dnn api, the ptr (prevPtr and classPtr) will not equal to 0.
//     And of cause the previous ptr and current ptr will not equal to each other.
//    println("prev = " + getPrevPtr().toHexString + " " +
//            this.getName() + "\tcurrent = " + getClassPtr().toHexString)
    if (getPrevPtr() != 0 && getClassPtr() != getPrevPtr()) {
      ev.getType() match {
        case "Double" =>
          MKL.SetPrevDouble(getPrevPtr(), getInputPtr())
        case "Float" =>
          MKL.SetPrevFloat(getPrevPtr(), getInputPtr())
        case _ =>
          throw new UnsupportedOperationException(s"Only Float/Double support")
      }
    }
  }

  def updateMklGradInput() : Unit = {
//    println("next = " + getNextPtr().toHexString + " " +
//            this.getName() + "\tcurrent = " + getClassPtr().toHexString)
    // when we don't compute the backward, we should convert the gradinput.
//    if (getNextPtr() != 0 && getClassPtr() != getNextPtr() && isNeedComputeBack()) {
    if (getNextPtr() != 0 && getClassPtr() != getNextPtr()) {
      ev.getType() match {
        case "Double" =>
          MKL.SetNextDouble(getNextPtr(), getOutputPtr())
        case "Float" =>
          MKL.SetNextFloat(getNextPtr(), getOutputPtr())
        case _ =>
          throw new UnsupportedOperationException(s"Only Float/Double support")
      }
    }
  }
}

object Module {
  def flatten[@specialized(Float, Double) T: ClassTag](parameters: Array[Tensor[T]])(
    implicit ev: TensorNumeric[T]): Tensor[T] = {
    val compactedTensor = isCompact(parameters)
    if (compactedTensor != null) {
      return compactedTensor
    }
    var i = 0
    var length = 0
    while (i < parameters.length) {
      require(parameters(i).isContiguous())
      length += parameters(i).nElement()
      i += 1
    }

    val result = Tensor[T](length)
    val resultStorage = result.storage()

    i = 0
    var offset = 0
    while (i < parameters.length) {
      System.arraycopy(parameters(i).storage().array(), parameters(i).storageOffset() - 1,
        resultStorage.array(), offset, parameters(i).nElement())
      parameters(i).set(resultStorage, offset + 1, parameters(i).size(), parameters(i).stride())
      offset += parameters(i).nElement()
      i += 1
    }

    result
  }

  def isCompact[@specialized(Float, Double) T: ClassTag](paramters: Array[Tensor[T]])(
    implicit ev: TensorNumeric[T]): Tensor[T] = {
    require(paramters.length > 0)
    var i = 1
    val storage = paramters(0).storage()
    var length = paramters(0).nElement()
    while (i < paramters.length) {
      if (!storage.eq(paramters(i).storage())) {
        return null
      }
      length += paramters(i).nElement()
      i += 1
    }

    if (length != storage.array().length) {
      return null
    }

    return Tensor(storage)
  }
}




