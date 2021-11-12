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

# Copyright 2017 The Ray Authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#  http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# This file is adapted from
# https://github.com/ray-project/ray/blob/master/python/ray/util/sgd/torch/torch_runner.py

from filelock import FileLock
import logging
import io
import itertools
import os
import tempfile
import torch
import torch.nn as nn
from torch.utils.data import DataLoader
from torch.utils.data.distributed import DistributedSampler
import torch.distributed as dist
from bigdl.orca import OrcaContext
from bigdl.orca.learn.pytorch.constants import SCHEDULER_STEP, NUM_STEPS
from bigdl.orca.learn.pytorch.training_operator import TrainingOperator
from bigdl.orca.learn.pytorch import utils

from pyspark import BarrierTaskContext
from pyspark.context import SparkContext
from contextlib import closing
import socket

logger = logging.getLogger(__name__)

try:
    from collections.abc import Iterable
except ImportError:
    from collections import Iterable


def find_free_port(tc):
    address = tc.getTaskInfos()[tc.partitionId()].address.split(":")[0]
    with closing(socket.socket(socket.AF_INET, socket.SOCK_STREAM)) as s:
        s.bind(("", 0))
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        tc.barrier()
        return f"{address}:{s.getsockname()[1]}"


def find_ip_and_port(pre_iter):
    tc = BarrierTaskContext().get()
    free_port = find_free_port(tc)
    return [free_port]


