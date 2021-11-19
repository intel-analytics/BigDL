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
import tensorflow as tf
from tensorflow.keras.layers import Input, Embedding, Dense, Flatten, concatenate, multiply
from sklearn.model_selection import train_test_split

from bigdl.orca import init_orca_context, stop_orca_context
from bigdl.orca.data.pandas import read_csv
from bigdl.orca.learn.tf2.estimator import Estimator


def build_model(num_users, num_items, layers=[20, 10], include_mf=True, mf_embed=20):
    num_layer = len(layers)
    user_input = Input(shape=(1,), dtype="int32", name="user_input")
    item_input = Input(shape=(1,), dtype="int32", name="item_input")

    mlp_embed_user = Embedding(input_dim=num_users + 1, output_dim=int(layers[0] / 2),
                               input_length=1)(user_input)
    mlp_embed_item = Embedding(input_dim=num_items + 1, output_dim=int(layers[0] / 2),
                               input_length=1)(item_input)
    user_latent = Flatten()(mlp_embed_user)
    item_latent = Flatten()(mlp_embed_item)

    mlp_latent = concatenate([user_latent, item_latent], axis=1)
    for idx in range(1, num_layer):
        layer = Dense(layers[idx], activation="relu",
                      name="layer%d" % idx)
        mlp_latent = layer(mlp_latent)

    if include_mf:
        mf_embed_user = Embedding(input_dim=num_users + 1, output_dim=mf_embed,
                                  input_length=1)(user_input)
        mf_embed_item = Embedding(input_dim=num_items + 1, output_dim=mf_embed,
                                  input_length=1)(item_input)
        mf_user_flatten = Flatten()(mf_embed_user)
        mf_item_flatten = Flatten()(mf_embed_item)

        mf_latent = multiply([mf_user_flatten, mf_item_flatten])
        concated_model = concatenate([mlp_latent, mf_latent], axis=1)
        prediction = Dense(1, activation="relu", name="prediction")(concated_model)
    else:
        prediction = Dense(1, activation="relu", name="prediction")(mlp_latent)

    model = tf.keras.Model([user_input, item_input], prediction)
    return model

cluster_mode = "local"
if cluster_mode == "local":
    sc = init_orca_context()
elif cluster_mode == "standalone":
    sc = init_orca_context("standalone", master="spark://", cores=8, num_nodes=4, memory="10g")
elif cluster_mode == "yarn":
    sc = init_orca_context("yarn", cores=8, num_nodes=4, memory="10g")

# Need spark3 to support delimiter with more than one character.
full_data = read_csv("/home/kai/Downloads/ml-1m/ratings.dat", sep="::", header=None, names=["user", "item", "label"],
                     usecols=[0, 1, 2], dtype={0: np.int32, 1: np.int32, 2: np.int32})
user_set = set(full_data["user"].unique())
item_set = set(full_data["item"].unique())

min_user_id = min(user_set)
max_user_id = max(user_set)
min_item_id = min(item_set)
max_item_id = max(item_set)
print(min_user_id, max_user_id, min_item_id, max_item_id)

def split_train_test(data):
    train, test = train_test_split(data, test_size=0.2, random_state=100)
    return train, test

train_data, test_data = full_data.transform_shard(split_train_test).split()

def model_creator(config):
    model = build_model(max_user_id, max_item_id)
    print(model.summary())
    optimizer = tf.keras.optimizers.Adam(1e-2)
    model.compile(optimizer=optimizer,
                  loss="mean_squared_error")
    return model

estimator = Estimator.from_keras(model_creator=model_creator,
                                 verbose=False)
estimator.fit(train_data,
              batch_size=800,
              epochs=5,
              steps_per_epoch=1000,
              feature_cols=["user", "item"],
              label_cols=["label"],
              validation_data=test_data,
              validation_steps=250)
tf.saved_model.save(estimator.get_model(), "./model")

stop_orca_context()
