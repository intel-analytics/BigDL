package com.intel.webscaleml.nn.nn

import com.intel.webscaleml.nn.optim.SGD
import com.intel.webscaleml.nn.tensor.{T, Tensor, torch}
import org.scalatest.{Matchers, FlatSpec}

class BCECriterionSpec extends FlatSpec with Matchers {

  "BCECriterion " should "return return right output and gradInput" in {
    val criterion = new BCECriterion[Double]()
    val output = torch.Tensor[Double](3)
    output(Array(1)) = 0.4
    output(Array(2)) = 0.5
    output(Array(3)) = 0.6
    val target = torch.Tensor[Double](3)
    target(Array(1)) = 0
    target(Array(2)) = 1
    target(Array(3)) = 1

    val loss = criterion.forward(output, target)
    loss should be (0.57159947 +- 1e-8)
    val gradInput = criterion.backward(output, target)
    gradInput(Array(1)) should be ( 0.5556 +- 0.0001)
    gradInput(Array(2)) should be (-0.6667 +- 0.0001)
    gradInput(Array(3)) should be (-0.5556 +- 0.0001)

  }

  "Binary LR " should "converge correctly" in {
    def specifiedModel() : Module[Double] = {
      val model = new Sequential[Double]()
      val linear = new Linear[Double](2, 1)
      linear.weight(Array(1, 1)) = 0.1
      linear.weight(Array(1, 2)) = - 0.6
//      linear.weight(Array(1, 3)) = 0.5
      linear.bias(Array(1)) = 0.05
      model.add(linear)
      model.add(new Sigmoid())
      model
    }

    def getTrainModel() : Module[Double] = {
      val model = new Sequential[Double]()
      model.add(new Linear[Double](2, 1))
      model.add(new Sigmoid[Double]())
      model
    }

    def feval(grad : Tensor[Double], module : Module[Double], criterion : Criterion[Double], input : Tensor[Double], target : Tensor[Double])(weights : Tensor[Double])
    : (Double, Tensor[Double]) = {
      module.training()
      grad.zero()
      val output = module.forward(input)
      val loss = criterion.forward(output, target)
      val gradOut = criterion.backward(output, target)
      module.backward(input, gradOut)
      (loss, grad)
    }

    val actualModel = specifiedModel()
    val trainSize = 100000
    val testSize = 10000

    val inputs = torch.Tensor[Double](trainSize, 2)
    val r = new scala.util.Random(1)
    inputs.apply1(v => r.nextDouble())
//    val mean = inputs.mean()
//    inputs.apply1(v => v - mean)

    val targets = actualModel.forward(inputs).resize(Array(trainSize)).apply1(v => Math.round(v))

    val trainModel = getTrainModel()
    val criterion = new BCECriterion[Double]()
    val (masterWeights, masterGrad) = trainModel.getParameters()
    //    val optm = new torchLBFGS()
    val optm = new SGD[Double]()
    val config = T ("learningRate" -> 10.0, "weightDecay" -> 0.0,
      "momentum" -> 0.0, "learningRateDecay" -> 0.0)

    val batchSize = 1000
    var epoch = 1
    while (epoch < 1000) {
      var i = 1
      var l = 0.0
      while (i <= inputs.size(1)) {
        val (grad, loss) = optm.optimize(feval(masterGrad, trainModel, criterion, inputs.narrow(1, i, batchSize), targets.narrow(1, i, batchSize)), masterWeights, config, config)
        l += loss(0)
        i += batchSize
      }
      println(l / inputs.size(1) * batchSize)
      if(l / inputs.size(1) * batchSize < 8e-3) epoch += 1
    }

    val testData = torch.Tensor[Double](testSize, 2)
    testData.apply1(v => r.nextDouble())
    val testTarget = actualModel.forward(testData).apply1(v => Math.round(v))

    val testResult = trainModel.forward(testData).apply1(v => Math.round(v))

    var corrects = 0
    var i = 1
    while(i <= testSize){
      if(testTarget(Array(i, 1))== testResult(Array(i, 1))) corrects += 1
      i += 1
    }

    println(corrects)



  }


}
