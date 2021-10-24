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


from pytorch_lightning import LightningModule
import onnxruntime
from functools import wraps
import torch

def onnxruntime_support(override_predict_step=True):

    def onnxruntime_decorator(cls):
        # class type check
        assert issubclass(cls, LightningModule),\
            "onnxruntime_support decorator is only valid for a LightningModule."

        # additional attributes
        cls._ortsess_up_to_date = False  # indicate if we need to build ortsess again
        cls._ortsess = None  # ortsess instance

        # _build_ortsess
        def _build_ortsess(self, input_sample=None, filepath="model.onnx", **kwargs):
            if input_sample is None and self.example_input_array is not None:
                input_sample = self.example_input_array
            self.to_onnx(filepath, input_sample, export_params=True, **kwargs)
            self._ortsess = onnxruntime.InferenceSession(filepath)
        cls._build_ortsess = _build_ortsess

        # inference_with_onnx
        def inference_with_onnx(self, input_data, **kwargs):
            '''
            :param input_data: numpy ndarray
            '''
            if not self._ortsess_up_to_date:
                input_sample = torch.Tensor(input_data)
                self._build_ortsess(input_sample=input_sample, **kwargs)
            input_name = self._ortsess.get_inputs()[0].name
            ort_inputs = {input_name: input_data}
            ort_outs = self._ortsess.run(None, ort_inputs)
            self._ortsess_up_to_date = True
        cls.inference_with_onnx = inference_with_onnx

        # on_fit_end
        def on_fit_end_additional(function):
            @wraps(function)
            def wrapped(*args, **kwargs):
                args[0]._ortsess_up_to_date = False
                return function(*args, **kwargs)
            return wrapped
        cls.on_fit_end = on_fit_end_additional(cls.on_fit_end)

        # predict_step
        if override_predict_step:
            def predict_step(self, batch, batch_idx):
                return self.inference_with_onnx(batch[0].numpy())
            cls.predict_step = predict_step

        return cls

    return onnxruntime_decorator
