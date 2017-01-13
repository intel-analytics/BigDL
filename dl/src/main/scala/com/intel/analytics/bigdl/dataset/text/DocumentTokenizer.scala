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

package com.intel.analytics.bigdl.dataset.text

import com.intel.analytics.bigdl.dataset.Transformer

import scala.collection.Iterator
import scala.collection.mutable.ArrayBuffer

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.sql.functions._
import org.apache.spark.sql._
import java.io._

import smile.nlp.tokenizer.{SimpleSentenceSplitter, SimpleTokenizer}

  /**
  * Transformer that tokenizes a Document (article)
  * into a Seq[Seq[String]] by using Stanford Tokenizer.
  *
  */

class DocumentTokenizer() extends Transformer[String, Array[Array[String]]] {
  override def apply(prev: Iterator[String]): Iterator[Array[Array[String]]] =
    prev.map(x => {
      val sentences = ArrayBuffer[Array[String]]()
      val sc = new SparkContext("local[1]", "DocumentTokenizer")
      val logData = sc.textFile(x, 2).filter(!_.isEmpty()).cache()

      val sqlContext = new SQLContext(sc)
      import sqlContext.implicits._

      val sentences_split = SimpleSentenceSplitter.getInstance.split(logData.collect().reduce((l,r)=>l+r))
      val tokenizer = new SimpleTokenizer(true)
      for (i <- sentences_split.indices){
          val words = tokenizer.split(sentences_split(i))
          sentences.append(words)
      }
      sentences.toArray
    })
}

object DocumentTokenizer {
  def apply(): DocumentTokenizer = new DocumentTokenizer()
}