class TorchPysparkRunner:
    """Manages a PyTorch model for training."""

    def __init__(self,
                 model_creator,
                 optimizer_creator,
                 size,
                 cluster_info,
                 cores_per_worker,
                 loss_creator=None,
                 metrics=None,
                 scheduler_creator=None,
                 training_operator_cls=None,
                 config=None,
                 use_tqdm=False,
                 scheduler_step_freq=None,
                 state_dict=None,
                 backend="torch-distributed",
                 mode="fit"):
        self.model_creator = model_creator
        self.optimizer_creator = optimizer_creator
        self.loss_creator = loss_creator
        self.scheduler_creator = scheduler_creator
        self.training_operator_cls = training_operator_cls or TrainingOperator
        self.config = {} if config is None else config

        self.timers = utils.TimerCollection()
        self.epochs = 0
        self.models = None
        self.optimizers = None
        self.metrics = metrics
        self.criterion = None
        self.schedulers = None
        self.train_loader = None
        self.validation_loader = None
        self.training_operator = None
        self.use_tqdm = use_tqdm
        self.scheduler_step_freq = scheduler_step_freq

        self.state_dict = state_dict
        self.size = size
        self.mode = mode
        self.backend = backend
        self.cluster_info = cluster_info

        self.setup(cores_per_worker)
        if self.backend == "torch-distributed":
            self.setup_distributed(self.mode, self.cluster_info)

    def _create_loss(self):
        if not self.loss_creator:
            return
        logger.debug("Creating loss.")
        if isinstance(self.loss_creator, torch.nn.modules.loss._Loss):
            self.criterion = self.loss_creator
        else:  # Torch loss is also callable.
            import types
            assert isinstance(self.loss_creator, types.FunctionType), \
                "Must provide a torch loss instance or a loss_creator function"
            self.criterion = self.loss_creator(self.config)

    def _create_schedulers_if_available(self):
        # Learning rate schedules are optional.
        if not self.scheduler_creator:
            return
        self.schedulers = self.scheduler_creator(self.given_optimizers,
                                                 self.config)

        if not isinstance(self.schedulers, Iterable):
            self.schedulers = [self.schedulers]

    def setup(self, cores_per_node):
        import torch
        torch.set_num_threads(cores_per_node)

    def setup_distributed(self, mode, cluster_info):

        if mode == "fit":
            self.rank = self._get_rank(cluster_info)
            print("cluster is: ", cluster_info)

            address = f"tcp://{cluster_info[0]}"
        else:
            self.rank = 0
            address = None
        self._setup_torch_distribute(url=address,
                                     world_rank=self.rank,
                                     world_size=self.size,
                                     mode=mode)

    def _setup_torch_distribute(self, url, world_rank, world_size, mode):
        from torch.nn.parallel import DistributedDataParallel
        if mode == "fit":
            dist.init_process_group(
                backend="gloo",
                init_method=url,
                rank=world_rank,
                world_size=world_size)
        self.backend = "torch-distributed"
        self.rank = world_rank
        self.size = world_size
        self.setup_components()
        if mode == "fit":
            training_models = [
                DistributedDataParallel(model)
                for model in self.models
            ]
        else:
            training_models = self.models
        self.setup_operator(training_models)

    def _get_rank(self, cluster_info):
        # As task placement may not be identical between two different jobs,
        # we cannot simply index cluster_info using partitionId to get current
        # ip and port.
        # The approach here is to first get all tasks' ip in this job and compute
        # a local rank by counting how many tasks has the same ip but with lower id.
        # We then use the local rank to find the right slot in cluster_info to find
        # the right global_rank.
        tc = BarrierTaskContext().get()
        infos = tc.getTaskInfos()
        idx = tc.partitionId()
        local_ip = infos[idx].address.split(":")[0]
        local_rank = 0
        for i in range(0, idx):
            if infos[i].address.startswith(local_ip):
                local_rank += 1
        global_rank = -1
        local_count = 0
        for node in cluster_info:
            if node.startswith(local_ip):
                local_count += 1
            global_rank += 1
            if local_count == local_rank + 1:
                break
        return global_rank

    def setup_components(self):
        """Runs the creator functions without any distributed coordination."""

        logger.debug("Creating model")
        self.models = self.model_creator(self.config)
        if isinstance(self.models, nn.Sequential) or not isinstance(self.models, Iterable):
            self.models = [self.models]
        assert all(isinstance(model, nn.Module) for model in self.models), (
            "All models must be PyTorch models: {}.".format(self.models))

        logger.debug("Creating optimizer.")
        self.optimizers = self.optimizer_creator(self.given_models,
                                                 self.config)
        if not isinstance(self.optimizers, Iterable):
            self.optimizers = [self.optimizers]

        self._create_schedulers_if_available()
        self._create_loss()

    def setup_components_horovod(self):
        import horovod.torch as hvd

        logger.debug("Creating model")
        self.models = self.model_creator(self.config)
        if not isinstance(self.models, Iterable):
            self.models = [self.models]
        else:
            raise ValueError("only support single model for now")

        assert all(isinstance(model, nn.Module) for model in self.models), (
            "All models must be PyTorch models: {}.".format(self.models))

        logger.debug("Creating optimizer.")
        self.optimizers = self.optimizer_creator(self.given_models,
                                                 self.config)
        if not isinstance(self.optimizers, Iterable):
            hvd.broadcast_parameters(self.models[0].state_dict(), root_rank=0)
            hvd.broadcast_optimizer_state(self.optimizers, root_rank=0)
            parameters = self.models[0].named_parameters()
            self.optimizers = hvd.DistributedOptimizer(self.optimizers,
                                                       named_parameters=parameters)
            self.optimizers = [self.optimizers]
        else:
            raise ValueError("only support one optimizer for now")

        self._create_schedulers_if_available()
        self._create_loss()

    def setup_operator(self, training_models):
        """Create the training operator."""
        self.training_operator =\
            self.training_operator_cls(
                self.config,
                models=training_models,
                optimizers=self.optimizers,
                criterion=self.criterion,
                world_rank=self.rank,
                schedulers=self.schedulers,
                use_tqdm=self.use_tqdm)

    def with_sampler(self, loader):
        logger.debug("Wrapping DistributedSampler on DataLoader")
        data_loader_args = {
            "dataset": loader.dataset,
            "batch_size": loader.batch_size,
            "shuffle": False,
            "num_workers": loader.num_workers,
            "collate_fn": loader.collate_fn,
            "pin_memory": loader.pin_memory,
            "drop_last": loader.drop_last,
            "timeout": loader.timeout,
            "worker_init_fn": loader.worker_init_fn,
            "sampler": DistributedSampler(loader.dataset,
                                          num_replicas=self.size,
                                          rank=self.rank)
        }
        return DataLoader(**data_loader_args)

    @staticmethod
    def should_wrap_dataloader(loader):
        from torch.utils.data import DataLoader
        try:
            from torch.utils.data import IterableDataset
            not_iterable = not isinstance(loader.dataset, IterableDataset)
        except Exception as e:
            not_iterable = TorchPysparkRunner
        return (isinstance(loader, DataLoader)
                and not_iterable)

    def train_epochs(self, data_creator, epochs=1, batch_size=32, profile=False,
                     info=None, wrap_dataloader=None):
        config = self.config.copy()
        if OrcaContext.serialize_data_creator:
            with FileLock(
                    os.path.join(tempfile.gettempdir(), ".orcadata.lock")):
                loader = data_creator(config, batch_size)
        else:
            loader = data_creator(config, batch_size)

        if wrap_dataloader is None:
            if TorchPysparkRunner.should_wrap_dataloader(loader):
                loader = self.with_sampler(loader)
        elif wrap_dataloader is True:
            loader = self.with_sampler(loader)
        stats_list = list()
        for i in range(epochs):
            stats = self.train_epoch(loader, profile=profile, info=info)
            stats_list.append(stats)

        state_dict = self.get_state_dict()

        if self.rank == 0:
            return [(state_dict, stats_list)]
        else:
            return []

    def train_epoch(self,
                    data_loader,
                    profile=False,
                    info=None):
        """Runs a training epoch and updates the model parameters."""
        if hasattr(self.train_loader, "sampler") and hasattr(
                self.train_loader.sampler, "set_epoch"):
            self.train_loader.sampler.set_epoch(self.epochs)
        logger.debug("Begin Training Step {}".format(self.epochs + 1))
        info = info or {}
        self._toggle_profiling(profile=profile)

        info.update({
            SCHEDULER_STEP: self.scheduler_step_freq
        })
        with self.timers.record("train_epoch"):
            data_loader = iter(data_loader)
            train_stats = self.training_operator.train_epoch(data_loader, info)

        self.epochs += 1
        # This is so that `epochs` is first in ordering.
        stats = dict(epoch=self.epochs, **train_stats)
        if profile:
            stats.update(profile=self.timers.stats())
        return stats

    def validate(self, data_creator, batch_size=32, num_steps=None, profile=False,
                 info=None, wrap_dataloader=None):
        """Evaluates the model on the validation data set."""
        config = self.config.copy()
        info = info or {}
        self._toggle_profiling(profile=profile)

        if OrcaContext.serialize_data_creator:
            with FileLock(
                    os.path.join(tempfile.gettempdir(), ".orcadata.lock")):
                loader = data_creator(config, batch_size)
        else:
            loader = data_creator(config, batch_size)

        if wrap_dataloader is None:
            if TorchPysparkRunner.should_wrap_dataloader(loader):
                loader = self.with_sampler(loader)
        elif wrap_dataloader is True:
            loader = self.with_sampler(loader)
        loader = iter(loader)
        if num_steps:
            loader = itertools.islice(loader, num_steps)

        self.load_state_dict(self.state_dict)

        with self.timers.record("validation"):
            validation_stats = self.training_operator.validate(loader,
                                                               info=info,
                                                               metrics=self.metrics)
        if profile:
            validation_stats.update(profile=self.timers.stats())

        if self.rank == 0:
            return [validation_stats]
        else:
            return []

    def predict(self, data_creator, batch_size=32, profile=False):
        """Evaluates the model on the validation data set."""
        config = self.config.copy()
        self._toggle_profiling(profile=profile)

        partition = data_creator(config, batch_size)
        params = {"batch_size": batch_size, "shuffle": False}
        for arg in ["shuffle", "sampler", "batch_sampler", "num_workers", "collate_fn",
                    "pin_memory", "drop_last", "timeout", "worker_init_fn",
                    "multiprocessing_context"]:
            if arg in config:
                params[arg] = config[arg]

        self.load_state_dict(self.state_dict)

        def predict_fn(shard):
            if isinstance(shard["x"], tuple) or isinstance(shard["x"], list):
                tensors = [torch.from_numpy(arr) for arr in shard["x"]]
            else:
                tensors = [torch.from_numpy(shard["x"])]
            dataset = torch.utils.data.TensorDataset(*tensors)
            data_loader = DataLoader(dataset, **params)
            y = self.training_operator.predict(iter(data_loader))

            return {"prediction": y}

        with self.timers.record("predict"):
            new_part = [predict_fn(shard) for shard in partition]
        return new_part

    def _toggle_profiling(self, profile=False):
        """Enables/Disables and resets timing profiles."""
        if profile:
            self.timers.enable()
            self.timers.reset()
        else:
            self.timers.disable()
        self.training_operator._set_timers(self.timers)

    def get_state_dict(self):
        """Returns the state of the runner."""
        state = {
            "epoch": self.epochs,
            "operator": self.training_operator.state_dict(),
            "models": [model.state_dict() for model in self.models],
            "optimizers": [opt.state_dict() for opt in self.optimizers]
        }
        if self.schedulers:
            state.update({
                "schedulers": [
                    scheduler.state_dict() for scheduler in self.schedulers
                ]
            })
        return state

    def load_state_dict(self, state):
        """Sets the state of the model."""
        for model, state_dict in zip(self.models, state["models"]):
            model.load_state_dict(state_dict)
        for optimizer, state_dict in zip(self.optimizers, state["optimizers"]):
            optimizer.load_state_dict(state_dict)
        if self.schedulers:
            for scheduler, state_dict in zip(self.schedulers,
                                             state["schedulers"]):
                scheduler.load_state_dict(state_dict)

        self.epochs = state["epoch"]
        self.training_operator.load_state_dict(state["operator"])

    def state_stream(self):
        """Returns a bytes object for the state dict."""
        state_dict = self.get_state_dict()
        _buffer = io.BytesIO()
        torch.save(state_dict, _buffer)
        return _buffer.getvalue()

    def load_state_stream(self, byte_obj):
        """Loads a bytes object the training state dict."""
        _buffer = io.BytesIO(byte_obj)
        state_dict = torch.load(_buffer)
        return self.load_state_dict(state_dict)

    def apply(self, fn):
        return fn()

    def apply_operator(self, fn):
        return fn(self.training_operator)

    def shutdown(self):
        """Attempts to shut down the worker."""
        dist.destroy_process_group()
        del self.training_operator
        del self.validation_loader
        del self.train_loader
        del self.criterion
        del self.optimizers
        del self.models

    @property
    def given_models(self):
        if len(self.models) > 1:
            return self.models
        else:
            return self.models[0]

    @property
    def given_optimizers(self):
        if len(self.optimizers) > 1:
            return self.optimizers
        else:
            return self.optimizers[0]

    @property
    def given_schedulers(self):
        if not self.schedulers:
            return self.schedulers
        if len(self.schedulers) > 1:
            return self.schedulers
        else:
            return self.schedulers[0]
