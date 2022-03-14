#!/bin/bash

SGX=1 ./pal_loader bash -c "/opt/jdk8/bin/java -Xms2g -Xmx2g -cp $SPARK_HOME/jars/*:$BIGDL_HOME/jars/bigdl-ppml-spark_3.1.2-2.1.0-SNAPSHOT-jar-with-dependencies.jar \
 com.intel.analytics.bigdl.ppml.example.HflLogisticRegression -d work/data/diabetes-hfl-1.csv"
