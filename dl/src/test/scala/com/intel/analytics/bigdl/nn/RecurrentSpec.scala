/*
 * Licensed to Intel Corporation under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Intel Corporation licenses this file to You under the Apache License, Version 2.0
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

package com.intel.analytics.bigdl.nn

import com.intel.analytics.bigdl.optim.SGD
import com.intel.analytics.bigdl.tensor.Tensor
import com.intel.analytics.bigdl.utils.RandomGenerator.RNG
import com.intel.analytics.bigdl.utils.T
import org.scalatest.{FlatSpec, Matchers}

@com.intel.analytics.bigdl.tags.Parallel
class RecurrentSpec extends FlatSpec with Matchers {

  "A Recurrent Language Model Module " should "converge for inside transpose" in {

    val hiddenSize = 4
    val inputSize = 5
    val outputSize = 5
    val bpttTruncate = 3
    val seed = 100
    RNG.setSeed(seed)

    val model = Sequential[Double]()
    model.add(Recurrent[Double](hiddenSize, bpttTruncate)
      .add(RnnCell[Double](inputSize, hiddenSize))
      .add(Tanh()))
      .add(Select(1, 1))
      .add(Linear[Double](hiddenSize, outputSize))

    val criterion = CrossEntropyCriterion[Double]()
    val logSoftMax = LogSoftMax[Double]()

    val (weights, grad) = model.getParameters()

    val input = Tensor[Double](Array(1, 5, inputSize))
    val labels = Tensor[Double](Array(1, 5))
    for (i <- 1 to 5) {
      val rdmLabel = Math.ceil(RNG.uniform(0.0, 1.0)*inputSize).toInt
      val rdmInput = Math.ceil(RNG.uniform(0.0, 1.0)*inputSize).toInt
      input.setValue(1, i, rdmInput, 1.0)
      labels.setValue(1, i, rdmLabel)
    }

    val state = T("learningRate" -> 0.5, "momentum" -> 0.0,
      "weightDecay" -> 0.0, "dampening" -> 0.0)
    val sgd = new SGD[Double]
    def feval(x: Tensor[Double]): (Double, Tensor[Double]) = {
      val output = model.forward(input).asInstanceOf[Tensor[Double]]
      val _loss = criterion.forward(output, labels)
      model.zeroGradParameters()
      val gradInput = criterion.backward(output, labels)
      model.backward(input, gradInput)
      (_loss, grad)
    }

    for (i <- 1 to 50) {
      val (_, loss) = sgd.optimize(feval, weights, state)
      println(s"${i}-th loss = ${loss(0)}")
    }

    val output = model.forward(input).asInstanceOf[Tensor[Double]]
    val logOutput = logSoftMax.forward(output)
    val prediction = logOutput.max(2)._2

    labels.squeeze() should be (prediction.squeeze())
  }

  "A Recurrent Module " should "converge in batch mode for inside transpose" in {

    val batchSize = 10
    val nWords = 5
    val hiddenSize = 4
    val inputSize = 5
    val outputSize = 5
    val bpttTruncate = 3
    val seed = 100
    RNG.setSeed(seed)

    val model = Sequential[Double]()
    model.add(Recurrent[Double](hiddenSize, bpttTruncate)
      .add(RnnCell[Double](inputSize, hiddenSize))
      .add(Tanh()))
      .add(Select(2, nWords))
      .add(Linear[Double](hiddenSize, outputSize))

    val criterion = CrossEntropyCriterion[Double]()
    val logSoftMax = LogSoftMax[Double]()

    val (weights, grad) = model.getParameters()

    val input = Tensor[Double](Array(batchSize, nWords, inputSize))
    val labels = Tensor[Double](batchSize)
    for (b <- 1 to batchSize) {
      for (i <- 1 to nWords) {
        val rdmInput = Math.ceil(RNG.uniform(0.0, 1.0) * inputSize).toInt
        input.setValue(b, i, rdmInput, 1.0)
      }
      val rdmLabel = Math.ceil(RNG.uniform(0.0, 1.0) * outputSize).toInt
      labels.setValue(b, rdmLabel)
    }

    val state = T("learningRate" -> 0.5, "momentum" -> 0.0,
      "weightDecay" -> 0.0, "dampening" -> 0.0)
    val sgd = new SGD[Double]
    def feval(x: Tensor[Double]): (Double, Tensor[Double]) = {
      val output = model.forward(input).asInstanceOf[Tensor[Double]]
      val _loss = criterion.forward(output, labels)
      model.zeroGradParameters()
      val gradInput = criterion.backward(output, labels)
      model.backward(input, gradInput)
      (_loss, grad)
    }

    val nEpoch = 50
    val start = System.nanoTime()
    for (i <- 1 to nEpoch) {
      val (_, loss) = sgd.optimize(feval, weights, state)
      println(s"${i}-th loss = ${loss(0)}")
    }
    val end = System.nanoTime()
    val wallClockTime = end - start

    val output = model.forward(input).asInstanceOf[Tensor[Double]]
    val logOutput = logSoftMax.forward(output)
    val prediction = logOutput.max(2)._2

    labels.squeeze() should be (prediction.squeeze())
    println(s"training time is ${(end - start) / (nEpoch*1e9)} seconds/epoch")
  }

  "A Recurrent Module " should "converge in batch mode the same as in legacyMode" in {

    val batchSize = 10
    val nWords = 5
    val hiddenSize = 4
    val inputSize = 5
    val outputSize = 5
    val bpttTruncate = 3
    val seed = 100
    RNG.setSeed(seed)

    val recurrent = Recurrent[Double](hiddenSize, bpttTruncate, timeDim = 2)
    val model = Sequential[Double]()
    model.add(recurrent
      .add(RnnCell[Double](inputSize, hiddenSize))
      .add(Tanh()))
      .add(Select(2, nWords))
      .add(Linear[Double](hiddenSize, outputSize))

    recurrent.setLegacyMode()

    val criterion = CrossEntropyCriterion[Double]()
    val logSoftMax = LogSoftMax[Double]()

    val (weights, grad) = model.getParameters()

    val input = Tensor[Double](Array(batchSize, nWords, inputSize))
    val labels = Tensor[Double](batchSize)
    for (b <- 1 to batchSize) {
      for (i <- 1 to nWords) {
        val rdmInput = Math.ceil(RNG.uniform(0.0, 1.0) * inputSize).toInt
        input.setValue(b, i, rdmInput, 1.0)
      }
      val rdmLabel = Math.ceil(RNG.uniform(0.0, 1.0) * outputSize).toInt
      labels.setValue(b, rdmLabel)
    }

    val state = T("learningRate" -> 0.5, "momentum" -> 0.0,
      "weightDecay" -> 0.0, "dampening" -> 0.0)
    val sgd = new SGD[Double]
    def feval(x: Tensor[Double]): (Double, Tensor[Double]) = {
      val output = model.forward(input).asInstanceOf[Tensor[Double]]
      val _loss = criterion.forward(output, labels)
      model.zeroGradParameters()
      val gradInput = criterion.backward(output, labels)
      model.backward(input, gradInput)
      (_loss, grad)
    }

    val nEpoch = 50
    val start = System.nanoTime()
    for (i <- 1 to nEpoch) {
      val (_, loss) = sgd.optimize(feval, weights, state)
      println(s"${i}-th loss = ${loss(0)}")
    }
    val end = System.nanoTime()
    val wallClockTime = end - start

    val output = model.forward(input).asInstanceOf[Tensor[Double]]
    val logOutput = logSoftMax.forward(output)
    val prediction = logOutput.max(2)._2

    labels.squeeze() should be (prediction.squeeze())
    println(s"training time is ${(end - start) / (nEpoch*1e9)} seconds/epoch")
  }

  "A Recurrent Module " should "converge in batch mode for outside transpose" in {

    val batchSize = 10
    val nWords = 5
    val hiddenSize = 4
    val inputSize = 5
    val outputSize = 5
    val bpttTruncate = 3
    val seed = 100
    RNG.setSeed(seed)

    val model = Sequential[Double]()
    model.add(Transpose(Array((1, 2))))
      .add(Recurrent[Double](hiddenSize, bpttTruncate, timeDim = 1)
      .add(RnnCell[Double](inputSize, hiddenSize))
      .add(Tanh()))
      .add(Transpose(Array((1, 2))))
      .add(Select(2, nWords))
      .add(Linear[Double](hiddenSize, outputSize))

    val criterion = CrossEntropyCriterion[Double]()
    val logSoftMax = LogSoftMax[Double]()

    val (weights, grad) = model.getParameters()

    val input = Tensor[Double](Array(batchSize, nWords, inputSize))
    val labels = Tensor[Double](batchSize)
    for (b <- 1 to batchSize) {
      for (i <- 1 to nWords) {
        val rdmInput = Math.ceil(RNG.uniform(0.0, 1.0) * inputSize).toInt
        input.setValue(b, i, rdmInput, 1.0)
      }
      val rdmLabel = Math.ceil(RNG.uniform(0.0, 1.0) * outputSize).toInt
      labels.setValue(b, rdmLabel)
    }

    val state = T("learningRate" -> 0.5, "momentum" -> 0.0,
      "weightDecay" -> 0.0, "dampening" -> 0.0)
    val sgd = new SGD[Double]
    def feval(x: Tensor[Double]): (Double, Tensor[Double]) = {
      val output = model.forward(input).asInstanceOf[Tensor[Double]]
      val _loss = criterion.forward(output, labels)
      model.zeroGradParameters()
      val gradInput = criterion.backward(output, labels)
      model.backward(input, gradInput)
      (_loss, grad)
    }

    val nEpoch = 50
    val start = System.nanoTime()
    for (i <- 1 to nEpoch) {
      val (_, loss) = sgd.optimize(feval, weights, state)
      println(s"${i}-th loss = ${loss(0)}")
    }
    val end = System.nanoTime()
    val wallClockTime = end - start

    val output = model.forward(input).asInstanceOf[Tensor[Double]]
    val logOutput = logSoftMax.forward(output)
    val prediction = logOutput.max(2)._2

    labels.squeeze() should be (prediction.squeeze())
    println(s"training time is ${(end - start) / (nEpoch*1e9)} seconds/epoch")
  }

  "A Recurrent Module " should "be slow in legacyMode for RnnCell" in {

    val batchSize = 50
    val nWords = 20
    val hiddenSize = 40
    val inputSize = 1000
    val outputSize = 1000
    val bpttTruncate = 5
    val seed = 100
    RNG.setSeed(seed)

    val recurrent = Recurrent[Double](hiddenSize, bpttTruncate, timeDim = 2)
    val model = Sequential[Double]()
    model.add(recurrent
      .add(RnnCell[Double](inputSize, hiddenSize))
      .add(Tanh()))
      .add(Select(2, nWords))
      .add(Linear[Double](hiddenSize, outputSize))

    recurrent.setLegacyMode()
    model.evaluate()

    val criterion = CrossEntropyCriterion[Double]()
    val logSoftMax = LogSoftMax[Double]()

    val (weights, grad) = model.getParameters()

    val input = Tensor[Double](Array(batchSize, nWords, inputSize))
    val labels = Tensor[Double](batchSize)
    for (b <- 1 to batchSize) {
      for (i <- 1 to nWords) {
        val rdmInput = Math.ceil(RNG.uniform(0.0, 1.0) * inputSize).toInt
        input.setValue(b, i, rdmInput, 1.0)
      }
      val rdmLabel = Math.ceil(RNG.uniform(0.0, 1.0) * outputSize).toInt
      labels.setValue(b, rdmLabel)
    }

    val nEpoch = 50
    var start = System.nanoTime()
    for (i <- 1 to nEpoch) {
      model.forward(input)
    }
    var end = System.nanoTime()
    var wallClockTime = end - start
    println(s"Average forward time is ${wallClockTime / (nEpoch * 1e9)} seconds")
    var totalClockTime = wallClockTime

    val output = model.forward(input).asInstanceOf[Tensor[Double]]
    val loss = criterion.forward(output, labels)
    val gradInput = criterion.backward(output, labels)
    start = System.nanoTime()
    for (i <- 1 to nEpoch) {
      model.backward(input, gradInput)
    }
    end = System.nanoTime()
    wallClockTime = end - start
    println(s"Average backward time is ${wallClockTime / (nEpoch * 1e9)} seconds")

    totalClockTime += wallClockTime
    println(s"Average total time is ${totalClockTime / (nEpoch * 1e9)} seconds")
  }

  "A Recurrent Module " should "be fast in contiguous tensor for RnnCell" in {

    val batchSize = 50
    val nWords = 20
    val hiddenSize = 40
    val inputSize = 1000
    val outputSize = 1000
    val bpttTruncate = 5
    val seed = 100
    RNG.setSeed(seed)

    val model = Sequential[Double]()
    model.add(Recurrent[Double](hiddenSize, bpttTruncate, timeDim = 2)
      .add(RnnCell[Double](inputSize, hiddenSize))
      .add(Tanh()))
      .add(Select(2, nWords))
      .add(Linear[Double](hiddenSize, outputSize))
    model.evaluate()

    val criterion = CrossEntropyCriterion[Double]()
    val logSoftMax = LogSoftMax[Double]()

    val (weights, grad) = model.getParameters()

    val input = Tensor[Double](Array(batchSize, nWords, inputSize))
    val labels = Tensor[Double](batchSize)
    for (b <- 1 to batchSize) {
      for (i <- 1 to nWords) {
        val rdmInput = Math.ceil(RNG.uniform(0.0, 1.0) * inputSize).toInt
        input.setValue(b, i, rdmInput, 1.0)
      }
      val rdmLabel = Math.ceil(RNG.uniform(0.0, 1.0) * outputSize).toInt
      labels.setValue(b, rdmLabel)
    }

    val nEpoch = 50
    var start = System.nanoTime()
    for (i <- 1 to nEpoch) {
      model.forward(input)
    }
    var end = System.nanoTime()
    var wallClockTime = end - start
    println(s"Average forward time is ${wallClockTime / (nEpoch * 1e9)} seconds")
    var totalClockTime = wallClockTime

    val output = model.forward(input).asInstanceOf[Tensor[Double]]
    val loss = criterion.forward(output, labels)
    val gradInput = criterion.backward(output, labels)
    start = System.nanoTime()
    for (i <- 1 to nEpoch) {
      model.backward(input, gradInput)
    }
    end = System.nanoTime()
    wallClockTime = end - start
    println(s"Average backward time is ${wallClockTime / (nEpoch * 1e9)} seconds")

    totalClockTime += wallClockTime
    println(s"Average total time is ${totalClockTime / (nEpoch * 1e9)} seconds")
  }

  "A Recurrent Module " should "perform correct gradient check" in {

    val hiddenSize = 4
    val inputSize = 5
    val outputSize = 5
    val bpttTruncate = 10
    val seed = 100
    RNG.setSeed(seed)
    val model = Sequential[Double]()
    model.add(Recurrent[Double](hiddenSize, bpttTruncate)
      .add(RnnCell[Double](inputSize, hiddenSize))
      .add(Tanh[Double]()))
      .add(Select(1, 1))
      .add(Linear[Double](hiddenSize, outputSize))

    model.reset()

    val input = Tensor[Double](Array(1, 5, inputSize))
    val labels = Tensor[Double](Array(1, 5))
    for (i <- 1 to 5) {
      val rdmLabel = Math.ceil(Math.random()*inputSize).toInt
      val rdmInput = Math.ceil(Math.random()*inputSize).toInt
      input.setValue(1, i, rdmInput, 1.0)
      labels.setValue(1, i, rdmLabel)
    }

    println("gradient check for input")
    val gradCheckerInput = new GradientChecker(1e-5, 1)
    val checkFlagInput = gradCheckerInput.checkLayer[Double](model, input)
    println("gradient check for weights")
    val gradCheck = new GradientCheckerRNN(1e-5, 1)
    val checkFlag = gradCheck.checkLayer(model, input, labels)
  }
}
