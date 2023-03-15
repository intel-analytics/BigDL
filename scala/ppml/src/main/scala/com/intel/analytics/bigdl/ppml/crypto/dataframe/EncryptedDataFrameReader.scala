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

package com.intel.analytics.bigdl.ppml.crypto.dataframe

import com.intel.analytics.bigdl.ppml.PPMLContext
import com.intel.analytics.bigdl.ppml.crypto.dataframe.EncryptedDataFrameWriter.writeCsv
import com.intel.analytics.bigdl.ppml.crypto.{AES_CBC_PKCS5PADDING, CryptoMode, PLAIN_TEXT}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.types.{StringType, StructField, StructType}
import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import com.intel.analytics.bigdl.ppml.kms.common.KeyLoaderManagement
import com.intel.analytics.bigdl.dllib.utils.Log4Error

/**
 *
 * @param sparkSession
 * @param encryptMode
 * @param primaryKeyName
 * @param keyLoaderManagement
 */
class EncryptedDataFrameReader(
      sparkSession: SparkSession,
      encryptMode: CryptoMode,
      primaryKeyName: String,
      keyLoaderManagement: KeyLoaderManagement) {
  protected val extraOptions = new scala.collection.mutable.HashMap[String, String]

  def option(key: String, value: String): this.type = {
    this.extraOptions += (key -> value)
    this
  }

  def setCryptoCodecContext(path: String): Unit = {
    encryptMode match {
      case PLAIN_TEXT =>
      case AES_CBC_PKCS5PADDING =>
        val dataKeyPlainText = keyLoaderManagement.retrieveKeyLoader(primaryKeyName)
                                                  .retrieveDataKeyPlainText(path)
      case _ =>
        Log4Error.invalidOperationError(false, "unknown EncryptMode " + CryptoMode.toString)
    }
  }

  def csv(path: String): DataFrame = {
    setCryptoCodecContext(path)
    sparkSession.read.options(extraOptions).csv(path)
  }

  def json(path: String): DataFrame = {
    setCryptoCodecContext(path)
    sparkSession.read.options(extraOptions).json(path)
  }

  def parquet(path: String): DataFrame = {
    if (encryptMode != PLAIN_TEXT) {
      val dataKeyPlainText = keyLoaderManagement.retrieveKeyLoader(primaryKeyName)
                                                .retrieveDataKeyPlainText(path)
      EncryptedDataFrameReader.setParquetKey(sparkSession, dataKeyPlainText)
    }
    sparkSession.read.parquet(path)
  }
}

object EncryptedDataFrameReader {
  private[bigdl] def toDataFrame(dataRDD: RDD[String]): DataFrame = {
    // get schema
    val sparkSession: SparkSession = SparkSession.builder().getOrCreate()
    val header = dataRDD.first()
    val schemaArray = header.split(",")
    val fields = schemaArray.map(fieldName => StructField(fieldName, StringType, true))
    val schema = StructType(fields)

    // remove title line
    val data_filter = dataRDD.filter(row => row != header)

    // create df
    val rowRdd = data_filter.map(s => Row.fromSeq(s.split(",")))
    sparkSession.createDataFrame(rowRdd, schema)
  }

  private[bigdl] def setParquetKey(sparkSession: SparkSession, dataKeyPlainText: String): Unit = {
    EncryptedDataFrameWriter.setParquetKey(sparkSession, dataKeyPlainText)
  }
}
