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


from bigdl.chronos.benchmark import generate_forecaster, generate_data, get_CPU_info, check_nano_env
import time
import numpy as np
import argparse
import os
from scipy import stats
import psutil
import subprocess
from bigdl.nano.utils.log4Error import invalidInputError


def train(args, model_path, forecaster, train_loader, records):
    """
    train stage will record throughput.
    """
    if args.training_processes:
        forecaster.num_processes = args.training_processes
    epochs = args.training_epochs
    forecaster.use_ipex = True if args.ipex else False

    start_time = time.time()
    forecaster.fit(train_loader, epochs=epochs)
    training_time = time.time() - start_time

    if args.framework == "tensorflow":
        training_sample_num = epochs * sum([x.shape[0] for x, _ in train_loader])
    else:
        training_sample_num = epochs * len(train_loader.dataset)
    forecaster.save(model_path)
    records['training_time'] = training_time
    records['training_sample_num'] = training_sample_num
    records['train_throughput'] = training_sample_num / training_time


def throughput(args, model_path, forecaster, train_loader, test_loader, records):
    """
    throughput stage will record inference throughput.
    """

    try:
        # load trained model
        forecaster.load(model_path)
    except:
        # if no ckpt can be used, then train a new one
        forecaster.fit(train_loader, epochs=1)

    # dataset size
    if args.framework == "tensorflow":
        inference_sample_num = sum([x.shape[0] for x, _ in test_loader])
    else:
        inference_sample_num = len(test_loader.dataset)

    if args.quantize:
        import onnxruntime
        sess_options = onnxruntime.SessionOptions()
        if args.cores:
            sess_options.intra_op_num_threads = args.cores
            sess_options.inter_op_num_threads = args.cores
        forecaster.quantize(test_loader, framework=args.quantize_type, sess_options=sess_options)
        print("QUANTIZATION DONE")

    # predict
    if 'torch' in args.inference_framework:
        import torch
        if args.cores:
            torch.set_num_threads(args.cores)
        st = time.time()
        with torch.no_grad():
            yhat = forecaster.predict(test_loader, quantize=args.quantize)
        total_time = time.time()-st
        records['torch_infer_throughput'] = inference_sample_num / total_time

    # predict with onnx
    if 'onnx' in args.inference_framework:
        if args.cores:
            forecaster.build_onnx(thread_num=args.cores)
        st = time.time()
        yhat = forecaster.predict_with_onnx(test_loader, quantize=args.quantize)
        total_time = time.time()-st
        records['onnx_infer_throughput'] = inference_sample_num / total_time

    # predict with openvino
    if 'openvino' in args.inference_framework:
        if args.cores:
            forecaster.build_openvino(thread_num=args.cores)
        st = time.time()
        yhat = forecaster.predict_with_openvino(test_loader, quantize=args.quantize)
        total_time = time.time()-st
        records['openvino_infer_throughput'] = inference_sample_num / total_time


def latency(args, model_path, forecaster, train_loader, test_loader, records):
    """
    latency stage will record inference latency.
    """

    try:
        # load trained model
        forecaster.load(model_path)
    except:
        # if no ckpt can be used, then train a new one
        forecaster.fit(train_loader, epochs=1)

    latency, latency_onnx, latency_vino = [], [], []
    latency_trim_portion = 0.1
    latency_percentile = [50, 90, 95, 99]

    if args.quantize:
        import onnxruntime
        sess_options = onnxruntime.SessionOptions()
        if args.cores:
            sess_options.intra_op_num_threads = args.cores
            sess_options.inter_op_num_threads = args.cores
        forecaster.quantize(test_loader, framework=args.quantize_type, sess_options=sess_options)
        print("QUANTIZATION DONE")

    # predict
    if 'torch' in args.inference_framework:
        import torch
        if args.cores:
            torch.set_num_threads(args.cores)
        with torch.no_grad():
            if args.model == 'autoformer':
                for x, y, x_, y_ in test_loader:
                    st = time.time()
                    yhat = forecaster.predict((x.numpy(), y.numpy(), x_.numpy(), y_.numpy()))
                    latency.append(time.time()-st)
            else:
                for x, y in test_loader:
                    st = time.time()
                    yhat = forecaster.predict(x.numpy(), quantize=args.quantize)
                    latency.append(time.time()-st)
        records['torch_latency'] = stats.trim_mean(latency, latency_trim_portion)
        records['torch_percentile_latency'] = np.percentile(latency, latency_percentile)

    # predict with onnx
    if 'onnx' in args.inference_framework:
        if args.cores:
            forecaster.build_onnx(thread_num=args.cores)
        for x, y in test_loader:
            st = time.time()
            yhat = forecaster.predict_with_onnx(x.numpy(), quantize=args.quantize)
            latency_onnx.append(time.time()-st)
        records['onnx_latency'] = stats.trim_mean(latency_onnx, latency_trim_portion)
        records['onnx_percentile_latency'] = np.percentile(latency_onnx, latency_percentile)

    # predict with openvino
    if 'openvino' in args.inference_framework:
        if args.cores:
            forecaster.build_openvino(thread_num=args.cores)
        for x, y in test_loader:
            st = time.time()
            yhat = forecaster.predict_with_openvino(x.numpy(), quantize=args.quantize)
            latency_vino.append(time.time()-st)
        records['openvino_latency'] = stats.trim_mean(latency_vino, latency_trim_portion)
        records['openvino_percentile_latency'] = np.percentile(latency_vino, latency_percentile)


