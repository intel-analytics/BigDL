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
# ==============================================================================
# Most of the pytorch code is adapted from:
# https://github.com/IntelAI/models/blob/master/quickstart/image_recognition/
# pytorch/resnet50/inference/cpu
#

import os
import argparse

import torch
import torch.nn as nn
import torch.utils.data
import torchvision.transforms as transforms
import torchvision.datasets as datasets
import torchvision.models as models

from bigdl.dllib.utils.log4Error import *
from bigdl.orca import init_orca_context, stop_orca_context

parser = argparse.ArgumentParser(description='PyTorch ImageNet Inference')
parser.add_argument('data', metavar='DIR',
                    help='path to dataset')
parser.add_argument('--workers_per_node', default=1, type=int,
                    help='number of torch workers on each node (default: 1)')
parser.add_argument('--ipex', action='store_true', default=False,
                    help='use intel pytorch extension')
parser.add_argument('--jit', action='store_true', default=False,
                    help='enable ipex jit fusionpath')
parser.add_argument('--int8', action='store_true', default=False,
                    help='enable ipex int8 path')
parser.add_argument('--bf16', action='store_true', default=False,
                    help='enable ipex bf16 path')
parser.add_argument('-b', '--batch_size', default=256, type=int)
parser.add_argument('--workers', default=4, type=int,
                    help='number of data loading workers (default: 4)')
parser.add_argument("--dummy", action='store_true',
                    help="using dummy data to test the performance of inference")
parser.add_argument('--hub', action='store_true', default=False,
                    help='use model with torch hub')
parser.add_argument('--pretrained', dest='pretrained', action='store_true',
                    help='use pre-trained model')


class DummyData(torch.utils.data.Dataset):

    def __init__(self, use_ipex, use_bf16):
        super(DummyData, self).__init__()
        self.len = 1000
        self.features = torch.randn(self.len, 3, 224, 224)
        self.labels = torch.arange(1, self.len + 1).long()
        if use_ipex:
            self.features = self.features.contiguous(memory_format=torch.channels_last)
        if use_bf16:
            self.features = self.features.to(torch.bfloat16)

    def __len__(self):
        return self.len

    def __getitem__(self, idx):
        return self.features[idx], self.labels[idx]


def main():
    args = parser.parse_args()
    print(args)

    if args.jit and args.int8:
        invalidInputError(False, "jit path is not available for int8 path using ipex")
    if not args.ipex:
        # for offical pytorch, int8 and jit path is not enabled.
        invalidInputError(not args.int8, "int8 path is not enabled for offical pytorch")
        invalidInputError(not args.jit, "jit path is not enabled for offical pytorch")

    init_orca_context(cluster_mode="local")

    validate(args)
    stop_orca_context()


