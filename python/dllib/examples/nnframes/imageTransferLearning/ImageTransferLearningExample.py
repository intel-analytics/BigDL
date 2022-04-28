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

import os

from bigdl.dllib.nn.criterion import *
from bigdl.dllib.nn.layer import *
from bigdl.dllib.optim.optimizer import Adam
from pyspark.ml import Pipeline
from pyspark.ml.evaluation import MulticlassClassificationEvaluator
from pyspark.sql.functions import col, udf
from pyspark.sql.types import DoubleType, StringType

from bigdl.dllib.nncontext import *
from bigdl.dllib.feature.image import *
from bigdl.dllib.nnframes import *

from optparse import OptionParser
from bigdl.dllib.utils.log4Error import *


if __name__ == "__main__":

    parser = OptionParser()
    parser.add_option("-m", dest="model_path",
                      help="Required. pretrained model path.")
    parser.add_option("-f", dest="image_path",
                      help="training data path.")
    parser.add_option("--b", "--batch_size", type=int, dest="batch_size", default="56",
                      help="The number of samples per gradient update. Default is 56.")
    parser.add_option("--nb_epoch", type=int, dest="nb_epoch", default="20",
                      help="The number of epochs to train the model. Default is 20.")
    parser.add_option("--r", "--learning_rate", type=float, dest="learning_rate", default="0.002",
                      help="The learning rate for the model. Default is 0.002.")
    parser.add_option("--cluster-mode", dest="clusterMode", default="local")

    (options, args) = parser.parse_args(sys.argv)

    if not options.model_path:
        parser.print_help()
        parser.error('model_path is required')

    if not options.image_path:
        parser.print_help()
        parser.error('image_path is required')

    conf = {}
    if options.clusterMode.startswith("yarn"):
        hadoop_conf = os.environ.get("HADOOP_CONF_DIR")
        invalidInputError(hadoop_conf,
                          "Directory path to hadoop conf not found for yarn-client" \
                          " mode.", "Please either specify argument hadoop_conf or" \
                                    "set the environment variable HADOOP_CONF_DIR")
        spark_conf = create_spark_conf().set("spark.executor.memory", "5g") \
            .set("spark.executor.cores", 2) \
            .set("spark.executor.instances", 2) \
            .set("spark.driver.memory", "2g")
        spark_conf.setAll(conf)

        if options.clusterMode == "yarn-client":
            sc = init_nncontext(spark_conf, cluster_mode="yarn-client", hadoop_conf=hadoop_conf)
        else:
            sc = init_nncontext(spark_conf, cluster_mode="yarn-cluster", hadoop_conf=hadoop_conf)
    elif options.clusterMode == "local":
        spark_conf = SparkConf().set("spark.driver.memory", "10g") \
            .set("spark.driver.cores", 4)
        sc = init_nncontext(spark_conf, cluster_mode="local")
    elif options.clusterMode == "spark-submit":
        sc = init_nncontext(cluster_mode="spark-submit")

    imageDF = NNImageReader.readImages(options.image_path, sc, resizeH=300, resizeW=300,
                                       image_codec=1)

    getName = udf(lambda row: os.path.basename(row[0]), StringType())
    getLabel = udf(lambda name: 1.0 if name.startswith('cat') else 2.0, DoubleType())
    labelDF = imageDF.withColumn("name", getName(col("image"))) \
        .withColumn("label", getLabel(col('name')))
    (trainingDF, validationDF) = labelDF.randomSplit([0.9, 0.1])

    # compose a pipeline that includes feature transform, pretrained model and Logistic Regression
    transformer = ChainedPreprocessing(
        [RowToImageFeature(), ImageResize(256, 256), ImageCenterCrop(224, 224),
         ImageChannelNormalize(123.0, 117.0, 104.0), ImageMatToTensor(), ImageFeatureToTensor()])

    preTrainedNNModel = NNModel(Model.loadModel(options.model_path), transformer) \
        .setFeaturesCol("image") \
        .setPredictionCol("embedding")

    lrModel = Sequential().add(Linear(1000, 2)).add(LogSoftMax())
    classifier = NNClassifier(lrModel, ClassNLLCriterion(), SeqToTensor([1000])) \
        .setLearningRate(options.learning_rate) \
        .setOptimMethod(Adam()) \
        .setBatchSize(options.batch_size) \
        .setMaxEpoch(options.nb_epoch) \
        .setFeaturesCol("embedding") \
        .setCachingSample(False) \

    pipeline = Pipeline(stages=[preTrainedNNModel, classifier])

    catdogModel = pipeline.fit(trainingDF)
    predictionDF = catdogModel.transform(validationDF).cache()
    predictionDF.sample(False, 0.1).show()

    evaluator = MulticlassClassificationEvaluator(
        labelCol="label", predictionCol="prediction", metricName="accuracy")
    accuracy = evaluator.evaluate(predictionDF)
    # expected error should be less than 10%
    print("Test Error = %g " % (1.0 - accuracy))

    print("finished...")
    sc.stop()
