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
# This example is adapted from
# https://www.kaggle.com/code/remekkinas/tps-5-pytorch-nn-for-tabular-step-by-step/notebook

from sklearn.model_selection import train_test_split
import torch
import torch.nn as nn
import torch.nn.functional as F2

from bigdl.orca import init_orca_context, stop_orca_context
import bigdl.orca.data.pandas
from bigdl.orca.data.transformer import *
from bigdl.orca.learn.pytorch import Estimator
from bigdl.orca.learn.metrics import Accuracy

import pytorch_lightning as pl
from pytorch_lightning.callbacks import Callback

init_orca_context(cluster_mode="local", cores=4, memory="3g")

# Load data
file_path = 'train.csv'
data_shard = bigdl.orca.data.pandas.read_csv(file_path)

# Drop duplicate columns
data_shard = data_shard.deduplicates()

# Labelencode y
def change_col_name(df):
    df = df.rename(columns={'id': 'id0'})
    return df
data_shard = data_shard.transform_shard(change_col_name)
encode = StringIndexer(inputCol='target')
data_shard = encode.fit_transform(data_shard)
def change_val(df):
    df['target'] = df['target']-1
    return df
data_shard = data_shard.transform_shard(change_val)

# Split train and test set
def split_train_test(data):
    RANDOM_STATE = 2021
    train, test = train_test_split(data, test_size=0.2, random_state=RANDOM_STATE)
    return train, test
train_shard, val_shard = data_shard.transform_shard(split_train_test).split()

# Transform the feature columns
feature_list = []
for i in range(50):
    feature_list.append('feature_' + str(i))
scale = MinMaxScaler(inputCol=feature_list, outputCol="x_scaled")
train_shard = scale.fit_transform(train_shard)
val_shard = scale.transform(val_shard)

# Change data types
def change_data_type(df):
    df['x_scaled'] = df['x_scaled'].apply(lambda x: np.array(x, dtype=np.float32))
    df['target'] = df['target'].apply(lambda x: np.long(x))
    return df
train_shard = train_shard.transform_shard(change_data_type)
val_shard = val_shard.transform_shard(change_data_type)


# Model
torch.manual_seed(0)
BATCH_SIZE = 64
NUM_CLASSES = 4
NUM_EPOCHS = 1
NUM_FEATURES = 50


def linear_block(in_features, out_features, p_drop, *args, **kwargs):
    return nn.Sequential(
        nn.Linear(in_features, out_features),
        nn.ReLU(),
        nn.Dropout(p=p_drop)
    )


class LightningMNISTClassifier(pl.LightningModule):
    def __init__(self):
        super(LightningMNISTClassifier, self).__init__()
        num_feature = NUM_FEATURES
        num_class = NUM_CLASSES
        self.linear = nn.Sequential(
            linear_block(num_feature, 100, 0.3),
            linear_block(100, 250, 0.3),
            linear_block(250, 128, 0.3),
        )

        self.out = nn.Sequential(
            nn.Linear(128, num_class)
        )

    def forward(self, x):
        x = self.linear(x)
        return self.out(x)

    def cross_entropy_loss(self, logits, labels):
        return F2.cross_entropy(logits, labels)

    def training_step(self, train_batch, batch_idx):
        x, y = train_batch
        outputs = self.forward(x[0])
        loss = self.cross_entropy_loss(outputs, y)
        self.log('train_loss', loss)
        # return loss
        return {"loss": loss, "predictions": outputs, "labels": y}

    def validation_step(self, val_batch, batch_idx):
        x, y = val_batch
        outputs = self.forward(x[0])
        loss = self.cross_entropy_loss(outputs, y)
        self.log('val_loss', loss)
        # return loss
        return {"loss": loss, "predictions": outputs, "labels": y}

    def configure_optimizers(self):
        optimizer = torch.optim.Adam(self.parameters(), lr=1e-3)
        lr_scheduler = torch.optim.lr_scheduler.StepLR(optimizer, step_size=1)
        optimizer_one = torch.optim.Adam(self.parameters(), lr=1e-3)
        optimizer_two = torch.optim.Adam(self.parameters(), lr=1e-3)
        # return optimizer
        return [{"optimizer": optimizer_one, "frequency": 5}, {"optimizer": optimizer_two, "frequency": 10}]
        # return [optimizer], [lr_scheduler]


class MyPrintingCallback(Callback):
    def on_train_start(self, trainer, pl_module):
        print("Training is starting")

    def on_train_end(self, trainer, pl_module):
        print("Training is ending")

def model_creator(config):
    model = LightningMNISTClassifier()
    return model

est = Estimator.from_torch(model=model_creator, metrics=[Accuracy()], backend="ray")


est.fit(data=train_shard, feature_cols=['x_scaled'], label_cols=['target'], validation_data=val_shard,
        epochs=1, batch_size=BATCH_SIZE, callbacks=[MyPrintingCallback()])

result = est.evaluate(data=val_shard, feature_cols=['x_scaled'], label_cols=['target'], batch_size=1)


for r in result:
    print(r, ":", result[r])

stop_orca_context()
