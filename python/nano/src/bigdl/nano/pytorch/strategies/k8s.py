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

from pytorch_lightning.strategies import DDPStrategy
from typing import Any, List, Optional, Union, Dict
import torch
import os
import warnings
from torch import nn
from torch.nn.parallel.distributed import DistributedDataParallel
from torch.optim.lr_scheduler import _LRScheduler

import pytorch_lightning as pl
from pytorch_lightning.trainer.states import TrainerFn
from pytorch_lightning.core.optimizer import LightningOptimizer
from pytorch_lightning.core.optimizer import _configure_schedulers_automatic_opt
from pytorch_lightning.core.optimizer import _configure_schedulers_manual_opt
from pytorch_lightning.core.optimizer import _set_scheduler_opt_idx, _validate_scheduler_api
from pytorch_lightning.plugins.environments import LightningEnvironment
from bigdl.nano.pytorch.utils import TORCH_VERSION_LESS_1_10
from bigdl.nano.utils.log4Error import invalidInputError
from bigdl.nano.pytorch.strategies.ipex.ipex_api import ipex_optimize


class DDPK8sStrategy(DDPStrategy):
    """Extending DDPStrategy to support IPEX and auto_lr on k8s."""

    strategy_name = "ddp_k8s"

    def __init__(
        self,
        num_processes: Optional[int] = None,
        cpu_for_each_process: Optional[List[List[int]]] = None,
        use_ipex=False,
        dtype=None,
        auto_lr=False,
        **kwargs: Any
    ):
        
        if num_processes is None:
            num_processes == os.environ["WORLD_SIZE"]
        """Create a DDPK8sStrategy."""
        invalidInputError(not (use_ipex and TORCH_VERSION_LESS_1_10),
                          "currently ipex with torch version under 1.10 is not supported.")

        device = 'cpu'
        parallel_devices = [torch.device(device) for _ in range(num_processes)]
        cluster_environment = LightningEnvironment()
        if use_ipex and dtype == torch.bfloat16 and 'precision_plugin' not in kwargs:
            from bigdl.nano.pytorch.strategies.ipex.ipex_strategy import IPEXBF16Precision
            super().__init__(parallel_devices=parallel_devices,
                             cluster_environment=cluster_environment,
                             precision_plugin=IPEXBF16Precision(), **kwargs)
        else:
            super().__init__(parallel_devices=parallel_devices,
                             cluster_environment=cluster_environment, **kwargs)
        self.cpu_for_each_process = cpu_for_each_process
        self.use_ipex = use_ipex
        self.dtype = dtype
        self.auto_lr = auto_lr

    # todo: refactor the following code into a mixedin class

    def setup(self, trainer: "pl.Trainer") -> None:
        """Setup the distributed environment of sub processes, we add ipex optimization here."""
        invalidInputError(self.model is not None, "You must specify the model.")

        super().setup(trainer)
        # `configure_ddp` will create a `DistributedDataParallel`, which has no
        # `test_step` method in pytorch_lightning 1.6, which causes error when
        # calling `trainer.test()`, so we call `configure_ddp` only when fitting
        trainer_fn = trainer.state.fn
        if trainer_fn != TrainerFn.FITTING:
            self.model.eval()

        if trainer.training and self.auto_lr:

            def _unpack_lightning_optimizer(opt):
                return opt._optimizer if isinstance(opt, LightningOptimizer) else opt

            optimizers = self.optimizers
            optimizers = [_unpack_lightning_optimizer(opt) for opt in optimizers]

            for optimizer in optimizers:
                for param_group in optimizer.param_groups:
                    param_group["lr"] *= self.world_size

            lr_scheduler_configs = self.lr_scheduler_configs
            for config in lr_scheduler_configs:
                scheduler = config.scheduler
                if isinstance(scheduler, _LRScheduler):
                    scheduler.base_lrs = [  # type: ignore
                        lr * self.world_size for lr in scheduler.base_lrs  # type: ignore
                    ]

        if self.use_ipex and not TORCH_VERSION_LESS_1_10:
            num_optimizers = len(self.optimizers)

            if num_optimizers == 1:
                optimizer = self.optimizers[0]
                ipex_optimize(self.model, optimizer=optimizer, inplace=True, dtype=self.dtype)
            elif num_optimizers == 0:
                ipex_optimize(self.model, inplace=True, dtype=self.dtype)
            else:
                warnings.warn(f"IPEX currently only support single optimizers, "
                              f"but got {num_optimizers}. Skip IPEX")

    def on_train_start(self):
        """Setup warmup lr_schedulers after resetting the train dataloaders."""
        # LightnigModule.train_dataloader() generate the training dataloaders after setup,
        # so attach the warmup lr_schedulers in on_train_start hook to infer warmup_steps.
        if not self.auto_lr:
            return
        if self.lr_scheduler_configs:
            warnings.warn(f"Nano warmup currently only support no scheduler, "
                          f"but got {len(self.lr_scheduler_configs)}. Skip warmup")
        else:
            trainer = self.lightning_module.trainer
            lr_schedulers = []
            warmup_params = {
                'start_factor': 1.0 / self.world_size,
                'end_factor': 1.0,
                'warmup_epochs': trainer.max_epochs // 10,
                'interval': 'epoch'
            }
            supported_keys = {'warmup_epochs'}
            if isinstance(self.auto_lr, dict):
                extra_keys = self.auto_lr.keys() - supported_keys
                if extra_keys:
                    warnings.warn(f"Found unsupported keys in the auto_lr dict: {extra_keys}")
                if 'warmup_epochs' not in self.auto_lr:
                    self.auto_lr = True
                    warnings.warn("Not found \"warmup_epochs\" in the auto_lr dict"
                                  " warmup_epochs is set by default")
                else:
                    invalidInputError(type(self.auto_lr['warmup_epochs']) is int,
                                      f"\"warmup_epochs\" is {type(self.auto_lr['warmup_epochs'])}",
                                      "expect \"warmup_epochs\" is a integer")
                    warmup_params['warmup_epochs'] = self.auto_lr['warmup_epochs']
            if type(self.auto_lr) is bool:
                # Call scheduler.step() after each minibatch rather than epoch if max_epochs < 10
                if warmup_params['warmup_epochs'] == 0:
                    train_loader = trainer.train_dataloader
                    max_steps = len(train_loader) * trainer.max_epochs
                    warmup_params['warmup_epochs'] = max_steps // 10
                    warmup_params['interval'] = 'step'
            for opt_idx, opt in enumerate(self.optimizers):
                from torch.optim.lr_scheduler import LambdaLR

                def lr_func(epoch):
                    current_epoch = trainer.current_epoch
                    start_factor = warmup_params['start_factor']
                    end_factor = warmup_params['end_factor']
                    total_iters = warmup_params['warmup_epochs']
                    if current_epoch > 0 and warmup_params['interval'] == 'step' \
                            or epoch > total_iters:
                        return 1.0
                    if epoch == 0:
                        return start_factor
                    return (end_factor - start_factor) * epoch / total_iters \
                        + start_factor
                scheduler = LambdaLR(optimizer=opt,
                                     lr_lambda=[lr_func] * len(opt.param_groups))
                lr_scheduler = {
                    'scheduler': scheduler,
                    'opt_idx': opt_idx,
                    'interval': warmup_params['interval']
                }
                lr_schedulers.append(lr_scheduler)

            # validates the lr_scheduler_configs, adapted from lightning
            # https://github.com/Lightning-AI/lightning/blob/1.6.4/pytorch_lightning/core/optimizer.py#L175
            lr_scheduler_configs = (
                _configure_schedulers_automatic_opt(lr_schedulers, None)
                if self.lightning_module.automatic_optimization
                else _configure_schedulers_manual_opt(lr_schedulers)
            )
            _set_scheduler_opt_idx(self.optimizers, lr_scheduler_configs)
            _validate_scheduler_api(lr_scheduler_configs, self.lightning_module)
            self.lr_scheduler_configs = lr_scheduler_configs

    def _setup_model(self, model: nn.Module) -> DistributedDataParallel:
        """Wraps the model into a 'DistributedDataParallel' module."""
        # we should override this method to change the creation of `DistributedDataParallel`
        return DistributedDataParallel(model, **self._ddp_kwargs)
