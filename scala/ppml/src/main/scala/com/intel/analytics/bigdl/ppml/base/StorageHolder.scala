/*
 * Copyright 2021 The BigDL Authors.
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

package com.intel.analytics.bigdl.ppml.base

import com.intel.analytics.bigdl.ppml.common.{FLDataType, Storage}
import com.intel.analytics.bigdl.ppml.generated.FGBoostServiceProto._
import com.intel.analytics.bigdl.ppml.generated.FlBaseProto.Table
import org.apache.logging.log4j.LogManager

/**
 * The storage holder holding all types of Storage
 * Aggregator could use this generic holder type to aggregate
 * @param flDataType the instance of storage of this holder, could be one of [[FLDataType]]
 */
class StorageHolder(flDataType: FLDataType) {
  private val logger = LogManager.getLogger(getClass)

  private var clientDataSize: Int = 0
  private var tableStorage: Storage[Table] = null
  private var splitStorage: Storage[DataSplit] = null
  private var leafStorage: Storage[TreeLeaves] = null
  private var branchStorage: Storage[java.util.List[BoostEval]] = null

  flDataType match {
    case FLDataType.TENSOR_MAP => tableStorage = new Storage[Table](flDataType.toString)
    case FLDataType.TREE_SPLIT => splitStorage = new Storage[DataSplit](flDataType.toString)
    case FLDataType.TREE_LEAVES => leafStorage = new Storage[TreeLeaves](flDataType.toString)
    case FLDataType.TREE_EVAL => branchStorage =
      new Storage[java.util.List[BoostEval]](flDataType.toString)
    case _ => throw new NotImplementedError()
  }
  def getVersion(): Int = {
    if (tableStorage != null) tableStorage.version
    else throw new NotImplementedError()
  }

  def getClientDataSize() = this.clientDataSize

  def putClientData(clientID: String, dataHolder: DataHolder) = {
    if (dataHolder.table != null) {
      tableStorage.clientData.put(clientID, dataHolder.table)
      clientDataSize = tableStorage.clientData.size()
      logger.debug(s"Put Table into client data map, current size: $clientDataSize")
    } else if (dataHolder.split != null) {
      splitStorage.clientData.put(clientID, dataHolder.split)
      clientDataSize = splitStorage.clientData.size()
      logger.debug(s"Put Split into client data map, current size: $clientDataSize")
    } else if (dataHolder.treeLeaves != null) {
      leafStorage.clientData.put(clientID, dataHolder.treeLeaves)
      clientDataSize = leafStorage.clientData.size()
      logger.debug(s"Put TreeLeaves into client data map, current size: $clientDataSize")
    } else if (dataHolder.boostEval != null) {
      branchStorage.clientData.put(clientID, dataHolder.boostEval)
      clientDataSize = branchStorage.clientData.size()
      logger.debug(s"Put TreeEval into client data map, current size: $clientDataSize")
    } else {
      throw new IllegalArgumentException("Data is empty, could not uploaded to server.")
    }
  }
  def getTableStorage() = this.tableStorage
  def getSplitStorage() = this.splitStorage
  def getLeafStorage() = this.leafStorage
  def getBranchStorage() = this.branchStorage
}
