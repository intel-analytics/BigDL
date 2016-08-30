package com.intel.webscaleml.nn.tensor

import java.util
import com.intel.webscaleml.nn.tensor.RandomGenerator._


object TensorNumericMath {
  trait TensorNumeric[@specialized(Float, Double) T] extends Serializable{

    def plus(x: T, y: T): T
    def minus(x: T, y: T): T
    def times(x: T, y: T): T
    def divide(x: T, y: T): T
    def exp(x: T): T
    def log(x: T): T
    def max(x: T, y:T): T
    def sqrt(x: T): T
    def abs(x: T): T
    def negative(x: T): T
    def pow(x: T): T
    def pow(x: T, y: T): T
    def isGreater(x: T, y:T): Boolean

    def rand() : T
    def randn() : T

    def gemm(transa : String, transb : String, m: Int, n: Int, k: Int, alpha: T, a: Array[T],
             aOffset: Int, lda: Int, b: Array[T], bOffset: Int, ldb: Int,
             beta: T, c: Array[T], cOffset: Int, ldc: Int)
    def gemv(trans:String, m:Int, n:Int, alpha:T, a:Array[T], aoffset:Int, lda:Int,
             x:Array[T], xOffset:Int, incx:Int, beta:T, y:Array[T], yOffset:Int, incy:Int)

    def axpy(n: Int, da: T, dx: Array[T], _dx_offset: Int, incx: Int, dy: Array[T], _dy_offset: Int, incy: Int)

    def dot(n: Int, dx: Array[T], _dx_offset: Int, incx: Int, dy: Array[T], _dy_offset: Int, incy: Int): T

    def ger(m: Int, n: Int, alpha: T, x: Array[T], _x_offset: Int, incx: Int, y: Array[T], _y_offset: Int,
             incy: Int, a: Array[T], _a_offset: Int, lda: Int)

    def fill(data: Array[T], fromIndex: Int, toIndex: Int, value: T) : Unit
    def fromType[@specialized(Float, Double, Int) K](k: K)(implicit c: ConvertableFrom[K]): T
    def toType[@specialized(Float, Double, Int) K](t: T)(implicit c: ConvertableTo[K]): K

    def getType(): String
  }

  class TensorNumericOps[@specialized(Float, Double) T](lhs: T)(implicit ev: TensorNumeric[T]) {
    def +(rhs: T): T = ev.plus(lhs, rhs)
    def -(rhs: T): T = ev.minus(lhs, rhs)
    def *(rhs: T): T = ev.times(lhs, rhs)
    def /(rhs: T): T = ev.divide(lhs, rhs)
  }

  object TensorNumeric {

    implicit object TensorNumericFloat extends TensorNumeric[Float] {
      def plus(x: Float, y: Float): Float = x + y
      def minus(x: Float, y: Float): Float = x - y
      def times(x: Float, y: Float): Float = x * y
      def divide(x: Float, y: Float): Float = x / y
      def exp(x: Float): Float = java.lang.Math.exp(x).toFloat
      def log(x: Float): Float = java.lang.Math.log(x).toFloat
      def max(x: Float, y:Float): Float = java.lang.Math.max(x, y)
      def sqrt(x: Float): Float = Math.sqrt(x.toDouble).toFloat
      def abs(x: Float): Float = Math.abs(x)
      def negative(x: Float): Float = -x
      def pow(x: Float): Float = Math.pow(x, -1).toFloat
      def pow(x: Float, y:Float): Float = Math.pow(x, y).toFloat
      def isGreater(x: Float, y:Float): Boolean = (x>y)

      def rand() : Float = RNG.uniform(0,1).toFloat
      def randn() : Float = RNG.normal(0,1).toFloat

      def gemm(transa : String, transb : String, m: Int, n: Int, k: Int, alpha: Float, a: Array[Float],
               aOffset: Int, lda: Int, b: Array[Float], bOffset: Int, ldb: Int,
               beta: Float, c: Array[Float], cOffset: Int, ldc: Int) = {
        DenseTensorBLAS.getTensorBLAS.sgemm(transa, transb, m, n, k, alpha, a, aOffset, lda, b,
          bOffset, ldb, beta, c, cOffset, ldc)
      }
      def gemv(trans:String, m:Int, n:Int, alpha:Float, a:Array[Float], aoffset:Int, lda:Int,
               x:Array[Float], xOffset:Int, incx:Int, beta:Float, y:Array[Float], yOffset:Int, incy:Int) = {
        DenseTensorBLAS.getTensorBLAS.sgemv(trans, m, n, alpha, a, aoffset, lda, x, xOffset, incx, beta, y, yOffset, incy)
      }
      def axpy(n: Int, da: Float, dx: Array[Float], _dx_offset: Int, incx: Int, dy: Array[Float], _dy_offset: Int, incy: Int) = {
        DenseTensorBLAS.getTensorBLAS.saxpy(n, da, dx, _dx_offset, incx, dy, _dy_offset, incy)
      }
      def dot(n: Int, dx: Array[Float], _dx_offset: Int, incx: Int, dy: Array[Float], _dy_offset: Int, incy: Int): Float = {
        DenseTensorBLAS.getTensorBLAS.sdot(n, dx, _dx_offset, incx, dy, _dy_offset, incy)
      }
      def ger(m: Int, n: Int, alpha: Float, x: Array[Float], _x_offset: Int, incx: Int, y: Array[Float], _y_offset: Int,
               incy: Int, a: Array[Float], _a_offset: Int, lda: Int) = {
        DenseTensorBLAS.getTensorBLAS.sger(m, n, alpha, x, _x_offset, incx, y, _y_offset, incy, a, _a_offset, lda)
      }
      def fill(data: Array[Float], fromIndex: Int, toIndex: Int, value: Float) : Unit = {
        util.Arrays.fill(data, fromIndex, toIndex, value)
      }
      def fromType[@specialized(Float, Double, Int) K](k: K)(implicit c: ConvertableFrom[K]) = c.toFloat(k)
      def toType[@specialized(Float, Double, Int) K](t: Float)(implicit c: ConvertableTo[K]) = c.fromFloat(t)

      def getType() = "Float"
    }

