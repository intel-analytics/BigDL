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
import tensorflow as tf


class Model(tf.keras.Model):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)

    def fit(self,
            x=None,
            y=None,
            batch_size=None,
            epochs=1,
            verbose='auto',
            callbacks=None,
            validation_split=0.,
            validation_data=None,
            shuffle=True,
            class_weight=None,
            sample_weight=None,
            initial_epoch=0,
            steps_per_epoch=None,
            validation_steps=None,
            validation_batch_size=None,
            validation_freq=1,
            max_queue_size=10,
            workers=1,
            use_multiprocessing=False,
            perf_tune=None):
        if perf_tune is not None:
            assert perf_tune in ["batch_size", "callbacks", "dataset"], \
                "perf_tune must be one of [\"batch_size\", \"callbacks\", \"dataset\"] or None"

        elif perf_tune == "batch_size":
            batch_size_2 = int(batch_size * 2)
            super(Model, self).fit(x, y, batch_size_2, epochs, verbose, callbacks, validation_split, validation_data,
                                   shuffle, class_weight,
                                   sample_weight, initial_epoch, steps_per_epoch, validation_steps,
                                   validation_batch_size, validation_freq,
                                   max_queue_size, workers, use_multiprocessing)
            batch_size_12 = int(batch_size / 4)
            super(Model, self).fit(x, y, batch_size_12, epochs, verbose, callbacks, validation_split, validation_data,
                                   shuffle, class_weight,
                                   sample_weight, initial_epoch, steps_per_epoch, validation_steps,
                                   validation_batch_size, validation_freq,
                                   max_queue_size, workers, use_multiprocessing)

        else:
            super(Model, self).fit(
                x, y, batch_size, epochs, verbose, callbacks, validation_split, validation_data, shuffle, class_weight,
                sample_weight, initial_epoch, steps_per_epoch, validation_steps, validation_batch_size, validation_freq,
                max_queue_size, workers, use_multiprocessing)
