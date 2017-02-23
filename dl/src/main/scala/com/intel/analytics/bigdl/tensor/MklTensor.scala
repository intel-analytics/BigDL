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

package com.intel.analytics.bigdl.tensor

import breeze.linalg.{DenseMatrix => BrzDenseMatrix, DenseVector => BrzDenseVector}
import org.apache.spark.mllib.linalg.{DenseMatrix, DenseVector, Matrix, Vector}
import com.intel.analytics.bigdl.mkl.MklDnnFloat
import com.intel.analytics.bigdl.nn.dnn.{MklDataSize, MklLayout}
import com.intel.analytics.bigdl.tensor.TensorNumericMath.TensorNumeric
import com.intel.analytics.bigdl.utils.Table

import scala.reflect.ClassTag

class MklTensor[T: ClassTag]()(implicit ev: TensorNumeric[T]) extends Tensor[T] {

  // TODO delete this method
  override def isMklTensor(): Boolean = true

  // storage used by mkl dnn must be allocated with dnnAllocateBuffer_F32/F64
  // if not, the result will not be consistent and reproducibility
  private[this] var _interStorage = 0L
  private[this] var _mklStorage = 0L

  private[this] var _usrStorage = Storage[T]()
  private[this] var _usrStorageOffset: Int = 1

  private[this] var _layoutUsr: Long = 0L // usr layout ptr
  private[this] var _layoutMkl: Long = 0L
  private[this] var _layoutInternal: Long = 0L // the mkl layout of previous layer or next layer

  private[this] var _mklToUsr: Long = 0L // convert mkl layout mem to scala layout mem
  private[this] var _usrToMkl: Long = 0L // convert scala layout mem to mkl layout mem

  private[this] var _internalToMkl: Long = 0L

  var hasConverted = false
  def setConverted(value: Boolean): Unit = {
    hasConverted = value
  }

  var _size = Array[Int]()
  var _stride = Array[Int]()

  var nDimension: Int = 0
  var inplace = true

  /**
   * If other is not a MklTensor, then we set usr storage to other.storage.
   * Otherwise create an internal layout and conversion primitive
   * between other.mklStorage and this.mklStorage.
   *
   * If the memory needs not be convert between internal and current mkl storage,
   * it will create a storage (need more memory) depends on in-place or
   * out-of-place, which is useful for mkl.ReLU.
   *
   * TODO WHY WE ADD USRONLY?
   *
   * @param other
   * @param inplace if it's true, it will not create a new storage and copy
   * @return current mkl tensor
   */
  def set(other: Tensor[T], inplace: Boolean): Tensor[T] = {
    if (other.isMklTensor()) {
      val otherTensor = other.asInstanceOf[MklTensor[T]]
      val currLayout = this.layoutMkl
      val otherLayout = other.asInstanceOf[MklTensor[T]].layoutMkl
      val isSame = MklDnnFloat.layoutCompare(currLayout, otherLayout)

      if (isSame != 1 && internalToMkl == 0) { // TODO internalToMkl primitive only set once
        setInternalToMkl(createOne(otherLayout, currLayout, isSame, internalToMkl))
      }

      // should not call mklStorage set method anyway for the logic consistency
      val newStorage = if (inplace) {
        otherTensor.mklStorage()
      } else {
        // out-of-place exists performance issue
        // TODO mem leak
        this.inplace = false

        ev.getType() match {
          case FloatType =>
            val buffer = if (interStorage == 0) {
              MklDnnFloat.allocateBuffer(otherTensor.layoutMkl)
            } else {
              interStorage
            }

            val size = MklDnnFloat.layoutGetMemorySize(layoutMkl)
            MklDnnFloat.buffercpy(buffer, otherTensor.mklStorage(), size)

            buffer
          case _ =>
            throw new UnsupportedOperationException(s"Only Float supported")
        }
      }

      setInterStorage(newStorage)
    } else {
      setUsrStorage(other.storage())
      setUsrStorageOffset(other.storageOffset())
    }
    this
  }

  override def set(other: Tensor[T]): Tensor[T] = {
    this.set(other, true)
  }

