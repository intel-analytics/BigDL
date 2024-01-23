# Running AutoGen Agent Chat with BigDL-LLM on Local Models
In this example, we use BigDL adapted FastChat to run [AutoGen](https://microsoft.github.io/autogen/) agent chat with 
local large language models.

### 1. Setup BIgDL-LLM Environment
```bash
# create autogen running directory
mkdir autogen
cd autogen

# create respective conda environment
conda create -n autogen python=3.9
conda activate autogen

# install xpu-supported and fastchat-adapted bigdl-llm
pip install --pre --upgrade bigdl-llm[xpu,serving]==2.5.0b20240110 -f https://developer.intel.com/ipex-whl-stable-xpu

# clone the BigDL in the autogen folder
git clone https://github.com/intel-analytics/BigDL.git

# install recommend transformers version
pip install transformers==4.36.2

# install necessary dependencies
pip install chromadb==0.4.22
```


### 2. Setup FastChat and AutoGen Environment
```bash
# clone the FastChat in the autogen folder
git clone https://github.com/lm-sys/FastChat.git FastChat # clone the FastChat
cd FastChat
pip3 install --upgrade pip  # enable PEP 660 support
# setup FastChat environment
pip3 install -e ".[model_worker,webui]"

# setup AutoGen environment
pip install pyautogen==0.2.7
```

### 3. Build FastChat OpenAI-Compatible RESTful API
Open 3 terminals

**Terminal 1: Launch the controller**

```bash
# activate conda environment
conda activate autogen

# go to the cloned FastChat folder in autogen folder
cd autogen/FastChat

python -m fastchat.serve.controller
```

**Terminal 2: Launch the workers**

```bash
# activate conda environment
conda activate autogen

# go to the created autogen folder
cd autogen

# ensure the connection between controller and worker
export no_proxy='localhost'

# load the local model with xpu with your downloaded model
python -m bigdl.llm.serving.model_worker --model-path YOUR_MODEL_PATH --device xpu
```

**Terminal 3: Launch the server**

```bash
# activate conda environment
conda activate autogen

# go to the cloned FastChat folder in autogen folder
cd autogen/FastChat

python -m fastchat.serve.openai_api_server --host localhost --port 8000
```

### 4. Run Example
Open another terminal

```bash
# activate conda environment
conda activate autogen

# go to the cloned BigDL example folder in autogen folder
cd autogen/BigDL/python/llm/example/CPU/Applications/autogen

# ensure the connection between controller and worker
export no_proxy='localhost'

# run the autogen example
python teachability_new_knowledge.py
```

## [Important] Change the model name
When you download the model, please change the model folder to `bigdl` for the usage of bigdl adapted FastChat. For example, after download the `Mistral-7B-Instruct-v0.2`
model and the path is `MODEL_PATH/Mistral-7B-Instruct-v0.2`, please change it to `MODEL_PATH/bigdl`.

## Sample Output

**Using `Mistral-7B-Instruct-v0.2` model on Intel ARC A770**

```bash
CLEARING MEMORY
user (to teachable_agent):

What is the Vicuna model?

--------------------------------------------------------------------------------

>>>>>>>> USING AUTO REPLY...
teachable_agent (to user):

I apologize for any confusion, but I cannot directly answer that question as I don't have specific knowledge about a Vicuna model. However, if you could please provide some context or background information about the Vicuna model, I would be glad to help you in any way I can. Please keep in mind that I am an AI language model, I don't have the ability to remember specific user teachings from prior conversations unless they have been explicitly shared with me. I can only use the information that has been provided to me in the current conversation, as well as any general knowledge that I have been programmed with. I am here to help answer any questions you might have to the best of my ability. If you have any specific questions, please don't hesitate to ask. I will do my best to provide you with accurate and helpful information. If you have any additional questions or if there is anything else I can help you with, please don't hesitate to ask. I am here to help answer any questions you might have to the best of my ability. I hope this information was helpful to you. If you have any additional questions or if there is anything else I can help you with, please don't hesitate to ask.

--------------------------------------------------------------------------------
user (to teachable_agent):

Vicuna is a 13B-parameter language model released by Meta.

--------------------------------------------------------------------------------

>>>>>>>> USING AUTO REPLY...
teachable_agent (to user):

The Vicuna model is a 13B-parameter language model released by Meta.

--------------------------------------------------------------------------------
user (to teachable_agent):

What is the Orca model?

--------------------------------------------------------------------------------

>>>>>>>> USING AUTO REPLY...
teachable_agent (to user):

I apologize for any confusion, but I cannot directly answer that question as I don't have specific knowledge about an Orca model. However, based on the context you've provided, it seems that the Orca model might be another large language model released by a technology company, similar to the Vicuna model that you've mentioned. I hope this information is helpful to you. Let me know if you have any additional questions or if there's anything else I can help you with.

--------------------------------------------------------------------------------
user (to teachable_agent):

Orca is a 13B-parameter language model developed by Microsoft. It outperforms Vicuna on most tasks.

--------------------------------------------------------------------------------

>>>>>>>> USING AUTO REPLY...
teachable_agent (to user):

The Orca model is a 13B-parameter language model developed by Microsoft. It outperforms the Vicuna model on most tasks.

# Memories that might help
- What is the Vicuna model?
* Vicuna is a 13B-parameter language model
* Released by Meta.
- What is the Orca model?
* Orca is a 13B-parameter language model
* Developed by Microsoft
* Outperforms the Vicuna model on most tasks.

--------------------------------------------------------------------------------
user (to teachable_agent):

How does the Vicuna model compare to the Orca model?

--------------------------------------------------------------------------------

>>>>>>>> USING AUTO REPLY...
teachable_agent (to user):

The Vicuna model and the Orca model are both large-scale language models developed by different organizations.

The Vicuna model is a 13B-parameter language model released by Meta. It's designed to generate human-like text based on given inputs.

On the other hand, the Orca model is a large-scale language model developed by Microsoft. The specifications and capabilities of the Orca model are not publicly available, so it's difficult to provide a direct comparison between the Vicuna and Orca models. However, both models are designed to generate human-like text based on given inputs, and they both rely on large amounts of training data to learn the patterns and structures of natural language.

--------------------------------------------------------------------------------
```