import os
import time
import argparse
import numpy as np
import pandas as pd 
import scipy.sparse as sp

import torch
import torch.nn as nn
import torch.optim as optim
import torch.utils.data as data
import torch.nn.functional as F 
from model import NCF

#Step 0: Parameters And Configuration

parser = argparse.ArgumentParser()
parser.add_argument("--dataset", 
    type=str, 
    default="ml-1m", 
    help="dataset name")
parser.add_argument("--model", 
    type=str, 
    default="NeuMF-end", 
    help="model name")
parser.add_argument("--main_path", 
    type=str, 
    default="./NCF-Data/", 
    help="main path")
parser.add_argument("--model_path", 
    type=str, 
    default="./models/", 
    help="model path")
parser.add_argument("--out", 
    type=bool, 
    default=True, 
    help="save model or not")
parser.add_argument("--cluster_mode", 
    type=str, 
    default="local", 
    help="")
parser.add_argument("--lr", 
    type=float, 
    default=0.001, 
    help="learning rate")
parser.add_argument("--dropout", 
    type=float, 
    default=0.0, 
    help="dropout rate")
parser.add_argument("--batch_size", 
    type=int, 
    default=256, 
    help="batch size for training")
parser.add_argument("--epochs", 
    type=int, 
    default=20, 
    help="training epoches")
parser.add_argument("--top_k", 
    type=int, 
    default=10, 
    help="compute metrics@top_k")
parser.add_argument("--factor_num", 
    type=int, 
    default=32, 
    help="predictive factors numbers in the model")
parser.add_argument("--num_layers", 
    type=int, 
    default=3, 
    help="number of layers in MLP model")
parser.add_argument("--num_ng", 
    type=int, 
    default=4, 
    help="sample negative items for training")
parser.add_argument("--test_num_ng", 
    type=int, 
    default=0, 
    help="sample part of negative items for testing")
parser.add_argument("--backend", 
    type=str, 
    default="spark", 
    help="backend used in estimator, ray or spark are supported")
parser.add_argument("--user_num", 
    type=int, 
    default=6040, 
    help="total user num in the ml-1m dataset")
parser.add_argument("--item_num", 
    type=int, 
    default=3952, 
    help="total item num in the ml-1m dataset")
args = parser.parse_args()

#Step 1: Init Orca Context

from bigdl.orca import init_orca_context, stop_orca_context, OrcaContext
init_orca_context(cores=1, memory="8g")

#Step 2: Define Dataset

from bigdl.orca.data import XShards
from bigdl.orca.data.pandas import read_csv
from sklearn.model_selection import train_test_split

def preprocess_data():   
    data_X = read_csv(
        args.dataset+"/ratings.dat", 
        sep="::", header=None, names=['user', 'item'], 
        usecols=[0, 1], dtype={0: np.int32, 1: np.int32})  
    data_X = data_X.partition_by("user",5)#num_partitions=5 
    return data_X

# prepare the train and test datasets
data_X = preprocess_data()

# construct the train and test xshards
def transform_to_dict(data):
    data["user"] = data["user"]-1
    data["item"] = data["item"]-1
    data_X = data.values.tolist()
    
    #calculate a dok matrix
    train_mat = sp.dok_matrix((args.user_num, args.item_num), dtype=np.int64)
    for row in data_X:
        train_mat[row[0], row[1]] = 1

    #negative sampling
    features_ps = data_X
    features_ng = []
    for x in features_ps:
        u = x[0]
        for t in range(args.num_ng):
            j = np.random.randint(args.item_num)
            while (u, j) in train_mat:
                j = np.random.randint(args.item_num)
            features_ng.append([u, j])

    labels_ps = [1 for _ in range(len(features_ps))]
    labels_ng = [0 for _ in range(len(features_ng))]

    features_fill = features_ps + features_ng
    labels_fill = labels_ps + labels_ng      
    data_XY = pd.DataFrame(data=features_fill,columns=["user","item"])
    data_XY["y"] = labels_fill

    #split training set and testing set
    train_data, test_data = train_test_split(data_XY, test_size=0.2, random_state=100)

    #transform dataset into dict
    train_data, test_data = train_data.to_numpy(), test_data.to_numpy()
    train_data, test_data = {"x": train_data[:,:2].astype(np.int64), "y": train_data[:,2].astype(np.float)}, {"x": test_data[:,:2].astype(np.int64), "y": test_data[:,2].astype(np.float)}  
    return train_data,test_data

train_shards, test_shards = data_X.transform_shard(transform_to_dict).split()

#Step 3: Define the Model

# create the model
def model_creator(config):
    model = NCF(args.user_num, args.item_num, args.factor_num, args.num_layers, args.dropout, args.model) # a torch.nn.Module
    model.train()
    return model

#create the optimizer
def optimizer_creator(model, config):
    return optim.Adam(model.parameters(), lr=args.lr)

#define the loss function
loss_function = nn.BCEWithLogitsLoss()

#Step 4: Fit with Orca Estimator

from bigdl.orca.learn.pytorch import Estimator 
from bigdl.orca.learn.metrics import Accuracy,AUC

# create the estimator
est = Estimator.from_torch(model=model_creator, optimizer=optimizer_creator, loss=loss_function, metrics=[Accuracy(),AUC()], backend=args.backend)# backend="ray" or "spark"

# fit the estimator
est.fit(data=train_shards, epochs=5, batch_size=args.batch_size, feature_cols=["x"], label_cols =["y"])

#Step 5: Evaluate and save the Model

# evaluate the model
result = est.evaluate(data=test_shards)
for r in result:
    print(r, ":", result[r])

# save the model
est.save("NCF_model") 

# stop orca context when program finishes
stop_orca_context()