  def createUsrLayout(dimension: Int, size: Array[Long], strides: Array[Long]): Unit = {
    ev.getType() match {
      case FloatType =>
        if (layoutUsr == 0) { // TODO if not equal to 0, we will delete and re-create.
          setLayoutUsr(MklDnnFloat.layoutCreate(dimension, size, strides))
        }
      case _ =>
        throw new UnsupportedOperationException(s"Only Float supported")
    }
  }

  def createMklLayout(primitive: Long, resType: Int): Unit = {
    if (primitive != 0) {
      ev.getType() match {
        case FloatType =>
          setLayoutMkl(MklDnnFloat.layoutCreateFromPrimitive(primitive, resType))
        case _ =>
          throw new UnsupportedOperationException(s"Only Float supported")
      }
    }
    require(layoutMkl != 0, s"create mkl layout failed.")

    setMklStorage(MklDnnFloat.allocateBuffer(layoutMkl))
    // TODO we should replace the println to standard log
    val size = MklDnnFloat.layoutGetMemorySize(layoutMkl) / MklDataSize.FLOAT
    require(size != 0, "allocate buffer failed.")
    println(s"resize mkl storage " +
      s"${MklDnnFloat.layoutGetMemorySize(layoutMkl) / MklDataSize.FLOAT} floats")
  }

  /**
   * check whether conversion prim has been created, and create a new one as return
   * @param src src layout
   * @param dst dst layout
   * @param same the layouts are same or not
   * @param prim old conversion primitive
   * @return a new primitive, if the layouts are same, return 0
   */
  private[this] def createOne(src: Long, dst: Long, same: Int, prim: Long): Long = {
    import scala.language.implicitConversions
    implicit def bool2int(b: Boolean) = if (b) 1 else 0

    val ret = if (src != 0 && dst != 0 && same != 1) {
      ev.getType() match {
        case FloatType =>

          if (prim != 0) {
            MklDnnFloat.deletePrimitive(prim)
          }

          val conversion = MklDnnFloat.conversionCreate(src, dst)
          require(conversion != 0, "create mkl dnn conversion (mkl <-> usr) failed.")
          conversion
        case _ =>
          throw new UnsupportedOperationException(s"Only Float supported")
      }
    } else {
      0L
    }

    ret
  }

  def createConversion(): Unit = {
    // be care of MklDnnFloat.layoutCompare return value, if two layout are same,
    // it will return 1
//    val usrSameMkl = ev.getType() match {
//      case "Float" => MklDnnFloat.layoutCompare(layoutUsr, layoutMkl)
//      case _ =>
//        throw new UnsupportedOperationException(s"Only Float supported")
//    }
    val usrSameMkl = 0

    // if usrToMkl or mklToUsr has been created, we should delete it first
    setMklToUsr(createOne(layoutMkl, layoutUsr, usrSameMkl, mklToUsr))
    setUsrToMkl(createOne(layoutUsr, layoutMkl, usrSameMkl, usrToMkl))
  }

  def createConversion(layout: MklLayout,
                       primitive: Long,
                       resType: Int): Unit = {
    createUsrLayout(layout.dimension, layout.size, layout.strides)
    createMklLayout(primitive, resType)
    createConversion()
  }

  /**
   * if current tensor has an internal tensor, it will ignores the dense tensor
   *
   * @return the mkl storage
   */
  def getConvertedStorage(): Long = {
    // 1. if internal storage exists, check the internal layout, then convert it to mkl storage
    // 2. otherwise convert usr storage to mkl storage

    // internal storage layout is same as mkl storage layout
    if (!hasConverted) {
      val retStorage = if (internalToMkl == 0 && interStorage != 0) {
        interStorage
      } else if (internalToMkl != 0) {
        ev.getType() match {
          case FloatType => MklDnnFloat.conversionExecuteMklToMkl(interStorage, mklStorage,
            internalToMkl)
          case _ => throw new UnsupportedOperationException(s"Only Float is supported.")
        }

        mklStorage
      } else if (usrToMkl != 0) {
        // It will always create usr->mkl conversion.
        ev.getType() match {
          case FloatType => MklDnnFloat.conversionExecuteToMkl(
            usrStorage.array().asInstanceOf[Array[Float]],
            usrStorageOffset - 1, mklStorage, usrToMkl)
          case _ => throw new UnsupportedOperationException(s"Only Float is supported.")
        }

        mklStorage
      } else {
        // never reach here
        throw new RuntimeException(s"Something wrong with primitive creation.")
      }
      retStorage
    } else {
      mklStorage
    }
  }

