# set -x
SPARK_EXTRA_JAR_PATH=/ppml/trusted-big-data-ml/work/spark-3.1.2/jars/spark-encrypt-io-0.2-SNAPSHOT.jar
SPARK_JOB_MAIN_CLASS=com.intel.analytics.bigdl.ppml.examples.LocalCryptoExample
KMS_TYPE=SimpleKeyManagementService
INPUT_FILE_PATH=$1
LOCAL_IP=$2

secure_password=`openssl rsautl -inkey /ppml/trusted-big-data-ml/work/password/key.txt -decrypt </ppml/trusted-big-data-ml/work/password/output.bin`

/opt/jdk8/bin/java \
  -cp '/ppml/trusted-big-data-ml/work/spark-3.1.2/conf/:/ppml/trusted-big-data-ml/work/spark-3.1.2/jars/*:/ppml/trusted-big-data-ml/work/spark-3.1.2/examples/jars/spark-example-sql-e2e.jar' \
  -Xmx12g \
  org.apache.spark.deploy.SparkSubmit \
  --master local[2] \
  --conf spark.driver.host=$LOCAL_IP \
  --conf spark.driver.memory=16g \
  --num-executors 2 \
  --executor-cores 8 \
  --executor-memory 16g \
  --conf spark.ssl.enabled=true \
  --conf spark.ssl.port=8043 \
  --conf spark.ssl.keyPassword=$secure_password \
  --conf spark.ssl.keyStore=/ppml/trusted-big-data-ml/work/keys/keystore.jks \
  --conf spark.ssl.keyStorePassword=$secure_password \
  --conf spark.ssl.keyStoreType=JKS \
  --conf spark.ssl.trustStore=/ppml/trusted-big-data-ml/work/keys/keystore.jks \
  --conf spark.ssl.trustStorePassword=$secure_password \
  --conf spark.ssl.trustStoreType=JKS \
  --class $SPARK_JOB_MAIN_CLASS \
  $SPARK_EXTRA_JAR_PATH \
  $INPUT_FILE_PATH \
  $KMS_TYPE 2>&1 | tee simple-local-cryptos-example.log