def validate(args):

    def val_loader_func(config, batch_size):
        if not args.dummy:
            valdir = os.path.join(args.data, 'val')
            normalize = transforms.Normalize(mean=[0.485, 0.456, 0.406],
                                             std=[0.229, 0.224, 0.225])
            val_loader = torch.utils.data.DataLoader(
                datasets.ImageFolder(valdir, transforms.Compose([
                    transforms.Resize(256),
                    transforms.CenterCrop(224),
                    transforms.ToTensor(),
                    normalize,
                ])),
                batch_size=batch_size, shuffle=False,
                num_workers=args.workers, pin_memory=True)
        else:
            # dummy data, always running channle last for fp32, bf16, int8
            val_loader = torch.utils.data.DataLoader(
                DummyData(args.ipex, args.bf16),
                batch_size=args.batch_size, shuffle=False,
                num_workers=args.workers, pin_memory=True)
        return val_loader

    def model_creator(config):
        arch = 'resnet50'
        if args.hub:
            torch.set_flush_denormal(True)
            model = torch.hub.load('facebookresearch/WSL-Images', arch)
        else:
            # create model
            if args.pretrained:
                print("=> using pre-trained model '{}'".format(args.arch))
                model = models.__dict__[arch](pretrained=True)
            else:
                print("=> creating model '{}'".format(args.arch))
                model = models.__dict__[arch]()

        if args.ipex:
            print("using ipex model to do inference\n")
            import intel_extension_for_pytorch as ipex

            if args.int8:
                model.eval()
                if not args.calibration:
                    from torch.ao.quantization import MinMaxObserver, PerChannelMinMaxObserver, QConfig
                    x = torch.randn(args.batch_size, 3, 224, 224).contiguous(memory_format=torch.channels_last)
                    qconfig = QConfig(
                        activation=MinMaxObserver.with_args(qscheme=torch.per_tensor_symmetric, dtype=torch.qint8),
                        weight=PerChannelMinMaxObserver.with_args(dtype=torch.qint8,
                                                                  qscheme=torch.per_channel_symmetric))
                    prepared_model = ipex.quantization.prepare(model, qconfig, x, inplace=True)
                    prepared_model.load_qconf_summary(qconf_summary=args.configure_dir)
                    model = ipex.quantization.convert(prepared_model)
                    model = torch.jit.trace(model, x)
                    model = torch.jit.freeze(model.eval())
                    y = model(x)
                    y = model(x)
                    print("running int8 evaluation step\n")

                import torch.fx.experimental.optimization as optimization

                model = optimization.fuse(model, inplace=True)
                conf = ipex.quantization.QuantConf(args.configure_dir)
                x = torch.randn(args.batch_size, 3, 224, 224) \
                    .contiguous(memory_format=torch.channels_last)
                model = ipex.quantization.convert(model, conf, x)
                with torch.no_grad():
                    y = model(x)
                    print(model.graph_for(x))
                print("running int8 evaluation step\n")
            else:
                # for ipex path, always convert model to channels_last for bf16, fp32.
                # TODO: int8 path: https://jira.devtools.intel.com/browse/MFDNN-6103
                model = model.to(memory_format=torch.channels_last)
                model.eval()

                if args.bf32:
                    ipex.set_fp32_math_mode(mode=ipex.FP32MathMode.BF32, device="cpu")
                    print("using bf32 fmath mode\n")

                if args.bf16:
                    model = ipex.optimize(model, dtype=torch.bfloat16, inplace=True)
                    print("running bfloat16 evaluation step\n")
                else:
                    model = ipex.optimize(model, dtype=torch.float32, inplace=True)
                    print("running fp32 evaluation step\n")

                if args.jit:
                    x = torch.randn(args.batch_size, 3, 224, 224) \
                        .contiguous(memory_format=torch.channels_last)
                    if args.bf16:
                        x = x.to(torch.bfloat16)
                        with torch.cpu.amp.autocast(), torch.no_grad():
                            model = torch.jit.trace(model, x).eval()
                    else:
                        with torch.no_grad():
                            model = torch.jit.trace(model, x).eval()
                    model = torch.jit.freeze(model)
        else:
            print("using official pytorch model to do inference\n")

        model.eval()
        return model

    def optimizer_creator(model, config):
        optimizer = torch.optim.SGD(model.parameters(), lr=0.1,
                                    momentum=0.9,
                                    weight_decay=1e-4)
        return optimizer


    from bigdl.orca.learn.pytorch import Estimator
    from bigdl.orca.learn.metrics import Accuracy

    backend = "ray"
    est = Estimator.from_torch(model=model_creator,
                               optimizer=optimizer_creator,
                               loss=nn.CrossEntropyLoss(),
                               metrics=[Accuracy()],
                               backend=backend,
                               workers_per_node=args.workers_per_node)
    if not args.jit and args.bf16:
        with torch.cpu.amp.autocast():
            result = est.evaluate(data=val_loader_func, batch_size=args.batch_size, profile=True)
    else:
        result = est.evaluate(data=val_loader_func, batch_size=args.batch_size, profile=True)
    for r in result:
        print(r, ":", result[r])

    print('---------')
    print('total num_samples:', result['num_samples'])
    print('batch_size:', args.batch_size)
    num_samples = result['num_samples'] / args.workers_per_node
    print('num_samples for each worker:', num_samples)
    print('num_batches for each worker:', num_samples / args.batch_size)
    mean_validation_s = result['profile']['mean_validation_s']
    mean_eval_fwd_s = result['profile']['mean_eval_fwd_s']
    print('ave_val_time for each worker:', mean_validation_s)
    print('ave_val_time for each batch:', mean_eval_fwd_s)
    latency = mean_eval_fwd_s / args.batch_size * 1000
    perf = args.batch_size / mean_eval_fwd_s
    print('inference latency %.3f ms' % latency)
    print("throughput: {:.3f} fps".format(perf))

if __name__ == '__main__':
    main()
