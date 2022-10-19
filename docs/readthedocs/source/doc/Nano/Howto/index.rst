Nano How-to Guides
=========================
.. note::
    This page is still a work in progress. We are adding more guides.

In Nano How-to Guides, you could expect to find multiple task-oriented, bite-sized, and executable examples. These examples will show you various tasks that BigDL-Nano could help you accomplish smoothly.

Training Optimization
-------------------------

PyTorch Lightning
~~~~~~~~~~~~~~~~~~~~~~~~~
* `How to accelerate a PyTorch Lightning application on training workloads through Intel® Extension for PyTorch* <Training/PyTorchLightning/accelerate_pytorch_lightning_training_ipex.html>`_
* `How to accelerate a PyTorch Lightning application on training workloads through multiple instances <Training/PyTorchLightning/accelerate_pytorch_lightning_training_multi_instance.html>`_
* `How to use the channels last memory format in your PyTorch Lightning application for training <Training/PyTorchLightning/pytorch_lightning_training_channels_last.html>`_
* `How to conduct BFloat16 Mixed Precision training in your PyTorch Lightning application <Training/PyTorchLightning/pytorch_lightning_training_bf16.html>`_
* `How to accelerate a computer vision data processing pipeline <Training/PyTorchLightning/pytorch_lightning_cv_data_pipeline.html>`_

.. toctree::
    :maxdepth: 1
    :hidden:

    Training/PyTorchLightning/accelerate_pytorch_lightning_training_ipex
    Training/PyTorchLightning/accelerate_pytorch_lightning_training_multi_instance
    Training/PyTorchLightning/pytorch_lightning_training_channels_last
    Training/PyTorchLightning/pytorch_lightning_training_bf16
    Training/PyTorchLightning/pytorch_lightning_cv_data_pipeline

TensorFlow
~~~~~~~~~~~~~~~~~~~~~~~~~
* `How to accelerate a TensorFlow Keras application on training workloads through multiple instances <Training/TensorFlow/accelerate_tensorflow_training_multi_instance.html>`_
* |tensorflow_training_embedding_sparseadam_link|_

.. |tensorflow_training_embedding_sparseadam_link| replace:: How to optimize your model with a sparse ``Embedding`` layer and ``SparseAdam`` optimizer
.. _tensorflow_training_embedding_sparseadam_link: Training/TensorFlow/tensorflow_training_embedding_sparseadam.html

.. toctree::
    :maxdepth: 1
    :hidden:

    Training/TensorFlow/accelerate_tensorflow_training_multi_instance
    Training/TensorFlow/tensorflow_training_embedding_sparseadam

General
~~~~~~~~~~~~~~~~~~~~~~~~~
* `How to choose the number of processes for multi-instance training <Training/General/choose_num_processes_training.html>`_

.. toctree::
    :maxdepth: 1
    :hidden:

    Training/General/choose_num_processes_training


Inference Optimization
-------------------------

PyTorch
~~~~~~~~~~~~~~~~~~~~~~~~~

* `How to accelerate a PyTorch inference pipeline through ONNXRuntime <Inference/PyTorch/accelerate_pytorch_inference_onnx.html>`_
* `How to accelerate a PyTorch inference pipeline through OpenVINO <Inference/PyTorch/accelerate_pytorch_inference_openvino.html>`_
* `How to quantize your PyTorch model for inference using Intel Neural Compressor <Inference/PyTorch/quantize_pytorch_inference_inc.html>`_
* `How to quantize your PyTorch model for inference using OpenVINO Post-training Optimization Tools <Inference/PyTorch/quantize_pytorch_inference_pot.html>`_
* `How to find accelerated method with minimal latency using InfereceOptimizer <Inference/PyTorch/inference_optimizer_optimize.html>`_

.. toctree::
    :maxdepth: 1
    :hidden:

    Inference/PyTorch/accelerate_pytorch_inference_onnx
    Inference/PyTorch/accelerate_pytorch_inference_openvino
    Inference/PyTorch/quantize_pytorch_inference_inc
    Inference/PyTorch/quantize_pytorch_inference_pot
    Inference/PyTorch/inference_optimizer_optimize

Install
-------------------------
* `How to install BigDL-Nano in Google Colab <install_in_colab.html>`_
* `How to install BigDL-Nano on Windows <windows_guide.html>`_

.. toctree::
    :maxdepth: 1
    :hidden:

    install_in_colab
    windows_guide