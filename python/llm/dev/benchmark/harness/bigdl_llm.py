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

from bigdl.llm.transformers import AutoModelForCausalLM
import torch
import inspect
from lm_eval.models.huggingface import AutoCausalLM
from lm_eval import utils
from functools import partial


# wrap  and force the Reorderer to be in a decrease order
# This is a workaround to avoid frequent memory allocation which may cause OOM
def force_decrease_order(Reorderer):
    def DecreaseReorderer(arr, fn):
        def _collate(x):
            len, tokens = fn(x)
            len = - abs(len)
            return len, tokens
        return Reorderer(arr, _collate)
    return DecreaseReorderer
utils.Reorderer = force_decrease_order(utils.Reorderer)


class BigDLLM(AutoCausalLM):
    AUTO_MODEL_CLASS = AutoModelForCausalLM
    AutoCausalLM_ARGS = inspect.getfullargspec(AutoCausalLM.__init__).args
    def __init__(self, *args, **kwargs):
        if 'device' in kwargs and 'xpu' in kwargs['device']:
            import intel_extension_for_pytorch
        self.bigdl_llm_kwargs = {}
        keys = list(kwargs.keys())
        for k in keys:
            if k not in self.AutoCausalLM_ARGS:
                self.bigdl_llm_kwargs[k] = kwargs[k]
                kwargs.pop(k)   
        AutoModelForCausalLM.from_pretrained = partial(AutoModelForCausalLM.from_pretrained, **self.bigdl_llm_kwargs)
        kwargs['trust_remote_code'] = kwargs.get('trust_remote_code', True)
        super().__init__(*args, **kwargs)

    @property
    def add_special_tokens(self) -> bool:
        return False
    
    def _model_call(self, inps):
        """
        inps: a torch tensor of shape [batch, sequence]
        the size of sequence may vary from call to call
        returns: a torch tensor of shape [batch, sequence, vocab] with the
        logits returned from the model
        """
        with torch.inference_mode():
            inps = inps.to(self.device)
            res = self.model(inps)[0]
            return res