This document starts with simple native [python examples](#python-examples) and native [spark examples](#spark-examples), which is an easy way to verify if the Trusted PPML environment is correctly set up. Then [Trusted Data Analysis](#trusted-data-analysis), [Trusted ML](#trusted-ml), [Trusted DL](#trusted-dl) and [Trusted FL](#trusted-fl) examples are given.

## Python Examples
### Run Trusted Python Helloworld
<details><summary>This example shows how to run trusted native python Helloworld.</summary>


Run the script to run trusted Python Helloworld:

```bash
bash work/start-scripts/start-python-helloworld-sgx.sh
```

Open another terminal and check the log:

```bash
sudo docker exec -it spark-local cat /ppml/trusted-big-data-ml/test-helloworld-sgx.log | egrep "Hello World"
```

The result should look something like this:

> Hello World
</details>

### Run Trusted Python Numpy
<details><summary>This example shows how to run trusted native python numpy.</summary>


Run the script to run trusted Python Numpy:

```bash
bash work/start-scripts/start-python-numpy-sgx.sh
```

Open another terminal and check the log:

```bash
sudo docker exec -it spark-local cat /ppml/trusted-big-data-ml/test-numpy-sgx.log | egrep "numpy.dot"
```

The result should look something like this:

>  numpy.dot: 0.034211914986371994 sec
</details>

## Spark Examples
### Run Trusted Spark Pi
<details><summary>This example runs a simple Spark PI program.</summary>


Run the script to run trusted Spark Pi:

```bash
bash work/start-scripts/start-spark-local-pi-sgx.sh
```

Open another terminal and check the log:

```bash
sudo docker exec -it spark-local cat /ppml/trusted-big-data-ml/test-pi-sgx.log | egrep "roughly"
```

The result should look something like this:

> Pi is roughly 3.146760
#### mode * 4
</details>


### Run Trusted Spark Wordcount
<details><summary>This example runs a simple Spark Wordcount program.</summary>


Run the script to run trusted Spark Wordcount:

```bash
bash work/start-scripts/start-spark-local-wordcount-sgx.sh
```

Open another terminal and check the log:

```bash
sudo docker exec -it spark-local cat /ppml/trusted-big-data-ml/test-wordcount-sgx.log | egrep "print"
```

The result should look something like this:

> print("Hello: 1
>
> print(sys.path);: 1
#### spark local & k8s
</details>

## Trusted Data Analysis
### Run Trusted Spark SQL
<details><summary>This example shows how to run trusted Spark SQL.</summary>

  
First, make sure that the paths of resource in `/ppml/trusted-big-data-ml/work/spark-2.4.6/examples/src/main/python/sql/basic.py` are the same as the paths of `people.json`  and `people.txt`.

Run the script to run trusted Spark SQL:

```bash
bash work/start-scripts/start-spark-local-sql-sgx.sh
```

Open another terminal and check the log:

```bash
sudo docker exec -it spark-local cat /ppml/trusted-big-data-ml/test-sql-basic-sgx.log | egrep "Justin"
```

The result should look something like this:

>| 19| Justin|
>
>| Justin|
>
>| Justin| 20|
>
>| 19| Justin|
>
>| 19| Justin|
>
>| 19| Justin|
>
>Name: Justin
>
>| Justin|
</details>

### Run Trusted Spark SQL (TPC-H)
<details><summary>TPC-H with Trusted SparkSQL on Kubernetes</summary>
https://bigdl.readthedocs.io/en/latest/doc/PPML/QuickStart/tpc-h_with_sparksql_on_k8s.html
</details>


### Run Trusted Spark SQL (TPC-DS)
<details><summary>TPC-DS with Trusted SparkSQL on Kubernetes</summary>
https://bigdl.readthedocs.io/en/latest/doc/PPML/QuickStart/tpc-ds_with_sparksql_on_k8s.html
</details>


### Run Trusted SimpleQuery
<details><summary>Trusted SimpleQuery</summary>
https://github.com/liu-shaojun/BigDL/blob/ppml_doc/ppml/README.md#examples-of-using-end-to-end-ppml-workflow
</details>

## Trusted ML 
### Run Trusted Spark XGBoost Regressor
<details><summary>This example shows how to run trusted Spark XGBoost Regressor.</summary>


First, make sure that `Boston_Housing.csv` is under `work/data` directory or the same path in the `start-spark-local-xgboost-regressor-sgx.sh`. Replace the value of `RABIT_TRACKER_IP` with your own IP address in the script.

Run the script to run trusted Spark XGBoost Regressor and it would take some time to show the final results:

```bash
bash work/start-scripts/start-spark-local-xgboost-regressor-sgx.sh
```

Open another terminal and check the log:

```bash
sudo docker exec -it spark-local cat /ppml/trusted-big-data-ml/test-bigdl-xgboost-regressor-sgx.log | egrep "prediction" -A19
```

The result should look something like this:

> | features|label| prediction|
>
> +--------------------+-----+------------------+
>
> |[41.5292,0.0,18.1...| 8.5| 8.51994514465332|
>
> |[67.9208,0.0,18.1...| 5.0| 5.720333099365234|
>
> |[20.7162,0.0,18.1...| 11.9|10.601168632507324|
>
> |[11.9511,0.0,18.1...| 27.9| 26.19390106201172|
>
> |[7.40389,0.0,18.1...| 17.2|16.112293243408203|
>
> |[14.4383,0.0,18.1...| 27.5|25.952226638793945|
>
> |[51.1358,0.0,18.1...| 15.0| 14.67484188079834|
>
> |[14.0507,0.0,18.1...| 17.2|16.112293243408203|
>
> |[18.811,0.0,18.1,...| 17.9| 17.42863655090332|
>
> |[28.6558,0.0,18.1...| 16.3| 16.0191593170166|
>
> |[45.7461,0.0,18.1...| 7.0| 5.300708770751953|
>
> |[18.0846,0.0,18.1...| 7.2| 6.346951007843018|
>
> |[10.8342,0.0,18.1...| 7.5| 6.571983814239502|
>
> |[25.9406,0.0,18.1...| 10.4|10.235769271850586|
>
> |[73.5341,0.0,18.1...| 8.8| 8.460335731506348|
>
> |[11.8123,0.0,18.1...| 8.4| 9.193297386169434|
>
> |[11.0874,0.0,18.1...| 16.7|16.174896240234375|
>
> |[7.02259,0.0,18.1...| 14.2| 13.38729190826416|
</details>
  
### Run Trusted Spark XGBoost Classifier
<details><summary>This example shows how to run trusted Spark XGBoost Classifier.</summary>


Before running the example, download the sample dataset from [pima-indians-diabetes](https://raw.githubusercontent.com/jbrownlee/Datasets/master/pima-indians-diabetes.data.csv) dataset. After downloading the dataset, make sure that `pima-indians-diabetes.data.csv` is under `work/data` directory or the same path in the `start-spark-local-xgboost-classifier-sgx.sh`. Replace `path_of_pima_indians_diabetes_csv` with your path of `pima-indians-diabetes.data.csv`  and the value of `RABIT_TRACKER_IP` with your own IP address in the script.

Run the script to run trusted Spark XGBoost Classifier and it would take some time to show the final results:

```bash
bash start-spark-local-xgboost-classifier-sgx.sh
```

Open another terminal and check the log:

```bash
sudo docker exec -it spark-local cat /ppml/trusted-big-data-ml/test-xgboost-classifier-sgx.log | egrep "prediction" -A7
```

The result should look something like this:

> | f1|  f2| f3| f4|  f5| f6|  f7| f8|label|    rawPrediction|     probability|prediction|
>
> +----+-----+----+----+-----+----+-----+----+-----+--------------------+--------------------+----------+
>
> |11.0|138.0|74.0|26.0|144.0|36.1|0.557|50.0| 1.0|[-0.8209581375122...|[0.17904186248779...|    1.0|
>
> | 3.0|106.0|72.0| 0.0| 0.0|25.8|0.207|27.0| 0.0|[-0.0427864193916...|[0.95721358060836...|    0.0|
>
> | 6.0|117.0|96.0| 0.0| 0.0|28.7|0.157|30.0| 0.0|[-0.2336160838603...|[0.76638391613960...|    0.0|
>
> | 2.0| 68.0|62.0|13.0| 15.0|20.1|0.257|23.0| 0.0|[-0.0315906107425...|[0.96840938925743...|    0.0|
>
> | 9.0|112.0|82.0|24.0| 0.0|28.2|1.282|50.0| 1.0|[-0.7087597250938...|[0.29124027490615...|    1.0|
>
> | 0.0|119.0| 0.0| 0.0| 0.0|32.4|0.141|24.0| 1.0|[-0.4473398327827...|[0.55266016721725...|    0.0|
</details>

## Trusted DL
### Run Trusted Spark BigDL
<details><summary>This example shows how to run trusted Spark BigDL.</summary>


Run the script to run trusted Spark BigDL and it would take some time to show the final results:

```bash
bash work/start-scripts/start-spark-local-bigdl-sgx.sh
```

Open another terminal and check the log:

```bash
sudo docker exec -it spark-local cat /ppml/trusted-big-data-ml/test-bigdl-lenet-sgx.log | egrep "Accuracy"
```

The result should look something like this:

> creating: createTop1Accuracy
>
> 2021-06-18 01:39:45 INFO DistriOptimizer$:180 - [Epoch 1 60032/60000][Iteration 469][Wall Clock 457.926565s] Top1Accuracy is Accuracy(correct: 9488, count: 10000, accuracy: 0.9488)
>
> 2021-06-18 01:46:20 INFO DistriOptimizer$:180 - [Epoch 2 60032/60000][Iteration 938][Wall Clock 845.747782s] Top1Accuracy is Accuracy(correct: 9696, count: 10000, accuracy: 0.9696)
</details>

### Run Trusted Spark Orca Data
<details><summary>This example shows how to run trusted Spark Orca Data.</summary>


Before running the example, download the NYC Taxi dataset in Numenta Anomaly Benchmark from [here](https://raw.githubusercontent.com/numenta/NAB/master/data/realKnownCause/nyc_taxi.csv) for demo. After downloading the dataset, make sure that `nyc_taxi.csv` is under `work/data` directory or the same path in the `start-spark-local-orca-data-sgx.sh`. Replace  `path_of_nyc_taxi_csv` with your path of `nyc_taxi.csv` in the script.

Run the script to run trusted Spark Orca Data and it would take some time to show the final results:

```bash
bash start-spark-local-orca-data-sgx.sh
```

Open another terminal and check the log:

```bash
sudo docker exec -it spark-local cat /ppml/trusted-big-data-ml/test-orca-data-sgx.log | egrep -a "INFO data|Stopping" -A10
```

The result should contain the content look like this:

>INFO data collected: [        timestamp value
>
>0   2014-07-01 00:00:00 10844
>
>1   2014-07-01 00:30:00  8127
>
>2   2014-07-01 01:00:00  6210
>
>3   2014-07-01 01:30:00  4656
>
>4   2014-07-01 02:00:00  3820
>
>...          ...  ...
>
>10315 2015-01-31 21:30:00 24670
>
>10316 2015-01-31 22:00:00 25721
>
>10317 2015-01-31 22:30:00 27309
>
>10318 2015-01-31 23:00:00 26591
>
>\--
>
>INFO data2 collected: [        timestamp value      datetime hours awake
>
>0  2014-07-01 00:00:00 10844 2014-07-01 00:00:00   0   1
>
>1  2014-07-01 00:30:00  8127 2014-07-01 00:30:00   0   1
>
>2  2014-07-01 03:00:00  2369 2014-07-01 03:00:00   3   0
>
>3  2014-07-01 04:30:00  2158 2014-07-01 04:30:00   4   0
>
>4  2014-07-01 05:00:00  2515 2014-07-01 05:00:00   5   0
>
>...         ...  ...         ...  ...  ...
>
>5215 2015-01-31 17:30:00 23595 2015-01-31 17:30:00   17   1
>
>5216 2015-01-31 18:30:00 27286 2015-01-31 18:30:00   18   1
>
>5217 2015-01-31 19:00:00 28804 2015-01-31 19:00:00   19   1
>
>5218 2015-01-31 19:30:00 27773 2015-01-31 19:30:00   19   1
>
>\--
>
>Stopping orca context
</details>

### Run Trusted Spark Orca Learn Tensorflow Basic Text Classification
<details><summary>This example shows how to run Trusted Spark Orca learn Tensorflow basic text classification.</summary>


Run the script to run Trusted Spark Orca learn Tensorflow basic text classification and it would take some time to show the final results. To run this example in standalone mode, replace `-e SGX_MEM_SIZE=32G \` with `-e SGX_MEM_SIZE=64G \` in `start-distributed-spark-driver.sh`

```bash
bash start-spark-local-orca-tf-text.sh
```

Open another terminal and check the log:

```bash
sudo docker exec -it spark-local cat test-orca-tf-text.log | egrep "results"
```

The result should be similar to:

>INFO results: {'loss': 0.6932533979415894, 'acc Top1Accuracy': 0.7544000148773193}
</details>

## Trusted FL
### Trusted FL example
<details><summary>expand/fold</summary>
content
</details>
