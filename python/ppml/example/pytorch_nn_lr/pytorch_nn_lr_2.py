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

import numpy as np
import pandas as pd

import torch
from torch import nn
from bigdl.ppml.fl.estimator import Estimator
from bigdl.ppml.fl.algorithms.psi import PSI
from bigdl.ppml.fl.nn.pytorch.utils import set_one_like_parameter


class LocalModel(nn.Module):
    def __init__(self, num_feature) -> None:
        super().__init__()
        self.dense = nn.Linear(num_feature, 1)

    def forward(self, x):
        x = self.dense(x)
        return x


if __name__ == '__main__':
    df_train = pd.read_csv('.data/diabetes-vfl-2.csv')

    # this should wait for the merge of 2 FLServer (Py4J Java gRPC and Python gRPC)
    # df_train['ID'] = df_train['ID'].astype(str)
    # psi = PSI()
    # intersection = psi.get_intersection(list(df_train['ID']))
    # df_train = df_train[df_train['ID'].isin(intersection)]
    
    df_x = df_train
    x = df_x.to_numpy(dtype="float32")
    y = None
    
    model = LocalModel(len(df_x.columns))
    loss_fn = nn.BCELoss()
    ppl = Estimator.from_torch(client_model=model,
                               client_id='2',
                               loss_fn=loss_fn,
                               optimizer_cls=torch.optim.SGD,
                               optimizer_args={'lr':1e-5},
                               target='localhost:8980')
    response = ppl.fit(x, y)
    result = ppl.predict(x)
    print(result[:5])
