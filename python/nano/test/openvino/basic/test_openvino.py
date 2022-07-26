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
from tempfile import TemporaryDirectory
from unittest import TestCase
import os
from bigdl.nano.openvino import OpenVINOModel
import numpy as np


class TestOpenVINO(TestCase):
    def test_openvino_model(self):
        os.system("omz_downloader --name resnet18-xnor-binary-onnx-0001")

        openvino_model = OpenVINOModel("./intel/resnet18-xnor-binary-onnx-0001/FP16-INT1/resnet18-xnor-binary-onnx-0001.xml")
        x = np.random.randn(1, 3, 224, 224)
        y_hat = openvino_model.forward_step(x)
        assert tuple(next(iter(y_hat)).shape) == (1, 1000)
