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
import copy
import warnings
from logging import warning
from typing import Any, List, Optional

import pytorch_lightning as pl
import torch
from pytorch_lightning.plugins.environments import LightningEnvironment
from torch import nn
from torch.nn.modules.loss import _Loss
from torchmetrics.metric import Metric

from bigdl.nano.common import check_avx512
from bigdl.nano.pytorch.lightning import LightningModuleFromTorch
from bigdl.nano.pytorch.plugins.ddp_spawn import DDPSpawnPlugin

distributed_backends = ["spawn", "ray"]


class Trainer(pl.Trainer):

    def __init__(self, num_processes: int = 1,
                 use_ipex: bool = False,
                 enable_bf16=False,
                 distributed_backend="spawn",
                 cpu_for_each_process: Optional[List[List[int]]] = None,
                 *args: Any, **kwargs: Any) -> None:
        """
        A pytorch lightning trainer that uses bigdl-nano optimization.
        :param num_processes: number of processes in distributed training. default: 4.
        :param use_ipex: whether we use ipex as accelerator for trainer. default: True.
        :param cpu_for_each_process: A list of length `num_processes`, each containing a list of
            indices of cpus each process will be using. default: None, and the cpu will be
            automatically and evenly distributed among processes.
        """

        # Check keyword arguments
        if "accelerator" in kwargs:
            warning(f"""Accelerator will be specified by bigdl-nano,
            accelerator entered {kwargs['accelerator']} will be ignored. """)

            kwargs.pop('accelerator')
        if "plugins" in kwargs:
            warning(f"""Plugins will be specified by bigdl-nano,
             plugines entered {kwargs['plugins']} will be ignored. """)

            kwargs.pop('plugins')
        if cpu_for_each_process is not None:
            if len(cpu_for_each_process) != num_processes:
                raise ValueError(f"The length of `cpu_for_each_process` ("
                                 f"{len(cpu_for_each_process)}) is not equal to the number of"
                                 f" processes {num_processes}.")

        # Initialize trainer
        if use_ipex and not check_avx512():
            warning("Enable ipex in a cpu instruction set"
                    " without avx512 may cause some random error."
                    "Fall back to cpu device.")
            use_ipex = False

        if num_processes == 1:
            accelerator = None
            if use_ipex:
                from bigdl.nano.pytorch.accelerators.ipex_accelerator import IPEXAccelerator
                accelerator = IPEXAccelerator(enable_bf16=enable_bf16)
            super().__init__(accelerator=accelerator, *args, **kwargs)
        else:
            plugin = None
            assert distributed_backend in distributed_backends, \
                f"Distributed backends supported now are spawn and ray," \
                " but get {distributed_backend}."
            if distributed_backend == "spawn":
                if use_ipex:
                    import intel_pytorch_extension as ipex
                    device = ipex.DEVICE
                else:
                    device = "cpu"
                plugin = DDPSpawnPlugin(parallel_devices=[
                    torch.device(device) for _ in range(num_processes)],
                    cpu_for_each_process=cpu_for_each_process,
                    cluster_environment=LightningEnvironment())
            elif distributed_backend == "ray":
                # Import RayPlugins may entangle with openmp even if it has not been used,
                # which leads to an unacceptably low performance.
                # So we import when we need.
                from bigdl.nano.pytorch.plugins.ray_distributed import RayPlugin
                plugin = RayPlugin(num_workers=num_processes,  # type: ignore
                                   use_ipex=use_ipex)

            accelerator = None
            if use_ipex:
                from bigdl.nano.pytorch.accelerators.ipex_accelerator import IPEXAccelerator
                accelerator = IPEXAccelerator(training_type_plugin=plugin,  # type: ignore
                                              enable_bf16=enable_bf16)

            super().__init__(accelerator=accelerator,
                             plugins=[plugin], *args, **kwargs)

    @staticmethod
    def compile(model: nn.Module,
                loss: _Loss = None,
                optimizer: torch.optim.Optimizer = None,
                metrics: List[Metric] = None,
                onnx: bool = False):
        """
        Construct a pytorch-lightning model. If model is already a pytorch-lightning model,
        return model. If model is pytorch model, construct a new pytorch-lightning module
        with model, loss and optimizer.

        :param model:       A model instance.
        :param loss:        Loss to construct pytorch-lightning model.
                            Should be None if model is instance of pl.LightningModule.
        :param optimizer:   Optimizer to construct pytorch-lightning model Should be None.
                            if model is instance of pl.LightningModule.
        :param metrics:     A list of torchmetrics to validate/test performance.
        :param onnx:        Indicates if onnxruntime support should be binded to the
                            returned model.
        :return:            A LightningModule object.
        """
        assert isinstance(model, nn.Module), \
            "Model must be instance of nn.Module but got {}".format(model.__class__)

        pl_model = None
        if isinstance(model, pl.LightningModule):
            assert not (loss or optimizer), \
                "Loss and optimizer should be None if model is a pytorch-lightning model."
            pl_model = model
        else:
            assert loss and optimizer, \
                "Loss and optimizer are required to construct a LightningModule instance."
            pl_model = LightningModuleFromTorch(model, loss, optimizer, metrics)

        if onnx:
            try:
                from bigdl.nano.pytorch.onnx.onnxrt_inference import bind_onnxrt_methods
                return bind_onnxrt_methods(pl_model)
            except ImportError:
                warnings.warn("You should install onnx and onnxruntime to set `onnx=True`")
                return pl_model
        else:
            return pl_model

    def quantize(self, model, calib_dataloader, val_dataloader=None, metric: str = None,
                 backend='inc', conf=None, framework='pytorch_fx', approach='ptsq',
                 strategy='bayesian', accuracy_criterion=None, timeout=0, max_trials=1):
        """
        Calibrate a Pytorch-Lightning model for post-training quantization.

        :param model:       A Pytorch-Lightning model to be quantized.
        :param calib_dataloader:    Iterable dataloader for calibration.
        :param val_dataloader:      Iterable dataloader for evaluation.
        :param metric:              Eetric for evaluation.
        :param backend:             inc or nncf(nncf is not supported yet). Default: inc
        :param conf:        A path to conf yaml file for quantization.
                            Default: None, use default config.
        :param framework:   Supported values are tensorflow, pytorch, pytorch_fx, pytorch_ipex,
                            onnxrt_integer, onnxrt_qlinear or mxnet; allow new framework backend
                            extension. Default: pytorch_fx. Consistent with Intel Neural Compressor
                            Quantization.

        :param approach:    ptsq, ptdq or qat.
                            ptsq: post_training_static_quant,
                            ptdq: post_training_dynamic_quant,
                            qat: quant_aware_training.
                            Default: post_training_static_quant.
        :param strategy:    bayesian, basic, mse, sigopt. Default: bayesian.
        :param accuracy_criterion:  Tolerable accuracy drop.
                                    accuracy_criterion = {'relative': 0.1, higher_is_better=True}
                                    allows relative accuracy loss: 1%. accuracy_criterion =
                                    {'absolute': 0.99, higher_is_better=Flase} means accuracy < 0.99
                                     must be satisfied.
        :param timeout:     Tuning timeout (seconds). Default: 0,  which means early stop.
                            Combine with max_trials field to decide when to exit.
        :param max_trials:  Max tune times. Default: 1.
                            Combine with timeout field to decide when to exit.
                            "timeout=0, max_trials=1" means it will try quantization only once and
                            return satisfying best model.
        """
        if backend == 'inc':
            from bigdl.nano.quantization import QuantizationINC
            quantizer = QuantizationINC(framework=framework, conf=conf, approach=approach,
                                        strategy=strategy, accuracy_criterion=accuracy_criterion,
                                        timeout=timeout, max_trials=max_trials)
            q_litmodel = copy.deepcopy(model)
            quantizer.model = model

            def eval_func(model_to_eval):
                if val_dataloader:
                    q_litmodel.model = model_to_eval
                    val_outputs = self.validate(q_litmodel, val_dataloader)
                    return val_outputs[0][f'val/{metric}']
                else:
                    return 1             # Fake Evaluation

            quantizer.eval_func = eval_func
            quantizer.calib_dataloader = calib_dataloader
            quantized = quantizer()
            if quantized:
                q_litmodel.model = quantized.model
                return q_litmodel
            return None
        else:
            raise NotImplementedError("Backend {} is not implemented.".format(backend))