  def backToUsr(usr: Tensor[T]): Unit = {
    require(mklToUsr != 0, s"Somthing wrong with primitive creation.")

    MklDnnFloat.conversionExecuteToUsr(
      usr.storage().array().asInstanceOf[Array[Float]],
      usr.storageOffset() - 1,
      mklStorage(),
      mklToUsr)
  }

  def backToUsr(): Storage[T] = {
    require(mklToUsr != 0, s"Somthing wrong with primitive creation.")

    if (usrStorage.length() != size.product) {
      usrStorage.resize(size.product)
    }

    MklDnnFloat.conversionExecuteToUsr(
      usrStorage.array().asInstanceOf[Array[Float]],
      usrStorageOffset - 1,
      mklStorage(),
      mklToUsr)

    usrStorage
  }

  override def storageOffset(): Int = _usrStorageOffset

  override def dim(): Int = nDimension

  override def nElement(): Int = {
    if (this.nDimension == 0) {
      0
    } else {
      var n = 1
      var d = 0
      while (d < this.nDimension) {
        n = n * this._size(d)
        d += 1
      }
      n
    }
  }

  override def size(): Array[Int] = _size.slice(0, this.nDimension)

  override def size(dim: Int): Int = {
    require(dim > 0 && dim <= this.nDimension,
      s"dimension ${dim} out of range of ${this.nDimension}D tensor")
    _size(dim - 1)
  }

  override def stride(): Array[Int] = _stride.clone()

  override def stride(dim: Int): Int = {
    require(dim > 0 && dim <= this.nDimension,
      s"dimension ${dim} out of range of ${this.nDimension}D tensor")
    _stride(dim - 1)
  }

  override def resizeAs(src: Tensor[_]): Tensor[T] = {
    this._size = new Array[Int](src.size().length)
    Array.copy(src.size(), 0, _size, 0, src.size().length)
    this._stride = new Array[Int](src.stride().length)
    Array.copy(src.stride(), 0, _stride, 0, src.stride().length)
    this.nDimension = src.nDimension()
    this
  }

  override def resize(sizes: Array[Int], strides: Array[Int] = null): Tensor[T] = {
    this._size = new Array[Int](sizes.length)
    Array.copy(sizes, 0, _size, 0, sizes.length)
    this._stride = new Array[Int](sizes.length)
    if (strides == null) {
      this._stride(0) = 1
      for (i <- 1 until sizes.length) {
        this._stride(i) = this._stride(i - 1) * this._size(i - 1)
      }
    } else {
      Array.copy(strides, 0, _stride, 0, strides.length)
    }
    this.nDimension = sizes.length
    this
  }

  override def zero(): Tensor[T] = {
    ev.getType() match {
      case FloatType => MklDnnFloat.setZero(mklStorage(),
        MklDnnFloat.layoutGetMemorySize(layoutMkl))
      case _ => throw new UnsupportedOperationException(s"Only Float supported")
    }
    this
  }

  override def apply1(func: T => T): Tensor[T] = {
    val func2 = new TensorFunc2[T] {
      override def apply(data: Array[T], index: Int): Unit = {
        data(index) = func(data(index))
      }
    }
    DenseTensorApply.apply1[T](this, func2)
    this
  }

  // {{ getter && setter

  def layoutUsr: Long = _layoutUsr
  def setLayoutUsr(value: Long): Unit = {
    _layoutUsr = value
  }

  def layoutMkl: Long = _layoutMkl
  def setLayoutMkl(value: Long): Unit = {
    _layoutMkl = value
  }

  private[this] def layoutInternal: Long = _layoutInternal
  private[this] def setLayoutInternal(value: Long): Unit = {
    _layoutInternal = value
  }

  private[this] def internalToMkl: Long = _internalToMkl
  private[this] def setInternalToMkl(value: Long): Unit = {
    _internalToMkl = value
  }

  def mklToUsr: Long = _mklToUsr
  def setMklToUsr(value: Long): Unit = {
    _mklToUsr = value
  }

  def usrToMkl: Long = _usrToMkl
  def setUsrToMkl(value: Long): Unit = {
    _usrToMkl = value
  }

  def interStorage: Long = _interStorage
  def setInterStorage(value: Long): Unit = {
    _interStorage = value
  }

