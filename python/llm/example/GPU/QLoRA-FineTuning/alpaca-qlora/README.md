# Alpaca QLoRA & QA-LoRA Finetuning

This example ports [Alpaca-LoRA](https://github.com/tloen/alpaca-lora/tree/main) to BigDL-LLM (using either [QLoRA](https://arxiv.org/abs/2305.14314) or [QA-LoRA](https://arxiv.org/abs/2309.14717) algorithm) on [Intel GPU](../../README.md). Several models (Llama2 / Falcon / Mistral) have been verified based on this example.

### 0. Requirements
To run this example with BigDL-LLM on Intel GPUs, we have some recommended requirements for your machine, please refer to [here](../../README.md#requirements) for more information.

### 1. Install

```bash
conda create -n llm python=3.9
conda activate llm
# below command will install intel_extension_for_pytorch==2.1.10+xpu
# you can install specific ipex/torch version for your need
pip install --pre --upgrade bigdl-llm[xpu_2.1] -f https://developer.intel.com/ipex-whl-stable-xpu
pip install datasets transformers==4.34.0
pip install fire peft==0.5.0
pip install oneccl_bind_pt==2.1.100+xpu -f https://developer.intel.com/ipex-whl-stable-xpu # necessary to run distributed finetuning
pip install accelerate==0.23.0
```

### 2. Configures OneAPI environment variables
```bash
# intel_extension_for_pytorch==2.1.10+xpu requires oneAPI 2024.0
source /opt/intel/oneapi/setvars.sh
```

### 3. Finetune

Here, we provide example usages on different models and different hardwares. Please refer to the appropriate script based on your device:

#### 3.1 Llama2 series
<details><summary>Show LLaMA2-7B example</summary>

#### QLoRA

##### Finetuning LLaMA2-7B on single Arc A770

```bash
bash finetune_llama2_7b_arc_1_card.sh
```

##### Finetuning LLaMA2-7B on two Arc A770

```bash
bash finetune_llama2_7b_arc_2_card.sh
```

##### Finetuning LLaMA2-7B on single Data Center GPU Flex 170

```bash
bash finetune_llama2_7b_flex_170_1_card.sh
```

##### Finetuning LLaMA2-7B on three Data Center GPU Flex 170

```bash
bash finetune_llama2_7b_flex_170_3_card.sh
```

##### Finetuning LLaMA2-7B on single Intel Data Center GPU Max 1100

```bash
bash finetune_llama2_7b_pvc_1100_1_card.sh
```

##### Finetuning LLaMA2-7B on four Intel Data Center GPU Max 1100

```bash
bash finetune_llama2_7b_pvc_1100_4_card.sh
```

##### Finetuning LLaMA2-7B on single Intel Data Center GPU Max 1550

```bash
bash finetune_llama2_7b_pvc_1550_1_card.sh
```

##### Finetuning LLaMA2-7B on four Intel Data Center GPU Max 1550

```bash
bash finetune_llama2_7b_pvc_1550_4_card.sh
```

#### QA-LoRA
##### Finetuning LLaMA2-7B on single Arc A770

```bash
bash qalora_finetune_llama2_7b_arc_1_card.sh
```

##### Finetuning LLaMA2-7B on two Arc A770

```bash
bash qalora_finetune_llama2_7b_arc_2_card.sh
```

##### Finetuning LLaMA2-7B on single Tile Intel Data Center GPU Max 1550

```bash
bash qalora_finetune_llama2_7b_pvc_1550_1_tile.sh
```
</details>


#### 3.2 Mistral

#### 3.3 Falcon-40B


### 4. (Optional) Resume Training
If you fail to complete the whole finetuning process, it is suggested to resume training from a previously saved checkpoint by specifying `resume_from_checkpoint` to the local checkpoint folder as following:**
```bash
python ./alpaca_qlora_finetuning.py \
    --base_model "meta-llama/Llama-2-7b-hf" \
    --data_path "yahma/alpaca-cleaned" \
    --output_dir "./bigdl-qlora-alpaca" \
    --resume_from_checkpoint "./bigdl-qlora-alpaca/checkpoint-1100"
```

### 5. Sample Loss Output
```log
{'loss': 1.9231, 'learning_rate': 2.9999945367033285e-05, 'epoch': 0.0}                                                                                                                            
{'loss': 1.8622, 'learning_rate': 2.9999781468531096e-05, 'epoch': 0.01}                                                                                                                           
{'loss': 1.9043, 'learning_rate': 2.9999508305687345e-05, 'epoch': 0.01}                                                                                                                           
{'loss': 1.8967, 'learning_rate': 2.999912588049185e-05, 'epoch': 0.01}                                                                                                                            
{'loss': 1.9658, 'learning_rate': 2.9998634195730358e-05, 'epoch': 0.01}                                                                                                                           
{'loss': 1.8386, 'learning_rate': 2.9998033254984483e-05, 'epoch': 0.02}                                                                                                                           
{'loss': 1.809, 'learning_rate': 2.999732306263172e-05, 'epoch': 0.02}                                                                                                                             
{'loss': 1.8552, 'learning_rate': 2.9996503623845395e-05, 'epoch': 0.02}                                                                                                                           
  1%|█                                                                                                                                                         | 8/1164 [xx:xx<xx:xx:xx, xx s/it]
```

### 6. Merge the adapter into the original model
```
python ./export_merged_model.py --repo-id-or-model-path REPO_ID_OR_MODEL_PATH --adapter_path ./outputs/checkpoint-200 --output_path ./outputs/checkpoint-200-merged
```

Then you can use `./outputs/checkpoint-200-merged` as a normal huggingface transformer model to do inference.

### 7. Troubleshooting
- If you fail to finetune on multi cards because of following error message:
  ```bash
  RuntimeError: oneCCL: comm_selector.cpp:57 create_comm_impl: EXCEPTION: ze_data was not initialized
  ```
  Please try `sudo apt install level-zero-dev` to fix it.
