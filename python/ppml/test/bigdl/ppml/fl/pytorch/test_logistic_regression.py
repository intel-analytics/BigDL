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

# import threading
# from multiprocessing import Process
# import unittest
# import numpy as np
# import pandas as pd
# import os

# import torch
# from bigdl.ppml.fl.pytorch.utils import set_one_like_parameter
# from bigdl.ppml.fl.pytorch.fl_server import FLServer
# from torch import nn
# import logging

# from bigdl.ppml.fl.pytorch.pipeline import PytorchPipeline

# resource_path = os.path.join(os.path.dirname(__file__), "../resources")

# def mock_process(data_train):
#     df_train = pd.read_csv(os.path.join(resource_path, data_train))
#     if 'Outcome' in df_train:
#         df_x = df_train.drop('Outcome', 1)
#         df_y = df_train['Outcome']
#     else:
#         df_x = df_train
#         df_y = None
#     x = df_x.to_numpy(dtype="float32")
#     y = np.expand_dims(df_y.to_numpy(dtype="float32"), axis=1) if df_y is not None else None
    
#     model = LogisticRegressionNetwork1(len(df_x.columns))
#     set_one_like_parameter(model)
#     loss_fn = nn.BCELoss()
#     optimizer = torch.optim.SGD(model.parameters(), lr=1e-3)
#     server_model = LogisticRegressionNetwork2()
#     ppl = PytorchPipeline(model, loss_fn, optimizer)
#     ppl.add_server_model(server_model)
#     response = ppl.fit(x, y)
#     logging.info(response)
#     return ppl


# class TestLogisticRegression(unittest.TestCase):
#     fmt = '%(asctime)s %(levelname)s {%(module)s:%(lineno)d} - %(message)s'
#     logging.basicConfig(format=fmt, level=logging.DEBUG)
#     def setUp(self) -> None:
#         self.fl_server = FLServer(loss_fn='binary_cross_entropy', optimizer='sgd', client_num=2)
#         self.fl_server.build() 
#         self.fl_server.start()


#     def test_two_party_logistic_regression(self) -> None:
#         df_train = pd.read_csv(
#             os.path.join(resource_path, 'pima-indians-diabetes.csv'))
        
    
#         df_x1 = df_train[['Pregnancies','Glucose','BloodPressure','SkinThickness','Outcome']]
#         df_x2 = df_train[['Insulin','BMI','DiabetesPedigreeFunction','Age']]
#         df_y = df_train['Outcome']
#         model = LogisticRegressionNetwork(len(df_x1.columns), len(df_x2.columns))
#         set_one_like_parameter(model)
#         loss_fn = nn.BCELoss()
#         optimizer = torch.optim.SGD(model.parameters(), lr=1e-4)
        
#         x1 = torch.tensor(df_x1.to_numpy(dtype="float32"))
#         x2 = torch.tensor(df_x2.to_numpy(dtype="float32"))
#         y = torch.tensor(np.expand_dims(df_y.to_numpy(dtype="float32"), axis=1))
#         assert len(x1) == len(x2) == len(y)
#         batch_size = 4
        
#         i, size = 0, len(x1)
#         # just abandon last batch to reduce code
#         while i + batch_size < len(x1):
#             X1, X2, Y = x1[i:i + batch_size], x2[i:i + batch_size], y[i:i + batch_size]
#             pred = model(X1, X2)
#             loss = loss_fn(pred, Y)
#             optimizer.zero_grad()
#             loss.backward()
#             optimizer.step()
#             i += batch_size
#             if i % 100 == 0:
#                 loss, current = loss.item(), i
#                 print(f"loss: {loss:>7f}  [{current:>3d}/{size:>3d}]")
        
#         mock_party2 = threading.Thread(target=mock_process, 
#             args=('diabetes-vfl-2.csv',))
#         mock_party2.start()        
#         ppl = mock_process(data_train='diabetes-vfl-1.csv')
#         mock_party2.join()


# class LogisticRegressionNetwork1(nn.Module):
#     def __init__(self, num_feature) -> None:
#         super().__init__()
#         self.dense = nn.Linear(num_feature, 1)

#     def forward(self, x):
#         x = self.dense(x)
#         return x

# class LogisticRegressionNetwork2(nn.Module):
#     def __init__(self) -> None:
#         super().__init__()
#         self.sigmoid = nn.Sigmoid()

#     def forward(self, x):
#         x = self.sigmoid(x)
#         return x

# class LogisticRegressionNetwork(nn.Module):
#     def __init__(self, num_feature1, num_feature2) -> None:
#         super().__init__()
#         self.dense1 = nn.Linear(num_feature1, 1)
#         self.dense2 = nn.Linear(num_feature2, 1)
#         self.sigmoid = nn.Sigmoid()

#     def forward(self, x1, x2):
#         x1 = self.dense1(x1)
#         x2 = self.dense2(x2)
#         x = torch.stack([x1, x2])
#         x = torch.sum(x, dim=0)
#         x = self.sigmoid(x)
#         return x

# if __name__ == '__main__':
#     unittest.main()