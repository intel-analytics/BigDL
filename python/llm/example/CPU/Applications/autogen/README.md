## Running AutoGen Agent Chat with BigDL-LLM on Local Models
In this example, we use BigDL adapted FastChat to run [AutoGen](https://microsoft.github.io/autogen/) agent chat with 
local large language models.

### 1. Prepare Environment
```bash
# create autogen running directory
mkdir autogen
cd autogen

# create respective conda environment
conda create -n autogen python=3.9
conda activate autogen

# install fastchat-adapted bigdl-llm
pip install --pre --upgrade bigdl-llm[serving]==2.5.0b20240110

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

# load the local model with cpu with your downloaded model
python -m bigdl.llm.serving.model_worker --model-path YOUR_MODEL_PATH --device cpu
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

**Using `Mistral-7B-Instruct-v0.2` model on Intel i9-12900K**

```bash
CLEARING MEMORY
user (to teachable_agent):

What is the Vicuna model?

--------------------------------------------------------------------------------

>>>>>>>> USING AUTO REPLY...
teachable_agent (to user):

I apologize for any confusion, but I don't have enough context or prior information from our conversations to know specifically what you mean by "the Vicuna model." Vicunas are a species of camelid native to South America, but there is no known statistical or machine learning model named after them in the field of data science or artificial intelligence. If you could please provide more context or details about what you mean by "the Vicuna model," I would be happy to help you with any related questions or information you might have.

--------------------------------------------------------------------------------
user (to teachable_agent):

Vicuna is a 13B-parameter language model released by Meta.

--------------------------------------------------------------------------------

>>>>>>>> USING AUTO REPLY...
teachable_agent (to user):

Thank you for providing the additional context. Based on the new information, the Vicuna model is a 13B-parameter language model developed and released by Meta (formerly Facebook). However, I would need to access more detailed information about this model to fully understand its capabilities, architecture, and applications. If you have any specific questions or details about the Vicuna model, feel free to ask and I will do my best to help you.

--------------------------------------------------------------------------------
user (to teachable_agent):

What is the Orca model?

--------------------------------------------------------------------------------

>>>>>>>> USING AUTO REPLY...
teachable_agent (to user):

I apologize for any confusion, but I cannot directly answer your question about the Orca model without more context or information. The term "Orca model" is not mentioned in the context provided in our previous conversation, which was about the Vicuna model being a 13B-parameter language model released by Meta. If you could please provide more details or context about the Orca model, such as who developed it, when it was released, and what type of model it is, I would be happy to help you with any related questions or information you might have.

--------------------------------------------------------------------------------
user (to teachable_agent):

Orca is a 13B-parameter language model developed by Microsoft. It outperforms Vicuna on most tasks.

--------------------------------------------------------------------------------

>>>>>>>> USING AUTO REPLY...
teachable_agent (to user):

Thank you for providing the additional context about the Orca model being a 13B-parameter language model developed by Microsoft that outperforms the Vicuna model on most tasks. With this information, we can infer that both the Vicuna and Orca models are large-scale language models with a similar number of parameters, and that the Orca model has better performance based on the tasks it has been evaluated on. However, without more specific details about the models' architectures, capabilities, and applications, it is difficult to provide a comprehensive comparison or analysis. If you have any specific questions or details about the Vicuna or Orca models, feel free to ask and I will do my best to help you.

--------------------------------------------------------------------------------
user (to teachable_agent):

How does the Vicuna model compare to the Orca model?

--------------------------------------------------------------------------------

>>>>>>>> USING AUTO REPLY...
teachable_agent (to user):

Based on the given memories, the Vicuna model and the Orca model are both 13B-parameter language models, meaning they have similar capacity and architecture. However, the text states that the Orca model, developed by Microsoft, outperforms the Vicuna model on most tasks. Therefore, the Orca model can be considered more advanced or effective than the Vicuna model based on the provided information. It's important to note that this comparison is based on the specific task or set of tasks mentioned in the text, and the performance of the models may vary depending on the specific use case or dataset.

--------------------------------------------------------------------------------
(yuxuan-qwen) arda@arda-arc05:~/yuxuan/autogen-test$ python test_2_teachability_new_knowledge.py

CLEARING MEMORY
user (to teachable_agent):

What is the Vicuna model?

--------------------------------------------------------------------------------

>>>>>>>> USING AUTO REPLY...
teachable_agent (to user):

I apologize for any confusion, but I don't have enough context or prior information from our conversations to know specifically what you mean by "the Vicuna model." Vicunas are a species of camelid native to South America, but there is no known statistical or machine learning model named after them in the field of data science or artificial intelligence. If you could please provide more context or details about what you mean by "the Vicuna model," I would be happy to help you with any related questions or information you might have.

--------------------------------------------------------------------------------
user (to teachable_agent):

Vicuna is a 13B-parameter language model released by Meta.

--------------------------------------------------------------------------------

>>>>>>>> USING AUTO REPLY...
teachable_agent (to user):

Thank you for providing the additional context. Based on the new information, the Vicuna model is a 13B-parameter language model developed and released by Meta (formerly Facebook). However, I would need to access more detailed information about this model to fully understand its capabilities, architecture, and applications. If you have any specific questions or details about the Vicuna model, feel free to ask and I will do my best to help you.

--------------------------------------------------------------------------------
user (to teachable_agent):

What is the Orca model?

--------------------------------------------------------------------------------

>>>>>>>> USING AUTO REPLY...
teachable_agent (to user):

I apologize for any confusion, but I cannot directly answer your question about the Orca model without more context or information. The term "Orca model" is not mentioned in the context provided in our previous conversation, which was about the Vicuna model being a 13B-parameter language model released by Meta. If you could please provide more details or context about the Orca model, such as who developed it, when it was released, and what type of model it is, I would be happy to help you with any related questions or information you might have.

--------------------------------------------------------------------------------
user (to teachable_agent):

Orca is a 13B-parameter language model developed by Microsoft. It outperforms Vicuna on most tasks.

--------------------------------------------------------------------------------

>>>>>>>> USING AUTO REPLY...
teachable_agent (to user):

Thank you for providing the additional context about the Orca model being a 13B-parameter language model developed by Microsoft that outperforms the Vicuna model on most tasks. With this information, we can infer that both the Vicuna and Orca models are large-scale language models with a similar number of parameters, and that the Orca model has better performance based on the tasks it has been evaluated on. However, without more specific details about the models' architectures, capabilities, and applications, it is difficult to provide a comprehensive comparison or analysis. If you have any specific questions or details about the Vicuna or Orca models, feel free to ask and I will do my best to help you.

--------------------------------------------------------------------------------
user (to teachable_agent):

How does the Vicuna model compare to the Orca model?

--------------------------------------------------------------------------------

>>>>>>>> USING AUTO REPLY...
teachable_agent (to user):

Based on the given memories, the Vicuna model and the Orca model are both 13B-parameter language models, meaning they have similar capacity and architecture. However, the text states that the Orca model, developed by Microsoft, outperforms the Vicuna model on most tasks. Therefore, the Orca model can be considered more advanced or effective than the Vicuna model based on the provided information. It's important to note that this comparison is based on the specific task or set of tasks mentioned in the text, and the performance of the models may vary depending on the specific use case or dataset.

--------------------------------------------------------------------------------
```