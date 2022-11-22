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
# Some of the code is adapted from
# https://github.com/guoyang9/NCF/blob/master/data_utils.py
#

import os
import numpy as np
import pandas as pd
import scipy.sparse as sp

from sklearn.model_selection import train_test_split

from bigdl.orca.data.pandas import read_csv
from bigdl.orca.data.transformer import StringIndexer, MinMaxScaler


def ng_sampling(data, user_num, item_num, num_ng):
    data_X = data.values.tolist()

    # calculate a dok matrix
    train_mat = sp.dok_matrix((user_num, item_num), dtype=np.int64)
    for row in data_X:
        train_mat[row[0], row[1]] = 1

    # negative sampling
    features_ps = data_X
    features_ng = []
    for x in features_ps:
        u = x[0]
        for t in range(num_ng):
            j = np.random.randint(item_num)
            while (u, j) in train_mat:
                j = np.random.randint(item_num)
            features_ng.append([u, j])

    labels_ps = [1.0 for _ in range(len(features_ps))]
    labels_ng = [0.0 for _ in range(len(features_ng))]

    features_fill = features_ps + features_ng
    labels_fill = labels_ps + labels_ng
    data_XY = pd.DataFrame(data=features_fill, columns=["user", "item"], dtype=np.int64)
    data_XY["label"] = labels_fill
    return data_XY


def split_dataset(data):
    train_data, test_data = train_test_split(data, test_size=0.2, random_state=100)
    return train_data, test_data


def prepare_data(dataset_dir, num_ng=4):
    feature_cols = ['user', 'item',
                    'gender', 'occupation', 'zipcode', 'category',  # sparse features
                    'age']  # dense features
    label_cols = ["label"]

    users = read_csv(
        os.path.join(dataset_dir, 'users.dat'),
        sep="::", header=None, names=['user', 'gender', 'age', 'occupation', 'zipcode'],
        usecols=[0, 1, 2, 3, 4])
    ratings = read_csv(
        os.path.join(dataset_dir, 'ratings.dat'),
        sep="::", header=None, names=['user', 'item'],
        usecols=[0, 1])
    movies = read_csv(
        os.path.join(dataset_dir, 'movies.dat'),
        sep="::", header=None, names=['item', 'category'],
        usecols=[0, 2])

    # calculate numbers of user and item
    user_set = set(users["user"].unique())
    item_set = set(movies["item"].unique())
    user_num = max(user_set) + 1
    item_num = max(item_set) + 1

    # Categorical encoding
    indexer = StringIndexer('gender')
    users = indexer.fit_transform(users)
    indexer = StringIndexer('zipcode')
    users = indexer.fit_transform(users)
    indexer = StringIndexer('category')
    movies = indexer.fit_transform(movies)

    # Calculate input_dims for each sparse features
    sparse_feats_input_dims = []
    sparse_feat_set = set(users["gender"].unique())
    sparse_feats_input_dims.append(max(sparse_feat_set)+1)
    sparse_feat_set = set(users["occupation"].unique())
    sparse_feats_input_dims.append(max(sparse_feat_set)+1)
    sparse_feat_set = set(users["zipcode"].unique())
    sparse_feats_input_dims.append(max(sparse_feat_set)+1)
    sparse_feat_set = set(movies["category"].unique())
    sparse_feats_input_dims.append(max(sparse_feat_set)+1)

    # scale dense features
    scaler = MinMaxScaler(inputCol=['age'], outputCol='age_scaled')
    users = scaler.fit_transform(users)

    def process_age(shard):
        # Convert DenseVector of double to float
        shard['age'] = shard['age'][0].astype(np.float32)
        return shard.drop(columns=['age_scaled'])

    users = users.transform_shard(process_age)

    # Negative sampling
    ratings = ratings.partition_by("user")
    ratings = ratings.transform_shard(lambda shard: ng_sampling(shard, user_num, item_num, num_ng))

    # Merge XShards
    data = users.merge(ratings, on='user')
    data = data.merge(movies, on='item')

    # Split dataset
    train_data, test_data = data.transform_shard(split_dataset).split()
    return train_data, test_data, user_num, item_num, sparse_feats_input_dims, \
        feature_cols, label_cols


if __name__ == "__main__":
    from bigdl.orca import init_orca_context, stop_orca_context

    sc = init_orca_context()
    train_data, test_data, user_num, item_num, sparse_feats_input_dims, \
        feature_cols, label_cols = prepare_data("./ml-1m")
    train_data.save_pickle("train_xshards")
    test_data.save_pickle("test_xshards")
    stop_orca_context()