  def usrStorage: Storage[T] = _usrStorage
  def setUsrStorage(value: Storage[T]): Unit = {
    _usrStorage = value
  }

  def usrStorageOffset: Int = _usrStorageOffset
  def setUsrStorageOffset(value: Int): Unit = {
    _usrStorageOffset = value
  }

  def mklStorage(): Long = _mklStorage
  def setMklStorage(value: Long): Unit = {
    _mklStorage = value
  }

  /**
   * the storage will convert the storage to dense tensor defaultly.
   * @return storage
   */
  def storage: Storage[T] = {
    backToUsr()
  }
  def setStorage(value: Storage[T]): Unit = {
    _usrStorage = value
  }

  def release(): Unit = {
    ev.getType() match {
      case FloatType =>
        for (primitive <- List(usrToMkl, mklToUsr, internalToMkl)) {
          if (primitive != 0) {
            MklDnnFloat.deletePrimitive(primitive)
          }
        }

        setUsrToMkl(0L)
        setMklToUsr(0L)
        setInternalToMkl(0L)

        // if mkl storage comes from another Tensor, we should not free it twice.
        if (mklStorage() != interStorage) {
          MklDnnFloat.releaseBuffer(mklStorage())
          setMklStorage(0L)
        }

        if (!inplace && interStorage != 0) {
          MklDnnFloat.releaseBuffer(interStorage)
        }

        setInterStorage(0L)

        for (layout <- List(layoutUsr, layoutMkl, layoutInternal)) {
          if (layout != 0) {
            MklDnnFloat.layoutDelete(layout)
          }
        }

        setLayoutInternal(0L)
        setLayoutMkl(0L)
        setLayoutUsr(0L)

      case _ => throw new UnsupportedOperationException(s"Only Float supported")
    }
  }

  // }} ---------------------------------------------

  // unsupport methods

  def returnInt(): Int = {
    require(false, "MklTensor unsupported method")
    0
  }

  def returnTensor(): Tensor[T] = {
    require(false, "MklTensor unsupported method")
    Tensor[T]()
  }

  def returnIntArray(): Array[Int] = {
    require(false, "MklTensor unsupported method")
    Array[Int]()
  }

  def returnT(): T = {
    require(false, "MklTensor unsupported method")
    ev.fromType(0)
  }

  def returnUnit(): Unit = {
    require(false, "MklTensor unsupported method")
  }

  def returnBoolean(): Boolean = {
    require(false, "MklTensor unsupported method")
    false
  }

  def returnThis(): this.type = {
    require(false, "MklTensor unsupported method")
    this
  }

  def returnString(): String = {
    require(false, "MklTensor unsupported method")
    ""
  }

  def returnChar(): Char = {
    require(false, "MklTensor unsupported method")
    'a'
  }

  def returnTuple(): (Tensor[T], Tensor[T]) = {
    require(false, "MklTensor unsupported method")
    (Tensor[T](), Tensor[T]())
  }

  override def squeeze(): Tensor[T] = returnTensor()

  override def squeeze(dim: Int): Tensor[T] = returnTensor()

  override def resize(size1: Int): Tensor[T] = returnTensor()

  override def resize(size1: Int, size2: Int): Tensor[T] = returnTensor()

  override def resize(size1: Int, size2: Int, size3: Int): Tensor[T] = returnTensor()

  override def resize(size1: Int, size2: Int, size3: Int, size4: Int): Tensor[T] = returnTensor()

  override def resize(size1: Int, size2: Int, size3: Int, size4: Int, size5: Int): Tensor[T] =
    returnTensor()

  override def view(sizes: Array[Int]): Tensor[T] = returnTensor()

  override def unfold(dim: Int, size: Int, step: Int): Tensor[T] = returnTensor()

  override def fill(v: T): Tensor[T] = returnTensor()

  override def randn(): Tensor[T] = returnTensor()

  override def bernoulli(p: Double): Tensor[T] = returnTensor()

  override def rand(): Tensor[T] = returnTensor()

  override def set(storage: Storage[T],
                   storageOffset: Int = 1,
                   sizes: Array[Int] = null,
                   strides: Array[Int] = null): Tensor[T] = returnTensor()

  override def set(): Tensor[T] = {
    this
  }