def result(args, records):
    print(">>>>>>>>>>>>> test-run information >>>>>>>>>>>>>")
    print("Model:", args.model)
    print("Stage:", args.stage)
    print("Dataset:", args.dataset)
    if args.cores:
        print("Cores:", args.cores)
    else:
        print("Cores:", psutil.cpu_count(logical=False) *
              int(subprocess.getoutput('cat /proc/cpuinfo | '
                                       'grep "physical id" | sort -u | wc -l')))
    print("Lookback:", args.lookback)
    print("Horizon:", args.horizon)

    if args.stage == 'train':
        print("\n>>>>>>>>>>>>> train result >>>>>>>>>>>>>")
        print("avg throughput: {}".format(records['train_throughput']))
        print(">>>>>>>>>>>>> train result >>>>>>>>>>>>>")
    elif args.stage == 'latency':
        for framework in args.inference_framework:
            print("\n>>>>>>>>>>>>> {} latency result >>>>>>>>>>>>>".format(framework))
            print("avg latency: {}ms".format(records[framework+'_latency'] * 1000))
            print("p50 latency: {}ms".format(records[framework+'_percentile_latency'][0] * 1000))
            print("p90 latency: {}ms".format(records[framework+'_percentile_latency'][1] * 1000))
            print("p95 latency: {}ms".format(records[framework+'_percentile_latency'][2] * 1000))
            print("p99 latency: {}ms".format(records[framework+'_percentile_latency'][3] * 1000))
            print(">>>>>>>>>>>>> {} latency result >>>>>>>>>>>>>".format(framework))
    else:
        for framework in args.inference_framework:
            print("\n>>>>>>>>>>>>> {} throughput result >>>>>>>>>>>>>".format(framework))
            print("avg throughput: {}".format(records[framework+'_infer_throughput']))
            print(">>>>>>>>>>>>> {} throughput result >>>>>>>>>>>>>".format(framework))


