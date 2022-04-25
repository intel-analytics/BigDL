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
import tempfile

import torch
from torch import nn
from torch.utils.data import TensorDataset, DataLoader
import torchmetrics

import numpy as np

from bigdl.nano.pytorch.trainer import Trainer
from bigdl.nano.pytorch.vision.models import vision

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


class MultiInputModel(nn.Module):
    def __init__(self):
        super().__init__()

        self.layer_1 = nn.Linear(28 * 28, 128)
        self.layer_2 = nn.Linear(28 * 28, 128)
        self.layer_3 = nn.Linear(256, 2)

    def forward(self, x1, x2):
        x1 = self.layer_1(x1)
        x2 = self.layer_2(x2)
        x = torch.cat([x1, x2], axis=1)

        return self.layer_3(x)


class TestOnnx(TestCase):

    def test_trainer_compile_with_onnx(self):
        # TODO: to remove this ut
        model = ResNet18(10, pretrained=False, include_top=False, freeze=True)
        loss = nn.CrossEntropyLoss()
        optimizer = torch.optim.Adam(model.parameters(), lr=0.01)
        trainer = Trainer(max_epochs=1)

        pl_model = Trainer.compile(model, loss, optimizer, onnx=True)
        x = torch.rand((10, 3, 256, 256))
        y = torch.ones((10, ), dtype=torch.long)
        ds = TensorDataset(x, y)
        train_loader = DataLoader(ds, batch_size=2)
        trainer.fit(pl_model, train_loader)

        for x, y in train_loader:
            onnx_res = pl_model.inference(x.numpy())  # onnxruntime
            pytorch_res = pl_model.inference(x.numpy(), backend=None).numpy()  # native pytorch
            pl_model.eval_onnx()
            forward_res = pl_model(x).numpy()
            pl_model.exit_onnx()
            np.testing.assert_almost_equal(onnx_res, pytorch_res, decimal=5)  # same result
            np.testing.assert_almost_equal(onnx_res, forward_res, decimal=5)  # same result

        trainer = Trainer(max_epochs=1)
        trainer.fit(pl_model, train_loader)

        pl_model.eval_onnx()  # update the ortsess with default settings

        for x, y in train_loader:
            pl_model.inference(x.numpy())

    def test_trainer_trace_onnx(self):
        model = ResNet18(10, pretrained=False, include_top=False, freeze=True)
        loss = nn.CrossEntropyLoss()
        optimizer = torch.optim.Adam(model.parameters(), lr=0.01)
        trainer = Trainer(max_epochs=1)

        pl_model = Trainer.compile(model, loss, optimizer, onnx=True)
        x = torch.rand((10, 3, 256, 256))
        y = torch.ones((10, ), dtype=torch.long)
        ds = TensorDataset(x, y)
        train_loader = DataLoader(ds, batch_size=2)
        trainer.fit(pl_model, train_loader)

        onnx_model = trainer.trace(pl_model, accelerator="onnxruntime")

        for x, y in train_loader:
            model.eval()
            with torch.no_grad():
                forward_res_pytorch = pl_model(x).numpy()
            forward_res_onnx = onnx_model(x).numpy()
            np.testing.assert_almost_equal(forward_res_onnx, forward_res_pytorch, decimal=5)

    def test_multiple_input_onnx(self):
        # TODO: to remove this ut
        model = MultiInputModel()
        loss = nn.CrossEntropyLoss()
        optimizer = torch.optim.Adam(model.parameters(), lr=0.01)
        trainer = Trainer(max_epochs=1)

        pl_model = Trainer.compile(model, loss, optimizer, onnx=True)
        x1 = torch.randn(100, 28 * 28)
        x2 = torch.randn(100, 28 * 28)
        y = torch.zeros(100).long()
        y[0:50] = 1
        train_loader = DataLoader(TensorDataset(x1, x2, y), batch_size=32, shuffle=True)
        trainer.fit(pl_model, train_loader)

        for x1, x2, y in train_loader:
            onnx_res = pl_model.inference([x1.numpy(), x2.numpy()])  # onnxruntime
            pytorch_res = pl_model.inference([x1.numpy(), x2.numpy()], backend=None).numpy()  # native pytorch
            pl_model.eval_onnx()
            forward_res = pl_model(x1, x2).numpy()
            pl_model.exit_onnx()
            np.testing.assert_almost_equal(onnx_res, pytorch_res, decimal=5)  # same result
            np.testing.assert_almost_equal(onnx_res, forward_res, decimal=5)  # same result

        trainer = Trainer(max_epochs=1)
        trainer.fit(pl_model, train_loader)

        pl_model.eval_onnx()  # update the ortsess with default settings

        for x1, x2, y in train_loader:
            pl_model.inference([x1.numpy(), x2.numpy()])

    def test_trainer_trace_multiple_input_onnx(self):
        model = MultiInputModel()
        loss = nn.CrossEntropyLoss()
        optimizer = torch.optim.Adam(model.parameters(), lr=0.01)
        trainer = Trainer(max_epochs=1)

        pl_model = Trainer.compile(model, loss, optimizer, onnx=True)
        x1 = torch.randn(100, 28 * 28)
        x2 = torch.randn(100, 28 * 28)
        y = torch.zeros(100).long()
        y[0:50] = 1
        train_loader = DataLoader(TensorDataset(x1, x2, y), batch_size=32, shuffle=True)
        trainer.fit(pl_model, train_loader)

        onnx_model = trainer.trace(pl_model, accelerator="onnxruntime")

        for x1, x2, y in train_loader:
            model.eval()
            with torch.no_grad():
                forward_res_pytorch = pl_model(x1, x2).numpy()
            forward_res_onnx = onnx_model(x1, x2).numpy()
            np.testing.assert_almost_equal(forward_res_onnx, forward_res_pytorch, decimal=5)


if __name__ == '__main__':
    pytest.main([__file__])
