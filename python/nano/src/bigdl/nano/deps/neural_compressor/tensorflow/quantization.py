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
from bigdl.nano.utils.log4Error import invalidInputError
from ..core import BaseQuantization
from .metric import TensorflowINCMetric
from tensorflow.keras.metrics import Metric
from .utils import Dataloader


class TensorflowQuantization(BaseQuantization):
    def __init__(self, framework='tensorflow', **kwargs):
        """
        Create a Intel Neural Compressor Quantization object for Pytorch.
        """
        kwargs['framework'] = framework
        super().__init__(**kwargs)
        self._inc_metric_cls = TensorflowINCMetric

    def _pre_execution(self, model, calib_dataloader=None, metric=None):
        # get inputs/outputs
        def get_tensors_name(tensors):
            return [tensor.name for tensor in tensors]

        if self.cfg.model.inputs is None:
            self.cfg.model.inputs = get_tensors_name(model.inputs)
        if self.cfg.model.outputs is None:
            self.cfg.model.outputs = get_tensors_name(model.outputs)

        if calib_dataloader:
            calib_dataloader = Dataloader(calib_dataloader, 1)

        return model, calib_dataloader, metric

    def _post_execution(self, q_model):
        # TODO: Need to wrapp q_model similar to Pytorch
        return q_model

    @property
    def valid_frameworks(self):
        return ('tensorflow')

    @property
    def valid_approaches(self):
        return ('post_training_static_quant')

    def sanity_check_before_execution(self, model, calib_dataloader, metric):
        invalidInputError(model.inputs is not None and model.outputs is not None,
                          "A keras.Model for quantization must include Input layers. "
                          "Please create the model by functional API"
                          " keras.Model(inputs=.., outputs=..).\n"
                          "More details in https://keras.io/api/models/model/")
        
        super().sanity_check_before_execution(model, calib_dataloader, metric)
