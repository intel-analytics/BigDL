/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
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

package com.intel.analytics.bigdl.optim

import com.intel.analytics.bigdl._
import com.intel.analytics.bigdl.dataset.{DistributedDataSet, MiniBatch, DataSet => DataSource}
import com.intel.analytics.bigdl.parameters.AllReduceParameter2
import com.intel.analytics.bigdl.tensor.Tensor
import com.intel.analytics.bigdl.tensor.TensorNumericMath.TensorNumeric
import com.intel.analytics.bigdl.utils._
import org.apache.log4j.Logger
import org.apache.spark.TaskContext
import org.apache.spark.rdd.RDD

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.reflect.ClassTag

object DistriOptimizer2 {
  import Optimizer._

  val logger = Logger.getLogger(getClass)

  /**
   * Optimizer cache some metadata on each executor
   *
   * @param localModels cached models
   * @param modelWeights weights of the cached models
   * @param modelGradients gradients of the cached models
   * @param localCriterions cached criterion
   * @param localStates cached state
   * @param gradient tensor buffer
   * @tparam T
   */
  case class Cache[T](
    localModels: Array[Module[T]],
    modelWeights: Array[Tensor[T]],
    modelGradients: Array[Tensor[T]],
    localCriterions: Array[Criterion[T]],
    localStates: Array[Table],
    gradient: Tensor[T],
    var moduleTimeList: Array[Long] = null
  )

