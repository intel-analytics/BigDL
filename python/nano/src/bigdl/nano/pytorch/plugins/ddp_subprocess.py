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

# This file is adapted from https://github.com/PyTorchLightning
# /pytorch-lightning/blob/master/pytorch_lightning/plugins/training_type/ddp_spawn.py
#
# Copyright The PyTorch Lightning team.
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

import cloudpickle
import multiprocessing
import os
import subprocess
import sys
import copy
from typing import Any, Optional, Callable
from tempfile import TemporaryDirectory

import pytorch_lightning as pl
from pytorch_lightning.strategies.launchers import _Launcher
from pytorch_lightning.strategies import Strategy

from bigdl.nano.pytorch.plugins.ddp_spawn import DDPSpawnStrategy
from bigdl.nano.common.cpu_schedule import schedule_workers

import logging

log = logging.getLogger(__name__)

class _DDPSubprocessLauncher(_Launcher):
    def __init__(self, strategy: Strategy) -> None:
        self._strategy = strategy

    @property
    def is_interactive_compatible(self) -> bool:
        """Returns whether this launcher can work in interactive environments such as Jupyter notebooks."""
        return False

    def launch(self, function: Callable, *args: Any, trainer: Optional["pl.Trainer"] = None, **kwargs: Any) -> Any:
        """Creates new processes, then calls the given function.

        Arguments:
            function: A callback function to execute after all processes have been created.
                It is up to the implementation of this function to synchronize the processes, e.g., with barriers.
            *args: Optional positional arguments to be passed to the given function.
            trainer: Optional reference to the :class:`~pytorch_lightning.trainer.trainer.Trainer`.
            **kwargs: Optional keyword arguments to be passed to the given function.
        """
        os.environ["MASTER_PORT"] = str(self._strategy.cluster_environment.main_port)

        if self._strategy.cpu_for_each_process is None:
            cpu_procs = schedule_workers(self._strategy.num_processes)
        else:
            cpu_procs = self._strategy.cpu_for_each_process

        with TemporaryDirectory() as temp_dir:
            with open(os.path.join(temp_dir, "strategy.pkl"), "wb") as f:
                cloudpickle.dump(self._strategy, f)
            with open(os.path.join(temp_dir, "args.pkl"), "wb") as f:
                cloudpickle.dump((args, kwargs), f)
            with open(os.path.join(temp_dir, "function.pkl"), 'wb') as f:
                cloudpickle.dump(function, f)

            processes = []
            cwd_path = os.path.split(os.path.realpath(__file__))[0]
            for i in range(self._strategy.num_processes):
                env = copy.deepcopy(os.environ)

                env.update({
                    "KMP_AFFINITY": f"granularity=fine,proclist"
                                    f"=[{','.join([str(i) for i in cpu_procs[i]])}],explicit",
                    "OMP_NUM_THREADS": str(len(cpu_procs[i])),
                    "PROCESS_IDX": str(i),
                })
                log.debug(f"[Process {i}]: using KMP_AFFINITY: {env['KMP_AFFINITY']}")
                log.debug(f"[Process {i}]: using OMP_NUM_THREADS: {env['OMP_NUM_THREADS']}")

                processes.append(subprocess.Popen([sys.executable, f"{cwd_path}/worker.py",
                                                temp_dir], env=env))

            for _, process in enumerate(processes):
                process.wait()

            for _, process in enumerate(processes):
                assert process.returncode == 0, "Subprocess incorrectly exit, \
                                                check the trainer configure or usage"


class DDPSubprocessStrategy(DDPSpawnStrategy):

    strategy_name = "ddp_subprocess"

    def _configure_launcher(self):
        self._launcher = _DDPSubprocessLauncher(self)
