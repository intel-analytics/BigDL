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
package com.intel.analytics.bigdl.nn

import com.intel.analytics.bigdl.Module
import com.intel.analytics.bigdl.nn.Graph.ModuleNode
import com.intel.analytics.bigdl.nn.abstractnn.{AbstractModule, Activity}
import com.intel.analytics.bigdl.nn.ops._
import com.intel.analytics.bigdl.nn.tf.WithoutInput
import com.intel.analytics.bigdl.tensor.Tensor
import com.intel.analytics.bigdl.tensor.TensorNumericMath.TensorNumeric
import com.intel.analytics.bigdl.utils.{Edge, Node, T}

import scala.collection.mutable
import scala.reflect.ClassTag

/**
 * Scheduler of a graph execution. It supports a graph with cycle. Please note that the cycle must
 * be created from ControlNodes.while.
 *
 * Scheduler also records execution status. So some const graph won't be executed multiple times.
 *
 * @param inputNodes start nodes
 * @param outputNodes target nodes
 * @tparam T
 */
private[bigdl] class Scheduler[T](
    inputNodes: Seq[ModuleNode[T]], outputNodes: Seq[ModuleNode[T]]
  ) {

  import Scheduler._

  private val readyQueue = new mutable.Queue[ModuleNode[T]]()
  private val nodeStatus = new NodeStatusManager[T]()

  /**
   * User must reset the scheduler after first use it or finish a graph execution
   */
  def reset(): Unit = {
    readyQueue.clear()
    inputNodes.foreach(n => {
      readyQueue.enqueue(n)
    })
    nodeStatus.removeUnConstStatus()
  }


  /**
   * If every output nodes is executed. Please note if some of the output nodes has not been
   * executed but the execution can't move forward, an exception will be thrown
   * @return
   */
  def isFinished(): Boolean = {
    val isEmpty = readyQueue.isEmpty
    if (isEmpty) {
      outputNodes.foreach(n => {
        require(!nodeStatus.notExecuted(n), "Some output nodes have not been executed")
      })
    }
    isEmpty
  }


  /**
   * Fetch a node to execute. Don't call it when isFinished is true, or it will throw an exception
   * @return
   */
  def fetch(): ModuleNode[T] = {
    var node = readyQueue.dequeue()
    while (nodeStatus.isConst(node)) {
      schedule(node)
      node = readyQueue.dequeue()
    }
    node
  }

  /**
   * Schedule nodes depend on the given node
   * @param node
   */
  def schedule(node: ModuleNode[T]): Unit = {
    // Update status of current node
    node.element match {
      case e: Enter[_] =>
        require(node.prevNodes.length == 1, "Enter only accept one node input")
        nodeStatus(node) = nodeStatus.stackIteration(nodeStatus(node.prevNodes.head))
      case x: Exit[_] =>
        require(node.prevNodes.length == 1, "Exit only accept one node input")
        nodeStatus(node) = nodeStatus.unstackIteration(nodeStatus.iteration(node.prevNodes.head))
      case n: NextIteration[_] =>
        require(node.prevNodes.length == 1, "NextIteration only accept one node input")
        val lastIteration = nodeStatus.iteration(node.prevNodes.head)
        lastIteration.outOfDate = true
        nodeStatus(node) = Iteration(lastIteration.prev)
      case _ =>
        nodeStatus(node) = if (node.prevNodes.length == 0) {
          if (node.element.isInstanceOf[com.intel.analytics.bigdl.nn.tf.Const[_]]) {
            Const()
          } else {
            Ready()
          }
        } else {
          val constNodes = node.prevNodes.filter(nodeStatus.isConst(_))
          if (constNodes.length == node.prevNodes.length) {
            Const()
          } else {
            val iterationNodes = node.prevNodes.filter(nodeStatus.isIteration(_))
              .filter(!nodeStatus.iteration(_).outOfDate)
              .filter(!_.element.isInstanceOf[LoopCondition[_]])
            if (iterationNodes.length == 0) {
              Ready()
            } else {
              nodeStatus.iteration(iterationNodes.head)
            }
          }
        }
    }

    // Schedule next nodes
    node.element match {
      case s: SwitchOps[_] =>
        val switchNode = node.asInstanceOf[SwitchControlNode[Module[T]]]
        selectNexts(switchNode.availableNodes(), node)
      case _ =>
        selectNexts(node.nextNodes, node)
    }
  }

  private def selectNexts(candidateNodes: Seq[ModuleNode[T]], curNode: ModuleNode[T]): Unit = {
    candidateNodes.foreach(nextNode => {
      if (nextNode.element.isInstanceOf[MergeOps[_]]) {
        val merge = nextNode.element.asInstanceOf[MergeOps[_]]
        require(nodeStatus.notExecuted(nextNode), s"Merge node(${nextNode.element.getName()}) " +
          s"should not be executed twice out of loop or in a same iteration of a loop")
        merge.setSwitch(nextNode.prevNodes.indexOf(curNode) + 1)
        readyQueue.enqueue(nextNode)
      } else {
        if (isNodeReady(nextNode)) {
          readyQueue.enqueue(nextNode)
        }
      }
    })
  }

  private def isNodeReady(node: ModuleNode[T]): Boolean = {
    if (node.prevNodes.filter(nodeStatus.notExecuted(_)).length != 0) {
      return false
    }
    node.prevNodes.filter(_.isInstanceOf[SwitchControlNode[_]]).foreach(n => {
      if (!n.asInstanceOf[SwitchControlNode[T]].availableNodes().contains(node)) {
        return false
      }
    })

    return true
  }
}