  private[optim] def optimize[T: ClassTag](
    dataset: DistributedDataSet[MiniBatch[T]],
    coresPerNode: Int,
    state: Table,
    endWhen: Trigger,
    metrics: Metrics,
    models: RDD[Cache[T]],
    model: Module[T],
    optimMethod: OptimMethod[T],
    parameters: AllReduceParameter2[T],
    validationTrigger: Option[Trigger],
    validationDataSet: Option[DataSet[MiniBatch[T]]],
    validationMethods: Option[Array[ValidationMethod[T]]],
    cacheTrigger: Option[Trigger],
    cachePath: Option[String],
    isOverWrite: Boolean
  )(implicit ev: TensorNumeric[T]) = {
    val sc = dataset.originRDD().sparkContext
    val partitionNum = dataset.originRDD().partitions.length
    var wallClockTime = 0L
    var lastEpochTime = 0L
    val driverState = T("epoch" -> state.get[Int]("epoch").getOrElse(1),
      "neval" -> state.get[Int]("neval").getOrElse(1))
    val _subModelNumber = Engine.getEngineType match {
      case MklBlas => coresPerNode
      case _ => throw new IllegalArgumentException()
    }
    var accumulateCount = 0
    val shuffleBefore = System.nanoTime()
    logger.info(s"config $state")
    logger.info(s"Shuffle data")
    dataset.shuffle()
    val shuffleEnd = System.nanoTime()
    logger.info(s"Shuffle data complete. Takes ${(shuffleEnd - shuffleBefore) / 1e9}s")

    var tasks: ArrayBuffer[Future[_]] = new ArrayBuffer()
    var threshold = Long.MaxValue
    var timeout = Long.MaxValue
    var iteration = 0
    val dropPercentage = state.get[Double]("dropPercentage").get
    val warmupIterationNum = state.get[Int]("warmupIterationNum").get
    val comupteThresholdbatchSize = state.get[Int]("comupteThresholdbatchSize").get
    val driverSubModelNum = partitionNum * _subModelNumber
    var dropModelNumBatch = 0
    var lossArray = new Array[Double](_subModelNumber)

    var epochStart = System.nanoTime()
    while (!endWhen(driverState)) {
      val _header = header(driverState[Int]("epoch"), accumulateCount, dataset.size(),
        driverState[Int]("neval"), wallClockTime)
      val lossSum = sc.accumulator(0.0, "loss sum")
      val recordsNum = sc.accumulator(0, "record number")
      metrics.set("computing time for each node", mutable.ArrayBuffer[Double](), sc)
      metrics.set("computing time average", 0.0, sc, partitionNum)
      metrics.set("aggregate gradient time", 0.0, sc, partitionNum)

      metrics.set("task1 time from worker", mutable.ArrayBuffer[Double](), sc)
      metrics.set("task2 time from worker", mutable.ArrayBuffer[Double](), sc)
      metrics.set("task1 time from driver", mutable.ArrayBuffer[Double](), sc)
      metrics.set("task2 time from driver", mutable.ArrayBuffer[Double](), sc)
      metrics.set("get weights average", 0.0, sc, partitionNum)
      metrics.set("get weights for each node", mutable.ArrayBuffer[Double](), sc)
      metrics.set("get gradients average", 0.0, sc, partitionNum)
      metrics.set("get gradients for each node", mutable.ArrayBuffer[Double](), sc)

      val driverMetrics = metrics
      val start = System.nanoTime()

      val finishedModelNum = dataset.data(train = true).zipPartitions(
        models, true)(
        (data, modelIter) => {
          val task1WorkStart = System.nanoTime()
          val cached = modelIter.next()
          val syWStart = System.nanoTime()
          val weightsResult = parameters.getWeights(cached.modelWeights.head)
          val tensorBuffer = new Array[(Tensor[T], Tensor[T])](_subModelNumber)
          tasks += Engine.default.invoke(() => {
            val batch = data.next()
            var b = 0
            require(batch.data.size(1) == batch.labels.size(1))
            val stackSize = batch.data.size(1) / _subModelNumber
            while (b < _subModelNumber) {
              tensorBuffer(b) = (batch.data.narrow(1, b * stackSize + 1, stackSize),
                batch.labels.narrow(1, b * stackSize + 1, stackSize))
              b += 1
            }
          })
          tasks ++= Engine.default.invoke((0 until _subModelNumber).map(i => () => {
            cached.localModels(i).training()
            cached.localModels(i).zeroGradParameters()
          }))
          Engine.default.sync(tasks)
          weightsResult.waitResult()
          val weightSyncTime = System.nanoTime() - syWStart
          driverMetrics.add("get weights average", weightSyncTime)
          driverMetrics.add("get weights for each node", weightSyncTime)
          tasks.clear()

          // ======================Start train models===================================
          var time = System.nanoTime()
          if(iteration > warmupIterationNum + comupteThresholdbatchSize - 1) {
            timeout = threshold - weightSyncTime
          }
          val pre = (iteration % comupteThresholdbatchSize) * _subModelNumber
          val trainingThreads = Engine.default.invokeAndWait2((0 until _subModelNumber).map(i =>
            () => {
              val trainStart = System.nanoTime()
              val localModel = cached.localModels(i)
              localModel.training()
              val localCriterion = cached.localCriterions(i)
              val (input, target) = tensorBuffer(i)
              val output = localModel.forward(input)
              lossArray(i) = ev.toType[Double](localCriterion.forward(output, target))
              val errors = localCriterion.backward(output, target)
              localModel.backward(input, errors)
              cached.moduleTimeList(i + pre) = System.nanoTime() - trainStart + weightSyncTime
              i
            }
          ), timeout)
          val computingTime = System.nanoTime() - time
          driverMetrics.add("computing time average", computingTime)
          driverMetrics.add("computing time for each node", computingTime)

          val finishedThreads = trainingThreads.filter(!_.isCancelled).map(_.get())
          recordsNum += finishedThreads.size * tensorBuffer.head._2.size(1)
          var i = 0
          while (i < finishedThreads.size) {
            lossSum += lossArray(finishedThreads(i))
            i += 1
          }

          if (finishedThreads.size > 0) {
            time = System.nanoTime()
            val gradLength = cached.modelGradients(0).nElement()
            val taskSize = gradLength / _subModelNumber
            val extraTask = gradLength % _subModelNumber

            (0 until _subModelNumber).diff(finishedThreads).foreach(i =>
              cached.modelGradients(i).zero()
            )

            // copy multi-model gradient to the buffer
            val parallelNum = if (taskSize == 0) extraTask else _subModelNumber
            Engine.default.invokeAndWait((0 until parallelNum).map(tid => () => {
              val offset = tid * taskSize + math.min(tid, extraTask)
              val length = taskSize + (if (tid < extraTask) 1 else 0)
              var i = 0
              while (i < cached.modelGradients.length) {
                if (i == 0) {
                  cached.gradient.narrow(1, offset + 1, length)
                    .copy(cached.modelGradients(i).narrow(1, offset + 1, length))
                } else {
                  cached.gradient.narrow(1, offset + 1, length)
                    .add(cached.modelGradients(i).narrow(1, offset + 1, length))
                }
                i += 1
              }
            }))
            driverMetrics.add("aggregate gradient time", System.nanoTime() - time)
          }

          parameters.putGradients(cached.gradient)
          driverMetrics.add("task1 time from worker", System.nanoTime() - task1WorkStart)
          Iterator(finishedThreads.size)
        }).reduce(_ + _)
      metrics.add("task1 time from driver", System.nanoTime() - start)

      dropModelNumBatch += (driverSubModelNum - finishedModelNum)
      if (finishedModelNum > driverSubModelNum * 0.7) {
        val task2Start = System.nanoTime()
        val value = lossSum.value / finishedModelNum
        models.mapPartitions(modelIter => {
          val task2WorkerStart = System.nanoTime()
          val modelCache = modelIter.next()

          val getGstart = System.nanoTime()
          parameters.aggregrateGradientParition()
          val getGTime = System.nanoTime() - getGstart
          driverMetrics.add("get gradients average", getGTime)
          driverMetrics.add("get gradients for each node", getGTime)

          parameters.gradientPartition.div(ev.fromType(finishedModelNum))
          modelCache.localStates.head("neval") = driverState[Int]("neval")
          modelCache.localStates.head("epoch") = driverState[Int]("epoch")
          optimMethod.optimize(_ => (ev.fromType(value), parameters.gradientPartition),
            parameters.weightPartition, modelCache.localStates.head, modelCache.localStates.head)

          parameters.sendWeightPartition()

          driverMetrics.add("task2 time from worker", System.nanoTime() - task2WorkerStart)
          Iterator.empty
        }).count()

        metrics.add("task2 time from driver", System.nanoTime()-task2Start)

        accumulateCount += recordsNum.value
        val end = System.nanoTime()
        wallClockTime += end - start
        logger.info(s"${_header} Train ${recordsNum.value} in ${(end - start) / 1e9}seconds. " +
          s"Throughput is ${recordsNum.value / ((end - start) / 1e9)} records/second. Loss is ${
            lossSum.value / finishedModelNum
          }. ")
        logger.debug("\n" + metrics.summary())
        logger.info("Dropped modules: " + (driverSubModelNum - finishedModelNum))
        lossArray = new Array[Double](_subModelNumber)

        // compute threshold
        iteration += 1
        if (iteration > warmupIterationNum && iteration % comupteThresholdbatchSize == 0) {
          val moduleTimeList = models.mapPartitions { iter =>
            iter.next().moduleTimeList.iterator
          }.collect()

          val k = (dropPercentage * comupteThresholdbatchSize * driverSubModelNum).toInt
          val dropModelsNum = driverSubModelNum - dropModelNumBatch
          if (k > dropModelsNum) {
            threshold = Util.kthLargest(moduleTimeList, 0, moduleTimeList.length-1,
              k - dropModelsNum)
          } else {
            threshold = (threshold * 1.01).toLong
          }
          logger.info("threshold: " + threshold)

          // clear moduleTimeList in each node
          models.mapPartitions { iter =>
            val timeList = iter.next.moduleTimeList
            var i = 0
            while (i < timeList.length) {
              timeList(i) = 0
              i += 1
            }
            Iterator.empty
          }.count()
          dropModelNumBatch = 0
        }

        driverState("neval") = driverState[Int]("neval") + 1
        if (accumulateCount >= dataset.size()) {
          val epochEnd = System.nanoTime()
          wallClockTime = lastEpochTime + epochEnd - epochStart
          lastEpochTime = wallClockTime
          epochStart = System.nanoTime()
          logger.info(s"${_header} Epoch finished. Wall clock time is ${wallClockTime / 1e6}ms")

          driverState("epoch") = driverState[Int]("epoch") + 1
          dataset.shuffle()
          accumulateCount = 0
        }
        validate(
          validationTrigger,
          validationDataSet,
          validationMethods,
          coresPerNode,
          models,
          wallClockTime,
          driverState
        )

        checkpoint(
          cacheTrigger,
          cachePath,
          isOverWrite,
          wallClockTime,
          models,
          model,
          driverState,
          parameters
        )
      } else {
        logger.info(s"Warning!!! Ignore this iteration as more than 30% " +
          s"module is dropped!! Finished modules number: ${finishedModelNum}")
      }
    }
  }


