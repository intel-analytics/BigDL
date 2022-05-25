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

from multiprocessing import Process
import unittest
import numpy as np
import pandas as pd
import os

from bigdl.ppml.fl import *
from bigdl.ppml.fl.nn.fl_server import FLServer
from bigdl.ppml.fl.nn.fl_client import FLClient
from bigdl.ppml.fl.nn.pytorch.utils import set_one_like_parameter
from bigdl.ppml.fl.utils import init_fl_context
from bigdl.ppml.fl.nn.tensorflow.pipeline import TensorflowPipeline

import tensorflow as tf
print("TensorFlow version:", tf.__version__)

from tensorflow.keras.layers import Dense, Flatten, Conv2D
from tensorflow.keras import Model




resource_path = os.path.join(os.path.dirname(__file__), "../../resources")


class TestCorrectness(unittest.TestCase):
    fmt = '%(asctime)s %(levelname)s {%(module)s:%(lineno)d} - %(message)s'
    logging.basicConfig(format=fmt, level=logging.INFO)
    def setUp(self) -> None:
        self.fl_server = FLServer()
        self.fl_server.build()
        self.fl_server.start()
    
    def tearDown(self) -> None:
        self.fl_server.stop()

    def test_mnist(self) -> None:
        """
        following code is copied from pytorch quick start
        link: https://www.tensorflow.org/tutorials/quickstart/advanced
        """
        mnist = tf.keras.datasets.mnist

        (x_train, y_train), (x_test, y_test) = mnist.load_data()
        x_train, x_test = x_train / 255.0, x_test / 255.0

        # Add a channels dimension
        x_train = x_train[..., tf.newaxis].astype("float32")
        x_test = x_test[..., tf.newaxis].astype("float32")
        train_ds = tf.data.Dataset.from_tensor_slices(
        (x_train, y_train)).shuffle(10000).batch(32)

        test_ds = tf.data.Dataset.from_tensor_slices((x_test, y_test)).batch(32)


        model = NeuralNetwork()
        loss_object = tf.keras.losses.SparseCategoricalCrossentropy(from_logits=True)
        optimizer = tf.keras.optimizers.Adam()
        train_loss = tf.keras.metrics.Mean(name='train_loss')
        train_accuracy = tf.keras.metrics.SparseCategoricalAccuracy(name='train_accuracy')

        test_loss = tf.keras.metrics.Mean(name='test_loss')
        test_accuracy = tf.keras.metrics.SparseCategoricalAccuracy(name='test_accuracy')
        @tf.function
        def train_step(images, labels):
            with tf.GradientTape() as tape:                
                # training=True is only needed if there are layers with different
                # behavior during training versus inference (e.g. Dropout).
                predictions = model(images, training=True)
                loss = loss_object(labels, predictions)
            gradients = tape.gradient(loss, model.trainable_variables)
            optimizer.apply_gradients(zip(gradients, model.trainable_variables))

            train_loss(loss)
            train_accuracy(labels, predictions)

        @tf.function
        def test_step(images, labels):
            # training=False is only needed if there are layers with different
            # behavior during training versus inference (e.g. Dropout).
            predictions = model(images, training=False)
            t_loss = loss_object(labels, predictions)

            test_loss(t_loss)
            test_accuracy(labels, predictions)


        EPOCHS = 1
        for epoch in range(EPOCHS):
            # Reset the metrics at the start of the next epoch
            train_loss.reset_states()
            train_accuracy.reset_states()
            test_loss.reset_states()
            test_accuracy.reset_states()

            for images, labels in train_ds:
                train_step(images, labels)

            for test_images, test_labels in test_ds:
                test_step(test_images, test_labels)

            print(
                f'Epoch {epoch + 1}, '
                f'Loss: {train_loss.result()}, '
                f'Accuracy: {train_accuracy.result() * 100}, '
                f'Test Loss: {test_loss.result()}, '
                f'Test Accuracy: {test_accuracy.result() * 100}'
            )

        
        # TODO: set fixed parameters
        vfl_model_1 = NeuralNetworkPart1()
        optimizer = tf.keras.optimizers.Adam()

        vfl_client_ppl = TensorflowPipeline(vfl_model_1, loss_object, optimizer)
        vfl_model_2 = NeuralNetworkPart2()
        vfl_client_ppl.add_server_model(vfl_model_2, loss_object, tf.keras.optimizers.Adam)
        vfl_client_ppl.fit(train_ds)
        # assert np.allclose(pytorch_loss_list, vfl_client_ppl.loss_history), \
        #     "Validation failed, correctness of PPML and native Pytorch not the same"
    

class NeuralNetwork(Model):
    def __init__(self):
        super().__init__()
        self.conv1 = Conv2D(32, 3, activation='relu')
        self.flatten = Flatten()
        self.d1 = Dense(128, activation='relu')
        self.d2 = Dense(10)

    def call(self, x):
        x = self.conv1(x)
        x = self.flatten(x)
        x = self.d1(x)
        return self.d2(x)

class NeuralNetworkPart1(Model):
    def __init__(self):
        super().__init__()
        self.conv1 = Conv2D(32, 3, activation='relu')
        self.flatten = Flatten()

    def forward(self, x):
        x = self.conv1(x)
        x = self.flatten(x)
        return x

class NeuralNetworkPart2(Model):
    def __init__(self):
        super().__init__()
        self.d1 = Dense(128, activation='relu')
        self.d2 = Dense(10)

    def forward(self, x):
        x = self.d1(x)
        return self.d2(x)


if __name__ == '__main__':
    unittest.main()