  override def transpose(dim1: Int, dim2: Int): Tensor[T] = returnTensor()

  override def t(): Tensor[T] = returnTensor()

  override def select(dim: Int, index: Int): Tensor[T] = returnTensor()

  override def clone(): Tensor[T] = returnTensor()

  override def copy(other: Tensor[T]): Tensor[T] = returnTensor()

  override def narrow(dim: Int, index: Int, size: Int): Tensor[T] = returnTensor()

  override def map(other: Tensor[T], func: (T, T) => T): Tensor[T] = returnTensor()

  override def apply(index: Int): Tensor[T] = returnTensor()

  override def apply(table: Table): Tensor[T] = returnTensor()

  override def update(table: Table, value: T): Unit = returnUnit()

  override def update(table: Table, src: Tensor[T]): Unit = returnUnit()

  override def update(index: Int, src: Tensor[T]): Unit = returnUnit()

  override def apply(indexes: Array[Int]): T = returnT()

  override def valueAt(d1: Int): T = returnT()

  override def valueAt(d1: Int, d2: Int): T = returnT()

  override def valueAt(d1: Int, d2: Int, d3: Int): T = returnT()

  override def valueAt(d1: Int, d2: Int, d3: Int, d4: Int): T = returnT()

  override def valueAt(d1: Int, d2: Int, d3: Int, d4: Int, d5: Int): T = returnT()

  private def getOffset(z: Int, dim: Int): Int = returnInt()

  override def update(index: Int, value: T): Unit = returnT()

  override def update(indexes: Array[Int], value: T): Unit = returnUnit()

  override def setValue(d1: Int, d2: Int, d3: Int, d4: Int, value: T): this.type = returnThis()

  override def setValue(d1: Int, d2: Int, d3: Int, d4: Int, d5: Int, value: T): this.type =
    returnThis()

  override def setValue(d1: Int, d2: Int, d3: Int, value: T): this.type = returnThis()

  override def setValue(d1: Int, d2: Int, value: T): this.type = returnThis()

  override def setValue(d1: Int, value: T): this.type = returnThis()

  override def update(func: T => Boolean, value: T): Unit = returnUnit()

  override def isContiguous(): Boolean = returnBoolean()

  override def contiguous(): Tensor[T] = returnTensor()

  override def isSameSizeAs(other: Tensor[_]): Boolean = returnBoolean()

  override def split(size: Int, dim: Int): Array[Tensor[T]] = {
    require(false, "MklTensor unsupported method")
    Array[Tensor[T]]()
  }

  // scalastyle:off methodName
  override def +(s: T): Tensor[T] = returnTensor()

  override def +(t: Tensor[T]): Tensor[T] = returnTensor()

  override def -(s: T): Tensor[T] = returnTensor()

  override def -(t: Tensor[T]): Tensor[T] = returnTensor()

  override def unary_-(): Tensor[T] = returnTensor()

  override def /(s: T): Tensor[T] = returnTensor()

  override def /(t: Tensor[T]): Tensor[T] = returnTensor()

  override def *(s: T): Tensor[T] = returnTensor()

  override def *(t: Tensor[T]): Tensor[T] = returnTensor()

  // scalastyle:on methodName

  override def sum(): T = returnT()

  override def sum(dim: Int): Tensor[T] = returnTensor()

  override def sum(x: Tensor[T], dim: Int): Tensor[T] = returnTensor()

  override def mean(): T = returnT()

  override def mean(dim: Int): Tensor[T] = returnTensor()

  override def max(): T = returnT()

  override def max(dim: Int): (Tensor[T], Tensor[T]) = returnTuple()

  override def max(values: Tensor[T], indices: Tensor[T], dim: Int): (Tensor[T], Tensor[T]) =
    returnTuple()

  override def min(): T = returnT()

  override def min(dim: Int): (Tensor[T], Tensor[T]) = returnTuple()

  override def min(values: Tensor[T], indices: Tensor[T], dim: Int): (Tensor[T], Tensor[T]) =
    returnTuple()

  override def scatter(dim: Int, index: Tensor[T], src: Tensor[T]): Tensor[T] = returnTensor()

  override def add(value: T, y: Tensor[T]): Tensor[T] = returnTensor()

  override def add(x: Tensor[T]): Tensor[T] = returnTensor()

