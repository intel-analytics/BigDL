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

from collections import namedtuple
import torch
from torch import nn
import subprocess
from importlib.util import find_spec
import time
import numpy as np
from copy import deepcopy

from bigdl.nano.utils.log4Error import invalidInputError, invalidOperationError
from bigdl.nano.pytorch import Trainer

_whole_acceleration_options = ["inc", "ipex", "onnxruntime", "openvino", "pot",
                               "bf16", "jit", "channels_last"]

CompareMetric = namedtuple("CompareMetric", ["method_name", "latency", "accuracy"])


class AccelerationOption(object):
    def __init__(self, *args, **kwargs):
        '''
        initialize optimization option
        '''
        for option in _whole_acceleration_options:
            setattr(self, option, kwargs.get(option, False))

    def get_precision(self):
        if self.inc:
            return "int8"
        if self.bf16:
            return "bf16"
        return "fp32"

    def get_accelerator(self):
        if self.onnxruntime:
            return "onnxruntime"
        if self.openvino:
            return "openvino"
        if self.jit:
            return "jit"
        return None


# acceleration method combinations, developers may want to register some new
# combinations here
ALL_INFERENCE_ACCELERATION_METHOD = \
    {
        "original": AccelerationOption(),
        "None_fp32_ipex": AccelerationOption(ipex=True),
        "None_bf16": AccelerationOption(bf16=True),
        "None_bf16_ipex": AccelerationOption(bf16=True, ipex=True),
        "None_int8": AccelerationOption(inc=True),
        "jit_fp32": AccelerationOption(jit=True),
        "jit_fp32_ipex": AccelerationOption(jit=True, ipex=True),
        "jit_fp32_ipex_clast": AccelerationOption(jit=True, ipex=True,
                                                  channels_last=True),
        "openvino_fp32": AccelerationOption(openvino=True),
        "openvino_int8": AccelerationOption(openvino=True, inc=True),
        "onnxruntime_fp32": AccelerationOption(onnxtunrime=True),
        "onnxruntime_int8_qlinear": AccelerationOption(onnxruntime=True, inc=True),
        "onnxruntime_int8_integer": AccelerationOption(onnxruntime=True, inc=True),
    }


