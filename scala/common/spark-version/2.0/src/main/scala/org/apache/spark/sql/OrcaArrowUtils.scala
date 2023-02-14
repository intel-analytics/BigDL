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

package org.apache.spark.sql

import org.apache.spark.api.java.JavaRDD
import org.apache.spark.rdd.RDD
import java.util.{List => JList}


class OrcaArrowUtils() {
  def orcaToDataFrame(jrdd: JavaRDD[String], schemaString: String,
  sqlContext: SQLContext): DataFrame = {
    null.asInstanceOf[DataFrame]
  }

  def sparkdfTopdf(sdf: DataFrame, sqlContext: SQLContext, batchSize: Int = -1): RDD[String] = {
    null.asInstanceOf[RDD[String]]
  }

  def openVINOOutputToSDF(df: DataFrame,
                          outputRDD: JavaRDD[String],
                          outputNames: JList[String],
                          outShapes: JList[JList[Int]]): DataFrame = {
    null.asInstanceOf[DataFrame]
  }
}
