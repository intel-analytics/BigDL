#!/bin/bash
cd /ppml

deploy_mode=cluster && \
driver_name=pyspark-simplequery-on-k8s-cluster-sgx && \
secure_password=`openssl rsautl -inkey /ppml/password/key.txt -decrypt </ppml/password/output.bin` && \
export SPARK_LOCAL_IP=$LOCAL_IP && \
/opt/jdk8/bin/java \
    -cp $SPARK_HOME/conf/:$SPARK_HOME/jars/*:$SPARK_HOME/examples/jars/*:$BIGDL_HOME/jars/* \
    -Xmx10g org.apache.spark.deploy.SparkSubmit \
    --master $RUNTIME_SPARK_MASTER \
    --deploy-mode $deploy_mode \
    --name $driver_name \
    --conf spark.driver.host=$LOCAL_IP \
    --conf spark.driver.port=54321 \
    --conf spark.driver.memory=32g \
    --conf spark.executor.cores=8 \
    --conf spark.executor.memory=32g \
    --conf spark.executor.instances=4 \
    --conf spark.cores.max=32 \
    --conf spark.kubernetes.authenticate.driver.serviceAccountName=spark \
    --conf spark.kubernetes.container.image=$RUNTIME_K8S_SPARK_IMAGE \
    --conf spark.kubernetes.driver.podTemplateFile=/ppml/spark-driver-template.yaml \
    --conf spark.kubernetes.executor.podTemplateFile=/ppml/spark-executor-template.yaml \
    --conf spark.kubernetes.executor.deleteOnTermination=false \
    --conf spark.kubernetes.container.image.pullPolicy=Always \
    --conf spark.network.timeout=10000000 \
    --conf spark.executor.heartbeatInterval=10000000 \
    --conf spark.python.use.daemon=false \
    --conf spark.python.worker.reuse=false \
    --conf spark.kubernetes.sgx.enabled=true \
    --conf spark.kubernetes.sgx.executor.jvm.mem=6g \
    --conf spark.kubernetes.sgx.driver.jvm.mem=4g \
    --conf spark.authenticate=true \
    --conf spark.authenticate.secret=$secure_password \
    --conf spark.kubernetes.executor.secretKeyRef.SPARK_AUTHENTICATE_SECRET="spark-secret:secret" \
    --conf spark.kubernetes.driver.secretKeyRef.SPARK_AUTHENTICATE_SECRET="spark-secret:secret" \
    --conf spark.authenticate.enableSaslEncryption=true \
    --conf spark.network.crypto.enabled=true \
    --conf spark.network.crypto.keyLength=128 \
    --conf spark.network.crypto.keyFactoryAlgorithm=PBKDF2WithHmacSHA1 \
    --conf spark.io.encryption.enabled=true \
    --conf spark.io.encryption.keySizeBits=128 \
    --conf spark.io.encryption.keygen.algorithm=HmacSHA1 \
    --conf spark.ssl.enabled=true \
    --conf spark.ssl.port=8043 \
    --conf spark.ssl.keyPassword=$secure_password \
    --conf spark.ssl.keyStore=/ppml/keys/keystore.jks \
    --conf spark.ssl.keyStorePassword=$secure_password \
    --conf spark.ssl.keyStoreType=JKS \
    --conf spark.ssl.trustStore=/ppml/keys/keystore.jks \
    --conf spark.ssl.trustStorePassword=$secure_password \
    --conf spark.ssl.trustStoreType=JKS \
    --jars local://$SPARK_HOME/examples/jars/scopt_2.12-3.7.1.jar,$(echo $BIGDL_HOME/jars/* |tr ' ' ',' | sed "s#${BIGDL_HOME}#local://${BIGDL_HOME}#g") \
    --py-files local:///ppml/bigdl-$BIGDL_VERSION/python/bigdl-ppml-spark_$SPARK_VERSION-$BIGDL_VERSION-python-api.zip,local:///ppml/bigdl-$BIGDL_VERSION/python/bigdl-spark_$SPARK_VERSION-$BIGDL_VERSION-python-api.zip,local:///ppml/bigdl-$BIGDL_VERSION/python/bigdl-dllib-spark_$SPARK_VERSION-$BIGDL_VERSION-python-api.zip \
    local:///ppml/data/SimpleQueryExampleWithSimpleKMS/code/simple_query_example.py \
    --simple_app_id 465227134889 \
    --simple_app_key 799072978028 \
    --primary_key_path /ppml/data/SimpleQueryExampleWithSimpleKMS/keys/simple/primaryKey \
    --data_key_path /ppml/data/SimpleQueryExampleWithSimpleKMS/keys/simple/dataKey \
    --input_path /ppml/data/SimpleQueryExampleWithSimpleKMS/ppml_test/input \
    --output_path /ppml/data/SimpleQueryExampleWithSimpleKMS/ppml_test/output \
    --input_encrypt_mode AES/CBC/PKCS5Padding \
    --output_encrypt_mode plain_text 2>&1 | tee pyspark-simplequery-on-k8s-cluster-sgx.log
