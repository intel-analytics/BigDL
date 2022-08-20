import os
from tkinter import N
from torch import nn
import torch
from unittest import TestCase
from copy import deepcopy
import pytorch_lightning as pl

import torchvision.transforms as transforms
from torchvision.datasets import CIFAR10
from torchvision.models import resnet18
from torch.utils.data import DataLoader
from torch.utils.data.sampler import SequentialSampler
from bigdl.nano.pytorch.vision.models import ImageClassifier
from bigdl.nano.pytorch.vision.models import vision
from bigdl.nano.pytorch import Trainer
from bigdl.nano.pytorch.inference import Optimizer
import torchmetrics
import torch
import torch.nn.functional as F


data_transform = transforms.Compose([
        transforms.ToTensor(),
        transforms.Normalize((0.5, 0.5, 0.5), (0.5, 0.5, 0.5))
    ])


test_data_transform = transforms.Compose([
        transforms.ToTensor(),
        transforms.Normalize((0.5, 0.5, 0.5), (0.5, 0.5, 0.5))
    ])


def create_data_loader(dir, batch_size, num_workers, transform, subset=50, shuffle=True, sampler=False):
    train_set = CIFAR10(root=dir, train=True,
                        download=True, transform=transform)
    # `subset` is the number of subsets. The larger the number, the smaller the training set.
    mask = list(range(0, len(train_set), subset))
    train_subset = torch.utils.data.Subset(train_set, mask)
    if sampler:
        sampler_set = SequentialSampler(train_subset)
        data_loader = DataLoader(train_subset, batch_size=batch_size, shuffle=shuffle,
                             num_workers=num_workers, sampler=sampler_set)
    else:
        data_loader = DataLoader(train_subset, batch_size=batch_size, shuffle=shuffle,
                             num_workers=num_workers)
    return data_loader


def create_test_data_loader(dir, batch_size, num_workers, transform, subset=50):
    '''
    This function is to create a fixed dataset without any randomness
    '''
    train_set = CIFAR10(root=dir, train=False,
                        download=True, transform=transform)
    # `subset` is the number of subsets. The larger the number, the smaller the training set.
    mask = list(range(0, len(train_set), subset))
    train_subset = torch.utils.data.Subset(train_set, mask)

    data_loader = DataLoader(train_subset, batch_size=batch_size, shuffle=False,
                             num_workers=num_workers)
    return data_loader


class Net(nn.Module):
    def __init__(self, l1=8, l2=16):
        super(Net, self).__init__()
        self.conv1 = nn.Conv2d(3, 6, 5)
        self.pool = nn.MaxPool2d(2, 2)
        self.conv2 = nn.Conv2d(6, 16, 5)
        self.fc1 = nn.Linear(16 * 5 * 5, l1)
        self.fc2 = nn.Linear(l1, l2)
        self.fc3 = nn.Linear(l2, 10)

    def forward(self, x):
        x = self.pool(F.relu(self.conv1(x)))
        x = self.pool(F.relu(self.conv2(x)))
        x = x.reshape(-1, 16 * 5 * 5)
        x = F.relu(self.fc1(x))
        x = F.relu(self.fc2(x))
        x = self.fc3(x)
        return x


class TestInferencePipeline(TestCase):
    num_workers = 0
    data_dir = os.path.join(os.path.dirname(__file__), "data")
    metric = torchmetrics.Accuracy(num_classes=10, top_k=1)
    max_epochs = 1

    model = Net()
    test_loader = create_data_loader(data_dir, 1, num_workers, data_transform, subset=10, shuffle=False)
    train_loader = create_data_loader(data_dir, 32, num_workers, data_transform, subset=10, shuffle=True)
    loss = nn.CrossEntropyLoss()
    optimizer = torch.optim.Adam(model.parameters(), lr=0.001)

    trainer = Trainer(max_epochs=max_epochs)
    model = Trainer.compile(model, loss, optimizer)
    trainer.fit(model, train_loader)

    def test_pipeline(self):
        inference_opt = Optimizer()
        inference_opt.optimize(model=self.model, 
                               training_data=self.train_loader,
                               validation_data=self.test_loader, 
                               metric=self.metric,
                               cpu_num=1)
        for key, value in inference_opt.optimized_model_dict.items():
            print(key, value["latency"], value["accuracy"])
        acc_model, option = inference_opt.get_best_model()
        print(option)
        acc_model, option = inference_opt.get_best_model(accelerator="onnxruntime")
        assert option == "" or "onnxruntime" in  option
        acc_model, option = inference_opt.get_best_model(precision="int8")
        assert option == "" or "inc" in  option
        acc_model, option = inference_opt.get_best_model(accuracy_criterion=0.1,
                                                         direction="max")

if __name__ == "__main__":
    test = TestInferencePipeline()
    test.test_pipeline()