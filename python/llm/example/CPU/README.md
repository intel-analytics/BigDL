# BigDL-LLM Examples on Intel CPU

This folder contains examples of running BigDL-LLM on Intel CPU:

- [HF-Transformers-AutoModels](HF-Transformers-AutoModels): running any ***Hugging Face Transformers*** model on BigDL-LLM (using the standard AutoModel APIs)
- [QLoRA-FineTuning](QLoRA-FineTuning): running ***QLoRA finetuning*** using BigDL-LLM on intel CPUs
- [vLLM-Serving](vLLM-Serving): running ***vLLM*** serving framework on intel CPUs (with BigDL-LLM low-bit optimized models)
- [Deepspeed-AutoTP](https://github.com/intel-analytics/BigDL/tree/main/python/llm/example/CPU/Deepspeed-AutoTP): running distributed inference using ***DeepSpeed AutoTP*** (with BigDL-LLM low-bit optimized models)
- [LangChain](LangChain): running ***LangChain*** applications on BigDL-LLM
- [Applications](Applications): running LLM applications (such as agent, streaming-llm) on BigDl-LLM
- [PyTorch-Models](PyTorch-Models): running any PyTorch model on BigDL-LLM (with "one-line code change")
- [Native-Models](Native-Models): converting & running LLM in `llama`/`chatglm`/`bloom`/`gptneox`/`starcoder` model family using native (cpp) implementation


## System Support
**Hardware**:
- Intel® Core™ processors
- Intel® Xeon® processors

**Operating System**:
- Ubuntu 20.04 or later
- CentOS 7 or later
- Windows 10/11, with or without WSL
