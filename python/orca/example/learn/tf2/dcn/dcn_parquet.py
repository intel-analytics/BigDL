import math

import tensorflow as tf
from bigdl.friesian.feature import FeatureTable
from bigdl.orca import init_orca_context
import tensorflow_recommenders as tfrs
from bigdl.orca.learn.tf2.estimator import Estimator
from pyspark.sql.types import IntegerType, StringType
from pyspark.sql.functions import col
from bigdl.friesian.feature.utils import featuretable_to_xshards
from bigdl.orca.data.tf.tf2_data import Dataset


class DCN(tfrs.Model):

    def __init__(self, use_cross_layer, deep_layer_sizes, projection_dim=None):
        super().__init__()

        self.embedding_dimension = 32

        str_features = ["movie_id", "user_id", "zip_code",
                        "occupation"]
        int_features = ["gender", "age"]

        self._all_features = str_features + int_features
        self._embeddings = {}

        # Compute embeddings for string features.
        for feature_name in str_features:
            vocabulary = vocabularies[feature_name]
            self._embeddings[feature_name] = tf.keras.Sequential(
                [tf.keras.layers.StringLookup(
                    vocabulary=vocabulary, mask_token=None),
                    tf.keras.layers.Embedding(len(vocabulary) + 1,
                                              self.embedding_dimension)
                ])

        # Compute embeddings for int features.
        for feature_name in int_features:
            vocabulary = vocabularies[feature_name]
            self._embeddings[feature_name] = tf.keras.Sequential(
                [tf.keras.layers.IntegerLookup(
                    vocabulary=vocabulary, mask_value=None),
                    tf.keras.layers.Embedding(len(vocabulary) + 1,
                                              self.embedding_dimension)
                ])

        if use_cross_layer:
            self._cross_layer = tfrs.layers.dcn.Cross(
                projection_dim=projection_dim,
                kernel_initializer="glorot_uniform")
        else:
            self._cross_layer = None

        self._deep_layers = [tf.keras.layers.Dense(layer_size, activation="relu")
                             for layer_size in deep_layer_sizes]

        self._logit_layer = tf.keras.layers.Dense(1)

        self.task = tfrs.tasks.Ranking(
            loss=tf.keras.losses.MeanSquaredError(reduction=tf.keras.losses.Reduction.NONE),
            metrics=[tf.keras.metrics.RootMeanSquaredError("RMSE")]
        )

    def call(self, features):
        # Concatenate embeddings
        embeddings = []
        for feature_name in self._all_features:
            embedding_fn = self._embeddings[feature_name]
            embeddings.append(embedding_fn(features[feature_name]))

        x = tf.concat(embeddings, axis=1)

        # Build Cross Network
        if self._cross_layer is not None:
            x = self._cross_layer(x)

        # Build Deep Network
        for deep_layer in self._deep_layers:
            x = deep_layer(x)

        return self._logit_layer(x)

    def compute_loss(self, features, training=False):
        labels = features.pop("rating")
        scores = self(features)
        return self.task(
            labels=labels,
            predictions=scores,
        )


cols = ["movie_id", "user_id", "gender", "age", "occupation", "zip_code", "rating"]
str_features = ["movie_id", "user_id", "zip_code", "occupation"]
int_features = ["gender", "age"]
init_orca_context("local", cores=4, memory="2g", init_ray_on_spark=True)

data_dir = "/Users/yita/Documents/intel/data"
table = FeatureTable(FeatureTable.read_parquet(data_dir + "/total.parquet").df.repartition(8))
gender_dict = {'M': 0, 'F': 1}
gender_to_int = lambda x: gender_dict[x]
table = table.apply("gender", "gender", gender_to_int, dtype="int")
table = table.ordinal_shuffle_partition()
from pyspark.sql.functions import spark_partition_id, asc, desc
table.df.withColumn("partitionId", spark_partition_id()).groupBy("partitionId").count().orderBy(asc("count")).show()
print(table.df.schema)
# convert dtype
train_df = table.df
for c in str_features:
    train_df = train_df.withColumn(c, col(c).cast(StringType()))
for c in int_features:
    train_df = train_df.withColumn(c, col(c).cast(IntegerType()))
print(train_df.schema)
table = FeatureTable(train_df)
# generate vocab
vocabularies = {}
for col in cols:
    vocabularies[col] = table.select(col).df.distinct().rdd.map(lambda row: row[col]).collect()

table = FeatureTable(table.df.limit(100_000).repartition(7))
c = table.df.rdd.getNumPartitions()
train_tbl, test_tbl = table.random_split([0.8, 0.2])
train_count = train_tbl.size()
steps = math.ceil(train_count / 8192)
print("train size: ", train_count, ", steps: ", steps)
test_count = test_tbl.size()
val_steps = math.ceil(test_count / 8192)
print("test size: ", test_count, ", steps: ", val_steps)

train_xshards = featuretable_to_xshards(train_tbl)
a = train_xshards.rdd.getNumPartitions()
val_xshards = featuretable_to_xshards(test_tbl)
b = train_xshards.rdd.getNumPartitions()

train_dataset = Dataset.from_tensor_slices(train_xshards)
val_dataset = Dataset.from_tensor_slices(val_xshards)

config = {
    "lr": 0.01
}


def model_creator(config):
    model = DCN(use_cross_layer=True, deep_layer_sizes=[192, 192])
    model.compile(optimizer=tf.keras.optimizers.Adam(config['lr']))
    return model


estimator = Estimator.from_keras(model_creator=model_creator,
                                 verbose=True,
                                 config=config,
                                 backend="tf2", workers_per_node=2)

# estimator = Estimator.from_keras(model_creator=model_creator,
#                                  verbose=True,
#                                  config=config,
#                                  backend="spark",
#                                  model_dir="/Users/yita/Documents/intel/data")

estimator.fit(train_dataset, 8, batch_size=8192, steps_per_epoch=steps, validation_data=val_dataset,
              validation_steps=val_steps)

# estimator.fit(train_dataset, 8, batch_size=8192, steps_per_epoch=steps)
