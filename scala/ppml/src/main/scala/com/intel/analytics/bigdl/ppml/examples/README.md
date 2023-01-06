## End to end Spark GBT Example On CriteoClickLogsDataset

### Usage

Run this example in spark local mode:

1.select a KeyManagementService(`SimpleKeyManagementService` or `EHSMKeyManagementService`)

2.prepare 1g [dataset](https://ailab.criteo.com/download-criteo-1tb-click-logs-dataset/) and a primaryKey and a dataKey(refer to [this](https://github.com/intel-analytics/BigDL/blob/main/ppml/services/kms-utils/docker/README.md))

3.prepare a BigDL PPML Client Container(refer to PPML tutorial)

4.run the following command in the container

> your input file can be CSV, JSON, PARQUET or other textfile with or without encryption. if input file is not encrypted, specify the `inputEncryptMode==plain_text`. else, for encrypted CSV, JSON and other textfile, specify the `inputEncryptMode==AES/CBC/PKCS5Padding`. for encrypted parquet file, specify the `inputEncryptMode==AES_GCM_CTR_V1 or AES_GCM_V1`.
>
> in this example, the input file is a plain CSV file

- for `SimpleKeyManagementService` 

  ```bash
  /opt/jdk8/bin/java \
      -cp '/ppml/trusted-big-data-ml/spark-encrypt-io-0.3.0-SNAPSHOT.jar:/ppml/trusted-big-data-ml/work/bigdl-2.1.0-SNAPSHOT/jars/*:/ppml/trusted-big-data-ml/work/spark-3.1.2/conf/:/ppml/trusted-big-data-ml/work/spark-3.1.2/jars/*:/ppml/trusted-big-data-ml/work/spark-3.1.2/examples/jars/*' -Xmx16g \
      org.apache.spark.deploy.SparkSubmit \
      --master local[4] \
      --executor-memory 8g \
      --driver-memory 8g \
      --class com.intel.analytics.bigdl.ppml.examples.xgbClassifierTrainingExampleOnCriteoClickLogsDataset \
      --conf spark.network.timeout=10000000 \
      --conf spark.executor.heartbeatInterval=10000000 \
      --verbose \
      --jars local:///ppml/trusted-big-data-ml/work/bigdl-2.1.0-SNAPSHOT/jars/bigdl-ppml-spark_3.1.2-2.1.0-SNAPSHOT.jar \
      local:///ppml/trusted-big-data-ml/work/bigdl-2.1.0-SNAPSHOT/jars/bigdl-ppml-spark_3.1.2-2.1.0-SNAPSHOT.jar \
      --trainingDataPath /your/training/data/path \
      --modelSavePath /your/model/save/path \
      --inputEncryptMode plain_text \
      --primaryKeyPath /your/primary/key/path/primaryKey \
      --dataKeyPath /your/data/key/path/dataKey \
      --kmsType SimpleKeyManagementService \
      --simpleAPPID your_app_id \
      --simpleAPIKEY your_api_key \
      --maxDepth 5 \
      --maxIter 100
  ```

- for `EHSMKeyManagementService`

  ```bash
  /opt/jdk8/bin/java \
      -cp '/ppml/trusted-big-data-ml/spark-encrypt-io-0.3.0-SNAPSHOT.jar:/ppml/trusted-big-data-ml/work/bigdl-2.1.0-SNAPSHOT/jars/*:/ppml/trusted-big-data-ml/work/spark-3.1.2/conf/:/ppml/trusted-big-data-ml/work/spark-3.1.2/jars/*:/ppml/trusted-big-data-ml/work/spark-3.1.2/examples/jars/*' -Xmx16g \
      org.apache.spark.deploy.SparkSubmit \
      --master local[4] \
      --executor-memory 8g \
      --driver-memory 8g \
      --class com.intel.analytics.bigdl.ppml.examples.xgbClassifierTrainingExampleOnCriteoClickLogsDataset \
      --conf spark.network.timeout=10000000 \
      --conf spark.executor.heartbeatInterval=10000000 \
      --verbose \
      --jars local:///ppml/trusted-big-data-ml/work/bigdl-2.1.0-SNAPSHOT/jars/bigdl-ppml-spark_3.1.2-2.1.0-SNAPSHOT.jar \
      local:///ppml/trusted-big-data-ml/work/bigdl-2.1.0-SNAPSHOT/jars/bigdl-ppml-spark_3.1.2-2.1.0-SNAPSHOT.jar \
      --trainingDataPath /your/training/data/path \
      --modelSavePath /your/model/save/path \
      --inputEncryptMode plain_text \
      --primaryKeyPath /your/primary/key/path/primaryKey \
      --dataKeyPath /your/data/key/path/dataKey \
      --kmsType EHSMKeyManagementService \
      --kmsServerIP you_kms_server_ip \
      --kmsServerPort you_kms_server_port \
      --ehsmAPPID your_app_id \
      --ehsmAPIKEY your_api_key \
      --maxDepth 5 \
      --maxIter 100
  ```

## LocalCryptoExample with BKeywhiz KMS

### Usage

- for `BKeywhizKeyManagementService`

  ```bash
  /opt/jdk8/bin/java \
      -cp ${BIGDL_HOME}/jars/*:${SPARK_HOME}/conf/:${SPARK_HOME}/jars/* \
      -Xmx16g \
      org.apache.spark.deploy.SparkSubmit \
      --master local[4] \
      --class com.intel.analytics.bigdl.ppml.examples.LocalCryptoExample \
      --verbose \
      --jars local:///ppml/trusted-big-data-ml/work/bigdl-${BIGDL_VERSION}/jars/bigdl-ppml-spark_${SPARK_VERSION}-${BIGDL_VERSION}.jar \
      local:////ppml/trusted-big-data-ml/work/spark-${SPARK_VERSION}/examples/jars/spark-examples_2.12-$SPARK_VERSION.jar \
      --primaryKeyPath specify_a_primary_key_name_like_BobPrimaryKey1 \
      --dataKeyPath specify_a_primary_key_name_like_BobDataKey1 \
      --kmsType BKeywhizKeyManagementService \
      --kmsServerIP bkeywhiz_kms_server_ip \
      --kmsServerPort bkewyhiz_kms_server_port \
  ```

  
