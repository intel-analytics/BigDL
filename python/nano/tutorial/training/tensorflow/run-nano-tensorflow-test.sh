export ANALYTICS_ZOO_ROOT=${ANALYTICS_ZOO_ROOT}
export NANO_HOME=${ANALYTICS_ZOO_ROOT}/python/nano/src
export NANO_TUTORIAL_TEST_DIR=${ANALYTICS_ZOO_ROOT}/python/nano/tutorial/training/tensorflow

set -e

export NUM_EPOCHS=1

# the number of batches to run is limited for testing purposes
sed -i 's/steps_per_epoch=steps_per_epoch/steps_per_epoch=(steps_per_epoch \/\/ 10)/' $NANO_TUTORIAL_TEST_DIR/tensorflow_train_multi_instance.py

python $NANO_TUTORIAL_TEST_DIR/tensorflow_sparse_embedding.py
python $NANO_TUTORIAL_TEST_DIR/tensorflow_train_multi_instance.py
