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


import pickle
import grpc
from numpy import ndarray
from bigdl.ppml.fl.pytorch.generated.nn_service_pb2 import TrainRequest, UploadModelRequest
from bigdl.ppml.fl.pytorch.generated.nn_service_pb2_grpc import *
from bigdl.ppml.fl.pytorch.protobuf_utils import ndarray_map_to_tensor_map
import uuid
from torch.utils.data import DataLoader
from bigdl.dllib.utils.log4Error import invalidInputError

class FLClient(object):
    def __init__(self) -> None:
        self.channel = grpc.insecure_channel("localhost:8980")
        self.nn_stub = NNServiceStub(self.channel)
        self.client_uuid = str(uuid.uuid4())

    
    def train(self, x):
        tensor_map = ndarray_map_to_tensor_map(x)
        train_request = TrainRequest(clientuuid=self.client_uuid,
                                     data=tensor_map)
        
        response = self.nn_stub.train(train_request)
        if response.code == 1:
            invalidInputError(False,
                              response.response)
        return response

    def upload_model(self, model):
        # upload model to server
        model = pickle.dumps(model)        
        self.nn_stub.upload_model(UploadModelRequest(model_bytes=model))


    
