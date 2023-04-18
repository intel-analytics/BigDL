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

import os
import platform
import pytest
from unittest import TestCase

import torch
import torchmetrics
from torch.utils.data import DataLoader, TensorDataset
from torch import nn

from bigdl.nano.pytorch.lightning import LightningModule
from bigdl.nano.pytorch import Trainer

from test.pytorch.utils._train_torch_lightning import create_data_loader, data_transform
from test.pytorch.utils._train_torch_lightning import create_test_data_loader
from test.pytorch.tests.train.trainer.test_lightning import ResNet18
from bigdl.nano.utils.common import compare_version
import operator

import copy

num_classes = 10
batch_size = 32
dataset_size = 256
num_workers = 0
data_dir = "/tmp/data"


# Linear Model: 1 unit without bias, initialize to 1
class LinearModel(nn.Module):
    def __init__(self):
        super().__init__()
        # need this line to avoid optimizer raise empty variable list
        self.fc1 = nn.Linear(1, 1, bias=False)
        self.fc1.weight.data.fill_(1.0)

    def forward(self, input_):
        return self.fc1(input_)

class TestPlugin(TestCase):
    model = ResNet18(pretrained=False, include_top=False, freeze=True)
    loss = nn.CrossEntropyLoss()
    optimizer = torch.optim.Adam(model.parameters(), lr=0.01)
    data_loader = create_data_loader(data_dir, batch_size, num_workers,
                                     data_transform, subset=dataset_size)
    test_data_loader = create_test_data_loader(data_dir, batch_size, num_workers,
                                               data_transform, subset=dataset_size)

    def setUp(self):
        test_dir = os.path.dirname(__file__)
        # project_test_dir = BigDL/python/nano
        project_test_dir = os.path.abspath(
            os.path.join(test_dir, "..", "..", "..", "..", "..")
        )
        os.environ['PYTHONPATH'] = project_test_dir

    @pytest.mark.skipif(compare_version("torch", operator.ge, "2.0.0") and compare_version("pytorch_lightning", operator.lt, '2.0.0'),
                        reason="We have not upgraded version of pytorch_lightning.")
    def test_trainer_subprocess_plugin(self):
        pl_model = LightningModule(
            self.model, self.loss, self.optimizer,
            metrics=[torchmetrics.F1Score('multiclass', num_classes=num_classes),
                     torchmetrics.Accuracy('multiclass', num_classes=num_classes)]
        )
        trainer = Trainer(num_processes=2, distributed_backend="subprocess",
                          max_epochs=4)
        trainer.fit(pl_model, self.data_loader, self.test_data_loader)
        trainer.test(pl_model, self.test_data_loader)

    @pytest.mark.skipif(compare_version("torch", operator.ge, "2.0.0") and compare_version("pytorch_lightning", operator.lt, '2.0.0'),
                        reason="We have not upgraded version of pytorch_lightning.")
    def test_trainer_subprocess_sys_path(self):
        """test whether child process can inherit parent process's sys.path"""
        # add current directory to sys.path and
        # import model from test_lightning.py which is in current directory
        import sys
        sys.path.append(os.path.dirname(__file__))
        from test_lightning import ResNet18

        model = ResNet18(pretrained=False, include_top=False, freeze=True)
        pl_model = LightningModule(
            model, self.loss, self.optimizer,
            metrics=[torchmetrics.F1Score('multiclass', num_classes=num_classes),
                     torchmetrics.Accuracy('multiclass', num_classes=num_classes)]
        )
        trainer = Trainer(num_processes=2, distributed_backend="subprocess",
                          max_epochs=4)
        trainer.fit(pl_model, self.data_loader, self.test_data_loader)
        trainer.test(pl_model, self.test_data_loader)

    def test_trainer_subprocess_correctness(self):
        # dataset: features: [0, 0, 1, 1] / labels: [0, 0, 0, 0]
        # model: y = w * x
        # loss = (wx)^2
        # dloss/dw = 2x^2*w
        # end of first iteration:
        #    avg_grad = avg([0, 0, 2, 2]) = 1
        #    weight = 1.0 - 0.5 * avg_grad = 0.5
        # end of second iteration:
        #    avg_grad = avg([0, 0, 1, 1]) = 0.5
        #    weight = 0.5 - 0.5 * avg_grad = 0.25

        linear = LinearModel()
        pl_model = LightningModule(
            model = linear,
            optimizer = torch.optim.SGD(linear.parameters(), lr=0.5),
            loss=torch.nn.MSELoss(),
            metrics=[torchmetrics.MeanSquaredError()]
        )
        trainer = Trainer(num_processes=2, distributed_backend="subprocess", max_epochs=2,
                          auto_lr=False)
        features = torch.tensor([[0.0],[0.0],[1.0],[1.0]])
        labels = torch.tensor([[0.0],[0.0],[0.0],[0.0]])

        dataset = TensorDataset(features,labels)
        train_loader = DataLoader(dataset=dataset, batch_size=2, shuffle=False)
        trainer.fit(pl_model, train_loader, train_loader)

        assert pl_model.model.fc1.weight.data == 0.25, "subprocess plugin works incorrect"
        return

    def test_trainer_spawn_correctness(self):
        # same as subprocess test

        linear = LinearModel()
        pl_model = LightningModule(
            model = linear,
            optimizer = torch.optim.SGD(linear.parameters(), lr=0.5),
            loss=torch.nn.MSELoss(),
            metrics=[torchmetrics.MeanSquaredError()]
        )
        trainer = Trainer(num_processes=2, distributed_backend="spawn", max_epochs=2, auto_lr=False)
        features = torch.tensor([[0.0],[0.0],[1.0],[1.0]])
        labels = torch.tensor([[0.0],[0.0],[0.0],[0.0]])

        dataset = TensorDataset(features,labels)
        train_loader = DataLoader(dataset=dataset, batch_size=2, shuffle=False)
        trainer.fit(pl_model, train_loader, train_loader)

        assert pl_model.model.fc1.weight.data == 0.25, "spawn plugin works incorrect"
        return

    @pytest.mark.skipif(platform.system() != "Linux", reason="torch_ccl is only avaiable on Linux")
    @pytest.mark.skipif(compare_version("torch", operator.ge, "2.0.0") and compare_version("pytorch_lightning", operator.lt, '2.0.0'),
                        reason="We have not upgraded version of pytorch_lightning.")
    def test_trainer_subprocess_with_ccl(self):
        pl_model = LightningModule(
            self.model, self.loss, self.optimizer,
            metrics=[torchmetrics.F1Score('multiclass', num_classes=num_classes),
                     torchmetrics.Accuracy('multiclass', num_classes=num_classes)]
        )
        trainer = Trainer(num_processes=2, distributed_backend="subprocess",
                          process_group_backend='ccl', max_epochs=4)
        trainer.fit(pl_model, self.data_loader, self.test_data_loader)
        trainer.test(pl_model, self.test_data_loader)

    @pytest.mark.skipif(platform.system() != "Linux", reason="torch_ccl is only avaiable on Linux")
    @pytest.mark.skipif(compare_version("torch", operator.ge, "2.0.0") and compare_version("pytorch_lightning", operator.lt, '2.0.0'),
                        reason="We have not upgraded version of pytorch_lightning.")
    def test_trainer_spawn_with_ccl(self):
        pl_model = LightningModule(
            self.model, self.loss, self.optimizer,
            metrics=[torchmetrics.F1Score('multiclass', num_classes=num_classes),
                     torchmetrics.Accuracy('multiclass', num_classes=num_classes)]
        )
        trainer = Trainer(num_processes=2, distributed_backend="spawn",
                          process_group_backend='ccl', max_epochs=4)
        trainer.fit(pl_model, self.data_loader, self.test_data_loader)
        trainer.test(pl_model, self.test_data_loader)


if __name__ == '__main__':
    pytest.main([__file__])
