package com.intel.analytics.bigdl.ppml.python

import com.intel.analytics.bigdl.ppml.PPMLContext
import com.intel.analytics.bigdl.ppml.crypto.dataframe.DataFrameHelper
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession

import java.io.File
import scala.collection.JavaConverters._

class PPMLContextPythonSpec extends DataFrameHelper{
  override val repeatedNum = 100
  val ppmlArgs: Map[String, String] = Map(
    "spark.bigdl.kms.simple.id" -> appid,
    "spark.bigdl.kms.simple.key" -> appkey,
    "spark.bigdl.kms.key.primary" -> primaryKeyPath,
    "spark.bigdl.kms.key.data" -> dataKeyPath
  )

  val pyPPMLArgs: Map[String, String]= Map(
    "kms_type" -> "SimpleKeyManagementService",
    "simple_app_id" -> appid,
    "simple_app_key" -> appkey,
    "primary_key_path" -> primaryKeyPath,
    "data_key_path" -> dataKeyPath
  )

  val conf: SparkConf = new SparkConf().setMaster("local[4]")
  val sc: PPMLContext = PPMLContext.initPPMLContext(conf, "testApp", ppmlArgs)
  val sparkSession: SparkSession = sc.getSparkSession()
  val ppmlContextPython: PPMLContextPython[Float] = PPMLContextPython.ofFloat
  import sparkSession.implicits._


  "init PPMLContext with app name" should "work" in {
    ppmlContextPython.createPPMLContext("testApp")
  }

  "init PPMLContext with app name & args" should "work" in {
    ppmlContextPython.createPPMLContext("testApp", pyPPMLArgs.asJava)
  }

  "read plain csv file" should "work" in {
    val encryptedDataFrameReader = ppmlContextPython.read(sc, "plain_text")
    ppmlContextPython.option(encryptedDataFrameReader, "header", "true")
    val df = ppmlContextPython.csv(encryptedDataFrameReader, plainFileName)

    df.count() should be (repeatedNum)

    val content = df.schema.map(_.name).mkString(",") + "\n" +
      df.collect().map(v => s"${v.get(0)},${v.get(1)},${v.get(2)}").mkString("\n")

    content + "\n" should be (data)
  }

  "read encrypted csv file" should "work" in {
    val encryptedDataFrameReader = ppmlContextPython.read(sc, "AES/CBC/PKCS5Padding")
    ppmlContextPython.option(encryptedDataFrameReader, "header", "true")
    val df = ppmlContextPython.csv(encryptedDataFrameReader, encryptFileName)

    df.count() should be (repeatedNum)

    val content = df.schema.map(_.name).mkString(",") + "\n" +
      df.collect().map(v => s"${v.get(0)},${v.get(1)},${v.get(2)}").mkString("\n")

    content + "\n" should be (data)
  }

  "write plain csv file" should "work" in {
    val data = Seq(("Java", "20000"), ("Python", "100000"), ("Scala", "3000"))
    val df = data.toDF("language", "user")
    val dataContent = df.orderBy("language").collect()
      .map(v => s"${v.get(0)},${v.get(1)}").mkString("\n")

    // write to csv file
    val csvDir = new File(dir, "csv/plain").getCanonicalPath
    val encryptedDataFrameWriter = ppmlContextPython.write(sc, df, "plain_text")
    ppmlContextPython.mode(encryptedDataFrameWriter, "overwrite")
    ppmlContextPython.option(encryptedDataFrameWriter, "header", true)
    ppmlContextPython.csv(encryptedDataFrameWriter, csvDir)

    // read for validation
    val encryptedDataFrameReader = ppmlContextPython.read(sc, "plain_text")
    ppmlContextPython.option(encryptedDataFrameReader, "header", "true")
    val csvDF = ppmlContextPython.csv(encryptedDataFrameReader, csvDir)

    csvDF.count() should be (3)

    val csvContent = csvDF.orderBy("language").collect()
      .map(v => s"${v.get(0)},${v.get(1)}").mkString("\n")

    csvContent should be (dataContent)
  }

  "write encrypted csv file" should "work" in {
    val data = Seq(("Java", "20000"), ("Python", "100000"), ("Scala", "3000"))
    val df = data.toDF("language", "user")
    val dataContent = df.orderBy("language").collect()
      .map(v => s"${v.get(0)},${v.get(1)}").mkString("\n")

    // write to csv file
    val csvDir = new File(dir, "csv/encrypted").getCanonicalPath
    val encryptedDataFrameWriter = ppmlContextPython.write(sc, df, "AES/CBC/PKCS5Padding")
    ppmlContextPython.mode(encryptedDataFrameWriter, "overwrite")
    ppmlContextPython.option(encryptedDataFrameWriter, "header", true)
    ppmlContextPython.csv(encryptedDataFrameWriter, csvDir)

    // read for validation
    val encryptedDataFrameReader = ppmlContextPython.read(sc, "AES/CBC/PKCS5Padding")
    ppmlContextPython.option(encryptedDataFrameReader, "header", "true")
    val csvDF = ppmlContextPython.csv(encryptedDataFrameReader, csvDir)

    csvDF.count() should be (3)

    val csvContent = csvDF.orderBy("language").collect()
      .map(v => s"${v.get(0)},${v.get(1)}").mkString("\n")

    csvContent should be (dataContent)
  }

  "write and read plain parquet file" should "work" in {
    val data = Seq(("Java", "20000"), ("Python", "100000"), ("Scala", "3000"))
    val df = data.toDF("language", "user")
    val dataContent = df.orderBy("language").collect()
      .map(v => s"${v.get(0)},${v.get(1)}").mkString("\n")

    // write a parquet file
    val parquetPath = new File(dir, "parquet/plain").getCanonicalPath
    val encryptedDataFrameWriter = ppmlContextPython.write(sc, df, "plain_text")
    ppmlContextPython.mode(encryptedDataFrameWriter, "overwrite")
    ppmlContextPython.parquet(encryptedDataFrameWriter, parquetPath)

    // read a parquet file
    val encryptedDataFrameReader = ppmlContextPython.read(sc, "plain_text")
    val parquetDF = ppmlContextPython.parquet(encryptedDataFrameReader, parquetPath)

    val parquetContent = parquetDF.orderBy("language").collect()
      .map(v => s"${v.get(0)},${v.get(1)}").mkString("\n")

    parquetContent should be (dataContent)
  }

  "write and read encrypted parquet file" should "work" in {
    val data = Seq(("Java", "20000"), ("Python", "100000"), ("Scala", "3000"))
    val df = data.toDF("language", "user")
    val dataContent = df.orderBy("language").collect()
      .map(v => s"${v.get(0)},${v.get(1)}").mkString("\n")

    // write a parquet file
    val parquetPath = new File(dir, "parquet/encrypted").getCanonicalPath
    val encryptedDataFrameWriter = ppmlContextPython.write(sc, df, "AES_GCM_CTR_V1")
    ppmlContextPython.mode(encryptedDataFrameWriter, "overwrite")
    ppmlContextPython.parquet(encryptedDataFrameWriter, parquetPath)

    // read a parquet file
    val encryptedDataFrameReader = ppmlContextPython.read(sc, "AES_GCM_CTR_V1")
    val parquetDF = ppmlContextPython.parquet(encryptedDataFrameReader, parquetPath)

    val parquetContent = parquetDF.orderBy("language").collect()
      .map(v => s"${v.get(0)},${v.get(1)}").mkString("\n")

    parquetContent should be (dataContent)
  }

}
