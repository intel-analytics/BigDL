#
# Copyright 2021 The BigDL Authors.
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
<<<<<<< HEAD

import pandas as pd
import numpy as np
from bigdl.ppml import *


def get_input_type(x, y=None):
    if isinstance(x, pd.DataFrame):
        if y is not None and not isinstance(y, pd.DataFrame):
            raise ValueError(f"Feature is DataFrame, label should be DataFrame, but got {type(y)}")
        return "DataFrame"
    elif isinstance(x, np.ndarray):
        if y is not None and not isinstance(y, np.ndarray):
            raise ValueError(
                f"Feature is Numpy NdArray, label should be Numpy NdArray, but got {type(y)}")
        return "NdArray"
    else:
        raise ValueError(f"Supported argument types: DataFrame, NdArray, but got {type(x)}")

def convert_to_numpy(x, y=None, feature_columns=None, label_columns=None):
=======
import pandas as pd
import numpy as np


def convert_to_numpy(x, dataframe_columns=None):
>>>>>>> upstream/branch-2.0
    """
    :param x: The input to convert
    :param dataframe_columns: applicable if x is pandas.DataFrame, the column to convert
    :return: the converted numpy.ndarray
    """
<<<<<<< HEAD
    arg_type = get_input_type(x, y)
    if arg_type == "DataFrame":
        x = [x[col] for col in feature_columns]
        y = [y[col] for col in label_columns] if y else None
    return JTensor.from_ndarray(x), JTensor.from_ndarray(y)

=======
    if isinstance(x, pd.DataFrame):
        return [x[col] for col in dataframe_columns]
    elif isinstance(x, np.ndarray):
        return [x]
    elif isinstance(x, list):
        for e in x:
            if not isinstance(x, np.ndarray):
                raise Exception("only NdArray type is supported for list input")
        return x
    else:
        raise Exception("Input could be Pandas DataFrame or Numpy NDArray or list of NDArray, but got", type(x))
>>>>>>> upstream/branch-2.0


