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
import operator
import tempfile
from unittest import TestCase

import pytest
import torch
from pytorch_lightning import LightningModule
from torch import nn
from test.pytorch.utils._train_torch_lightning import create_data_loader, data_transform
import torchmetrics
from torchvision.models import resnet18

from bigdl.nano.pytorch import Trainer
from bigdl.nano.pytorch import InferenceOptimizer
from bigdl.nano.pytorch.vision.models import vision
from bigdl.nano.utils.common import invalidOperationError
from bigdl.nano.utils.common import compare_version
from bigdl.nano.utils.pytorch import TORCH_VERSION_LESS_2_0
from bigdl.nano.utils.common import _avx2_checker

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


class LitResNet18(LightningModule):
    def __init__(self, num_classes, pretrained=True, include_top=False, freeze=True):
        super().__init__()
        backbone = vision.resnet18(pretrained=pretrained, include_top=include_top, freeze=freeze)
        output_size = backbone.get_output_size()
        head = nn.Linear(output_size, num_classes)
        self.classify = nn.Sequential(backbone, head)

    def forward(self, *args):
        return self.classify(args[0])


class ModelCannotCopy(ResNet18):
    def __deepcopy__(self, obj):
        invalidOperationError(False, "This model cannot be deepcopy")


class TestINC(TestCase):
    model = ResNet18(10, pretrained=False, include_top=False, freeze=True)
    loss = nn.CrossEntropyLoss()
    optimizer = torch.optim.Adam(model.parameters(), lr=0.01)
    train_loader = create_data_loader(data_dir, batch_size, num_workers, data_transform)
    user_defined_pl_model = LitResNet18(10)

    def test_quantize_inc_ptq_compiled(self):
        # Test if a Lightning Module compiled by nano works
        train_loader_iter = iter(self.train_loader)
        trainer = Trainer(max_epochs=1)
        pl_model = Trainer.compile(self.model, self.loss, self.optimizer)
        x = next(train_loader_iter)[0]

        # Case 1: Default
        qmodel = InferenceOptimizer.quantize(pl_model,
                                             calib_data=self.train_loader)
        assert qmodel
        out = qmodel(x)
        assert out.shape == torch.Size([256, 10])

        # Case 2: Override by arguments
        qmodel = InferenceOptimizer.quantize(pl_model,
                                             calib_data=self.train_loader,
                                             metric=torchmetrics.F1Score('multiclass', num_classes=10),
                                             approach='static',
                                             tuning_strategy='basic',
                                             accuracy_criterion={'relative': 0.99,
                                                                 'higher_is_better': True})

        assert qmodel
        out = qmodel(x)
        assert out.shape == torch.Size([256, 10])

        # Case 3: Dynamic quantization
        qmodel = InferenceOptimizer.quantize(pl_model, approach='dynamic')
        assert qmodel
        out = qmodel(x)
        assert out.shape == torch.Size([256, 10])

        # Case 4: Invalid approach
        invalid_approach = 'qat'
        with pytest.raises(RuntimeError, match="Approach should be 'static' or 'dynamic', "
                                               "{} is invalid.".format(invalid_approach)):
            InferenceOptimizer.quantize(pl_model, approach=invalid_approach)

        # Case 5: Test if registered metric can be fetched successfully
        qmodel = InferenceOptimizer.quantize(pl_model,
                                             calib_data=self.train_loader,
                                             metric=torchmetrics.F1Score('multiclass', num_classes=10),
                                             accuracy_criterion={'relative': 0.99,
                                                                'higher_is_better': True})
        assert qmodel
        out = qmodel(x)
        assert out.shape == torch.Size([256, 10])

        trainer.validate(qmodel, self.train_loader)
        trainer.test(qmodel, self.train_loader)
        trainer.predict(qmodel, self.train_loader)

        # save and load
        with tempfile.TemporaryDirectory() as tmp_dir_name:
            InferenceOptimizer.save(qmodel, tmp_dir_name)
            loaded_qmodel = InferenceOptimizer.load(tmp_dir_name, pl_model)
            assert loaded_qmodel
            out = loaded_qmodel(x)
            assert out.shape == torch.Size([256, 10])

    def test_quantize_inc_ptq_customized(self):
        # Test if a Lightning Module not compiled by nano works
        train_loader_iter = iter(self.train_loader)
        x = next(train_loader_iter)[0]

        qmodel = InferenceOptimizer.quantize(self.user_defined_pl_model,
                                             calib_data=self.train_loader)
        assert qmodel
        out = qmodel(x)
        assert out.shape == torch.Size([256, 10])

        # save and load
        with tempfile.TemporaryDirectory() as tmp_dir_name:
            InferenceOptimizer.save(qmodel, tmp_dir_name)
            loaded_qmodel = InferenceOptimizer.load(tmp_dir_name, self.user_defined_pl_model)
            assert loaded_qmodel
            out = loaded_qmodel(x)
            assert out.shape == torch.Size([256, 10])

    def test_quantize_inc_ptq_with_tensor(self):
        train_loader_iter = iter(self.train_loader)
        trainer = Trainer(max_epochs=1)
        pl_model = Trainer.compile(self.model, self.loss, self.optimizer)
        x = next(train_loader_iter)[0]

        # Case 1: quantize with single tensor
        qmodel = InferenceOptimizer.quantize(pl_model,
                                             calib_data=x)
        assert qmodel
        out = qmodel(x)
        assert out.shape == torch.Size([256, 10])
        
        # Case 2: quantize with tensor tuple
        qmodel = InferenceOptimizer.quantize(pl_model,
                                             # fake a label
                                             calib_data=(x, torch.ones(1)))
        assert qmodel
        out = qmodel(x)
        assert out.shape == torch.Size([256, 10])

    def test_quantize_inc_ptq_compiled_context_manager(self):
        # Test if a Lightning Module compiled by nano works
        train_loader_iter = iter(self.train_loader)
        pl_model = Trainer.compile(self.model, self.loss, self.optimizer)
        x = next(train_loader_iter)[0]

        qmodel = InferenceOptimizer.quantize(pl_model,
                                             calib_data=self.train_loader,
                                             thread_num=2)
        assert qmodel

        with InferenceOptimizer.get_context(qmodel):
            assert torch.get_num_threads() == 2
            out = qmodel(x)
        assert out.shape == torch.Size([256, 10])
        
        with tempfile.TemporaryDirectory() as tmp_dir_name:
            InferenceOptimizer.save(qmodel, tmp_dir_name)
            model = InferenceOptimizer.load(tmp_dir_name, pl_model)

        with InferenceOptimizer.get_context(model):
            assert torch.get_num_threads() == 2
            out = model(x)
        assert out.shape == torch.Size([256, 10])

    def test_quantize_inc_ptq_compiled_additional_attributes(self):
        # Test if a Lightning Module compiled by nano works
        train_loader_iter = iter(self.train_loader)
        pl_model = Trainer.compile(self.model, self.loss, self.optimizer)
        # patch a attribute
        pl_model.channels = 3
        def hello():
            print("hello world!")
        # patch a function
        pl_model.hello = hello
        x = next(train_loader_iter)[0]

        qmodel = InferenceOptimizer.quantize(pl_model,
                                             calib_data=self.train_loader,
                                             thread_num=2)
        assert qmodel
        assert qmodel.channels == 3
        qmodel.hello()
        with pytest.raises(
            AttributeError,
            match="'PytorchQuantizedModel' object has no attribute 'width'"
        ):
            qmodel.width

        with InferenceOptimizer.get_context(qmodel):
            assert torch.get_num_threads() == 2
            out = qmodel(x)
        assert out.shape == torch.Size([256, 10])

        # save & load with original model
        with tempfile.TemporaryDirectory() as tmp_dir_name:
            InferenceOptimizer.save(qmodel, tmp_dir_name)
            load_model = InferenceOptimizer.load(tmp_dir_name, model=pl_model)
        assert load_model.channels == 3
        load_model.hello()
        with pytest.raises(
            AttributeError,
            match="'PytorchQuantizedModel' object has no attribute 'width'"
        ):
            load_model.width

    # This UT will fail with INC < 2.0
    @pytest.mark.skipif(compare_version("neural_compressor", operator.lt, "2.0"), reason="")
    @pytest.mark.skipif(not TORCH_VERSION_LESS_2_0 and not _avx2_checker(), reason="")
    def test_ipex_int8_quantize_with_model_cannot_deepcopy(self):
        model = ModelCannotCopy(num_classes=10)
        InferenceOptimizer.quantize(model,
                                    calib_data=self.train_loader,
                                    method="ipex",
                                    # inplace=False is setting to disable back up ipex quantization
                                    inplace=False)

    # INC 1.14 and 2.0 doesn't supprot quantizing pytorch-lightning module,
    # but we have some workaround for pl models returned by our `Trainer.compile`
    def test_quantize_with_pl_model(self):
        trainer = Trainer(max_epochs=1)
        pl_model = Trainer.compile(self.model, self.loss, self.optimizer)
        trainer.fit(pl_model, self.train_loader)
        InferenceOptimizer.quantize(pl_model,
                                    calib_data=self.train_loader)

    def test_quantize_tuning(self):
        trainer = Trainer(max_epochs=1)
        pl_model = Trainer.compile(self.model, self.loss, self.optimizer)
        trainer.fit(pl_model, self.train_loader)
        train_loader_iter = iter(self.train_loader)
        x = next(train_loader_iter)[0]
        
        # 1. test no tuning
        qmodel = InferenceOptimizer.quantize(pl_model,
                                             calib_data=self.train_loader)
        assert qmodel

        # 2. test eval_func with relative accuracy_criterion
        def eval_func(model):
            acc = 0
            for sample, label in self.train_loader:
                logits = model(sample)
                pred = logits.argmax(dim=1)
                acc += torch.eq(pred, label).sum().float().item()
            return acc

        fp32_baseline = eval_func(pl_model)
        qmodel = InferenceOptimizer.quantize(pl_model,
                                             calib_data=self.train_loader,
                                             eval_func=eval_func,
                                             accuracy_criterion={'relative': 0.8,
                                                                 'higher_is_better': True},
                                             timeout=0,
                                             max_trials=10,
                                             thread_num=8)
        assert qmodel

        with InferenceOptimizer.get_context(qmodel):
            out = qmodel(x)
        assert out.shape == torch.Size([256, 10])
        int8_value = eval_func(qmodel)
        assert (fp32_baseline - int8_value)/fp32_baseline <= 0.8

        # 3. test eval_func with absolute accuracy_criterion
        if compare_version("neural_compressor", operator.ge, "2.0"):
            # for inc 1.x, the value of accuracy_criterion is limited to [0, 1),
            # this ut will always fail for current eval function
            # for inc 2.0, the value of accuracy_criterion is not limited
            # so here just test inc 2.0
            qmodel = InferenceOptimizer.quantize(pl_model,
                                                 calib_data=self.train_loader,
                                                 eval_func=eval_func,
                                                 accuracy_criterion={'absolute': 100,
                                                                     'higher_is_better': True},
                                                 timeout=0,
                                                 max_trials=10,
                                                 thread_num=8)
            assert qmodel
            int8_value = eval_func(qmodel)
            assert int8_value - fp32_baseline <= 100

        # 4. test metric
        if compare_version("torchmetrics", operator.ge, "0.11.0"):
            metric = torchmetrics.F1Score('multiclass', num_classes=10)
        else:
            metric = torchmetrics.F1Score(10)
        qmodel = InferenceOptimizer.quantize(pl_model,
                                             calib_data=self.train_loader,
                                             metric=metric,
                                             accuracy_criterion={'relative': 0.99,
                                                                 'higher_is_better': True},
                                             timeout=0,
                                             max_trials=10,
                                             thread_num=8)
        assert qmodel

    @pytest.mark.skipif(not TORCH_VERSION_LESS_2_0 and not _avx2_checker(), reason="")
    def test_quantize_loading_behavior(self):
        # test nn.Module
        model = resnet18()
        input_sample = torch.randn(1, 3, 224, 224)
        # test pytorch_fx
        qmodel = InferenceOptimizer.quantize(model,
                                             calib_data=input_sample)
        with tempfile.TemporaryDirectory() as tmp_dir_name:
            InferenceOptimizer.save(qmodel, tmp_dir_name)
            load_model = InferenceOptimizer.load(tmp_dir_name, model=model)
        with InferenceOptimizer.get_context(load_model):
            load_model(input_sample)
        
        if compare_version("neural_compressor", operator.ge, "2.0"):
            # save & load of INC ipex quantized model only works when inc version >= 2.0
            # test pytorch_ipex with wrong input
            qmodel = InferenceOptimizer.quantize(model,
                                                calib_data=input_sample,
                                                method='ipex')
            with tempfile.TemporaryDirectory() as tmp_dir_name:
                InferenceOptimizer.save(qmodel, tmp_dir_name)
                with pytest.raises(
                    RuntimeError,
                    match="For INC ipex quantizated model, you need to set input_sample when loading model."
                    ):
                    load_model = InferenceOptimizer.load(tmp_dir_name, model=model)

            # test pytorch_ipex with right input
            qmodel = InferenceOptimizer.quantize(model,
                                                calib_data=input_sample,
                                                method='ipex')
            with tempfile.TemporaryDirectory() as tmp_dir_name:
                InferenceOptimizer.save(qmodel, tmp_dir_name)
                load_model = InferenceOptimizer.load(tmp_dir_name, model=model, input_sample=input_sample)
            with InferenceOptimizer.get_context(load_model):
                load_model(input_sample)
