# BigDL LLM
`bigdl-llm` is an SDK for large language model (LLM). It helps users develop AI applications that contains LLM on Intel XPU by using less computing and memory resources.`bigdl-llm` utilize a highly optimized GGML on Intel XPU.

Users could use `bigdl-llm` to
- Convert their model to lower precision
- Use command line tool like `llama.cpp` to run the model inference
- Use transformers like API to run the model inference
- Integrate the model in `langchain` pipeline

Currently `bigdl-llm` has supported
- Precision: INT4
- Model Family: llama, gptneox, bloom, starcoder
- Platform: Ubuntu 20.04 or later, CentOS 7 or later, Windows 10/11
- Device: CPU
- Python: 3.9 (recommended) or later 

## Installation
BigDL-LLM is a self-contained SDK library for model loading and inferencing. Users could directly
```bash
pip install --pre --upgrade bigdl-llm
```
While model conversion procedure will rely on some 3rd party libraries. Add `[all]` option for installation to prepare environment.
```bash
pip install --pre --upgrade bigdl-llm[all]
```

## Usage
A standard procedure for using `bigdl-llm` contains 3 steps:

1. Download model from huggingface hub
2. Convert model from huggingface format to GGML format
3. Inference using `llm-cli`, transformers like API, or `langchain`.

### Convert your model
A python function and a command line tool `llm-convert` is provided to transform the model from huggingface format to GGML format.

**Note: If you want to convert your model with lora adapter, please make sure the base model is in the huggingface format and the lora model should contains `adapter_config.json` and `adapter_model.bin`(The PEFT format). If these conditions are not met, please follow the readme privoded by the LoRA you are using and merge it with the base model before converting.**

Here is an example to use `llm-convert` command line tool.
```bash
# pth model
llm-convert "/path/to/llama-7b-hf/" --model-format pth --outfile "/path/to/llama-7b-int4/" --model-family "llama" --lora-id-or-path /path/to/gpt4all-lora
# gptq model
llm-convert "/path/to/vicuna-13B-1.1-GPTQ-4bit-128g/" --model-format gptq --outfile "/path/to/vicuna-13B-int4/" --model-family "llama"
```
> An example GPTQ model can be found [here](https://huggingface.co/TheBloke/vicuna-13B-1.1-GPTQ-4bit-128g/tree/main)

Here is an example to use `llm_convert` python API.
```bash
from bigdl.llm import llm_convert
# pth model
llm_convert(model="/path/to/llama-7b-hf/",
            outfile="/path/to/llama-7b-int4/",
            model_format="pth",
            lora_id_or_path="/path/to/gpt4all-lora",
            model_family="llama")
# gptq model
llm_convert(model="/path/to/vicuna-13B-1.1-GPTQ-4bit-128g/",
            outfile="/path/to/vicuna-13B-int4/",
            model_format="gptq",
            model_family="llama")
```

### Inferencing

#### llm-cli command line
llm-cli is a command-line interface tool that follows the interface as the main program in `llama.cpp`.

```bash
# text completion
llm-cli -t 16 -x llama -m "/path/to/llama-7b-int4/bigdl-llm-xxx.bin" -p 'Once upon a time,'

# chatting
llm-cli -t 16 -x llama -m "/path/to/llama-7b-int4/bigdl-llm-xxx.bin" -i --color

# help information
llm-cli -x llama -h
```

#### Transformers like API
You can also load the converted model using `BigdlForCausalLM` with a transformer like API, 
```python
from bigdl.llm.transformers import BigdlForCausalLM
llm = BigdlForCausalLM.from_pretrained("/path/to/llama-7b-int4/bigdl-llm-xxx.bin",
                                           model_family="llama")
prompt="What is AI?"
```
and simply do inference end-to-end like
```python
output = llm(prompt, max_tokens=32)
```
If you need to seperate the tokenization and generation, you can also do inference like
```python
tokens_id = llm.tokenize(prompt)
output_tokens_id = llm.generate(tokens_id, max_new_tokens=32)
output = llm.batch_decode(output_tokens_id)
```


Alternatively, you can load huggingface model directly using `AutoModelForCausalLM.from_pretrained`. 

```python
from bigdl.llm.transformers import AutoModelForCausalLM

# option 1: load huggingface checkpoint
llm = AutoModelForCausalLM.from_pretrained("/path/to/llama-7b-hf/",
                                           model_family="llama")

# option 2: load from huggingface hub repo
llm = AutoModelForCausalLM.from_pretrained("decapoda-research/llama-7b-hf",
                                           model_family="llama")
```

You can then use the the model the same way as you use transformers.
```python
# Use transformers tokenizer
tokenizer = AutoTokenizer.from_pretrained(model_ckpt)
tokens = tokenizer("what is ai").input_ids
tokens_id = llm.generate(tokens, max_new_tokens=32)
tokenizer.batch_decode(tokens_id)
```

#### llama-cpp-python like API
`llama-cpp-python` has become a popular pybinding for `llama.cpp` program. Some users may be familiar with this API so `bigdl-llm` reserve this API and extend it to other model families (e.g., gptneox, bloom)

```python
from bigdl.llm.models import Llama, Bloom, Gptneox, Starcoder

llm = Llama("/path/to/llama-7b-int4/bigdl-llm-xxx.bin", n_threads=4)
result = llm("what is ai")
```

#### langchain integration
TODO

## Examples
We prepared several examples in https://github.com/intel-analytics/BigDL/tree/main/python/llm/example

## Dynamic library BOM
To avoid difficaulties during the installtion. `bigdl-llm` release the C implementation by dynamic library or executive file. The compilation details are stated below. **These information is only for reference, no compilation procedure is needed for our users.** `GLIBC` version may affect the compatibility.

| Model family | Platform | Compiler           | GLIBC |
| ------------ | -------- | ------------------ | ----- |
| llama        | Linux    | GCC 9.4.0          | 2.17  |
| llama        | Windows  | MSVC 19.36.32532.0 |       |
| gptneox      | Linux    | GCC 9.4.0          | 2.17  |
| gptneox      | Windows  | MSVC 19.36.32532.0 |       |
| bloom        | Linux    | GCC 9.4.0          | 2.31  |
| bloom        | Windows  | MSVC 19.36.32532.0 |       |
| starcoder    | Linux    | GCC 9.4.0          | 2.31  |
| starcoder    | Windows  | MSVC 19.36.32532.0 |       |
