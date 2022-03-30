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

from gc import callbacks
import os
from shutil import rmtree
import torch
import time 
import pandas as pd
from bigdl.nano.pytorch.vision.datasets import ImageFolder
from bigdl.nano.pytorch.vision.transforms import transforms
import argparse
from torch.utils.data import DataLoader
from torchvision.datasets.utils import download_and_extract_archive
from pytorch_lightning.callbacks import Callback
from bigdl.nano.pytorch.vision.models import resnet50 
from bigdl.nano.pytorch.vision.models import ImageClassifier 
from bigdl.nano.common import init_nano 
from bigdl.nano.pytorch.trainer import Trainer

DATA_URL = "https://storage.googleapis.com/mledu-datasets/cats_and_dogs_filtered.zip"

parser = argparse.ArgumentParser(description='PyTorch Cat Dog')
parser.add_argument('--batch_size', default=32, type=int, help='batch size')
parser.add_argument('--epochs', default=2, type=int, help='epoch number')
parser.add_argument('--root_dir', default=None, help='path to cat vs dog dataset'
                    'which should have two folders `cat` and `dog`, each containing'
                    'cat and dog pictures.')
parser.add_argument('--remove_data', default=True, help='if to remove dataset after performance test. Default is true, i.e. remove after test')
parser.add_argument('--output_to_csv', default=True, help='if output performance test result to csv file')
parser.add_argument('--csv_path', default=None, help='output performance test result to csv file')
train_start, train_end = None, None 

results_dir = os.path.join(os.path.dirname(__file__), "../results")
# for training timing
class MyCallback(Callback):
    def on_train_end(self, trainer, pl_module):
        global train_end 
        train_end = time.time()

    def on_train_start(self, trainer, pl_module):
        global train_start
        train_start = time.time()
    

class Classifier(ImageClassifier):

    def __init__(self):
        backbone = resnet50(pretrained=True, include_top=False, freeze=True)
        super().__init__(backbone=backbone, num_classes=2)

    def configure_optimizers(self):
        return torch.optim.Adam(self.parameters(), lr=0.002, amsgrad=True)


def create_data_loader(root_dir, batch_size):
    dir_path = os.path.realpath(root_dir)
    data_transform = transforms.Compose([
        transforms.Resize(256),
        transforms.ColorJitter(),
        transforms.RandomCrop(224),
        transforms.RandomHorizontalFlip(),
        transforms.Resize(128),
        transforms.ToTensor()
    ])

    catdogs = ImageFolder(dir_path, data_transform)
    dataset_size = len(catdogs)
    train_split = 0.8
    train_size = int(dataset_size * train_split)
    val_size = dataset_size - train_size
    train_set, val_set = torch.utils.data.random_split(catdogs, [train_size, val_size])

    train_loader = DataLoader(train_set, batch_size=batch_size, shuffle=True, num_workers=0)
    val_loader = DataLoader(val_set, batch_size=batch_size, shuffle=False, num_workers=0)
    size = (train_size, val_size)
    return train_loader, val_loader, size


def write_to_csv(result, columns, path):
    if not os.path.exists(results_dir):
        os.mkdir(results_dir)

    df = pd.DataFrame(result, columns=columns)
    df.to_csv(path, index=False, sep=',')
    return 


def main():
    args = parser.parse_args()
    init_nano()

    if args.root_dir is None:
        root_path = "data"
        download_and_extract_archive(url=DATA_URL,download_root="data",remove_finished=True)
    else:
        root_path = args.root_dir
        
    train_loader, val_loader, (train_size, _) = create_data_loader(root_path, args.batch_size)
    classifier = Classifier()
    trainer = Trainer(max_epochs=args.epochs, callbacks=[MyCallback()])

    print("Nano Pytorch cat-vs-dog example")
    print("start performance testing")
    trainer.fit(classifier, train_loader)
    trainer.test(classifier, val_loader)

    throughput = train_size * args.epochs / (train_end - train_start)
    print(f"training using {train_end - train_start}s, thoughput is {throughput} img/s") 

    if args.output_to_csv:
        if args.csv_path is None:
            write_to_csv([throughput], ["throughput"], os.path.join(results_dir, "nano-cat-vs-dog-throughput.csv"))
        else:
            write_to_csv([throughput], ["throughput"], os.path.join(results_dir, args.csv_path))

    if args.remove_data:
        print("Remove cat-vs-dog dataset after test.")
        rmtree(root_path)
        
if __name__ == "__main__":
    main()