  private def checkpoint[T: ClassTag](
    cacheTrigger: Option[Trigger],
    cachePath: Option[String],
    isOverWrite: Boolean,
    wallClockTime: Long,
    models: RDD[Cache[T]],
    model: Module[T],
    state: Table,
    parameters: AllReduceParameter2[T])
  : Unit = {
    if (cacheTrigger.isDefined) {
      val trigger = cacheTrigger.get
      if (trigger(state) && cachePath.isDefined) {
        println(s"[Wall Clock ${wallClockTime / 1e9}s] Save model to ${cachePath.get}")
        saveModel(getModel(models, model, parameters), cachePath, isOverWrite,
          s".${state[Int]("neval")}")
        saveState(models.map(_.localStates.head).first(), cachePath, isOverWrite, s"" +
          s".${state[Int]("neval")}")
      }
    }
  }

  private def initThreadModels[T: ClassTag](
    model: Module[T],
    dataset: DistributedDataSet[MiniBatch[T]],
    criterion: Criterion[T],
    state: Table,
    nodeNumber: Int,
    coresPerNode: Int,
    checkSingleton: Boolean,
    parameters: AllReduceParameter2[T]
    )(implicit ev: TensorNumeric[T]) = {
    val sc = dataset.originRDD().sparkContext
    val broadcast = sc.broadcast((model, criterion, state))
    val _subModelNumber = Engine.getEngineType match {
      case MklBlas => coresPerNode
      case _ => throw new IllegalArgumentException
    }

    require(dataset.originRDD().partitions.length == nodeNumber,
      s"Passed in rdd partition number ${dataset.originRDD().partitions.length}" +
        s" is not equal to configured node number ${nodeNumber}")

    val partitionNum = dataset.originRDD().partitions.length
    val comupteThresholdbatchSize = state.get[Int]("comupteThresholdbatchSize").get
    val models = dataset.originRDD().mapPartitions(_ => {
      val (broadcastModel, broadcastCriterion, broadcastState) = broadcast.value
      if (checkSingleton) {
        require(Engine.checkSingleton(), "Detect multi-task run on one Executor/Container. " +
          "Currently not support this")
      }
      val cached = (0 until _subModelNumber).map { _ =>
        val localModel = broadcastModel.cloneModule()
        val localCriterion = broadcastCriterion.cloneCriterion()
        val localState = broadcastState.clone()
        val (weights, grads) = localModel.getParameters()
        (localModel, weights, grads, localCriterion, localState)
      }.toArray

      val weights = cached.head._2
      cached.map(c =>
        if (!c._2.eq(weights)) {
          c._2.storage().set(weights.storage())
        }
      )

      logger.info("model thread pool size is " + Engine.model.getPoolSize)
      parameters.init(weights)

      Iterator(Cache(
        cached.map(_._1), // models
        cached.map(_._2), // weights
        cached.map(_._3), // gradients
        cached.map(_._4), // criterions
        cached.map(_._5), // states
        cached.head._2.clone(), // a tensor buffer
        new Array[Long](_subModelNumber * comupteThresholdbatchSize)
      ))
    }).persist()
    models.setName("Thread Model RDD")
    logger.info("Cache thread models...")
    models.count()
    logger.info("Cache thread models... done")
    models
  }


