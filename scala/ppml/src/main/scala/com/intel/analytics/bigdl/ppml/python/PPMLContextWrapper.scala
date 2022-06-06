package com.intel.analytics.bigdl.ppml.python

import com.intel.analytics.bigdl.ppml.PPMLContext
import com.intel.analytics.bigdl.ppml.crypto.CryptoMode
import com.intel.analytics.bigdl.ppml.crypto.dataframe.EncryptedDataFrameReader
import org.apache.spark.sql.DataFrame
import org.slf4j.{Logger, LoggerFactory}

import java.util

object PPMLContextWrapper {
  def ofFloat: PPMLContextWrapper[Float] = new PPMLContextWrapper[Float]()

  def ofDouble: PPMLContextWrapper[Double] = new PPMLContextWrapper[Double]()
}

class PPMLContextWrapper[T]() {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  def createPPMLContext(appName: String): PPMLContext = {
    logger.info("create PPMLContextWrapper with appName")
    PPMLContext.initPPMLContext(appName)
  }

  def createPPMLContext(appName: String, ppmlArgs: util.Map[String, String]): PPMLContext = {
    logger.info("create PPMLContextWrapper with appName & ppmlArgs")
    logger.info("appName: " + appName)
    logger.info("ppmlArgs: " + ppmlArgs)
    import scala.collection.JavaConverters._
    PPMLContext.initPPMLContext(appName, ppmlArgs.asScala.toMap)
  }

  def read(sc: PPMLContext, cryptoModeStr: String): EncryptedDataFrameReader = {
    val cryptoMode = CryptoMode.parse(cryptoModeStr)
    sc.read(cryptoMode)
  }

  def loadKeys(sc: PPMLContext,
               primaryKeyPath: String, dataKeyPath: String): Unit = {
    logger.info("load keys...")
    logger.info("primaryKeyPath: " + primaryKeyPath)
    logger.info("dataKeyPath: " + dataKeyPath)
    sc.loadKeys(primaryKeyPath, dataKeyPath)
  }

  /**
   * EncryptedDataFrameReader method
   */

  def option(encryptedDataFrameReader: EncryptedDataFrameReader,
             key: String, value: String): EncryptedDataFrameReader = {
    encryptedDataFrameReader.option(key, value)
  }

  def csv(encryptedDataFrameReader: EncryptedDataFrameReader, path: String): DataFrame = {
    encryptedDataFrameReader.csv(path)
  }
}