class Optimizer:

    def __init__(self):
        '''
        initialize an optimizer
        '''
        # optimized_model_dict handles the optimized model and some metadata
        # in {"method_name": {"latency": ..., "accuracy": ..., "model": ...}}
        self.optimized_model_dict = {}

    def optimize(self, model,
                 training_data,
                 validation_data=None,
                 metric=None,
                 cpu_num: int=None,
                 trials: int=100):
        '''
        This function will give all available inference acceleration methods a try
        and record the latency, accuracy and model instance inside the Optimizer for
        future usage.

        :param model: A nn.module to be optimized
        :param training_data: A pytorch dataloader for training dataset.
               Users should be careful with this parameter since this dataloader
               might be exposed to the model, which causing data leak. The
               batch_size of this dataloader is important as well, users may
               want to set it to the same batch size you may want to use the model
               in real deploy environment. E.g. batch size should be set to 1
               if you would like to use the accelerated model in an online service.
        :param validation_data: (optional) A pytorch dataloader for accuracy evaluation
               This is only needed when users care about the possible accuracy drop.
        :param metric: (optional) A callable object takes prediction and target
               and returns a accuracy value in this calling method `metric(pred, target)`
        :param cpu_num: (optional) a int represents how many cores is needed for
               inference.
        :param trials: (optional) a int represents the number of repetitions to calculate
               the average latency. The default value is 100.
        '''
        # check if model is a nn.Module or inherited from a nn.Module
        invalidInputError(isinstance(model, nn.Module), "model should be a nn module.")

        # get the available methods whose dep is met
        available_dict = _available_acceleration_combination()

        default_threads = torch.get_num_threads()
        cpu_num = default_threads if cpu_num is None else int(cpu_num)

        result_map = {}

        for method, available in available_dict.items():
            if available:
                instance = ALL_INFERENCE_ACCELERATION_METHOD[method]
                use_ipex = instance.ipex
                accelerator = instance.get_accelerator()
                precision = instance.get_precision()
                # if precision is fp32, then we will use trace method
                if precision == "fp32":
                    input_sample = tuple(next(iter(training_data))[:-1])
                    try:
                        if accelerator is None and use_ipex is False:
                            accelerated_model = model
                        else:
                            # TODO: remove the logging of tracing
                            if accelerator in ("jit", None):
                                use_channels_last = instance.channels_last
                                accelerated_model = Trainer.trace(model=model,
                                                                  accelerator=accelerator,
                                                                  use_ipex=use_ipex,
                                                                  channels_last=use_channels_last,
                                                                  input_sample=input_sample)
                            else:
                                accelerated_model = Trainer.trace(model=model,
                                                                  accelerator=accelerator,
                                                                  input_sample=input_sample)
                    except Exception as e:
                        print(e)
                        continue

                # if precision is int8 or bf16, then we will use quantize method
                elif precision in ("int8", "bf16"):
                    ort_method = _detect_ort_method(method)
                    try:
                        # TODO: remove the logging of quantization
                        accelerated_model = Trainer.quantize(model=deepcopy(model),
                                                             precision=precision,
                                                             accelerator=accelerator,
                                                             use_ipex=use_ipex,
                                                             calib_dataloader=training_data,
                                                             method=ort_method)
                    except Exception as e:
                        print(e)
                        continue

                result_map[method] = {}

                def func_test(model, input_sample):
                    # with torch.cpu.amp.autocast():
                    model(*input_sample)

                torch.set_num_threads(cpu_num)
                try:
                    result_map[method]["latency"] =\
                        _throughput_calculate_helper(trials, func_test,
                                                     accelerated_model, input_sample)
                except Exception as e:
                    result_map.pop(method)
                    torch.set_num_threads(default_threads)
                    continue

                torch.set_num_threads(default_threads)
                if validation_data is not None and metric is not None:
                    result_map[method]["accuracy"] =\
                        _accuracy_calculate_helper(accelerated_model,
                                                   metric, validation_data)

                result_map[method]["model"] = accelerated_model

            else:
                pass

        self.optimized_model_dict = result_map

    def get_best_model(self,
                       accelerator: str = None,
                       precision: str = None,
                       use_ipex: bool = None,
                       accuracy_criterion: float = None,
                       direction: str = "max"):
        '''
        :param accelerator: (optional) Use accelerator 'None', 'onnxruntime',
               'openvino', 'jit', defaults to None. If not None, then will only find the
               model with this specific accelerator.
        :param precision: (optional) Supported type: 'int8', 'bf16',
               defaults to None which represents 'fp32'. If not None, the will
               only find the model with thie specific precision.
        :param use_ipex: (optional) if not NOne, then will only find the
               model with this specific ipex setting
        :param :param accuracy_criterion: (optional) a float represents tolerable
               accuracy drop percentage, defaults to None meaning no accuracy control.
        :param direction: (optional) A string that indicates the higher/lower
               better for the metric, "min" for the lower the better and "max" for the
               higher the better. Default value is "max".
        :return: best model, corresponding acceleration option
        '''
        invalidOperationError(len(self.optimized_model_dict) > 0,
                              "There is no optimized model. You should call .optimize() \
                              before get_best_model()")
        invalidInputError(accelerator in [None, 'onnxruntime', 'openvino', 'jit'],
                          "Only support accelerator 'onnxruntime', 'openvino' and 'jit'.")
        # TODO: include fp16?
        invalidInputError(precision in [None, 'int8', 'bf16'],
                          "Only support precision 'int8', 'bf16'.")
        invalidInputError(direction in ['min', 'max'],
                          "Only support direction 'min', 'max'.")

        best_model = self.optimized_model_dict["original"]["model"]
        best_metric = CompareMetric("original",
                                    self.optimized_model_dict["original"]["latency"],
                                    self.optimized_model_dict["original"]["accuracy"])

        for method in self.optimized_model_dict.keys():
            if method == "original":
                continue
            option = ALL_INFERENCE_ACCELERATION_METHOD[method]
            result = self.optimized_model_dict[method]
            if accelerator is not None:
                if not getattr(option, accelerator):
                    continue
            if precision is not None:
                if precision == 'bf16' and not option.bf16:
                    continue
                if precision == 'int8' and not option.inc:
                    continue
            if use_ipex:
                if not option.ipex:
                    continue

            if accuracy_criterion is not None:
                accuracy = result["accuracy"]
                compare_acc = best_metric.accuracy
                if direction == "min":
                    if (accuracy - compare_acc) / compare_acc > accuracy_criterion:
                        continue
                else:
                    if (compare_acc - accuracy) / compare_acc > accuracy_criterion:
                        continue

            # After the above conditions are met, the latency comparison is performed
            if result["latency"] < best_metric.latency:
                best_model = result["model"]
                best_metric = CompareMetric(method, result["latency"], result["accuracy"])

        return best_model, _format_acceleration_info(best_metric.method_name)


