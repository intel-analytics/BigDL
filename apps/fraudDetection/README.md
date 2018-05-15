As a highly simplified demo, this notebook uses the public data set to build a fraud detection example on Apache Spark.

The notebook is developed using Scala with Analytics Zoo for Spark 2.1 and BigDL.

How to run the notebook:

1. Refer to [Apache Toree](https://github.com/apache/incubator-toree/blob/master/README.md) for
how to use scala in Jupyter notebook.

An outline is:
```bash
pip install https://dist.apache.org/repos/dist/dev/incubator/toree/0.2.0/snapshots/dev1/toree-pip/toree-0.2.0.dev1.tar.gz
```

2. Build Analytics Zoo jar file under Spark 2.x.

3. To support the training for imbalanced data set in fraud detection, some Transformers and algorithms are developed in 
https://github.com/intel-analytics/analytics-zoo/tree/legacy/pipeline/fraudDetection.

4. build jar file with maven under the directory

```
mvn package
```

5. Start the notebook.

```
SPARK_OPTS='--master=local[1] --jars /path/to/zoo/jar/file,/.../target/fraud-1.0.1-SNAPSHOT-jar-with-dependencies.jar' TOREE_OPTS='--nosparkcontext' jupyter notebook
```
