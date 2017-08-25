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

package org.apache.spark.sparkExtension

import java.util.HashMap

import org.apache.spark.{SparkConf, SparkContext, SparkEnv}
import org.apache.spark.rpc._
import org.apache.spark.storage.{BlockId, BlockManagerId}
import org.apache.spark.util.{RpcUtils, Utils}
import org.apache.spark.SecurityManager

import scala.collection.mutable

private case class GetExecutorBlockList(executorId: Int)
private case class UpdateExecutorBlockList(executorId: Int, blockId: BlockId)
private case class ClearExecutorBlockList(executorId: Int)
private case class ResetTotal()

/**
 * ParameterManagerMasterEndpoint is an [[ThreadSafeRpcEndpoint]] on the driver
 * node to track statuses of all executor' blocks.
 */
private class ParameterManagerMasterEndpoint(
  override val rpcEnv: RpcEnv,
  conf: SparkConf)
  extends ThreadSafeRpcEndpoint {

  private val blocks = new HashMap[Int, mutable.HashSet[BlockId]]

  override def receiveAndReply(context: RpcCallContext): PartialFunction[Any, Unit] = {
    case GetExecutorBlockList(executorId) =>
      context.reply(getExecutorBlockList(executorId))

    case UpdateExecutorBlockList(executorId, blockId) =>
      context.reply(updateExecutorBlockList(executorId, blockId))

    case ClearExecutorBlockList(executorId) =>
      context.reply(clearExecutorBlockList(executorId))

    case ResetTotal() =>
      context.reply(resetTotal())

    case _ =>
      throw new UnsupportedOperationException("Unsupported request")
  }

  /** Get block lists with a given executorId */
  private def getExecutorBlockList(executorId: Int): Seq[BlockId] = {
    if (blocks.containsKey(executorId)) {
      blocks.get(executorId).toSeq
    } else Seq.empty
  }

  private var total = 0

  private def resetTotal(): Unit = {
    total = 0
  }

  /** Update block lists with a given executorId */
  private def updateExecutorBlockList(executorId: Int, blockId: BlockId): Unit = {
    blocks.synchronized {
        if (blocks.containsKey(executorId)) {
          blocks.get(executorId).add(blockId)
        }
        else {
          val hashset = new mutable.HashSet[BlockId]()
          hashset.add(blockId)
          blocks.put(executorId, hashset)
        }
    }
  }

  /** Clear block lists with a given executorId */
  private def clearExecutorBlockList(executorId: Int) = {
    blocks.synchronized {
      if (blocks.containsKey(executorId)) {
        blocks.get(executorId).clear()
      }
    }
  }
}

class ParameterManagerMaster(
  var driverEndpoint: RpcEndpointRef,
  isDriver: Boolean)
{
  def getBlockId(executorId: Int): Seq[BlockId] = {
    driverEndpoint.askWithRetry[Seq[BlockId]](GetExecutorBlockList(executorId))
  }

  def updateBlockId(executorId: Int, blockId: BlockId): Unit = {
    driverEndpoint.askWithRetry[Unit](UpdateExecutorBlockList(executorId, blockId))
  }

  def clearBlockId(executorId: Int): Unit = {
    driverEndpoint.askWithRetry[Unit](ClearExecutorBlockList(executorId))
  }

  def resetTotal(): Unit = {
    driverEndpoint.askWithRetry[Unit](ResetTotal())
  }
}

object ParameterManagerMaster {
  def createEnv(conf: SparkConf, driverPort: Option[Int]): ParameterManagerMaster = {
    val bindAddress = Utils.localHostName()
    val isDriver = if (!driverPort.isDefined) true else false
    val port = System.getProperty("BigDL.port", "7777").toInt
    val systemName = if (isDriver) "BigDLDriver" else "BigDLExecutor"
    val rpcEnv = RpcEnvWrapper.create(systemName, bindAddress, port, conf,
      SparkEnv.get.securityManager, clientMode = !isDriver)

    def registerOrLookupEndpoint(name: String, isDriver: Boolean, endpointCreator: => RpcEndpoint):
    RpcEndpointRef = {
      if (isDriver) {
        RpcEnvWrapper.setupEndpoint(rpcEnv, name, endpointCreator)
      } else {
        val driverHost = SparkEnv.get.blockManager.master.driverEndpoint.address.host
        RpcEnvWrapper.setupEndpointRef(rpcEnv, systemName, RpcAddress(driverHost, driverPort.get),
          name)
      }
    }

    new ParameterManagerMaster(registerOrLookupEndpoint(
      "ParameterManagerMaster", isDriver,
      new ParameterManagerMasterEndpoint(rpcEnv, conf)), isDriver)
  }
}
