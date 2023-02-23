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
from bigdl.orca.automl.search import SearchEngineFactory
from bigdl.dllib.utils.log4Error import *
from bigdl.dllib.utils.log4Error import invalidInputError
from numpy import ndarray
from typing import TYPE_CHECKING, Callable, Dict, List, Optional, Tuple, Union, Any

if TYPE_CHECKING:
    from bigdl.orca.automl.model.base_pytorch_model import ModelBuilder
    from pyspark.sql import DataFrame
    from ray.tune.sample import Categorical, Float, Integer, Function


class AutoEstimator:
    """
    Example:
        >>> auto_est = AutoEstimator.from_torch(model_creator=model_creator,
                                                optimizer=get_optimizer,
                                                loss=nn.BCELoss(),
                                                logs_dir="/tmp/zoo_automl_logs",
                                                resources_per_trial={"cpu": 2},
                                                name="test_fit")
        >>> auto_est.fit(data=data,
                         validation_data=validation_data,
                         search_space=create_linear_search_space(),
                         n_sampling=4,
                         epochs=1,
                         metric="accuracy")
        >>> best_model = auto_est.get_best_model()
    """

    def __init__(self,
                 model_builder: "ModelBuilder",
                 logs_dir: str="/tmp/auto_estimator_logs",
                 resources_per_trial: Optional[Dict[str, int]]=None,
                 remote_dir: Optional[str]=None,
                 name: Optional[str]=None) -> None:
        self.model_builder = model_builder
        self.searcher = SearchEngineFactory.create_engine(
            backend="ray",
            logs_dir=logs_dir,
            resources_per_trial=resources_per_trial,
            remote_dir=remote_dir,
            name=name)
        self._fitted = False
        self.best_trial = None

    @staticmethod
    def from_torch(*,
                   model_creator: Callable,
                   optimizer: Callable,
                   loss: Callable,
                   logs_dir: str="/tmp/auto_estimator_logs",
                   resources_per_trial: Optional[Dict[str, int]]=None,
                   name: str="auto_pytorch_estimator",
                   remote_dir: Optional[str]=None,
                   ) -> "AutoEstimator":
        """
        Create an AutoEstimator for torch.

        :param model_creator: PyTorch model creator function.
        :param optimizer: PyTorch optimizer creator function or pytorch optimizer name (string).
            Note that you should specify learning rate search space with key as "lr" or LR_NAME
            (from bigdl.orca.automl.pytorch_utils import LR_NAME) if input optimizer name.
            Without learning rate search space specified, the default learning rate value of 1e-3
            will be used for all estimators.
        :param loss: PyTorch loss instance or PyTorch loss creator function
            or pytorch loss name (string).
        :param logs_dir: Local directory to save logs and results. It defaults to
            "/tmp/auto_estimator_logs"
        :param resources_per_trial: Dict. resources for each trial. e.g. {"cpu": 2}.
        :param name: Name of the auto estimator. It defaults to "auto_pytorch_estimator"
        :param remote_dir: String. Remote directory to sync training results and checkpoints. It
            defaults to None and doesn't take effects while running in local. While running in
            cluster, it defaults to "hdfs:///tmp/{name}".

        :return: an AutoEstimator object.
        """
        from bigdl.orca.automl.model.base_pytorch_model import PytorchModelBuilder
        model_builder = PytorchModelBuilder(model_creator=model_creator,
                                            optimizer_creator=optimizer,
                                            loss_creator=loss)

        return AutoEstimator(model_builder=model_builder,
                             logs_dir=logs_dir,
                             resources_per_trial=resources_per_trial,
                             remote_dir=remote_dir,
                             name=name)

    @staticmethod
    def from_keras(*,
                   model_creator: Callable,
                   logs_dir: str="/tmp/auto_estimator_logs",
                   resources_per_trial: Optional[Dict[str, int]]=None,
                   name: str="auto_keras_estimator",
                   remote_dir: Optional[str]=None,
                   ) -> "AutoEstimator":
        """
        Create an AutoEstimator for tensorflow keras.

        :param model_creator: Tensorflow keras model creator function.
        :param logs_dir: Local directory to save logs and results. It defaults to
            "/tmp/auto_estimator_logs"
        :param resources_per_trial: Dict. resources for each trial. e.g. {"cpu": 2}.
        :param name: Name of the auto estimator. It defaults to "auto_keras_estimator"
        :param remote_dir: String. Remote directory to sync training results and checkpoints. It
            defaults to None and doesn't take effects while running in local. While running in
            cluster, it defaults to "hdfs:///tmp/{name}".

        :return: an AutoEstimator object.
        """
        from bigdl.orca.automl.model.base_keras_model import KerasModelBuilder
        model_builder = KerasModelBuilder(model_creator=model_creator)
        return AutoEstimator(model_builder=model_builder,
                             logs_dir=logs_dir,
                             resources_per_trial=resources_per_trial,
                             remote_dir=remote_dir,
                             name=name)

    def fit(self,
            data: Union[Callable, Tuple["ndarray", "ndarray"], "DataFrame"],
            epochs: int=1,
            validation_data: Optional[
                Union[Callable, Tuple["ndarray", "ndarray"], "DataFrame"]]=None,
            metric: Optional[Union[Callable, str]]=None,
            metric_mode: Optional[str]=None,
            metric_threshold: Optional[Union["Function", "Float", "Integer"]]=None,
            n_sampling: int=1,
            search_space: Optional[Dict]=None,
            search_alg: Optional[str]=None,
            search_alg_params: Optional[Dict]=None,
            scheduler: Optional[str]=None,
            scheduler_params: Optional[Dict]=None,
            feature_cols: Optional[List[str]]=None,
            label_cols: Optional[List[str]]=None,
            ) -> None:
        """
        Automatically fit the model and search for the best hyperparameters.

        :param data: train data.
            If the AutoEstimator is created with from_torch, data can be a tuple of
            ndarrays or a PyTorch DataLoader or a function that takes a config dictionary as
            parameter and returns a PyTorch DataLoader.
            If the AutoEstimator is created with from_keras, data can be a tuple of
            ndarrays or a function that takes a config dictionary as
            parameter and returns a Tensorflow Dataset.
            If data is a tuple of ndarrays, it should be in the form of (x, y),
            where x is training input data and y is training target data.
        :param epochs: Max number of epochs to train in each trial. Defaults to 1.
            If you have also set metric_threshold, a trial will stop if either it has been
            optimized to the metric_threshold or it has been trained for {epochs} epochs.
        :param validation_data: Validation data. Validation data type should be the same as data.
        :param metric: String or customized evaluation metric function.
            If string, metric is the evaluation metric name to optimize, e.g. "mse".
            If callable function, it signature should be func(y_true, y_pred), where y_true and
            y_pred are numpy ndarray. The function should return a float value as evaluation result.
        :param metric_mode: One of ["min", "max"]. "max" means greater metric value is better.
            You have to specify metric_mode if you use a customized metric function.
            You don't have to specify metric_mode if you use the built-in metric in
            bigdl.orca.automl.metrics.Evaluator.
        :param metric_threshold: a trial will be terminated when metric threshold is met
        :param n_sampling: Number of times to sample from the search_space. Defaults to 1.
            If hp.grid_search is in search_space, the grid will be repeated n_sampling of times.
            If this is -1, (virtually) infinite samples are generated
            until a stopping condition is met.
        :param search_space: a dict for search space
        :param search_alg: str, all supported searcher provided by ray tune
               (i.e."variant_generator", "random", "ax", "dragonfly", "skopt",
               "hyperopt", "bayesopt", "bohb", "nevergrad", "optuna", "zoopt" and
               "sigopt")
        :param search_alg_params: extra parameters for searcher algorithm besides search_space,
            metric and searcher mode
        :param scheduler: str, all supported scheduler provided by ray tune
        :param scheduler_params: parameters for scheduler
        :param feature_cols: feature column names if data is Spark DataFrame.
        :param label_cols: target column names if data is Spark DataFrame.
        """
        if self._fitted:
            invalidInputError(False,
                              "This AutoEstimator has already been fitted and cannot fit again.")

        metric_mode = AutoEstimator._validate_metric_mode(metric, metric_mode)
        feature_cols, label_cols = AutoEstimator._check_spark_dataframe_input(data,
                                                                              validation_data,
                                                                              feature_cols,
                                                                              label_cols)

        self.searcher.compile(data=data,
                              model_builder=self.model_builder,
                              epochs=epochs,
                              validation_data=validation_data,
                              metric=metric,
                              metric_mode=metric_mode,
                              metric_threshold=metric_threshold,
                              n_sampling=n_sampling,
                              search_space=search_space,
                              search_alg=search_alg,
                              search_alg_params=search_alg_params,
                              scheduler=scheduler,
                              scheduler_params=scheduler_params,
                              feature_cols=feature_cols,
                              label_cols=label_cols)
        self.searcher.run()
        self._fitted = True

    def get_best_model(self):
        """
        Return the best model found by the AutoEstimator

        :return: the best model instance
        """
        if not self.best_trial:
            self.best_trial = self.searcher.get_best_trial()
        best_model_path = self.best_trial.model_path
        best_config = self.best_trial.config
        best_automl_model = self.model_builder.build(best_config)
        best_automl_model.restore(best_model_path)
        return best_automl_model.model

    def get_best_config(self):
        """
        Return the best config found by the AutoEstimator

        :return: A dictionary of best hyper parameters
        """
        if not self.best_trial:
            self.best_trial = self.searcher.get_best_trial()
        best_config = self.best_trial.config
        return best_config

    def _get_best_automl_model(self):
        """
        This is for internal use only.
        Return the best automl model found by the AutoEstimator

        :return: an automl base model instance
        """
        if not self.best_trial:
            self.best_trial = self.searcher.get_best_trial()
        best_model_path = self.best_trial.model_path
        best_config = self.best_trial.config
        best_automl_model = self.model_builder.build(best_config)
        best_automl_model.restore(best_model_path)
        return best_automl_model

    @staticmethod
    def _validate_metric_mode(metric: Optional[Union[Callable, str]],
                              mode: Optional[str]) -> Optional[str]:
        if not mode:
            if callable(metric):
                invalidInputError(False,
                                  "You must specify `metric_mode` for your metric function")
            try:
                from bigdl.orca.automl.metrics import Evaluator
                mode = Evaluator.get_metric_mode(metric)
            except ValueError:
                pass
            if not mode:
                invalidInputError(False,
                                  f"We cannot infer metric mode with metric name of {metric}."
                                  f" Please specify the `metric_mode` parameter in"
                                  f" AutoEstimator.fit().")
        if mode not in ["min", "max"]:
            invalidInputError(False,
                              "`mode` has to be one of ['min', 'max']")
        return mode

    @staticmethod
    def _check_spark_dataframe_input(data: Union[Tuple["ndarray", "ndarray"],
                                                 Callable, "DataFrame"],
                                     validation_data: Union[Tuple["ndarray", "ndarray"],
                                                            Callable, "DataFrame"],
                                     feature_cols: Optional[List[str]],
                                     label_cols: Optional[List[str]]
                                     ) -> Tuple[Optional[List[str]], Optional[List[str]]]:

        def check_cols(cols, cols_name):
            if not cols:
                invalidInputError(False,
                                  f"You must input valid {cols_name} for Spark DataFrame"
                                  f" data input")
            if isinstance(cols, list):
                return cols
            if not isinstance(cols, str):
                invalidInputError(False,
                                  f"{cols_name} should be a string or a list of strings, "
                                  f"but got {type(cols)}")
            return [cols]

        from pyspark.sql import DataFrame
        if isinstance(data, DataFrame):
            feature_cols = check_cols(feature_cols, cols_name="feature_cols")
            label_cols = check_cols(label_cols, cols_name="label_cols")
            if validation_data:
                if not isinstance(validation_data, DataFrame):
                    invalidInputError(False,
                                      f"data and validation_data should be both Spark DataFrame, "
                                      f"but got validation_data of type {type(data)}")
        return feature_cols, label_cols
