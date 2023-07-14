# Dolly v1
In this directory, you will find examples on how you could apply BigDL-LLM INT4 optimizations on Dolly v1 models. For illustration purposes, we utilize the [databricks/dolly-v1-6b](https://huggingface.co/databricks/dolly-v1-6b) as a reference Dolly v1 model.

## 0. Requirements
To run these examples with BigDL-LLM, we have some recommended requirements for your machine, please refer to [here](../README.md#recommended-requirements) for more information.

## Example: Predict Tokens using `generate()` API
In the example [generate.py](./generate.py), we show a basic use case for a Dolly v1 model to predict the next N tokens using `generate()` API, with BigDL-LLM INT4 optimizations.
### 1. Install
We suggest using conda to manage environment:
```bash
conda create -n llm python=3.9
conda activate llm

pip install bigdl-llm[all] # install bigdl-llm with 'all' option
```

### 2. Config
It is recommended to set several environment variables for better performance. Please refer to [here](../README.md#best-known-configuration) for more information.

### 3. Run
```
python ./generate.py --repo-id-or-model-path REPO_ID_OR_MODEL_PATH --prompt PROMPT --n-predict N_PREDICT
```

Arguments info:
- `--repo-id-or-model-path REPO_ID_OR_MODEL_PATH`: argument defining the huggingface repo id for the Dolly v1 model to be downloaded, or the path to the huggingface checkpoint folder. It is default to be `'databricks/dolly-v1-6b'`.
- `--prompt PROMPT`: argument defining the prompt to be infered (with integrated prompt format for chat). It is default to be `'What is AI?'`.
- `--n-predict N_PREDICT`: argument defining the max number of tokens to predict. It is default to be `32`.

> **Note**: When loading the model in 4-bit, BigDL-LLM converts linear layers in the model into INT4 format. In theory, a *X*B model saved in 16-bit will requires approximately 2*X* GB of memory for loading, and ~0.5*X* GB memory for further inference.
>
> Please select the appropriate size of the Dolly v1 model based on the capabilities of your machine.

#### 3.1 Client
For better utilization of multiple cores on the client machine, it is recommended to use all the performance-cores along with their hyperthreads.

E.g. on Windows,
```powershell
# for a client machine with 8 Performance-cores
$env:OMP_NUM_THREADS=16
python ./generate.py
```

#### 3.2 Server
On server, it is recommended to run the example with all the physical cores of a single socket.

E.g. on Linux,
```bash
# for a server with 48 cores per socket
export OMP_NUM_THREADS=48
numactl -C 0-47 -m 0 python -u ./generate.py
```

#### 3.3 Sample Output
#### [databricks/dolly-v1-6b](https://huggingface.co/databricks/dolly-v1-6b)
```log
Inference time: xxxx s
-------------------- Prompt --------------------
Below is an instruction that describes a task. Write a response that appropriately completes the request.

### Instruction:
What is AI?

### Response:

-------------------- Output --------------------
Below is an instruction that describes a task. Write a response that appropriately completes the request.

### Instruction:
What is AI?

### Response:
AI is an umbrella term for a variety of technologies that enable computers to think and act like humans. AI can be used to automate tasks, analyze data, and
```
