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

from typing import Any, Dict, Optional, Union
import torch
import time
from bigdl.nano.pytorch.utils import LIGHTNING_VERSION_LESS_1_6
from pytorch_lightning.callbacks import Callback
from pytorch_lightning.loops.dataloader.evaluation_loop import EvaluationLoop
from bigdl.nano.utils.log4Error import invalidInputError
from collections import ChainMap
from torchmetrics import Metric


class ResetCallback(Callback):
    def on_train_end(self, trainer, pl_module) -> None:
        # reset current epoch = 0 after each run
        super().on_train_end(trainer, pl_module)
        if LIGHTNING_VERSION_LESS_1_6:
            trainer.fit_loop.current_epoch = 0
        else:
            trainer.fit_loop.epoch_progress.current.processed = 0


class LatencyAggregate(Metric):
    def __init__(self):
        super().__init__(compute_on_step=False)
        self.add_state("times", default=[])

    def update(self, latency):
        self.times.append(torch.Tensor([latency * 1000]).double())

    def compute(self):
        # achieve the core logic of how to average latency
        # todo : is there should any diff in single and multi process?
        self.times.sort()
        count = len(self.times)
        if count >= 3:
            threshold = max(int(0.1 * count), 1)
            infer_times_mid = self.times[threshold:-threshold]
        else:
            infer_times_mid = self.times[:]
        latency = sum(infer_times_mid) / len(infer_times_mid)
        return latency


class LatencyCallback(Callback):
    def __init__(self) -> None:
        super().__init__()
        self.time_avg = LatencyAggregate()

    def on_validation_start(self, trainer, pl_module) -> None:
        super().on_validation_start(trainer, pl_module)
        if not hasattr(pl_module, "time_avg"):
            pl_module.time_avg = self.time_avg

    def on_validation_epoch_end(self, trainer, pl_module) -> None:
        pl_module.log('latency', pl_module.time_avg)

    def on_validation_batch_start(self, trainer, pl_module, batch: Any,
                                  batch_idx: int, dataloader_idx: int) -> None:
        self.batch_latency = time.perf_counter()

    def on_validation_batch_end(self, trainer, pl_module, outputs, batch: Any,
                                batch_idx: int, dataloader_idx: int) -> None:
        batch_latency = time.perf_counter() - self.batch_latency
        pl_module.time_avg(batch_latency)


class CustomEvaluationLoop(EvaluationLoop):
    def __init__(self, verbose: bool = True) -> None:
        super().__init__(verbose=verbose)

    def on_run_end(self):
        self.trainer._logger_connector.epoch_end_reached()

        # hook
        self._evaluation_epoch_end(self._outputs)
        self._outputs = []  # free memory

        # hook
        self._on_evaluation_epoch_end()

        logged_outputs, self._logged_outputs = self._logged_outputs, []  # free memory
        # include any logged outputs on epoch_end
        epoch_end_logged_outputs = self.trainer._logger_connector.update_eval_epoch_metrics()
        all_logged_outputs = dict(ChainMap(*logged_outputs))  # list[dict] -> dict
        all_logged_outputs.update(epoch_end_logged_outputs)
        for dl_outputs in logged_outputs:
            dl_outputs.update(epoch_end_logged_outputs)

        # log metrics
        self.trainer._logger_connector.log_eval_end_metrics(all_logged_outputs)

        # hook
        self._on_evaluation_end()

        if self.verbose and self.trainer.is_global_zero:
            invalidInputError(self.trainer.state.stage is not None, "stage is wrong")
            self._print_results(logged_outputs, self.trainer.state.stage)

        return logged_outputs
