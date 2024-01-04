# Loading GGUF models
In this directory, you will find examples on how to load GGUF model into `bigdl-llm`.

## Verified Models(Q4_0)
- [Llama-2-7B-Chat-GGUF](https://huggingface.co/TheBloke/Llama-2-7B-Chat-GGUF/tree/main)
- [Mistral-7B-Instruct-v0.1-GGUF](https://huggingface.co/TheBloke/Mistral-7B-Instruct-v0.1-GGUF)
- [Mixtral-8x7B-v0.1-GGUF](https://huggingface.co/TheBloke/Mixtral-8x7B-v0.1-GGUF)
- [Baichuan2-7B-Chat-GGUF](https://huggingface.co/second-state/Baichuan2-7B-Chat-GGUF/tree/main)
- [Bloomz-7b1-GGUF](https://huggingface.co/hzjane/bloomz-7b1-gguf)
- [falcon-7b-quantized-gguf](https://huggingface.co/xaviviro/falcon-7b-quantized-gguf/tree/main)
- [mpt-7b-chat-gguf](https://huggingface.co/maddes8cht/mosaicml-mpt-7b-chat-gguf/tree/main)

## Requirements
To run these examples with BigDL-LLM, we have some recommended requirements for your machine, please refer to [here](../../../README.md#system-support) for more information.

**Important: Please make sure you have installed `transformers==4.36.0` to run the example.**


## Example: Load gguf model using `from_gguf()` API
In the example [generate.py](./generate.py), we show a basic use case to load a GGUF LLaMA2 model into `bigdl-llm` using `from_gguf()` API, with BigDL-LLM optimizations.

### 1. Install
We suggest using conda to manage the Python environment. For more information about conda installation, please refer to [here](https://docs.conda.io/en/latest/miniconda.html#).

After installing conda, create a Python environment for BigDL-LLM:
```bash
conda create -n llm python=3.9 # recommend to use Python 3.9
conda activate llm

pip install --pre --upgrade bigdl-llm[all] # install the latest bigdl-llm nightly build with 'all' option
pip install transformers==4.34.0  # upgrade transformers
```

**(Optional) Support for LLM-AWQ Backend**

BigDL-LLM uses `autoawq` as its default awq backend, and also automatically supports `llmawq` if your model is orginally quantized by llmawq. You can directly input an llmawq model in the same way as autoawq, after installing the following dependencies:

```bash
git clone https://github.com/mit-han-lab/llm-awq
cd llm-awq
pip install --upgrade pip  # enable PEP 660 support
pip install -e .
```

### 2. Run
After setting up the Python environment, you could run the example by following steps.

#### 2.1 Client
On client Windows machines, it is recommended to run directly with full utilization of all cores:
```powershell
python ./generate.py --model <path_to_gguf_model> --prompt 'What is AI?'
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
numactl -C 0-47 -m 0 python ./generate.py --model <path_to_gguf_model> --prompt 'What is AI?'
```
More information about arguments can be found in [Arguments Info](#23-arguments-info) section. The expected output can be found in [Sample Output](#24-sample-output) section.

#### 2.3 Arguments Info
In the example, several arguments can be passed to satisfy your requirements:

- `--model`: path to GGUF model, it should be a file with name like `llama-2-7b-chat.Q4_0.gguf`
- `--prompt PROMPT`: argument defining the prompt to be infered (with integrated prompt format for chat). It is default to be `'What is AI?'`.
- `--n-predict N_PREDICT`: argument defining the max number of tokens to predict. It is default to be `32`.

#### 2.4 Sample Output
#### [llama-2-7b-chat.Q4_0.gguf](https://huggingface.co/TheBloke/Llama-2-7B-Chat-GGUF/tree/main)
```log
Inference time: xxxx s
-------------------- Output --------------------
### HUMAN:
What is AI?

### RESPONSE:

AI is a term used to describe a type of computer software that is designed to perform tasks that typically require human intelligence, such as visual perception, speech
```