def main():
    # read input arguments
    # currently designed arguments
    parser = argparse.ArgumentParser(description='Benchmarking Parameters')
    parser.add_argument('-m', '--model', type=str, default='tcn', metavar='',
                        help=('model name, choose from tcn/lstm/seq2seq/nbeats/autoformer,'
                              ' default to "tcn".'))
    parser.add_argument('-s', '--stage', type=str, default='train', metavar='',
                        help=('stage name, choose from train/latency/throughput,'
                              ' default to "train".'))
    parser.add_argument('-d', '--dataset', type=str, default="tsinghua_electricity", metavar='',
                        help=('dataset name, choose from nyc_taxi/tsinghua_electricity/'
                              'synthetic_dataset, default to "tsinghua_electricity".'))
    parser.add_argument('-f', '--framework', type=str, default="torch", metavar='',
                        help='framework name, choose from torch/tensorflow, default to "torch".')
    parser.add_argument('-c', '--cores', type=int, default=0, metavar='',
                        help='core number, default to all physical cores.')
    parser.add_argument('-l', '--lookback', type=int, metavar='lookback', required=True,
                        help='required, the history time steps (i.e. lookback).')
    parser.add_argument('-o', '--horizon', type=int, metavar='horizon', required=True,
                        help='required, the output time steps (i.e. horizon).')

    # useful arguments which are not concluded in the currently designed pattern.
    parser.add_argument('--training_processes', type=int, default=1, metavar='',
                        help='number of processes when training, default to 1.')
    parser.add_argument('--training_batchsize', type=int, default=32, metavar='',
                        help='batch size when training, default to 32.')
    parser.add_argument('--training_epochs', type=int, default=1, metavar='',
                        help='number of epochs when training, default to 1.')
    parser.add_argument('--inference_batchsize', type=int, default=1, metavar='',
                        help='batch size when infering, default to 1.')
    parser.add_argument('--quantize', action='store_true',
                        help='if use the quantized model to predict, default to False.')
    parser.add_argument('--inference_framework', nargs='+', default=['torch'], metavar='',
                        help=('predict without/with accelerator, choose from torch/onnx/openvino,'
                        ' default to "torch" (i.e. predict without accelerator).'))
    parser.add_argument('--ipex', action='store_true',
                        help='if use ipex as accelerator for trainer, default to False.')
    parser.add_argument('--quantize_type', type=str, default='pytorch_fx', metavar='',
                        help=('quantize framework, choose from pytorch_fx/pytorch_ipex/'
                              'onnxrt_qlinearops/openvino, default to "pytorch_fx".'))
    parser.add_argument('--ckpt', type=str, default='checkpoints/tcn', metavar='',
                        help=('checkpoint path of a trained model, e.g. "checkpoints/tcn",'
                              ' default to "checkpoints/tcn".'))
    args = parser.parse_args()
    records = vars(args)

    # anomaly detection for input arguments
    models = ['tcn', 'lstm', 'seq2seq', 'nbeats', 'autoformer']
    stages = ['train', 'latency', 'throughput']
    datasets = ['tsinghua_electricity', 'nyc_taxi', 'synthetic_dataset']
    frameworks = ['torch', 'tensorflow']
    quantize_types = ['pytorch_fx', 'pytorch_ipex', 'onnxrt_qlinearops', 'openvino']
    quantize_torch_types = ['pytorch_fx', 'pytorch_ipex']
    invalidInputError(args.model in models,
                      f"-m/--model argument should be one of {models}, but get '{args.model}'")
    invalidInputError(args.stage in stages,
                      f"-s/--stage argument should be one of {stages}, but get '{args.stage}'")
    invalidInputError(args.dataset in datasets,
                      (f"-d/--dataset argument should be one of {datasets},"
                       " but get '{args.dataset}'"))
    invalidInputError(args.framework in frameworks,
                      (f"-f/--framework argument should be one of {frameworks},"
                       " but get '{args.framework}'"))
    invalidInputError(args.quantize_type in quantize_types,
                      (f"--quantize_type argument should be one of {quantize_types},"
                       " but get '{args.quantize_type}'"))
    if args.quantize and 'torch' in args.inference_framework:
        invalidInputError(args.quantize_type in quantize_torch_types,
                          (f"if inference framework is 'torch', then --quantize_type"
                           " argument should be one of {quantize_torch_types},"
                           " but get '{args.quantize_type}'"))

    if 'onnx' in args.inference_framework:
        args.quantize_type = 'onnxrt_qlinearops'
    elif 'openvino' in args.inference_framework:
        args.quantize_type = 'openvino'

    path = os.path.abspath(os.path.dirname(__file__))
    model_path = os.path.join(path, args.ckpt)

    # generate data
    train_loader, test_loader = generate_data(args)

    # initialize forecaster
    forecaster = generate_forecaster(args)

    # running stage
    if args.stage == 'train':
        train(args, model_path, forecaster, train_loader, records)
    elif args.stage == 'latency':
        latency(args, model_path, forecaster, train_loader, test_loader, records)
    elif args.stage == 'throughput':
        throughput(args, model_path, forecaster, train_loader, test_loader, records)

    # print results
    get_CPU_info()
    check_nano_env()
    result(args, records)


if __name__ == "__main__":
    main()