  private def validate[T](
    validationTrigger: Option[Trigger],
    validationDataSet: Option[DataSet[MiniBatch[T]]],
    validationMethods: Option[Array[ValidationMethod[T]]],
    coresPerNode: Int,
    models: RDD[Cache[T]],
    wallClockTime: Long,
    state: Table
  ): Unit = {
    if (validationTrigger.isEmpty || validationDataSet.isEmpty) {
      return
    }
    val trigger = validationTrigger.get
    if (!trigger(state)) {
      return
    }
    val vMethods = validationMethods.get
    val validateRDD = validationDataSet.get.toDistributed().data(train = false)
    logger.info(s"[Wall Clock ${wallClockTime / 1e9}s] Validate model...")
    val _subModelNumber = Engine.getEngineType match {
      case MklBlas => coresPerNode
      case _ => throw new IllegalArgumentException
    }
    models.zipPartitions(validateRDD)((modelIter, dataIter) => {
      val workingModels = modelIter.next().localModels
      workingModels.foreach(_.evaluate())
      dataIter.map(batch => {
        require(batch.data.size(1) == batch.labels.size(1))
        val stackSize = batch.data.size(1) / _subModelNumber
        val extraSize = batch.data.size(1) % _subModelNumber
        val parallelism = if (stackSize == 0) extraSize else _subModelNumber
        Engine.default.invokeAndWait(
          (0 until parallelism).map(b =>
            () => {
              val offset = b * stackSize + math.min(b, extraSize)
              val length = stackSize + (if (b < extraSize) 1 else 0)
              val input = batch.data.narrow(1, offset + 1, length)
              val target = batch.labels.narrow(1, offset + 1, length)
              val output = workingModels(b).forward(input)
              vMethods.map(validation => {
                validation(output, target)
              })
            }
          )
        ).reduce((left, right) => {
          left.zip(right).map { case (l, r) =>
            l + r
          }
        })
      })
    }).reduce((left, right) => {
      left.zip(right).map { case (l, r) =>
        l + r
      }
    }).zip(vMethods).foreach(r => {
      logger.info(s"${r._2} is ${r._1}")
    })
  }


