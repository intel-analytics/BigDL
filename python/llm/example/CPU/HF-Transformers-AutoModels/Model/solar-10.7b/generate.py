#
# Copyright 2016 The BigDL Authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

import torch
import time
import argparse
import numpy as np

from bigdl.llm.transformers import AutoModelForCausalLM
from transformers import AutoTokenizer

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Predict Tokens using `generate()` API for SOLAR-10.7B model')
    parser.add_argument('--repo-id-or-model-path', type=str, default="upstage/SOLAR-10.7B-Instruct-v1.0",
                        help='The huggingface repo id for the SOLAR-10.7B model to be downloaded'
                             ', or the path to the huggingface checkpoint folder')
    parser.add_argument('--prompt', type=str, default="AI是什么？",
                        help='Prompt to infer')
    parser.add_argument('--n-predict', type=int, default=32,
                        help='Max tokens to predict')

    args = parser.parse_args()
    model_path = args.repo_id_or_model_path

    # Load model in 4 bit,
    # which convert the relevant layers in the model into INT4 format
    model = AutoModelForCausalLM.from_pretrained(model_path,                   
                                                 device_map="cpu",
                                                 torch_dtype=torch.float16,
                                                 load_in_4bit=True,
                                                 trust_remote_code=True).float()

    # Load tokenizer
    tokenizer = AutoTokenizer.from_pretrained(model_path,
                                              trust_remote_code=True)
    
    # Generate predicted tokens
    with torch.inference_mode():
        conversation = [ {'role': 'user', 'content': args.prompt} ] 
        prompt = tokenizer.apply_chat_template(conversation, tokenize=False, add_generation_prompt=True)
        inputs = tokenizer(prompt, return_tensors="pt").to(model.device)
        st = time.time()
        # if your selected model is capable of utilizing previous key/value attentions
        # to enhance decoding speed, but has `"use_cache": false` in its model config,
        # it is important to set `use_cache=True` explicitly in the `generate` function
        # to obtain optimal performance with BigDL-LLM INT4 optimizations
        output = model.generate(**inputs,
                                use_cache=True,
                                max_length=args.n_predict)
        end = time.time()
        output_str = tokenizer.decode(output[0], skip_special_tokens=True)
        print(f'Inference time: {end-st} s')
        print('-'*20, 'Prompt', '-'*20)
        print(prompt)
        print('-'*20, 'Output', '-'*20)
        print(output_str)