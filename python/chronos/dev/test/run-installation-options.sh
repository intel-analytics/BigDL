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

cd "`dirname $0`"
cd ../..

export PYSPARK_PYTHON=python
export PYSPARK_DRIVER_PYTHON=python

# If init bigdl-nano, unset `MALLOC_CONF` and `OMP_NUM_THREADS` to avoid process kill
unset MALLOC_CONF
export OMP_NUM_THREADS=2

# ray stop -f

OPTIONS=$1
echo "Running chronos tests"
if [ -z "$OMP_NUM_THREADS" ]; then
    echo "OMP_NUM_THREADS is unset"
else
    echo $OMP_NUM_THREADS
fi
python -m pytest -v -m "${OPTIONS}" test/bigdl/chronos/autots \
                                    test/bigdl/chronos/data \
                                    test/bigdl/chronos/detector \
                                    test/bigdl/chronos/forecaster \
                                    test/bigdl/chronos/metric \
                                    test/bigdl/chronos/model \
                                    test/bigdl/chronos/pytorch \
                                    test/bigdl/chronos/simulator \
       -k "not test_tcn_keras_forecaster_quantization"

exit_status_0=$?
if [ $exit_status_0 -ne 0 ];
then
    exit $exit_status_0
fi

# When test [tensorflow,inference] option, need to trigger this ut
if [[ ${OPTIONS} =~ "tf2" ]] && ! [[ ${OPTIONS} =~ "not inference" ]]; then
    python -m pytest -s test/bigdl/chronos/forecaster/tf/\
test_tcn_keras_forecaster.py::TestTCNForecaster::test_tcn_keras_forecaster_quantization
fi

exit_status_0=$?
if [ $exit_status_0 -ne 0 ];
then
    exit $exit_status_0
fi

# ray stop -f