  override def add(x: Tensor[T], y: Tensor[T]): Tensor[T] = returnTensor()

  // Puts the result of x + value * y in current tensor
  override def add(x: Tensor[T], value: T, y: Tensor[T]): Tensor[T] = returnTensor()

  override def add(value: T): Tensor[T] = returnTensor()

  override def sub(value: T, y: Tensor[T]): Tensor[T] = returnTensor()

  override def sub(x: Tensor[T]): Tensor[T] = returnTensor()

  override def sub(x: Tensor[T], y: Tensor[T]): Tensor[T] = returnTensor()
  // Puts the result of x - value * y in current tensor
  override def sub(x: Tensor[T], value: T, y: Tensor[T]): Tensor[T] = returnTensor()

  override def sub(value: T): Tensor[T] = returnTensor()

  override def dot(y: Tensor[T]): T = returnT()

  override def cmax(value: T): Tensor[T] = returnTensor()

  override def dist(y: Tensor[T], norm: Int): T = returnT()

  override def addcmul(value: T, tensor1: Tensor[T], tensor2: Tensor[T]): Tensor[T] =
    returnTensor()

  override def addcmul(tensor1: Tensor[T], tensor2: Tensor[T]): Tensor[T] = returnTensor()

  override def addcdiv(value: T, tensor1: Tensor[T], tensor2: Tensor[T]): Tensor[T] =
    returnTensor()

  override def cmul(y: Tensor[T]): Tensor[T] = returnTensor()

  override def cmul(x: Tensor[T], y: Tensor[T]): Tensor[T] = returnTensor()

  override def cdiv(y: Tensor[T]): Tensor[T] = returnTensor()

  override def cdiv(x: Tensor[T], y: Tensor[T]): Tensor[T] = returnTensor()

  override def cmax(y: Tensor[T]): Tensor[T] = returnTensor()

  override def cmax(x: Tensor[T], y: Tensor[T]): Tensor[T] = returnTensor()

  override def mul(x: Tensor[T], value: T): Tensor[T] = returnTensor()

  override def mul(value: T): Tensor[T] = returnTensor()

  override def div(value: T): Tensor[T] = returnTensor()

  override def conv2(kernel: Tensor[T], vf: Char = 'V'): Tensor[T] = returnTensor()

  override def xcorr2(kernel: Tensor[T], vf: Char = 'V'): Tensor[T] = returnTensor()

  override def addmm(v1: T, M: Tensor[T], v2: T, mat1: Tensor[T], mat2: Tensor[T]): Tensor[T] =
    returnTensor()

  override def addmm(M: Tensor[T], mat1: Tensor[T], mat2: Tensor[T]): Tensor[T] = returnTensor()

  override def addmm(mat1: Tensor[T], mat2: Tensor[T]): Tensor[T] = returnTensor()

  override def addmm(v2: T, mat1: Tensor[T], mat2: Tensor[T]): Tensor[T] = returnTensor()

  override def addmm(v1: T, v2: T, mat1: Tensor[T], mat2: Tensor[T]): Tensor[T] = returnTensor()

  override def mm(mat1: Tensor[T], mat2: Tensor[T]): Tensor[T] = returnTensor()

  override def addr(t1: Tensor[T], t2: Tensor[T]): Tensor[T] = returnTensor()

  override def addr(v2: T, t1: Tensor[T], t2: Tensor[T]): Tensor[T] = returnTensor()

  override def addr(v1: T, t1: Tensor[T], v2: T, t2: Tensor[T]): Tensor[T] = returnTensor()

  override def addr(v1: T, t1: Tensor[T], v2: T, t2: Tensor[T], t3: Tensor[T]): Tensor[T] =
    returnTensor()

  override def addmv(beta: T,
                     vec1: Tensor[T],
                     alpha: T,
                     mat: Tensor[T],
                     vec2: Tensor[T]): Tensor[T] = returnTensor()

  override def addmv(beta: T, alpha: T, mat: Tensor[T], vec2: Tensor[T]): Tensor[T] =
    returnTensor()

  override def uniform(args: T*): T = returnT()

  override def repeatTensor(sizes: Array[Int]): Tensor[T] = returnTensor()

  override def expandAs(template: Tensor[T]): Tensor[T] = returnTensor()

  override def expand(sizes: Array[Int]): Tensor[T] = returnTensor()

