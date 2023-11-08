### Run Tensor-Parallel BigDL Transformers INT4 Inference with Deepspeed

#### 1. Install Dependencies

Install necessary packages (here Python 3.9 is our test environment):

```bash
bash install.sh
```

#### 2. Initialize Deepspeed Distributed Context

Like shown in example code `deepspeed_autotp.py`, you can construct parallel model with Python API:

```python
# Load in HuggingFace Transformers' model
from transformers import AutoModelForCausalLM

model = AutoModelForCausalLM.from_pretrained(...)


# Parallelize model on deepspeed
import deepspeed

model = deepspeed.init_inference(
    model, # an AutoModel of Transformers
    mp_size = world_size, # instance (process) count
    dtype=torch.float16,
    replace_method="auto")
```

Then, returned model is converted into a deepspeed InferenceEnginee type.

#### 3. Optimize Model with BigDL-LLM Low Bit

Distributed model managed by deepspeed can be further optimized with BigDL low-bit Python API, e.g. sym_int4:

```python
# Apply BigDL-LLM INT4 optimizations on transformers
from bigdl.llm import optimize_model

model = optimize_model(model.module.to(f'cpu'), low_bit='sym_int4')
model = model.to(f'cpu:{local_rank}') # move partial model to local rank
```

Then, a bigdl-llm transformers is returned, which in the following, can serve in parallel with native APIs.

#### 4. Start Python Code

You can try deepspeed with BigDL LLM by:

```bash
bash run.sh
```

If you want to run your own application, there are **necessary configurations in the script** which can also be ported to run your custom deepspeed application:

```bash
# run.sh
source bigdl-nano-init
unset OMP_NUM_THREADS # deepspeed will set it for each instance automatically
source /opt/intel/oneccl/env/setvars.sh
......
export FI_PROVIDER=tcp
export CCL_ATL_TRANSPORT=ofi
export CCL_PROCESS_LAUNCHER=none
```

Set the above configurations before running `deepspeed` please to ensure right parallel communication and high performance.
