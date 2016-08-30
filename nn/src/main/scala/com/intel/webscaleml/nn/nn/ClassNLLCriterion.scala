package com.intel.webscaleml.nn.nn

import com.intel.webscaleml.nn.tensor.TensorNumericMath.TensorNumeric
import com.intel.webscaleml.nn.tensor.{torch, Tensor}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.reflect.ClassTag

class ClassNLLCriterion[@specialized(Float, Double) T:ClassTag](weights: Tensor[T] = null, sizeAverage:Boolean = true)
                                                               (implicit ev: TensorNumeric[T]) extends Criterion[T] {
  private var gradInput : Tensor[T] = torch.Tensor[T]()
  private var total_weight = ev.fromType[Int](0)
  if(weights != null) require(weights.dim() == 1, "weights input should be 1-D Tensor")

  @transient
  private var results : Array[Future[(T, T)]] = null
  @transient
  private var resultsBackward : Array[Future[Unit]] = null

  override  def updateOutput(input: Tensor[T], target: Tensor[T]): T = {
    require(input.dim() == 1 || input.dim() == 2, "input tensor should be 1D or 2D")
    val nClasses = input.size(input.dim())
    if(input.dim() == 1) {
      val curTarget = ev.toType[Int](target.valueAt(1))
      assert(curTarget >= 1 && curTarget <= nClasses)
      total_weight = if(weights != null)  weights(Array(curTarget)) else ev.fromType[Int](1)
      output = ev.times(ev.negative(input.valueAt(curTarget)), total_weight)
    } else if(input.dim() == 2) {
      val batchSize = input.size(1)
      total_weight = ev.fromType[Int](0)
      output = ev.fromType[Int](0)

      if(results == null || results.length != batchSize) {
        results = new Array[Future[(T, T)]](batchSize)
      }

      var i = 1
      while(i <= batchSize) {
        val _i = i
        results(_i - 1) = Future {
          val curTarget = ev.toType[Int](target.valueAt(_i))
          assert(curTarget >= 1 && curTarget <= nClasses, s"curTarget ${curTarget} is out of range 1 to ${nClasses}")
          val curWeight = if (weights != null) weights.valueAt(curTarget) else ev.fromType[Int](1)
          (ev.times(input.valueAt(_i, curTarget), curWeight), curWeight)
        }
        i += 1
      }

      i = 0
      while(i < batchSize) {
        val (o, w) = Await.result(results(i), Duration.Inf)
        output = ev.minus(output, o)
        total_weight = ev.plus(total_weight, w)
        i += 1
      }
    }
    if(sizeAverage && total_weight != 0) {
      output = ev.divide(output, total_weight)
    }
    output
  }

  override  def updateGradInput(input: Tensor[T], target: Tensor[T]): Tensor[T] = {
    require(input.dim() == 1 || input.dim() == 2, "input tensor should be 1D or 2D")
    assert(ev.toType[Double](total_weight) > 0.0, "total weight must larger than 0")
    gradInput.resizeAs(input)
    gradInput.zero()

    if(input.dim() == 1) {
      val curTarget = ev.toType[Int](target.valueAt(1))
      gradInput.setValue(curTarget, if(weights!=null) ev.times(ev.fromType[Int](-1), weights.valueAt(curTarget)) else ev.fromType[Int](-1))
      if (sizeAverage) gradInput.setValue(curTarget, ev.divide(gradInput.valueAt(curTarget), total_weight))
    }
    else if(input.dim() == 2) {
      val batchSize = input.size(1)
      if(resultsBackward == null || resultsBackward.length != batchSize) {
        resultsBackward = new Array[Future[Unit]](batchSize)
      }

      var i = 1
      while(i <= batchSize) {
        val _i = i
        resultsBackward(_i - 1) = Future {
          val curTarget = ev.toType[Int](target.valueAt(_i))
          gradInput.setValue(_i, curTarget, if (weights != null) ev.times(ev.fromType[Int](-1),
            weights.valueAt(curTarget)) else ev.fromType[Int](-1))
          if (sizeAverage) gradInput.setValue(_i, curTarget, ev.divide(gradInput.valueAt(_i, curTarget), total_weight))
        }
        i += 1
      }

      i = 0
      while(i < batchSize) {
        Await.result(resultsBackward(i), Duration.Inf)
        i += 1
      }
    }
    gradInput
  }
}
