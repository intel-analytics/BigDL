package com.intel.analytics.bigdl.ppml.python

import com.intel.analytics.bigdl.ppml.PPMLContext
import org.scalatest.FunSuite

import java.util

class PPMLContextWrapperTest extends FunSuite {
  val ppmlContextWrapper: PPMLContextWrapper[Float] = PPMLContextWrapper.ofFloat

  def initArgs(): util.Map[String, String] = {
    val args = new util.HashMap[String, String]()
    args.put("kms_type", "SimpleKeyManagementService")
    args.put("simple_app_id", "465227134889")
    args.put("simple_app_key", "799072978028")
    args.put("primary_key_path", this.getClass.getClassLoader.getResource("primaryKey").getPath)
    args.put("data_key_path", this.getClass.getClassLoader.getResource("dataKey").getPath)
    args
  }

  def initAndRead(encryptMode: String, path: String): Unit = {
    val appName = "test"
    val args = initArgs()
    val cryptoMode = encryptMode

    val sc = ppmlContextWrapper.createPPMLContext(appName, args)
    val encryptedDataFrameReader = ppmlContextWrapper.read(sc, cryptoMode)
    ppmlContextWrapper.option(encryptedDataFrameReader, "header", "true")
    val df = ppmlContextWrapper.csv(encryptedDataFrameReader, path)

    assert(df.count() == 100)
  }

  test("init PPMLContext with app name") {
    val appName = "test"
    ppmlContextWrapper.createPPMLContext(appName)
  }

  test("init PPMLContext with app name & args") {
    val appName = "test"
    val args = initArgs()
    ppmlContextWrapper.createPPMLContext(appName, args)
  }

  test("read plain text csv file") {
    val cryptoMode = "plain_text"
    val path = this.getClass.getClassLoader.getResource("people.csv").getPath

    initAndRead(cryptoMode, path)
  }

  test("read encrypted csv file") {
    val cryptoMode = "AES/CBC/PKCS5Padding"
    val path = this.getClass.getClassLoader.getResource("encrypt-people").getPath

    initAndRead(cryptoMode, path)
  }

}
