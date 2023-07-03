# INT4 Inference Pipeline for Large Language Model using BigDL-LLM Transformers Style API

In this example, we show a pipeline to convert a large language model to low precision (INT4), and then conduct inference on the converted INT4 model, using BigDL-LLM transformers style API.

## Prepare Environment
We suggest using conda to manage environment:
```bash
conda create -n llm python=3.9
conda activate llm

pip install --pre --upgrade bigdl-llm[all]
```

## Run Example
```bash
python ./transformers_int4_pipeline.py --repo-id-or-model-path REPO_ID_OR_MODEL_PATH
```
arguments info:
- `--repo-id-or-model-path MODEL_PATH`: argument defining the huggingface repo id for the large language model to be downloaded, or the path to the huggingface checkpoint folder.

  > **Note** In this example, `--repo-id-or-model-path MODEL_PATH` is limited be one of `['decapoda-research/llama-7b-hf', 'THUDM/chatglm-6b']` to better demonstrate English and Chinese support. And it is default to be `'decapoda-research/llama-7b-hf'`.

## Sample Output for Inference
### 'decapoda-research/llama-7b-hf' Model
```log
Prompt: Once upon a time, there existed a little girl who liked to have adventures. She wanted to go to places and meet new people, and have fun
Output: Once upon a time, there existed a little girl who liked to have adventures. She wanted to go to places and meet new people, and have fun. She wanted to be a hero. She wanted to be a hero, but she didn't know how. She didn't know how to be a
Inference time: xxxx s
```

### 'THUDM/chatglm-6b' Model
```log
Prompt: 晚上睡不着应该怎么办
Output: 晚上睡不着应该怎么办 晚上睡不着可能会让人感到焦虑和不安,但以下是一些可能有用的建议:

1. 放松身体和思维:尝试进行深呼吸、渐进性
Inference time: xxxx s
```