  override def addmv(alpha: T, mat: Tensor[T], vec2: Tensor[T]): Tensor[T] = returnTensor()

  override def mv(mat: Tensor[T], vec2: Tensor[T]): Tensor[T] = returnTensor()

  override def baddbmm(beta: T,
                       M: Tensor[T],
                       alpha: T,
                       batch1: Tensor[T],
                       batch2: Tensor[T]): Tensor[T] = returnTensor()

  override def baddbmm(beta: T, alpha: T, batch1: Tensor[T], batch2: Tensor[T]): Tensor[T] =
    returnTensor()

  override def baddbmm(alpha: T, batch1: Tensor[T], batch2: Tensor[T]): Tensor[T] = returnTensor()

  override def bmm(batch1: Tensor[T], batch2: Tensor[T]): Tensor[T] = returnTensor()

  override def abs(): Tensor[T] = returnTensor()

  override def toBreezeVector(): BrzDenseVector[T] = throw new UnsupportedOperationException

  override def getType(): TensorDataType = throw new IllegalArgumentException

  override def toMLlibMatrix(): Matrix = throw new UnsupportedOperationException

  override def toBreezeMatrix(): BrzDenseMatrix[T] = throw new UnsupportedOperationException

  override def toMLlibVector(): Vector = throw new UnsupportedOperationException

  override def equals(obj: Any): Boolean = returnBoolean()

  override def hashCode(): Int = returnInt()

  override def toString(): String = returnString()

  override def diff(other: Tensor[T], count: Int, reverse: Boolean): Boolean = returnBoolean()

  override def reshape(sizes: Array[Int]): Tensor[T] = returnTensor()

  override def topk(k: Int,
                    dim: Int,
                    increase: Boolean,
                    result: Tensor[T],
                    indices: Tensor[T]): (Tensor[T], Tensor[T]) = returnTuple()

  override def pow(x: Tensor[T], n: T): Tensor[T] = returnTensor()

  override def pow(n: T): Tensor[T] = returnTensor()

  override def log(x: Tensor[T]): Tensor[T] = returnTensor()

  override def log(): Tensor[T] = returnTensor()

  override def exp(x: Tensor[T]): Tensor[T] = returnTensor()

  override def exp(): Tensor[T] = returnTensor()

  override def sqrt(x: Tensor[T]): Tensor[T] = returnTensor()

  override def sqrt(): Tensor[T] = returnTensor()

  override def log1p(x: Tensor[T]): Tensor[T] = returnTensor()

  override def log1p(): Tensor[T] = returnTensor()

  override def norm(y: Tensor[T], value: Int, dim: Int): Tensor[T] = returnTensor()

  override def abs(x: Tensor[T]): Tensor[T] = returnTensor()

  override def save(path: String, overWrite: Boolean): this.type = returnThis()

  override def maskedFill(mask: Tensor[T], value: T): Tensor[T] = returnTensor()

  override def maskedCopy(mask: Tensor[T], y: Tensor[T]): Tensor[T] = returnTensor()

  override def maskedSelect(mask: Tensor[T], res: Tensor[T]): Tensor[T] = returnTensor()

  override def gt(x: Tensor[T], y: Tensor[T]): Tensor[T] = returnTensor()

  override def lt(x: Tensor[T], y: Tensor[T]): Tensor[T] = returnTensor()

  override def le(x: Tensor[T], y: Tensor[T]): Tensor[T] = returnTensor()

  override def eq(x: Tensor[T], value: T): Tensor[T] = returnTensor()

  override def norm(value: Int): T = returnT()

  override def sign(): Tensor[T] = returnTensor()

  override def addSingletonDimension(t: Tensor[T], dim: Int = 1): Tensor[T] = returnTensor()

  override def ge(x: Tensor[T], value: Double): Tensor[T] = returnTensor()

  override def indexAdd(dim: Int, index: Tensor[T], y: Tensor[T]): Tensor[T] = returnTensor()

  override def index(dim: Int, index: Tensor[T], y: Tensor[T]): Tensor[T] = returnTensor()

  override def gather(dim: Int, index: Tensor[T], src: Tensor[T]): Tensor[T] = returnTensor()

  override def range(xmin: Double, xmax: Double, step: Int): Tensor[T] = returnTensor()
}

