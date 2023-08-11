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


import unittest
import os
import pytest
import time
import torch
from bigdl.llm.transformers import AutoModel, AutoModelForCausalLM, AutoModelForSpeechSeq2Seq
from transformers import AutoTokenizer

class TestTransformersAPI(unittest.TestCase):

    def setUp(self):        
        thread_num = os.environ.get('THREAD_NUM')
        if thread_num is not None:
            self.n_threads = int(thread_num)
        else:
            self.n_threads = 2

    def test_transformers_auto_model_int4(self):
        model_path = os.environ.get('ORIGINAL_CHATGLM2_6B_PATH')
        model = AutoModel.from_pretrained(model_path, trust_remote_code=True, load_in_4bit=True)
        tokenizer = AutoTokenizer.from_pretrained(model_path, trust_remote_code=True)
        input_str = "Tell me the capital of France.\n\n"

        with torch.inference_mode():
            st = time.time()
            input_ids = tokenizer.encode(input_str, return_tensors="pt")
            output = model.generate(input_ids, do_sample=False, max_new_tokens=32)
            output_str = tokenizer.decode(output[0], skip_special_tokens=True)
            end = time.time()
        print('Prompt:', input_str)
        print('Output:', output_str)
        print(f'Inference time: {end-st} s')
        res = 'Paris' in output_str        
        self.assertTrue(res)

    def test_transformers_auto_model_for_causal_lm_int4(self):
        model_path = os.environ.get('ORIGINAL_REPLIT_CODE_PATH')
        tokenizer = AutoTokenizer.from_pretrained(model_path, trust_remote_code=True)
        input_str = 'def hello():\n  print("hello world")\n'
        model = AutoModelForCausalLM.from_pretrained(model_path, trust_remote_code=True, load_in_4bit=True)
        with torch.inference_mode():
            
            st = time.time()
            input_ids = tokenizer.encode(input_str, return_tensors="pt")
            output = model.generate(input_ids, do_sample=False, max_new_tokens=32)
            output_str = tokenizer.decode(output[0], skip_special_tokens=True)
            end = time.time()
        print('Prompt:', input_str)
        print('Output:', output_str)
        print(f'Inference time: {end-st} s')
        res = '\nhello()' in output_str        
        self.assertTrue(res)
        

    def test_transformers_auto_model_for_speech_seq2seq_int4(self):
        from transformers import WhisperProcessor, WhisperForConditionalGeneration
        from datasets import load_from_disk
        model_path = os.environ.get('ORIGINAL_WHISPER_TINY_PATH')
        dataset_path = os.environ.get('SPEECH_DATASET_PATH')
        processor = WhisperProcessor.from_pretrained(model_path)
        ds = load_from_disk(dataset_path)
        sample = ds[0]["audio"]
        input_features = processor(sample["array"], sampling_rate=sample["sampling_rate"], return_tensors="pt").input_features
        model = AutoModelForSpeechSeq2Seq.from_pretrained(model_path, trust_remote_code=True, load_in_4bit=True)
        with torch.inference_mode():
            st = time.time()
            predicted_ids = model.generate(input_features)
            # decode token ids to text
            transcription = processor.batch_decode(predicted_ids, skip_special_tokens=False)
            end = time.time()        
        print('Output:', transcription)
        print(f'Inference time: {end-st} s')
        res = 'Mr. Quilter is the apostle of the middle classes and we are glad to welcome his gospel.' in transcription[0]
        self.assertTrue(res)

    def test_transformers_unify_api(self):
        from bigdl.llm.transformers import ChatGLMForCausalLM
        model_path = os.environ.get('ORIGINAL_CHATGLM2_6B_PATH')
        model = ChatGLMForCausalLM.from_pretrained(model_path, native=False, trust_remote_code=True, load_in_4bit=True)
        tokenizer = AutoTokenizer.from_pretrained(model_path, trust_remote_code=True)
        input_str = "Tell me the capital of France.\n\n"

        with torch.inference_mode():
            st = time.time()
            input_ids = tokenizer.encode(input_str, return_tensors="pt")
            output = model.generate(input_ids, do_sample=False, max_new_tokens=32)
            output_str = tokenizer.decode(output[0], skip_special_tokens=True)
            end = time.time()
        print('Prompt:', input_str)
        print('Output:', output_str)
        print(f'Inference time: {end-st} s')
        res = 'Paris' in output_str        
        self.assertTrue(res)

if __name__ == '__main__':
    pytest.main([__file__])
