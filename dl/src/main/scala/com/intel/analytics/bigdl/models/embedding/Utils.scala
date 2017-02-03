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
package com.intel.analytics.bigdl.models.embedding

import scopt.OptionParser

object Utils {
  /**
   * Options used by our word2vec model.
   *
   * @param saveLocation Save the model to the location
   * @param trainDataLocation The location of Training data
   * @param testDataLocation The location of test data
   * @param numNegSamples Number of negative samples per example.
   * @param embeddingSize Embedding dimension.
   * @param windowSize The number of words to predict to the left
   *                   and right of the target word.
   * @param minCount The minimum number of word occurrences for it
   *                 to be included in the vocabulary.
   * @param subsample Sub-sampling threshold for word occurrence.
   * @param alpha Negative sampling unigram distribution raised to alpha power
   * @param maxSentenceLength The maximum sentence length
   * @param numSimilarWord Output the number of most similar words given a
   *                       input during prediction
   * @param learningRate
   * @param batchSize
   */
  case class Word2VecConfig(
    saveLocation: String = "",
//    trainDataLocation: String = "/home/yao/Desktop/enwik9",
    trainDataLocation: String = "/home/yao/Downloads/corpus.txt",
    testDataLocation: String = "",
    w2vLocation: String = "./model",
    numNegSamples: Int = 5,
    embeddingSize: Int = 100,
    windowSize: Int = 5,
    minCount: Int = 10,
    subsample: Double = 1e-3,
    alpha: Double = 0.75,
    maxSentenceLength: Int = 1000,
    numClosestWords: Int = 5,
    numSimilarWord: Int = 5,
    learningRate: Double = 2.5,
    batchSize: Int = 128,
    coreNumber: Int = 4,
    nodeNumber: Int = 1,
    maxEpoch: Int = 3,
    modelSnapshot: Option[String] = None,
    stateSnapshot: Option[String] = None,
    checkpoint: Option[String] = None
  )

  def parse(args: Array[String]): Word2VecConfig = new OptionParser[Word2VecConfig]("word2vec") {
    help("help") text "prints this usage text"
    opt[String]("saveLocation")
      .text("")
      .action { (x, c) => c.copy(saveLocation = x) }

    opt[String]("trainDataLocation")
      .text("")
      .action { (x, c) => c.copy(trainDataLocation = x) }

    opt[String]("testDataLocation")
      .text("")
      .action { (x, c) => c.copy(testDataLocation = x) }

    opt[String]("model")
      .text("")
      .action { (x, c) => c.copy(w2vLocation = x) }

    opt[Int]("numNegSamples")
      .text("Negative samples per training example.")
      .action { (x, c) => c.copy(numNegSamples = x) }

    opt[Int]("embeddingSize")
      .text("Initial learning rate.")
      .action { (x, c) => c.copy(embeddingSize = x) }

    opt[Int]("windowSize")
      .text("The number of words to predict to the left and right of the target word.")
      .action { (x, c) => c.copy(windowSize = x) }

    opt[Int]("minCount")
      .text("The minimum number of word occurrences for it to be included in the vocabulary.")
      .action { (x, c) => c.copy(minCount = x) }

    opt[Double]("subsample")
      .text("Subsample threshold for word occurrence. Words that appear with higher " +
        "frequency will be randomly down-sampled. Set to 0 to disable.")
      .action { (x, c) => c.copy(subsample = x) }

    opt[Double]("maxSentenceLength")
      .text("The maximum threshold of sentence length accepted, if length than the threshold" +
        "the rest will be dropped")
      .action { (x, c) => c.copy(subsample = x) }

    opt[Int]("kNearestWords")
      .text("number of closest words that will be shown")
      .action { (x, c) => c.copy(numClosestWords = x) }

    opt[String]('f', "folder")
      .text("where you put the MNIST data")
      .action((x, c) => c.copy(trainDataLocation = x))

    opt[Int]('b', "batchSize")
      .text("batch size")
      .action((x, c) => c.copy(batchSize = x))

    opt[String]("model")
      .text("model snapshot location")
      .action((x, c) => c.copy(modelSnapshot = Some(x)))

    opt[String]("state")
      .text("state snapshot location")
      .action((x, c) => c.copy(stateSnapshot = Some(x)))

    opt[String]("checkpoint")
      .text("where to cache the model")
      .action((x, c) => c.copy(checkpoint = Some(x)))

    opt[Double]('r', "learningRate")
      .text("learning rate")
      .action((x, c) => c.copy(learningRate = x))

    opt[Int]('e', "maxEpoch")
      .text("epoch numbers")
      .action((x, c) => c.copy(maxEpoch = x))

    opt[Int]('c', "core")
      .text("cores number on each node")
      .action((x, c) => c.copy(coreNumber = x))

    opt[Int]('n', "node")
      .text("node number to train the model")
      .action((x, c) => c.copy(nodeNumber = x))

    opt[Int]('b', "batchSize")
      .text("batch size")
      .action((x, c) => c.copy(batchSize = x))


  }.parse(args, Word2VecConfig()).get
}
