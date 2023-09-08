# Benchmark tool for transformers int4 (separate 1st token and rest)

`benchmark_util.py` is used to provide a simple benchmark tool for transformer int4 model to calculate 1st token performance and the rest on CPU and GPU.

## CPU Usage
Just put this file into your benchmark directory, and then wrap your transformer int4 model with `BenchmarkWrapper` (`model = BenchmarkWrapper(model)`).
Take `chatglm-6b` as an example:
```python
import torch
from bigdl.llm.transformers import AutoModel
from transformers import AutoTokenizer
from benchmark_util import BenchmarkWrapper

model_path ='THUDM/chatglm-6b'
model = AutoModel.from_pretrained(model_path, trust_remote_code=True, load_in_4bit=True)
model = BenchmarkWrapper(model, do_print=True)
tokenizer = AutoTokenizer.from_pretrained(model_path, trust_remote_code=True)
prompt = "今天睡不着怎么办"
 
with torch.inference_mode():
    input_ids = tokenizer.encode(prompt, return_tensors="pt")
    output = model.generate(input_ids, do_sample=False, max_new_tokens=32)
    output_str = tokenizer.decode(output[0], skip_special_tokens=True)
```
Output will be like:
```bash
=========First token cost xx.xxxxs=========
=========Last token cost average xx.xxxxs (31 tokens in all)=========
```

## GPU Usage
Just put this file into your benchmark directory, and then wrap your transformer int4 model with `BenchmarkWrapper` (`model = BenchmarkWrapper(model)`).
Take `chatglm-6b` as an example:
```python
import torch
import intel_extension_for_pytorch as ipex
from bigdl.llm.transformers import AutoModel
from transformers import AutoTokenizer
from benchmark_util import BenchmarkWrapper

model_path ='THUDM/chatglm-6b'
model = AutoModel.from_pretrained(model_path, trust_remote_code=True, load_in_4bit=True)
model = model.to('xpu')
model = BenchmarkWrapper(model, do_print=True)
tokenizer = AutoTokenizer.from_pretrained(model_path, trust_remote_code=True)
prompt = "今天睡不着怎么办"
 
with torch.inference_mode():
    # wamup two times as use ipex
    for i in range(2):
        input_ids = tokenizer.encode(prompt, return_tensors="pt").to('xpu')
        output = model.generate(input_ids, do_sample=False, max_new_tokens=32)
        output_str = tokenizer.decode(output[0], skip_special_tokens=True)
    # collect performance data now
    for i in range(5):
        input_ids = tokenizer.encode(prompt, return_tensors="pt").to('xpu')
        output = model.generate(input_ids, do_sample=False, max_new_tokens=32)
        output_str = tokenizer.decode(output[0], skip_special_tokens=True)
```
Output will be like:
```bash
=========First token cost xx.xxxxs=========
=========Last token cost average xx.xxxxs (31 tokens in all)=========
```
