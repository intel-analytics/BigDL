#!/usr/bin/env bash

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

# This is the default script with maven parameters to release all the bigdl sub-packages
# built on top of Spark2(2.4.6) for linux.

set -e
RUN_SCRIPT_DIR=$(cd $(dirname $0) ; pwd)
echo $RUN_SCRIPT_DIR
BIGDL_DIR="$(cd ${RUN_SCRIPT_DIR}/../..; pwd)"
echo $BIGDL_DIR

if (( $# < 3)); then
  echo "Usage: release_default_linux_spark2.sh version quick_build upload suffix mvn_parameters"
  echo "Usage example: bash release_default_linux_spark2.sh default false true true"
  echo "Usage example: bash release_default_linux_spark2.sh 0.14.0.dev1 false false true"
  echo "Usage example: bash release_default_linux_spark2.sh 0.14.0.dev1 false false false -Ddata-store-url=.."
  exit -1
fi

version=$1
quick=$2
upload=$3
if (( $# < 4)); then
  suffix=false
  profiles=${*:4}
else
  suffix=$4
  profiles=${*:5}
fi

bash ${RUN_SCRIPT_DIR}/release_default_spark.sh linux ${version} ${quick} ${upload} 2.4.6 ${suffix} ${profiles}
