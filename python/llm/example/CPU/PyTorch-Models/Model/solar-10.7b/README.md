# SOLAR-10.7B
In this directory, you will find examples on how you could use BigDL-LLM `optimize_model` API to accelerate SOLAR-10.7B models. For illustration purposes, we utilize the [upstage/SOLAR-10.7B-Instruct-v1.0](https://huggingface.co/upstage/SOLAR-10.7B-Instruct-v1.0) as a reference SOLAR-10.7B model.

## Requirements
To run these examples with BigDL-LLM, we have some recommended requirements for your machine, please refer to [here](../README.md#recommended-requirements) for more information.

## Example: Predict Tokens using `generate()` API
In the example [generate.py](./generate.py), we show a basic use case for a SOLAR-10.7B model to predict the next N tokens using `generate()` API, with BigDL-LLM INT4 optimizations.
### 1. Install
We suggest using conda to manage the Python environment. For more information about conda installation, please refer to [here](https://docs.conda.io/en/latest/miniconda.html#).

After installing conda, create a Python environment for BigDL-LLM:
```bash
conda create -n llm python=3.9 # recommend to use Python 3.9
conda activate llm

pip install --pre --upgrade bigdl-llm[all] # install the latest bigdl-llm nightly build with 'all' option
pip install transformers==4.35.2 # required by SOLAR-10.7B
```

### 2. Run
After setting up the Python environment, you could run the example by following steps.

#### 2.1 Client
On client Windows machines, it is recommended to run directly with full utilization of all cores:
```powershell
python ./generate.py --prompt 'AI是什么？'
```
More information about arguments can be found in [Arguments Info](#23-arguments-info) section. The expected output can be found in [Sample Output](#24-sample-output) section.

#### 2.2 Server
For optimal performance on server, it is recommended to set several environment variables (refer to [here](../README.md#best-known-configuration-on-linux) for more information), and run the example with all the physical cores of a single socket.

E.g. on Linux,
```bash
# set BigDL-LLM env variables
source bigdl-llm-init

# e.g. for a server with 48 cores per socket
export OMP_NUM_THREADS=48
numactl -C 0-47 -m 0 python ./generate.py --prompt 'AI是什么？'
```
More information about arguments can be found in [Arguments Info](#23-arguments-info) section. The expected output can be found in [Sample Output](#24-sample-output) section.

#### 2.3 Arguments Info
In the example, several arguments can be passed to satisfy your requirements:

- `--repo-id-or-model-path`: str, argument defining the huggingface repo id for the SOLAR-10.7B model to be downloaded, or the path to the huggingface checkpoint folder. It is default to be `'upstage/SOLAR-10.7B-Instruct-v1.0'`.
- `--prompt`: str, argument defining the prompt to be inferred (with integrated prompt format for chat). It is default to be `'AI是什么？'`.
- `--n-predict`: int, argument defining the max number of tokens to predict. It is default to be `64`.

#### 2.4 Sample Output
#### [upstage/SOLAR-10.7B-Instruct-v1.0](https://huggingface.co/upstage/SOLAR-10.7B-Instruct-v1.0)
```log
Inference time: XXXX s
-------------------- Output --------------------
### User:
AI是什么？

### Assistant:
AI, 全称是人工智能(Artificial Intelligence)，是一种由计算机科学、心理学、罗伯特斯学、语义学等多个学科的结合而成的学科，主要研究计算机如何
```

```log
Inference time: XXXX s
-------------------- Output --------------------
### User:
What is AI?

### Assistant:
 AI, or Artificial Intelligence, refers to the simulation of human intelligence in machines that are programmed to think and learn like humans. This involves the development of computer systems that can perform tasks that would normally require human intelligence, such as visual perception, speech recognition, decision-making, and language translation. AI systems can
```