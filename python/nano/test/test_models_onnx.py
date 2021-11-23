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


import pytest
import os
from unittest import TestCase

import torch
from torch import nn

import numpy as np

from test._train_torch_lightning import create_data_loader, data_transform
from bigdl.nano.pytorch.trainer import Trainer
from bigdl.nano.pytorch.vision.models import vision
from test._train_torch_lightning import train_with_linear_top_layer

batch_size = 256
num_workers = 0
data_dir = os.path.join(os.path.dirname(__file__), "data")

class ResNet18(nn.Module):
    def __init__(self, num_classes, pretrained=True, include_top=False, freeze=True):
        super().__init__()
        backbone = vision.resnet18(pretrained=pretrained, include_top=include_top, freeze=freeze)
        output_size = backbone.get_output_size()
        head = nn.Linear(output_size, num_classes)
        self.model = nn.Sequential(backbone, head)

    def forward(self, x):
        return self.model(x)

class TestModelsVision(TestCase):
    
    def test_trainer_compile_with_onnx(self):
        model = ResNet18(10, pretrained=False, include_top=False, freeze=True)
        loss = nn.CrossEntropyLoss()
        optimizer = torch.optim.Adam(model.parameters(), lr=0.01)
        trainer = Trainer(max_epochs=1)

        pl_model = Trainer.compile(model, loss, optimizer, onnx=True)
        train_loader = create_data_loader(data_dir, batch_size,\
            num_workers, data_transform, subset=200)
        trainer.fit(pl_model, train_loader)
        assert pl_model._ortsess_up_to_date is False # ortsess is not up-to-date after training

        for x, y in train_loader:
            onnx_res = pl_model.inference(x.numpy(), file_path="/tmp/model.onnx")  # onnxruntime
            pytorch_res = pl_model.inference(x, backend=None).numpy()  # native pytorch
            assert pl_model._ortsess_up_to_date is True  # ortsess is up-to-date while inferencing
            np.testing.assert_almost_equal(onnx_res, pytorch_res, decimal=5)  # same result

        trainer.fit(pl_model, train_loader)
        assert pl_model._ortsess_up_to_date is False # ortsess is not up-to-date after training

        pl_model.update_ortsess()  # update the ortsess with default settings
        assert pl_model._ortsess_up_to_date is True # ortsess is up-to-date after updating

        trainer.predict(pl_model, train_loader)


if __name__ == '__main__':
    pytest.main([__file__])
