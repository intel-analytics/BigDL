# SOLAR-10.7B
In this directory, you will find examples on how you could use BigDL-LLM `optimize_model` API to accelerate SOLAR-10.7B models. For illustration purposes, we utilize the [upstage/SOLAR-10.7B-Instruct-v1.0](https://huggingface.co/upstage/SOLAR-10.7B-Instruct-v1.0) as a reference SOLAR-10.7B model.

## Requirements
To run these examples with BigDL-LLM on Intel GPUs, we have some recommended requirements for your machine, please refer to [here](../README.md#recommended-requirements) for more information.

## Example: Predict Tokens using `generate()` API
In the example [generate.py](./generate.py), we show a basic use case for a SOLAR-10.7B model to predict the next N tokens using `generate()` API, with BigDL-LLM INT4 optimizations on Intel GPUs.
### 1. Install
We suggest using conda to manage the Python environment. For more information about conda installation, please refer to [here](https://docs.conda.io/en/latest/miniconda.html#).

After installing conda, create a Python environment for BigDL-LLM:
```bash
conda create -n llm python=3.9 # recommend to use Python 3.9
conda activate llm

# below command will install intel_extension_for_pytorch==2.0.110+xpu as default
# you can install specific ipex/torch version for your need
pip install --pre --upgrade bigdl-llm[xpu] -f https://developer.intel.com/ipex-whl-stable-xpu
pip install transformers==4.35.2 # required by SOLAR-10.7B
```

### 2. Configures OneAPI environment variables
```bash
source /home/arda/intel/oneapi/setvars.sh
```

### 3. Run

For optimal performance on Arc, it is recommended to set several environment variables.

```bash
export USE_XETLA=OFF
export SYCL_PI_LEVEL_ZERO_USE_IMMEDIATE_COMMANDLISTS=1
```

```bash
python ./generate.py --prompt 'AI是什么？'
```

In the example, several arguments can be passed to satisfy your requirements:

- `--repo-id-or-model-path REPO_ID_OR_MODEL_PATH`: argument defining the huggingface repo id for the SOLAR-10.7B model (e.g `upstage/SOLAR-10.7B-Instruct-v1.0`) to be downloaded, or the path to the huggingface checkpoint folder. It is default to be `'upstage/SOLAR-10.7B-Instruct-v1.0'`.
- `--prompt PROMPT`: argument defining the prompt to be infered (with integrated prompt format for chat). It is default to be `'AI是什么？'`.
- `--n-predict N_PREDICT`: argument defining the max number of tokens to predict. It is default to be `64`.

#### 2.3 Sample Output
#### [upstage/SOLAR-10.7B-Instruct-v1.0](https://huggingface.co/upstage/SOLAR-10.7B-Instruct-v1.0) 
```log
Inference time: XXXX s
-------------------- Output --------------------
### User:
AI是什么？

### Assistant:
AI, 全称是人工智能(Artificial Intelligence)，是一种由计算机科学、心理学、罗伯特斯学、语义学等多个学科的结合而成的学科，它的目的是模仿
```

```log
Inference time: XXXX s
-------------------- Output --------------------
### User:
What is AI?

### Assistant:
 AI, or Artificial Intelligence, refers to the simulation of human intelligence in machines that are programmed to think and learn like humans. This involves the development of computer systems that can perform tasks that would normally require human intelligence, such as visual perception, speech recognition, decision-making, and language translation. AI systems can
```
