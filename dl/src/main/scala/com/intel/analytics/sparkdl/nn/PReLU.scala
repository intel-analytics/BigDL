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

import com.intel.analytics.sparkdl.tensor.{DenseTensorApply, Tensor, TensorFunc4, TensorFunc6}
import com.intel.analytics.sparkdl.tensor.TensorNumericMath.TensorNumeric
import com.intel.analytics.sparkdl.utils.Engine

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.reflect.ClassTag

/**
 * Applies parametric ReLU, which parameter varies the slope of the negative part.
 *
 * PReLU: f(x) = max(0, x) + a * min(0, x)
 *
 * nOutputPlane's default value is 0, that means using PReLU in shared version and has
 * only one parameters.
 *
 * Notice: Please don't use weight decay on this.
 *
 * @param nOutputPlane input map number. Default is 0.
 */
class PReLU[T: ClassTag](
  val nOutputPlane: Int = 0)
  (implicit ev: TensorNumeric[T]) extends TensorModule[T] {

  val weight: Tensor[T] = if (nOutputPlane == 0) {
    Tensor[T](1).fill(ev.fromType[Double](0.25))
  } else {
    Tensor[T](nOutputPlane).fill(ev.fromType[Double](0.25))
  }
  gradWeight = if (nOutputPlane == 0) {
    Tensor[T](1)
  } else {
    Tensor[T](nOutputPlane)
  }

  @transient private var results: Array[Future[Unit]] = null

  override def updateOutput(input: Tensor[T]): Tensor[T] = {
    require(input.isContiguous(), "input must be contiguous")
    output.resizeAs(input)

    if (nOutputPlane == 0) {
      val w = weight(Array(1))
      val func = new TensorFunc4[T] {
        override def apply (data1: Array[T], offset1: Int, data2: Array[T], offset2: Int): Unit = {
          data1(offset1) = if (ev.isGreater(data2(offset2), ev.fromType[Int](0))) {
            data2(offset2)
          } else {
            ev.times(w, data2(offset2))
          }
        }
      }
      DenseTensorApply.apply2[T](output, input, func)
    } else {
      require(input.nDimension() <= 4, s"${input.nDimension()}D input not supported")
      require(input.size((input.nDimension() + 1) % 2 + 1) == nOutputPlane,
        "wrong number of input planes")

      val (bs, ks) = input.nDimension() match {
        case 1 => (1, 1)
        case 2 => (input.size(1), 1)
        case 3 => (1, input.size(2) * input.size(3))
        case 4 => (input.size(1), input.size(3) * input.size(4))
      }

      val outputArray = output.storage().array()
      val inputArray = input.storage().array()
      val weightArray = weight.storage().array()
      val weightOffset = weight.storageOffset() - 1

      if (results == null || results.length != bs) {
        results = new Array[Future[Unit]](bs)
      }

      var i = 0
      while (i < bs) {
        val _i = i
        results(_i) = Future {
          var nInputOffset = input.storageOffset() - 1 + _i * nOutputPlane * ks
          var nOutputOffset = output.storageOffset() - 1 + _i * nOutputPlane * ks
          var j = 0
          while (j < nOutputPlane) {
            var k = 0
            while (k < ks) {
              outputArray(nOutputOffset + k) = if (ev.isGreater(inputArray(nInputOffset + k),
                ev.fromType[Int](0))) {
                inputArray(nInputOffset + k)
              } else {
                ev.times(weightArray(weightOffset + j), inputArray(nInputOffset + k))
              }
              k += 1
            }
            nInputOffset += ks
            nOutputOffset += ks
            j += 1
          }
        }(Engine.getInstance())
        i += 1
      }
      i = 0
      while (i < bs) {
        Await.result(results(i), Duration.Inf)
        i += 1
      }
    }

    output
  }

  override def updateGradInput(input: Tensor[T], gradOutput: Tensor[T]): Tensor[T] = {
    require(input.isContiguous(), "input must be contiguous")
    require(gradOutput.isContiguous(), "gradOuput must be contiguous")
    require(input.nElement() == gradOutput.nElement())
    gradInput.resizeAs(input)

    if (nOutputPlane == 0) {
      val w = weight(Array(1))
      val func = new TensorFunc6[T] {
        override def apply (data1: Array[T], offset1: Int, data2: Array[T], offset2: Int,
          data3: Array[T], offset3: Int): Unit = {
          data1(offset1) = if (ev.isGreater(data3(offset3), ev.fromType[Int](0))) {
            data2(offset2)
          } else {
            ev.times(w, data2(offset2))
          }
        }
      }
      DenseTensorApply.apply3[T](gradInput, gradOutput, input, func)
    } else {
      require(input.nDimension() <= 4, s"${input.nDimension()}D input not supported")
      require(input.size((input.nDimension() + 1) % 2 + 1) == nOutputPlane,
        "wrong number of input planes")

      val (bs, ks) = input.nDimension() match {
        case 1 => (1, 1)
        case 2 => (input.size(1), 1)
        case 3 => (1, input.size(2) * input.size(3))
        case 4 => (input.size(1), input.size(3) * input.size(4))
      }

      val inputArray = input.storage().array()
      val gradOutputArray = gradOutput.storage().array()
      val weightArray = weight.storage().array()
      val weightOffset = weight.storageOffset() - 1
      val gradInputArray = gradInput.storage().array()

      if (results == null || results.length != bs) {
        results = new Array[Future[Unit]](bs)
      }

      var i = 0
      while (i < bs) {
        val _i = i
        results(_i) = Future {
          var nInputOffset = input.storageOffset() - 1 + _i * nOutputPlane * ks
          var nGradOutputOffset = gradOutput.storageOffset() - 1 + _i * nOutputPlane * ks
          var nGradInputOffset = gradInput.storageOffset() - 1 + _i * nOutputPlane * ks
          var j = 0
          while (j < nOutputPlane) {
            val w = weightArray(weightOffset + j)
            var k = 0
            while (k < ks) {
              gradInputArray(nGradInputOffset + k) = if (ev.isGreater(inputArray(nInputOffset + k),
                ev.fromType[Int](0))) {
                gradOutputArray(nGradOutputOffset + k)
              } else {
                ev.times(w, gradOutputArray(nGradOutputOffset + k))
              }
              k += 1
            }
            nInputOffset += ks
            nGradInputOffset += ks
            nGradOutputOffset += ks
            j += 1
          }
        }(Engine.getInstance())
        i += 1
      }
      i = 0
      while (i < bs) {
        Await.result(results(i), Duration.Inf)
        i += 1
      }
    }
    gradInput
  }

  override def accGradParameters(input: Tensor[T], gradOutput: Tensor[T],
                                 scale: Double = 1.0): Unit = {
    require(input.isContiguous(), "input must be contiguous")
    require(gradOutput.isContiguous(), "gradOuput must be contiguous")
    require(input.nElement() == gradOutput.nElement())

    if (nOutputPlane == 0) {
      var sum = ev.fromType[Int](0)
      val func = new TensorFunc4[T] {
        override def apply (data1: Array[T], offset1: Int, data2: Array[T], offset2: Int): Unit = {
          if (ev.isGreater(ev.fromType[Int](0), data1(offset1))) {
            sum = ev.plus(sum, ev.times(data1(offset1), data2(offset2)))
          }
        }
      }
      DenseTensorApply.apply2[T](input, gradOutput, func)
      gradWeight.add(ev.times(ev.fromType[Double](scale), sum))
    } else {
      require(input.nDimension() <= 4, s"${input.nDimension()}D input not supported")
      require(input.size((input.nDimension() + 1) % 2 + 1) == nOutputPlane,
        "wrong number of input planes")
      val (bs, ks) = input.nDimension() match {
        case 1 => (1, 1)
        case 2 => (input.size(1), 1)
        case 3 => (1, input.size(2) * input.size(3))
        case 4 => (input.size(1), input.size(3) * input.size(4))
      }

      val inputArray = input.storage().array()
      val gradOutputArray = gradOutput.storage().array()
      val gradWeightArray = gradWeight.storage().array()
      val gradWeightOffset = gradWeight.storageOffset() - 1

      if (results == null || results.length != bs) {
        results = new Array[Future[Unit]](bs)
      }

      var i = 0
      while (i < bs) {
        val _i = i
        results(_i) = Future {
          var nInputOffset = input.storageOffset() - 1 + _i * nOutputPlane * ks
          var nGradOutputOffset = gradOutput.storageOffset() - 1 + _i * nOutputPlane * ks

          var j = 0
          while (j < nOutputPlane) {
            var sum = ev.fromType[Int](0)
            var k = 0
            while (k < ks) {
              if (ev.isGreater(ev.fromType[Int](0), inputArray(nInputOffset + k))) {
                sum = ev.plus(sum, ev.times(gradOutputArray(nGradOutputOffset + k),
                  inputArray(nInputOffset + k)))
              }
              k += 1
            }
            gradWeightArray(gradWeightOffset + j) = ev.plus(gradWeightArray(gradWeightOffset + j),
              ev.times(ev.fromType[Double](scale), sum))
            nInputOffset += ks
            nGradOutputOffset += ks
            j += 1
          }
        }(Engine.getInstance())
        i += 1
      }
      i = 0
      while (i < bs) {
        Await.result(results(i), Duration.Inf)
        i += 1
      }
    }
  }

  override def zeroGradParameters(): Unit = {
    gradWeight.zero()
  }

  override def parameters(): (Array[Tensor[T]], Array[Tensor[T]]) = {
    (Array(this.weight), Array(this.gradWeight))
  }

  override def toString(): String = {
    s"nn.PReLU($nOutputPlane)"
  }
}
