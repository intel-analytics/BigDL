#!/bin/bash

${SPARK_HOME}/bin/spark-submit \
    --master k8s://https://${kubernetes_master_url}:6443 \
    --deploy-mode cluster \
    --name spark-lgbm-criteo \
    --class com.intel.analytics.bigdl.dllib.example.nnframes.lightGBM.lgbmClassifierTrainingExampleOnCriteoClickLogsDataset \
    --conf spark.rpc.netty.dispatcher.numThreads=32 \
    --conf spark.kubernetes.container.image=intelanalytics/bigdl-ppml-trusted-big-data-ml-scala-occlum:2.4.0 \
    --conf spark.kubernetes.container.image.pullPolicy="IfNotPresent" \
    --conf spark.kubernetes.authenticate.driver.serviceAccountName=spark \
    --conf spark.kubernetes.executor.deleteOnTermination=false \
    --conf spark.kubernetes.driver.podTemplateFile=./driver.yaml \
    --conf spark.kubernetes.executor.podTemplateFile=./executor.yaml \
    --conf spark.kubernetes.file.upload.path=file:///tmp \
    --conf spark.kubernetes.executor.podNamePrefix="sparklgbm_criteo" \
    --conf spark.kubernetes.sgx.log.level=off \
    --num-executors 2 \
    --executor-cores 4 \
    --conf spark.cores.max=8 \
    --executor-memory 1g \
    --conf spark.kubernetes.driverEnv.SGX_DRIVER_JVM_MEM_SIZE="1G" \
    --conf spark.executorEnv.SGX_EXECUTOR_JVM_MEM_SIZE="5G" \
    --verbose \
    local:/opt/spark/examples/jars/spark-examples_2.12-3.1.3.jar \
    -i /host/data/lgbm_data -s /host/data/save_path -I 50 -d 10
