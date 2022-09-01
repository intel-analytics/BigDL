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

import torch
from torchmetrics import Accuracy
from _finetune import MilestonesFinetuning, TransferLearningModel, CatDogImageDataModule

from bigdl.nano.pytorch.trainer import Trainer
from bigdl.nano.pytorch import InferenceOptimizer


if __name__ == "__main__":
    # 1. Finetune on new dataset
    milestones: tuple = (1, 2)
    trainer = Trainer(max_epochs=2, callbacks=[MilestonesFinetuning(milestones)])
    model = TransferLearningModel(milestones=milestones)
    datamodule = CatDogImageDataModule()
    trainer.fit(model, datamodule)

    # 2. Define metric for accuracy calculation
    def accuracy(pred, target):
        pred = torch.sigmoid(pred)
        target = target.view((-1, 1)).type_as(pred).int()
        return Accuracy()(pred, target)

    # 3. Accelaration inference using InferenceOptimizer
    model.eval()
    optimizer = InferenceOptimizer()
    # optimize may take about 2 minutes to run all possible accelaration combinations
    optimizer.optimize(model=model,
                       # To obtain the latency of single sample, set batch_size=1
                       training_data=datamodule.train_dataloader(batch_size=1),
                       # here we only take part samples to calculate a rough accuracy
                       validation_data=datamodule.val_dataloader(limit_num_samples=160),
                       metric=accuracy,
                       direction="max",
                       cpu_num=1,
                       latency_sample_num=30)

    for key, value in optimizer.optimized_model_dict.items():
        print("accleration option: {}, latency: {:.4f}ms, accuracy: {:.4f}".format(key, value["latency"], value["accuracy"]))

    # 4. Get the best model under specific restrictions or without restrictions
    acc_model, option = optimizer.get_best_model(accelerator="onnxruntime")
    print("When accelerator is onnxruntime, the model with minimal latency is: ", option)

    acc_model, option = optimizer.get_best_model(accuracy_criterion=0.05)
    print("When accuracy drop less than 5%, the model with minimal latency is: ", option)

    acc_model, option = optimizer.get_best_model()
    print("The model with minimal latency is: ", option)

    # 5. Inference with accelerated model
    x_input = next(iter(datamodule.train_dataloader(batch_size=1)))[0]
    output = acc_model(x_input)
