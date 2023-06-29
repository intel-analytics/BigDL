#!/bin/bash

export ANALYTICS_ZOO_ROOT=${ANALYTICS_ZOO_ROOT}
export LLM_HOME=${ANALYTICS_ZOO_ROOT}/python/llm/src
export LLM_INFERENCE_TEST_DIR=${ANALYTICS_ZOO_ROOT}/python/llm/test/langchain
set -e

echo "# Start testing langchain"
start=$(date "+%s")

echo $BLOOM_INT4_CKPT_PATH
python -m pytest -s ${LLM_INFERENCE_TEST_DIR} -k 'test_langchain_llm_bloom'

now=$(date "+%s")
time=$((now-start))

echo "Bigdl-llm langchain tests finished"
echo "Time used:$time seconds"