def _inc_checker():
    '''
    check if intel neural compressor is installed
    '''
    return not find_spec("neural_compressor") is None


def _ipex_checker():
    '''
    check if intel pytorch extension is installed
    '''
    return not find_spec("intel_extension_for_pytorch") is None


def _onnxruntime_checker():
    '''
    check if onnxruntime and onnx is installed
    '''
    onnxruntime_installed = not find_spec("onnxruntime") is None
    onnx_installed = not find_spec("onnx") is None
    return onnxruntime_installed and onnx_installed


def _openvino_checker():
    '''
    check if openvino-dev is installed
    '''
    return not find_spec("openvino") is None


def _bf16_checker():
    '''
    bf16 availablity will be decided dynamically during the optimization
    '''
    msg = subprocess.check_output(["lscpu"]).decode("utf-8")
    return "avx512_bf16" in msg or "amx_bf16" in msg


def _detect_ort_method(method_name):
    method_name = method_name.split("_")[-1]
    if method_name in ["qlinear", "integer"]:
        return method_name
    return None


def _available_acceleration_combination():
    '''
    :return: a dictionary states the availablity (if meet depdencies)
    '''
    dependency_checker = {"inc": _inc_checker,
                          "ipex": _ipex_checker,
                          "onnxruntime": _onnxruntime_checker,
                          "openvino": _openvino_checker,
                          "pot": _openvino_checker,
                          "bf16": _bf16_checker}
    available_dict = {}
    for method, option in ALL_INFERENCE_ACCELERATION_METHOD.items():
        available_iter = True
        for name, value in option.__dict__.items():
            if value:
                if name in dependency_checker and not dependency_checker[name]():
                    available_iter = False
        available_dict[method] = available_iter
    return available_dict


def _throughput_calculate_helper(iterrun, func, *args):
    '''
    A simple helper to calculate median latency
    '''
    time_list = []
    for _ in range(iterrun):
        st = time.time()
        func(*args)
        time_list.append(time.time() - st)
    time_list.sort()
    # remove top and least 10% data.
    time_list = time_list[int(0.1 * iterrun): int(0.9 * iterrun)]
    return np.median(time_list) * 1000


def _accuracy_calculate_helper(model, metric, data):
    '''
    A quick helper to calculate accuracy
    '''
    metric_list = []
    # TODO: data should have same batchsize
    for i, (data_input, target) in enumerate(data):
        metric_list.append(metric(model(data_input), target).numpy())
    return np.mean(metric_list)


def _format_acceleration_info(method_name):
    '''
    Get a string represation for current method's acceleration option
    '''
    option = ALL_INFERENCE_ACCELERATION_METHOD[method_name]
    repr_str = ""
    for key, value in option.__dict__.items():
        if value:
            repr_str = repr_str + key + " + "
    if len(repr_str) > 0:
        repr_str = repr_str[:-2]
    return repr_str
