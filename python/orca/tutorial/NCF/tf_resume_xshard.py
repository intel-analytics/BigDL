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

# Step 0: Import necessary libraries
import math
import tensorflow as tf

from bigdl.orca.data import XShards
from bigdl.orca import init_orca_context, stop_orca_context
from bigdl.orca.learn.tf2 import Estimator

from process_xshards import get_feature_cols


# Step 1: Init Orca Context
init_orca_context(cluster_mode="local")


# Step 2: Read and process data using Xshards
train_data = XShards.load_pickle("./train_processed_xshards")
test_data = XShards.load_pickle("./test_processed_xshards")
feature_cols = get_feature_cols()
label_cols = ["label"]


# Step 3: Distributed training with Orca TF2 Estimator and load the model weight
backend = 'ray'  # 'ray' or 'spark'
est = Estimator.from_keras()
est.load('NCF_model')

batch_size = 10240
train_steps = math.ceil(len(train_data) / batch_size)
val_steps = math.ceil(len(test_data) / batch_size)
callbacks = [tf.keras.callbacks.TensorBoard(log_dir="./log")]

est.fit(train_data,
        epochs=2,
        batch_size=batch_size,
        feature_cols=feature_cols,
        label_cols=label_cols,
        steps_per_epoch=train_steps,
        callbacks=callbacks)


# Step 4: Distributed evaluation of the trained model
result = est.evaluate(test_data,
                      feature_cols=feature_cols,
                      label_cols=label_cols,
                      batch_size=batch_size,
                      num_steps=val_steps)
print("Evaluation results:")
for r in result:
    print("{}: {}".format(r, result[r]))


# Step 5: Save the trained TensorFlow model
est.save("NCF_resume_model")


# Step 6: Stop Orca Context when program finishes
stop_orca_context()
