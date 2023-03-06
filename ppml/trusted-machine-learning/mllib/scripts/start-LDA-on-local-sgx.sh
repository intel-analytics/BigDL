#!/bin/bash
# source code link: https://github.com/apache/spark/blob/master/examples/src/main/scala/org/apache/spark/examples/ml/LDAExample.scala
cd /ppml

input_file="data/mllib/sample_lda_libsvm_data.txt"

if [ -f "$input_file" ]; then
    echo "Input file exists."
else
    echo "Input file not exists, downloading the file"
    wget https://raw.githubusercontent.com/apache/spark/master/data/mllib/sample_lda_libsvm_data.txt -O "$input_file"
fi


export sgx_command="/opt/jdk8/bin/java \
    -cp "/ppml/spark-${SPARK_VERSION}/conf/:/ppml/spark-${SPARK_VERSION}/jars/*:/ppml/spark-${SPARK_VERSION}/examples/jars/*" -Xmx1g \
    org.apache.spark.deploy.SparkSubmit \
    --master local[2] \
    --driver-memory 32g \
    --driver-cores 8 \
    --executor-memory 32g \
    --executor-cores 8 \
    --num-executors 2 \
    --class org.apache.spark.examples.ml.LDAExample \
    --name LDAExample \
    --verbose \
    --jars local://${SPARK_HOME}/examples/jars/spark-examples_2.12-${SPARK_VERSION}.jar \
    local://${SPARK_HOME}/examples/jars/spark-examples_2.12-${SPARK_VERSION}.jar 3000"
gramine-sgx bash 2>&1 | tee random-forest-classfier-local-sgx.log