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

# This would makes sure Python is aware there is more than one sub-package within bigdl,
# physically located elsewhere.
# Otherwise there would be module not found error in non-pip's setting as Python would
# only search the first bigdl package and end up finding only one sub-package.


from typing import Optional, Union, Sequence, List
from bigdl.llm.utils.common import invalidInputError
import torch


class GenerationMixin:
    """
    A class containing all functions for auto-regressive text generation

    Pass custom parameter values to 'generate' .
    """
    def tokenize(self,
                 text: Union[str, List[str]],
                 add_bos: bool = True) -> List[int]:
        '''
        Decode the id to words

        :param text: The text or batch of text to be tokenized
        :param add_bos:

        :return: list of ids that indicates the tokens
        '''
        is_batched = True if isinstance(text, (list, tuple)) else False
        if not is_batched:
            text = [text]

        result = []
        for t in text:
            if isinstance(t, str):
                bstr = t.encode()
            else:
                bstr = t
            result.append(self._tokenize(bstr, add_bos))

        if not is_batched:
            result = result[0]
        return result

    def decode(self, tokens: List[int]) -> str:
        '''
        Decode the id to words

        Examples:
            >>> llm = AutoModelForCausalLM.from_pretrained("gpt4all-model-q4_0.bin",
                                                           model_family="llama")
            >>> tokens = llm.tokenize("Q: Tell me something about Intel. A:")
            >>> tokens_id = llm.generate(tokens, max_new_tokens=32)
            >>> llm.decode(tokens_id[0])

        :param tokens: list of ids that indicates the tokens, mostly generated by generate
        :return: decoded string
        '''
        return self.detokenize(tokens).decode()

    def batch_decode(self,
                     tokens: Union[List[int], List[List[int]]]) -> str:
        '''
        Decode the id to words

        :param tokens: list or a batch of list of ids that indicates the tokens,
                mostly generated by generate
        :return: decoded string
        '''
        is_batched = False
        if tokens is not None and len(tokens) > 0:
            if isinstance(tokens[0], Sequence):
                is_batched = True
            else:
                tokens = [tokens]
        else:
            return None

        results = []
        for t in tokens:
            results.append(self.decode(t))
        if not is_batched:
            results = results[0]
        return results

    def generate(
        self,
        inputs: Optional[Union[Sequence[int],
                         Sequence[Sequence[int]],
                         torch.Tensor]]=None,
        max_new_tokens: int = 128,
        top_k: int = 40,
        top_p: float = 0.95,
        temperature: float = 0.80,
        repetition_penalty: float = 1.1,
        reset: bool = True,
        frequency_penalty: float = 0.0,
        presence_penalty: float = 0.0,
        tfs_z: float = 1.0,
        mirostat_mode: int = 0,
        mirostat_tau: float = 5.0,
        mirostat_eta: float = 0.1,
        stop: Optional[Union[str, List[str]]]=[],  # TODO: rebase to support stopping_criteria
        **kwargs,
    ) -> Optional[Union[Sequence[int],
                  Sequence[Sequence[int]],
                  None]]:
        # TODO: modify docs
        """Create a generator of tokens from a prompt.

        Examples:
            >>> llm = AutoModelForCausalLM.from_pretrained("gpt4all-model-q4_0.bin",
                                                           model_family="llama")
            >>> tokens = llm.tokenize("Q: Tell me something about Intel. A:")
            >>> tokens_id = llm.generate(tokens, max_new_tokens=32)
            >>> llm.batch_decode(tokens_id)

        Args:
            tokens: The prompt tokens.
            top_k: The top-k sampling parameter.
            top_p: The top-p sampling parameter.
            temp: The temperature parameter.
            repeat_penalty: The repeat penalty parameter.
            reset: Whether to reset the model state.

        Yields:
            The generated tokens.
        """
        if isinstance(inputs, torch.Tensor):
            inputs = inputs.tolist()
        if inputs and len(inputs) > 0:
            if not isinstance(inputs[0], Sequence):
                inputs = [inputs]
        else:
            return None

        results = []
        for input in inputs:
            tokens = self._generate(tokens=input,
                                    top_k=top_k,
                                    top_p=top_p,
                                    temp=temperature,
                                    repeat_penalty=repetition_penalty,
                                    reset=reset,
                                    frequency_penalty=frequency_penalty,
                                    presence_penalty=presence_penalty,
                                    tfs_z=tfs_z,
                                    mirostat_mode=mirostat_mode,
                                    mirostat_tau=mirostat_tau,
                                    mirostat_eta=mirostat_eta,
                                    **kwargs)
            res_list = []
            word_count = 0
            for token in tokens:
                if word_count >= max_new_tokens:
                    break
                res_list.append(token)
                word_count += 1
            results.append(res_list)

        return results
