#!/bin/bash

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
#set -x
port=${port:-no_port}
token=${token:-no_token}

while [ $# -gt 0 ]; do

   if [[ $1 == *"--"* ]]; then
        param="${1/--/}"
        declare $param="$2"
   fi

  shift
done
echo $port $token

if [[ $port = "no_port" || -z $port ]]
then
    echo "the --port parameter should be a int value, and cannot be empty!"
    exit 1
elif [[ $token = "no_token" || -z $token ]]
then
    echo "the --token parameter should be a string value, and cannot be empty!"
    exit 1
fi

echo $BIGDL_HOME
jupyter-lab --notebook-dir=$BIGDL_HOME/apps --ip=0.0.0.0 --port=$port --no-browser --NotebookApp.token=$token --allow-root