object Scheduler {
  // Default parent of iteration status
  private val DEFAULT_ITERATION = Iteration(null)

  class NodeStatusManager[T] {
    private val nodeStatus = new mutable.HashMap[String, NodeStatus]()

    /**
     * Create an iteration status. Its parent status will be the given status if the given status
     * is Iteration; Or the parent status will point to the DEFAULT_ITERATION
     * @param status
     * @return
     */
    def stackIteration(status: NodeStatus): Iteration = {
      val parent = status match {
        case i: Iteration => i
        case _ => DEFAULT_ITERATION
      }
      Iteration(parent)
    }

    /**
     * Create a node status from an iteration status. If the iteration status parent is
     * DEFAULT_STATUS, return a new Ready status, or return parent iteration
     * @param status
     * @return
     */
    def unstackIteration(status: Iteration): NodeStatus = {
      if (status.prev.eq(DEFAULT_ITERATION)) {
        Ready()
      } else {
        status.prev
      }
    }

    /**
     * Update node status
     * @param node
     * @param status
     */
    def update(node: ModuleNode[T], status: NodeStatus): Unit = {
      require(node != null && status != null, "Not accept null")
      nodeStatus(node.element.getName()) = status
    }

    /**
     * Get status of node. Throw exception if it doesn't exist.
     * @param node
     * @return
     */
    def apply(node: ModuleNode[T]): NodeStatus = {
      nodeStatus(node.element.getName())
    }

    /**
     * Check if a given node status is const
     * @param node
     * @return
     */
    def isConst(node: ModuleNode[T]): Boolean = {
      nodeStatus.contains(node.element.getName()) &&
        nodeStatus(node.element.getName()).isInstanceOf[Const]
    }

    /**
     * Check if a given node status is iteration
     * @param node
     * @return
     */
    def isIteration(node: ModuleNode[T]): Boolean = {
      nodeStatus.contains(node.element.getName()) &&
        nodeStatus(node.element.getName()).isInstanceOf[Iteration]
    }

    /**
     * If the given node has been executed or out of date
     * @param node
     * @return
     */
    def notExecuted(node: ModuleNode[T]): Boolean = {
      if (!nodeStatus.contains(node.element.getName())) return true
      if (nodeStatus(node.element.getName()).isInstanceOf[Iteration] &&
        nodeStatus(node.element.getName()).asInstanceOf[Iteration].outOfDate) {
        return true
      }
      return false
    }

    /**
     * Get current node status. It must be Iteration or exception is thrown
     * @param node
     * @return
     */
    def iteration(node: ModuleNode[T]): Iteration = {
      nodeStatus(node.element.getName()).asInstanceOf[Iteration]
    }

    /**
     * Remove unconst node status
     * @return
     */
    def removeUnConstStatus(): this.type = {
      val iter = nodeStatus.iterator
      while (iter.hasNext) {
        val entry = iter.next()
        if (!entry._2.isInstanceOf[Const]) {
          nodeStatus.remove(entry._1)
        }
      }
      this
    }
  }


  /**
   * Node status
   */
  private[nn] sealed trait NodeStatus

  /**
   * Current node is const or all of its dependencies are from const node
   */
  private[nn] case class Const() extends NodeStatus

  /**
   * Current nodes has been executed, while it's not const
   */
  private[nn] case class Ready() extends NodeStatus

  /**
   * Current node is in a loop
   * @param outOfDate is out of date
   * @param prev the stacked iteration for nested loop
   */
  private[nn] case class Iteration(val prev: Iteration, var outOfDate: Boolean = false)
    extends NodeStatus
}
