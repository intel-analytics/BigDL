# Llama2
In this directory, you will find examples on how you could apply BigDL-LLM INT4 optimizations on Llama2 models. For illustration purposes, we utilize the [meta-llama/Llama-2-7b-chat-hf](https://huggingface.co/meta-llama/Llama-2-7b-chat-hf) and [meta-llama/Llama-2-13b-chat-hf](https://huggingface.co/meta-llama/Llama-2-13b-chat-hf) as reference Llama2 models.

## 0. Requirements
To run these examples with BigDL-LLM, we have some recommended requirements for your machine, please refer to [here](../README.md#recommended-requirements) for more information.

## Example: Predict Tokens using `generate()` API
In the example [generate.py](./generate.py), we show a basic use case for a Llama2 model to predict the next N tokens using `generate()` API, with BigDL-LLM INT4 optimizations.
### 1. Install
We suggest using conda to manage environment:
```bash
conda create -n llm python=3.9
conda activate llm

pip install bigdl-llm[all] # install bigdl-llm with 'all' option
```

### 2. Run
```
python ./generate.py --repo-id-or-model-path REPO_ID_OR_MODEL_PATH --prompt PROMPT --n-predict N_PREDICT
```

Arguments info:
- `--repo-id-or-model-path REPO_ID_OR_MODEL_PATH`: argument defining the huggingface repo id for the Llama2 model (e.g. `meta-llama/Llama-2-7b-chat-hf` and `meta-llama/Llama-2-13b-chat-hf`) to be downloaded, or the path to the huggingface checkpoint folder. It is default to be `'meta-llama/Llama-2-7b-chat-hf'`.
- `--prompt PROMPT`: argument defining the prompt to be infered (with integrated prompt format for chat). It is default to be `'What is AI?'`.
- `--n-predict N_PREDICT`: argument defining the max number of tokens to predict. It is default to be `32`.

> **Note**: When loading the model in 4-bit, BigDL-LLM converts linear layers in the model into INT4 format. In theory, a *X*B model saved in 16-bit will requires approximately 2*X* GB of memory for loading, and ~0.5*X* GB memory for further inference.
>
> Please select the appropriate size of the Llama2 model based on the capabilities of your machine.

#### 2.1 Client
On client Windows machine, it is recommended to run directly with full utilization of all cores:
```powershell
python ./generate.py 
```

#### 2.2 Server
For optimal performance on server, it is recommended to set several environment variables (refer to [here](../README.md#best-known-configuration-on-linux) for more information), and run the example with all the physical cores of a single socket.

E.g. on Linux,
```bash
# set BigDL-LLM env variables
source bigdl-llm-init

# e.g. for a server with 48 cores per socket
export OMP_NUM_THREADS=48
numactl -C 0-47 -m 0 python ./generate.py
```

#### 2.3 Sample Output
#### [meta-llama/Llama-2-7b-chat-hf](https://huggingface.co/meta-llama/Llama-2-7b-chat-hf)
```log
Inference time: xxxx s
-------------------- Prompt --------------------
### HUMAN:
What is AI?

### RESPONSE:

-------------------- Output --------------------
### HUMAN:
What is AI?

### RESPONSE:

AI is a term used to describe the development of computer systems that can perform tasks that typically require human intelligence, such as understanding natural language, recognizing images
```

#### [meta-llama/Llama-2-13b-chat-hf](https://huggingface.co/meta-llama/Llama-2-13b-chat-hf)
```log
Inference time: xxxx s
-------------------- Prompt --------------------
### HUMAN:
What is AI?

### RESPONSE:

-------------------- Output --------------------
### HUMAN:
What is AI?

### RESPONSE:

AI, or artificial intelligence, refers to the ability of machines to perform tasks that would typically require human intelligence, such as learning, problem-solving,
```

### 3. Speculative Decoding
Speculative decoding is a pivotal technique to accelerate the inference of large language models (LLMs) by employing a smaller draft/assistant model to predict the target model's outputs. Please refer [Assisted Generation](https://huggingface.co/blog/assisted-generation) for details. In the following example, we will use int4 assistant model to acclerate FP32 main model, without impacting accuarcy.

```bash
python speculative_decoding.py --repo-id-or-model-path REPO_ID_OR_MODEL_PATH --assistant-model-path ASSISTANT_MODEL_PATH --prompt PROMPT --n-predict N_PREDICT
```

New Arguments info:
- `--assistant-model-path ASSISTANT_MODEL_PATH`: argument defining the huggingface repo id for the assistant model (e.g. `meta-llama/Llama-2-7b-chat-hf` and `meta-llama/Llama-2-13b-chat-hf`) to be downloaded, or the path to the huggingface checkpoint folder. 

#### 3.1 Use additional model as assistant model
By default, we need to add a second model named draft/assistant model to generate draft proposals. Note that assistant model should be similar to main model. It's recommended to use a small version of main model, e.g., Llama-70B as main model and Llama-7B as assistant model.

```bash
python speculative_decoding.py --repo-id-or-model-path REPO_ID_OR_MODEL_PATH --assistant-model-path ASSISTANT_MODEL_PATH --prompt PROMPT --n-predict N_PREDICT
```

#### 3.2 Use int4 model as assistant model
In most cases, it's quite difficult to prepare or choose an assistant model. For example, there isn't any smaller version for main model. To resolve this problem, we can use int4 model as assistant model, which is much faster than the main model.

```bash
python speculative_decoding.py --repo-id-or-model-path REPO_ID_OR_MODEL_PATH --prompt PROMPT --n-predict N_PREDICT
```