    implicit object TensorNumericDouble extends TensorNumeric[Double]{
      def plus(x: Double, y: Double): Double = x + y
      def minus(x: Double, y: Double): Double = x - y
      def times(x: Double, y: Double): Double = x * y
      def divide(x: Double, y: Double): Double = x / y
      def exp(x: Double): Double = java.lang.Math.exp(x)
      def log(x: Double): Double = java.lang.Math.log(x)
      def max(x: Double, y:Double): Double = java.lang.Math.max(x, y)
      def sqrt(x: Double): Double = Math.sqrt(x)
      def abs(x: Double): Double = Math.abs(x)
      def negative(x: Double): Double = -x
      def pow(x: Double): Double = Math.pow(x, -1)
      def pow(x: Double, y: Double): Double = Math.pow(x, y)
      def isGreater(x: Double, y:Double): Boolean = (x>y)

      def rand() : Double = RNG.uniform(0, 1)
      def randn() : Double = RNG.normal(0, 1)

      def gemm(transa : String, transb : String, m: Int, n: Int, k: Int, alpha: Double, a: Array[Double],
               aOffset: Int, lda: Int, b: Array[Double], bOffset: Int, ldb: Int,
               beta: Double, c: Array[Double], cOffset: Int, ldc: Int) = {
        DenseTensorBLAS.getTensorBLAS.dgemm(transa, transb, m, n, k, alpha, a, aOffset, lda, b,
          bOffset, ldb, beta, c, cOffset, ldc)
      }
      def gemv(trans:String, m:Int, n:Int, alpha:Double, a:Array[Double], aoffset:Int, lda:Int,
               x:Array[Double], xOffset:Int, incx:Int, beta:Double, y:Array[Double], yOffset:Int, incy:Int) = {
        DenseTensorBLAS.getTensorBLAS.dgemv(trans, m, n, alpha, a, aoffset, lda, x, xOffset, incx, beta, y, yOffset, incy)
      }
      def axpy(n: Int, da: Double, dx: Array[Double], _dx_offset: Int, incx: Int, dy: Array[Double], _dy_offset: Int, incy: Int) = {
        DenseTensorBLAS.getTensorBLAS.daxpy(n, da, dx, _dx_offset, incx, dy, _dy_offset, incy)
      }
      def dot(n: Int, dx: Array[Double], _dx_offset: Int, incx: Int, dy: Array[Double], _dy_offset: Int, incy: Int): Double = {
        DenseTensorBLAS.getTensorBLAS.ddot(n, dx, _dx_offset, incx, dy, _dy_offset, incy)
      }
      def ger(m: Int, n: Int, alpha: Double, x: Array[Double], _x_offset: Int, incx: Int, y: Array[Double], _y_offset: Int,
              incy: Int, a: Array[Double], _a_offset: Int, lda: Int) = {
        DenseTensorBLAS.getTensorBLAS.dger(m, n, alpha, x, _x_offset, incx, y, _y_offset, incy, a, _a_offset, lda)
      }
      def fill(data: Array[Double], fromIndex: Int, toIndex: Int, value: Double) : Unit = {
        util.Arrays.fill(data, fromIndex, toIndex, value)
      }
      def fromType[@specialized(Float, Double, Int) K](k: K)(implicit c: ConvertableFrom[K]) = c.toDouble(k)
      def toType[@specialized(Float, Double, Int) K](t: Double)(implicit c: ConvertableTo[K]) = c.fromDouble(t)
      def getType() = "Double"
    }
  }
}