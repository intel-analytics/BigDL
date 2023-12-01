# Finetuning LLAMA Using QLoRA (experimental support)

This example demonstrates how to finetune a llama2-7b model using Big-LLM 4bit optimizations on [Intel CPUs](../README.md).


## Distributed Training Guide
1. Single node with single socket: [simple example](https://github.com/intel-analytics/BigDL/tree/main/python/llm/example/CPU/QLoRA-FineTuning#example-finetune-llama2-7b-using-qlora)
or [alpaca example](https://github.com/intel-analytics/BigDL/tree/main/python/llm/example/CPU/QLoRA-FineTuning/alpaca-qlora)
2. [Single node with multiple sockets](https://github.com/intel-analytics/BigDL/tree/main/python/llm/example/CPU/QLoRA-FineTuning/alpaca-qlora#guide-to-finetuning-qlora-on-one-node-with-multiple-sockets)
3. multiple nodes with multiple sockets

## Example: Finetune llama2-7b using QLoRA

This example is ported from [bnb-4bit-training](https://colab.research.google.com/drive/1VoYNfYDKcKRQRor98Zbf2-9VQTtGJ24k). 

### 1. Install

```bash
conda create -n llm python=3.9
conda activate llm
pip install --pre --upgrade bigdl-llm[all]
pip install transformers==4.34.0
pip install peft==0.5.0
pip install datasets
pip install accelerate==0.23.0
```

### 2. Finetune model
If the machine memory is not enough, you can try to set `use_gradient_checkpointing=True` in [here](https://github.com/intel-analytics/BigDL/blob/1747ffe60019567482b6976a24b05079274e7fc8/python/llm/example/CPU/QLoRA-FineTuning/qlora_finetuning_cpu.py#L53C6-L53C6). While gradient checkpointing may improve memory efficiency, it slows training by approximately 20%.
We Recommend using micro_batch_size of 8 for better performance using 48cores in this example. You can refer to [this guide](https://huggingface.co/docs/transformers/perf_train_gpu_one) for more details.
And remember to use `bigdl-llm-init` before you start finetuning, which can accelerate the job.

```
source bigdl-llm-init -t
python ./qlora_finetuning_cpu.py --repo-id-or-model-path REPO_ID_OR_MODEL_PATH --dataset DATASET
```

#### Sample Output
```log
{'loss': 2.5668, 'learning_rate': 0.0002, 'epoch': 0.03}
{'loss': 1.6988, 'learning_rate': 0.00017777777777777779, 'epoch': 0.06}
{'loss': 1.3073, 'learning_rate': 0.00015555555555555556, 'epoch': 0.1}
{'loss': 1.3495, 'learning_rate': 0.00013333333333333334, 'epoch': 0.13}
{'loss': 1.1746, 'learning_rate': 0.00011111111111111112, 'epoch': 0.16}
{'loss': 1.0794, 'learning_rate': 8.888888888888889e-05, 'epoch': 0.19}
{'loss': 1.2214, 'learning_rate': 6.666666666666667e-05, 'epoch': 0.22}
{'loss': 1.1698, 'learning_rate': 4.4444444444444447e-05, 'epoch': 0.26}
{'loss': 1.2044, 'learning_rate': 2.2222222222222223e-05, 'epoch': 0.29}
{'loss': 1.1516, 'learning_rate': 0.0, 'epoch': 0.32}
{'train_runtime': xxx, 'train_samples_per_second': xxx, 'train_steps_per_second': xxx, 'train_loss': 1.3923714351654053, 'epoch': 0.32}
100%|█████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████| 200/200 [xx:xx<xx:xx,  xxxs/it]
TrainOutput(global_step=200, training_loss=1.3923714351654053, metrics={'train_runtime': xx, 'train_samples_per_second': xx, 'train_steps_per_second': xx, 'train_loss': 1.3923714351654053, 'epoch': 0.32})
```

### 3. Merge the adapter into the original model
Using the [export_merged_model.py](https://github.com/intel-analytics/BigDL/blob/main/python/llm/example/GPU/QLoRA-FineTuning/export_merged_model.py) to merge.
```
python ./export_merged_model.py --repo-id-or-model-path REPO_ID_OR_MODEL_PATH --adapter_path ./outputs/checkpoint-200 --output_path ./outputs/checkpoint-200-merged
```

Then you can use `./outputs/checkpoint-200-merged` as a normal huggingface transformer model to do inference.

### 4. Use BigDL-LLM to verify the fine-tuning effect
Train more steps and try input sentence like `['quote'] -> [?]` to verify. For example, using `“QLoRA fine-tuning using BigDL-LLM 4bit optimizations on Intel CPU is Efficient and convenient” ->: ` to inference.
BigDL-LLM llama2 example [link](https://github.com/intel-analytics/BigDL/tree/main/python/llm/example/CPU/HF-Transformers-AutoModels/Model/llama2). Update the `LLAMA2_PROMPT_FORMAT = "{prompt}"`.
```bash
python ./generate.py --repo-id-or-model-path REPO_ID_OR_MODEL_PATH --prompt "“QLoRA fine-tuning using BigDL-LLM 4bit optimizations on Intel CPU is Efficient and convenient” ->:"  --n-predict 20
```

#### Sample Output
Base_model output
```log
Inference time: xxx s
-------------------- Prompt --------------------
“QLoRA fine-tuning using BigDL-LLM 4bit optimizations on Intel CPU is Efficient and convenient” ->:
-------------------- Output --------------------
“QLoRA fine-tuning using BigDL-LLM 4bit optimizations on Intel CPU is Efficient and convenient” ->: 💻 Fine-tuning a language model on a powerful device like an Intel CPU
```
Merged_model output
```log
Special tokens have been added in the vocabulary, make sure the associated word embeddings are fine-tuned or trained.
Inference time: xxx s
-------------------- Prompt --------------------
“QLoRA fine-tuning using BigDL-LLM 4bit optimizations on Intel CPU is Efficient and convenient” ->:
-------------------- Output --------------------
“QLoRA fine-tuning using BigDL-LLM 4bit optimizations on Intel CPU is Efficient and convenient” ->: ['bigdl'] ['deep-learning'] ['distributed-computing'] ['intel'] ['optimization'] ['training'] ['training-speed']
```
