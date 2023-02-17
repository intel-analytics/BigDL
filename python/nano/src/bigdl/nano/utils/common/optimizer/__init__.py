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


from .latency import latency_calculate_helper

from .acceleration_option import AccelerationOption
from .acceleration_option import available_acceleration_combination

from .format import format_acceleration_option
from .format import format_optimize_result

from .metric import CompareMetric

from .optimizer import BaseInferenceOptimizer

from .acceleration_env import AccelerationEnv
from .exec_with_worker import exec_with_worker
