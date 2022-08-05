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

import types
import torch
import pytorch_lightning as pl
from pytorch_lightning.strategies import SingleDeviceStrategy
from pytorch_lightning.accelerators.accelerator import Accelerator
from pytorch_lightning.plugins.precision import PrecisionPlugin
from bigdl.nano.utils.log4Error import invalidInputError
import intel_extension_for_pytorch as ipex

from .ipex_accelerator import IPEXAccelerator


class IPEXStrategy(SingleDeviceStrategy):
    """IPEX strategy."""

    strategy_name = "ipex"

    def __init__(
        self,
        accelerator: Accelerator = IPEXAccelerator(),
        precision_plugin: PrecisionPlugin = PrecisionPlugin(),
        enable_bf16=False,
    ) -> None:
        """
        Create a IPEXStrategy.

        :param accelerator: the accelerator to handle hardware
        :param precision_plugin: the plugin to handle precision-specific parts
        """
        self.enable_bf16 = enable_bf16

        super().__init__(accelerator=accelerator, precision_plugin=precision_plugin)

    def setup(self, trainer: pl.Trainer) -> None:
        """
        Setup plugins for the trainer fit and creates optimizers.

        We add IPEX's optimizations here.

        :param trainer: the trainer instance
        """
        super().setup(trainer)

        dtype = torch.bfloat16 if self.enable_bf16 else None
        if len(self.optimizers) == 0:
            ipex.optimize(self.model, inplace=True, dtype=dtype)
        elif len(self.optimizers) == 1:
            ipex.optimize(self.model, optimizer=self.optimizers[0], inplace=True, dtype=dtype)
        else:
            invalidInputError(False, "Ipex does not support more than one optimizers.")

        if self.enable_bf16:
            self.patch_on_before_batch_transfer_for_bf16_training(trainer)

    def patch_on_before_batch_transfer_for_bf16_training(self, trainer: pl.Trainer):
        def on_before_batch_transfer(self, batch, dataloader_idx):
            def convert_data_to_bf16(batch):
                if isinstance(batch, torch.Tensor) and batch.dtype == torch.float32:
                    batch = batch.bfloat16()
                elif isinstance(batch, list) or isinstance(batch, tuple):
                    batch = list(batch)
                    for index, t in enumerate(batch):
                        batch[index] = convert_data_to_bf16(t)
                return batch
            batch = convert_data_to_bf16(batch)
            return batch

        model = self.model or trainer.lightning_module
        setattr(model, '_original_on_before_batch_transfer', model.on_before_batch_transfer)
        setattr(model, 'on_before_batch_transfer',
                types.MethodType(on_before_batch_transfer, model))
