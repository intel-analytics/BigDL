#!/bin/bash
cd /ppml
export secure_password=`openssl rsautl -inkey /ppml/password/key.txt -decrypt </ppml/password/output.bin`
bash bigdl-ppml-submit.sh \
        --sgx-enabled true \
        --deploy-mode cluster \
        --master $RUNTIME_SPARK_MASTER \
        --sgx-driver-jvm-memory 6g\
        --sgx-executor-jvm-memory 6g\
        --num-executors 2 \
        --driver-memory 4g \
        --driver-cores 8 \
        --executor-memory 4g \
        --executor-cores 8\
        --conf spark.cores.max=64 \
        --conf spark.kubernetes.container.image=$RUNTIME_K8S_SPARK_IMAGE \
        --conf spark.kubernetes.container.image.pullPolicy=Always \
        --class org.apache.spark.examples.sql.SparkSQLExample \
        --name pyspark-sqlexample-gramine \
        --log-file pyspark-spark-sql-example-sgx.log \
        --verbose \
        /ppml/spark-$SPARK_VERSION/examples/src/main/python/sql/basic.py

echo "#### Excepted result(pyspark-spark-sql-example-sgx): 10"
echo "---- Actual result: "
cat pyspark-spark-sql-example-sgx.log | egrep -a 'Justin' | wc -l
