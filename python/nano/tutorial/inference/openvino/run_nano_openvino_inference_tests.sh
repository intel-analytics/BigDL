#!/bin/bash

export ANALYTICS_ZOO_ROOT=${ANALYTICS_ZOO_ROOT}
export NANO_HOME=${ANALYTICS_ZOO_ROOT}/python/nano/src
export NANO_TUTORIAL_TEST_DIR=${ANALYTICS_ZOO_ROOT}/python/nano/tutorial/inference/openvino/

set -e

python $NANO_TUTORIAL_TEST_DIR/openvino_inference_sync.py

python $NANO_TUTORIAL_TEST_DIR/openvino_inference_async.py