  private def getModel[T: ClassTag](models: RDD[Cache[T]], model: Module[T],
    parameters: AllReduceParameter2[T]): Module[T] = {
    val partitionNum = models.partitions.length
    val weights = models.mapPartitions(iter => {
      val cached = iter.next()
      val curPartitionId = TaskContext.getPartitionId()
      Iterator.single(Map(curPartitionId -> parameters.weightPartition))
    }).reduce(_ ++ _)

    val parameter = model.getParameters()._1
    val parameterLength = parameter.nElement()
    val taskSize = parameterLength / partitionNum
    require(taskSize != 0, "parameter length should not less than partition number")
    val extraSize = parameterLength % partitionNum

    (0 until partitionNum).map(pid => {
      val start = pid * taskSize + math.min(pid, extraSize)
      val length = taskSize + (if (pid < extraSize) 1 else 0)
      parameter.narrow(1, start + 1, length).copy(weights(pid))
    })

    model
  }
}

class DistriOptimizer2[T: ClassTag] private[optim](
  model: Module[T],
  dataset: DistributedDataSet[MiniBatch[T]],
  criterion: Criterion[T]
)
  (implicit ev: TensorNumeric[T])
  extends Optimizer[T, MiniBatch[T]](
    model, dataset, criterion) {

  def disableCheckSingleton(): this.type = {
    this.checkSingleton = false
    this
  }

  private var checkSingleton = true

  val metrics = new Metrics

  private var models: RDD[DistriOptimizer2.Cache[T]] = null

  override def optimize(): Module[T] = {
    optimMethod.clearHistory(state)
    state("dropPercentage") = dropPercentage
    state("warmupIterationNum") = warmupIterationNum
    state("comupteThresholdbatchSize") = comupteThresholdbatchSize

    require(Engine.nodeNumber().isDefined, "Node number is not set")
    val nodeNumber = Engine.nodeNumber().get
    val coresPerNode = Engine.coreNumber()

    val partitionNum = dataset.originRDD().partitions.length
    val size = model.getParameters()._1.nElement()
    val parameters = AllReduceParameter2.newParameter(partitionNum, size)

    models = DistriOptimizer2.initThreadModels(
      model, dataset, criterion, state, nodeNumber, coresPerNode, checkSingleton, parameters)


    DistriOptimizer2.optimize(
      dataset,
      coresPerNode,
      state,
      endWhen,
      metrics,
      models,
      model,
      optimMethod,
      parameters,
      validationTrigger,
      validationDataSet,
      validationMethods,
      checkpointTrigger,
      checkpointPath,
      isOverWrite
    )

    DistriOptimizer2.getModel(models, model, parameters)
  }
}


