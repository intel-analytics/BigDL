import torch
import time
import argparse
import numpy as np

from transformers import AutoTokenizer, GenerationConfig
from bigdl.llm import optimize_model
# you could tune the prompt based on your own model,
# here the prompt tuning refers to  # TODO: https://huggingface.co/microsoft/phi-1_5/blob/main/modeling_mixformer_sequential.py
PHI1_5_PROMPT_FORMAT = " Question:{prompt}\n\n Answer:"
generation_config = GenerationConfig(use_cache = True)

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Predict Tokens using `generate()` API for phixtral model')
    parser.add_argument('--repo-id-or-model-path', type=str, default="mlabonne/phixtral-4x2_8",
                        help='The huggingface repo id for the phi model to be downloaded'
                             ', or the path to the huggingface checkpoint folder')
    parser.add_argument('--prompt', type=str, default="What is AI?",
                        help='Prompt to infer')
    parser.add_argument('--n-predict', type=int, default=32,
                        help='Max tokens to predict')

    args = parser.parse_args()
    model_path = args.repo_id_or_model_path
    
    from transformers import AutoModelForCausalLM
    model = AutoModelForCausalLM.from_pretrained(model_path,
                                                 trust_remote_code=True)
    model = optimize_model(model)

    # Load tokenizer
    tokenizer = AutoTokenizer.from_pretrained(model_path,
                                              trust_remote_code=True)
    
    # Generate predicted tokens
    with torch.inference_mode():
        prompt = PHI1_5_PROMPT_FORMAT.format(prompt=args.prompt)
        input_ids = tokenizer.encode(prompt, return_tensors="pt")
        st = time.time()

        # Note that phixtral uses GenerationConfig to enable 'use_cache'
        output = model.generate(input_ids, do_sample=False, max_new_tokens=args.n_predict, generation_config = generation_config)

        end = time.time()
        output_str = tokenizer.decode(output[0], skip_special_tokens=True)
        print(f'Inference time: {end-st} s')
        print('-'*20, 'Prompt', '-'*20)
        print(prompt)
        print('-'*20, 'Output', '-'*20)
        print(output_str)
