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
from bigdl.orca import init_orca_context, stop_orca_context, OrcaContext
from bigdl.orca.learn.tf2 import Estimator

from process_spark_dataframe import get_feature_cols
from utils import *


# Step 1: Init Orca Context
args = parse_args("TensorFlow NCF Predicting with Spark DataFrame")
args.backend = "ray"
init_orca(args)
spark = OrcaContext.get_spark_session()


# Step 2: Load the model and data
est = Estimator.from_keras()
load_tf_model(est, args.model_dir, "NCF_model")

df = spark.read.parquet(os.path.join(args.data_dir, "test_processed_dataframe.parquet"))
feature_cols = get_feature_cols()


# Step 3: Distributed inference of the loaded model
predict_df = est.predict(df, batch_size=10240, feature_cols=feature_cols)
print("Prediction results of the first 5 rows:")
predict_df.show(5)


# Step 4: Save the prediction results
predict_df.write.parquet(os.path.join(args.data_dir, "test_predictions_dataframe.parquet"),
                         mode="overwrite")


# Step 5: Stop Orca Context when program finishes
stop_orca_context()
