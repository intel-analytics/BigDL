#### Table of Contents  
[1. What is BigDL PPML?](#1-what-is-bigdl-ppml)  
[2. Why BigDL PPML?](#2-why-bigdl-ppml)  
[3. Getting Started with PPML](#3-getting-started-with-ppml)  \
&ensp;&ensp;[3.1 BigDL PPML Hello World](#31-bigdl-ppml-hello-world) \
&ensp;&ensp;[3.2 BigDL PPML End-to-End Workflow](#32-bigdl-ppml-end-to-end-workflow) \
&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;[Step 0. Preparation your environment](#step-0-preparation-your-environment): detailed steps in [Prepare Environment](https://github.com/liu-shaojun/BigDL/blob/ppml_doc/ppml/docs/prepare_environment.md) \
&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;[Step 1. Encrypt and Upload Data](#step-1-encrypt-and-upload-data) \
&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;[Step 2. Build Big Data & AI applications](#step-2-build-big-data--ai-applications) \
&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;[Step 3. Submit Job](#step-3-submit-job): 4 deploy modes and 2 options to submit job  \
&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;[Step 4. Decrypt and Read Result](#step-4-decrypt-and-read-result) \
&ensp;&ensp;[3.3 More Examples](#33-more-examples) \
[4. Develop your own Big Data & AI applications with BigDL PPML](#4-develop-your-own-big-data--ai-applications-with-bigdl-ppml)


## 1. What is BigDL PPML?

Protecting data privacy and confidentiality is critical in a world where data is everywhere. In recent years, more and more countries have enacted data privacy legislation or are expected to pass comprehensive legislation to protect data privacy, the importance of privacy and data protection is increasingly recognized.

To better protect sensitive data, it’s helpful to think about it in all dimensions of data lifecycle: data at rest, data in transit, and data in use. Data being transferred on a network is “in transit”, data in storage is “at rest”, and data being processed is “in use”.

<p align="center">
  <img src="https://user-images.githubusercontent.com/61072813/177720405-60297d62-d186-4633-8b5f-ff4876cc96d6.png" alt="data lifecycle" width='390px' height='260px'/>
</p>

For protecting data in transit, enterprises often choose to encrypt sensitive data prior to moving or use encrypted connections (HTTPS, SSL, TLS, FTPS, etc) to protect the contents of data in transit. For protecting data at rest, enterprises can simply encrypt sensitive files prior to storing them or choose to encrypt the storage drive itself. However, the third state, data in use has always been a weakly protected target. There are three emerging solutions seek to reduce the data-in-use attack surface: homomorphic encryption, multi-party computation, and confidential computing. 

Among these, [Confidential computing](https://www.intel.com/content/www/us/en/security/confidential-computing.html) protects data in use by performing computation in a hardware-based [Trusted Execution Environment (TEE)](https://en.wikipedia.org/wiki/Trusted_execution_environment). [Intel® SGX](https://www.intel.com/content/www/us/en/developer/tools/software-guard-extensions/overview.html) is Intel’s Trusted Execution Environment (TEE), offering hardware-based memory encryption that isolates specific application code and data in memory. [Intel® TDX](https://www.intel.com/content/www/us/en/developer/articles/technical/intel-trust-domain-extensions.html) is the next generation Intel’s Trusted Execution Environment (TEE), introducing new, architectural elements to help deploy hardware-isolated, virtual machines (VMs) called trust domains (TDs).

[PPML](https://bigdl.readthedocs.io/en/latest/doc/PPML/Overview/ppml.html) (Privacy Preserving Machine Learning) in [BigDL 2.0](https://github.com/intel-analytics/BigDL) provides a Trusted Cluster Environment for secure Big Data & AI applications, even on untrusted cloud environment. By combining Intel Software Guard Extensions (SGX) with several other security technologies (e.g., attestation, key management service, private set intersection, federated learning, homomorphic encryption, etc.), BigDL PPML ensures end-to-end security enabled for the entire distributed workflows, such as Apache Spark, Apache Flink, XGBoost, TensorFlow, PyTorch, etc.

## 2. Why BigDL PPML?
PPML allows organizations to explore powerful AI techniques while working to minimize the security risks associated with handling large amounts of sensitive data. PPML protects data at rest, in transit and in use: compute and memory protected by SGX Enclaves, storage (e.g., data and model) protected by encryption, network communication protected by remote attestation and Transport Layer Security (TLS), and optional Federated Learning support. 

<p align="left">
  <img src="https://user-images.githubusercontent.com/61072813/177922914-f670111c-e174-40d2-b95a-aafe92485024.png" alt="data lifecycle" width='600px' />
</p>

With BigDL PPML, you can run trusted Big Data & AI applications
- **Trusted Spark SQL & Dataframe**: with the trusted Big Data analytics and ML/DL support, users can run standard Spark data analysis (such as Spark SQL, Dataframe, MLlib, etc.) in a secure and trusted fashion.
- **Trusted ML**: with the trusted Big Data analytics and ML/DL support, users can run distributed machine learning (such as MLlib, XGBoost) in a secure and trusted fashion.
- **Trusted DL**: with the trusted Big Data analytics and ML/DL support, users can run distributed deep learning (such as BigDL, Orca, Nano, DLlib) in a secure and trusted fashion.
- **Trusted FL (Federated Learning)**: TODO

## 3. Getting Started with PPML

### 3.1 BigDL PPML Hello World
In this section, you can get started with running a simple native python HelloWorld program and a simple native Spark Pi program locally in a BigDL PPML client container to get an initial understanding of the usage of ppml. 

<details><summary>Click to see detailed steps</summary>

**a. Prepare [Keys](https://github.com/liu-shaojun/BigDL/blob/ppml_doc/ppml/docs/prepare_environment.md#prepare-key-and-password) and Start the BigDL PPML client container**

```
#!/bin/bash

# ENCLAVE_KEY_PATH means the absolute path to the "enclave-key.pem"
# KEYS_PATH means the absolute path to the keys
# LOCAL_IP means your local IP address.
export ENCLAVE_KEY_PATH=YOUR_LOCAL_ENCLAVE_KEY_PATH
export KEYS_PATH=YOUR_LOCAL_KEYS_PATH
export LOCAL_IP=YOUR_LOCAL_IP
export DOCKER_IMAGE=intelanalytics/bigdl-ppml-trusted-big-data-ml-python-graphene:devel

sudo docker pull $DOCKER_IMAGE

sudo docker run -itd \
    --privileged \
    --net=host \
    --cpuset-cpus="0-5" \
    --oom-kill-disable \
    --device=/dev/gsgx \
    --device=/dev/sgx/enclave \
    --device=/dev/sgx/provision \
    -v $ENCLAVE_KEY_PATH:/graphene/Pal/src/host/Linux-SGX/signer/enclave-key.pem \
    -v /var/run/aesmd/aesm.socket:/var/run/aesmd/aesm.socket \
    -v $DATA_PATH:/ppml/trusted-big-data-ml/work/data \
    -v $KEYS_PATH:/ppml/trusted-big-data-ml/work/keys \
    --name=bigdl-ppml-client-local \
    -e LOCAL_IP=$LOCAL_IP \
    -e SGX_MEM_SIZE=64G \
    $DOCKER_IMAGE bash
```

**b. Run Python HelloWorld in BigDL PPML Client Container**
  
Run the [script](https://github.com/intel-analytics/BigDL/blob/main/ppml/trusted-big-data-ml/python/docker-graphene/start-scripts/start-python-helloworld-sgx.sh) to run trusted [Python HelloWorld](https://github.com/intel-analytics/BigDL/blob/main/ppml/trusted-big-data-ml/python/docker-graphene/examples/helloworld.py) in BigDL PPML client container:
```
sudo docker exec -it bigdl-ppml-client-local bash work/start-scripts/start-python-helloworld-sgx.sh
```
Check the log:
```
sudo docker exec -it bigdl-ppml-client-local cat /ppml/trusted-big-data-ml/test-helloworld-sgx.log | egrep "Hello World"
```
The result should look something like this:
> Hello World


**c. Run Spark Pi in BigDL PPML Client Container**

Run the [script](https://github.com/intel-analytics/BigDL/blob/main/ppml/trusted-big-data-ml/python/docker-graphene/start-scripts/start-spark-local-pi-sgx.sh) to run trusted [Spark Pi](https://github.com/apache/spark/blob/v3.1.2/examples/src/main/python/pi.py) in BigDL PPML client container:

```bash
sudo docker exec -it bigdl-ppml-client-local bash work/start-scripts/start-spark-local-pi-sgx.sh
```

Check the log:

```bash
sudo docker exec -it bigdl-ppml-client-local cat /ppml/trusted-big-data-ml/test-pi-sgx.log | egrep "roughly"
```

The result should look something like this:

> Pi is roughly 3.146760

</details>

### 3.2 BigDL PPML End-to-End Workflow
![image](https://user-images.githubusercontent.com/61072813/178393982-929548b9-1c4e-4809-a628-10fafad69628.png)
In this section we take SimpleQuery as an example to go through the entire end-to-end PPML workflow. SimpleQuery is simple example to query developers between the ages of 20 and 40 from people.csv. 

#### Step 0. Preparation your environment
To secure your Big Data & AI applications in BigDL PPML manner, you should prepare your environment first, including K8s cluster setup, K8s-SGX plugin setup, key/password preparation, key management service (KMS) and attestation service (AS) setup, BigDL PPML client container preparation. **Please follow the detailed steps in** [Prepare Environment](https://github.com/liu-shaojun/BigDL/blob/ppml_doc/ppml/docs/prepare_environment.md). 


#### Step 1. Encrypt and Upload Data
Encrypt the input data of your Big Data & AI applications (here we use SimpleQuery) and then upload encrypted data to the nfs server. More details in [Encrypt Your Data](https://github.com/liu-shaojun/BigDL/blob/ppml_doc/ppml/services/kms-utils/docker/README.md#3-enroll-generate-key-encrypt-and-decrypt).

1. Generate the input data `people.csv` for SimpleQuery application
you can use [generate_people_csv.py](https://github.com/analytics-zoo/ppml-e2e-examples/blob/main/spark-encrypt-io/generate_people_csv.py). The usage command of the script is `python generate_people.py </save/path/of/people.csv> <num_lines>`.

2. Encrypt `people.csv`
    ```
    docker exec -i $KMSUTIL_CONTAINER_NAME bash -c "bash /home/entrypoint.sh encrypt $appid $appkey $input_file_path"
    ```
#### Step 2. Build Big Data & AI applications
To build your own Big Data & AI applications, refer to [develop your own Big Data & AI applications with BigDL PPML](#4-develop-your-own-big-data--ai-applications-with-bigdl-ppml). The code of SimpleQuery is in [here](https://github.com/intel-analytics/BigDL/blob/main/scala/ppml/src/main/scala/com/intel/analytics/bigdl/ppml/examples/SimpleQuerySparkExample.scala), it is already built into bigdl-ppml-spark_3.1.2-2.1.0-SNAPSHOT.jar, and the jar is put into PPML image.

#### Step 3. Submit Job
When the Big Data & AI application and its input data is prepared, you are ready to submit BigDL PPML jobs. You need to choose the deploy mode and the way to submit job first.

* **There are 4 modes to submit job**:

    1. **local mode**: run jobs locally
        <p align="left">
          <img src="https://user-images.githubusercontent.com/61072813/174703141-63209559-05e1-4c4d-b096-6b862a9bed8a.png" width='250px' />
        </p>


    2. **local SGX mode**: run jobs locally with SGX guarded
        <p align="left">
          <img src="https://user-images.githubusercontent.com/61072813/174703165-2afc280d-6a3d-431d-9856-dd5b3659214a.png" width='250px' />
        </p>


    3. **client SGX mode**: run jobs in k8s client mode with SGX guarded
        <p align="left">
          <img src="https://user-images.githubusercontent.com/61072813/174703216-70588315-7479-4b6c-9133-095104efc07d.png" width='500px' />
        </p>


    4. **cluster SGX mode**: run jobs in k8s cluster mode with SGX guarded
        <p align="left">
          <img src="https://user-images.githubusercontent.com/61072813/174703234-e45b8fe5-9c61-4d17-93ef-6b0c961a2f95.png" width='500px' />
        </p>


* **There are two options to submit PPML jobs**:
    * use [PPML CLI](https://github.com/liu-shaojun/BigDL/blob/ppml_doc/ppml/docs/submit_job.md#ppml-cli) to submit jobs manually
    * use [helm chart](https://github.com/liu-shaojun/BigDL/blob/ppml_doc/ppml/docs/submit_job.md#helm-chart) to submit jobs automatically

Here we use **k8s client mode** and **PPML CLI** to run SimpleQuery. Check other modes, please see [PPML CLI Usage Examples](https://github.com/liu-shaojun/BigDL/blob/ppml_doc/ppml/docs/submit_job.md#usage-examples). Alternatively, you can also use Helm to submit jobs automatically, see the details in [Helm Chart Usage](https://github.com/liu-shaojun/BigDL/blob/ppml_doc/ppml/docs/submit_job.md#helm-chart).

  <details><summary>expand to see details of submitting SimpleQuery</summary>

    1. enter the ppml container
        ```
        docker exec -it ppml-spark-client bash
        ```
    2. run simplequery on k8s client mode
        ```
        #!/bin/bash
        export secure_password=`openssl rsautl -inkey /ppml/trusted-big-data-ml/work/password/key.txt -decrypt </ppml/trusted-big-data-ml/work/password/output.bin`
        bash bigdl-ppml-submit.sh \
                --master $RUNTIME_SPARK_MASTER \
                --deploy-mode client \
                --sgx-enabled true \
                --sgx-log-level error \
                --sgx-driver-memory 64g \
                --sgx-driver-jvm-memory 12g \
                --sgx-executor-memory 64g \
                --sgx-executor-jvm-memory 12g \
                --driver-memory 32g \
                --driver-cores 8 \
                --executor-memory 32g \
                --executor-cores 8 \
                --num-executors 2 \
                --conf spark.kubernetes.container.image=$RUNTIME_K8S_SPARK_IMAGE \
                --name spark-pi \
                --verbose \
                --class com.intel.analytics.bigdl.ppml.examples.SimpleQuerySparkExample \
                --jars local:///ppml/trusted-big-data-ml/spark-encrypt-io-0.3.0-SNAPSHOT.jar \
                local:///ppml/trusted-big-data-ml/work/data/simplequery/spark-encrypt-io-0.3.0-SNAPSHOT.jar \
                --inputPath /ppml/trusted-big-data-ml/work/data/simplequery/people_encrypted \
                --outputPath /ppml/trusted-big-data-ml/work/data/simplequery/people_encrypted_output \
                --inputPartitionNum 8 \
                --outputPartitionNum 8 \
                --inputEncryptModeValue AES/CBC/PKCS5Padding \
                --outputEncryptModeValue AES/CBC/PKCS5Padding \
                --primaryKeyPath /ppml/trusted-big-data-ml/work/data/simplequery/keys/primaryKey \
                --dataKeyPath /ppml/trusted-big-data-ml/work/data/simplequery/keys/dataKey \
                --kmsType EHSMKeyManagementService
                --kmsServerIP your_ehsm_kms_server_ip \
                --kmsServerPort your_ehsm_kms_server_port \
                --ehsmAPPID your_ehsm_kms_appid \
                --ehsmAPPKEY your_ehsm_kms_appkey
        ```


    3. check runtime status: exit the container or open a new terminal

        To check the logs of the Kubernetes job, run
        ```
        sudo kubectl logs $( sudo kubectl get pod | grep spark-pi-job | cut -d " " -f1 )
        ```
        To check the logs of the Spark driver, run
        ```
        sudo kubectl logs $( sudo kubectl get pod | grep "spark-pi-sgx.*-driver" -m 1 | cut -d " " -f1 )
        ```
        To check the logs of an Spark executor, run
        ```
        sudo kubectl logs $( sudo kubectl get pod | grep "spark-pi-.*-exec" -m 1 | cut -d " " -f1 )
        ```
  </details>

#### Step 4. Decrypt and Read Result
When the job is done, you can decrypt and read result of the job. More details in [Decrypt Job Result](https://github.com/liu-shaojun/BigDL/blob/ppml_doc/ppml/services/kms-utils/docker/README.md#3-enroll-generate-key-encrypt-and-decrypt).

  ```
  docker exec -i $KMSUTIL_CONTAINER_NAME bash -c "bash /home/entrypoint.sh decrypt $appid $appkey $input_path"
  ```

### 3.3 More Examples
In addition to the above Spark Pi and Python HelloWorld programs running locally, and simplequery application running on the k8s cluster, we also provide other examples including Trusted Data Analysis, Trusted ML, Trusted DL and Trusted FL. You can find these examples in [more examples](https://github.com/liu-shaojun/BigDL/blob/ppml_doc/ppml/docs/examples.md). 

## 4. Develop your own Big Data & AI applications with BigDL PPML

xxxx
