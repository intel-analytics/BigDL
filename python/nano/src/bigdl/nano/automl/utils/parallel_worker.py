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
import sys
import json
import cloudpickle

from pytorch_lightning.utilities.seed import reset_seed

if __name__ == '__main__':
    temp_dir = SafeLoad.safe_load(sys.argv[1])
    # process path traversal issue
    safe_dir = "/safe_dir/"
    dir_name = os.path.dirname(temp_dir)
    if '../' in dir_name:
        sys.exit(1)
    safe_dir = dir_name
    file_name = os.path.basename(temp_dir)
    temp_dir = os.path.join(safe_dir, file_name)
    with open(os.path.join(temp_dir, "search_kwargs.json"), 'r') as f:
        kwargs = json.load(f)
    with open(os.path.join(temp_dir, "search_func.pkl"), 'rb') as f:
        func = cloudpickle.load(f)

    # do we need to reset seed?
    # reset_seed()
    func(**kwargs)
