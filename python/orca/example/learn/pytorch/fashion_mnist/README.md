# PyTorch Fashion-MNIST example with Tensorboard visualization
We demonstrate how to easily show the graphical results of running synchronous distributed PyTorch training using PyTorch Estimator of Project Orca in BigDL. We use a simple convolutional nueral network model to train on fashion-MNIST dataset. See [here](https://pytorch.org/tutorials/intermediate/tensorboard_tutorial.html) for the original single-node version of this example provided by PyTorch. We provide two distributed PyTorch training backends for this example, namely "spark" and "ray".

## Prepare the environment

We recommend you to use Anaconda to prepare the environment, especially if you want to run on a yarn cluster:

```
conda create -n bigdl python=3.7  # "bigdl" is conda environment name, you can use any name you like.
conda activate bigdl
pip install torch
pip install torchvision
pip install matplotlib
pip install tensorboard

# For spark backend
pip install bigdl-orca
pip install tqdm  # progress bar

# For ray backend
pip install bigdl-orca[ray]
pip install tqdm  # progress bar
```

## Run on local after pip install

The default backend is `spark`:

```
python fashion_mnist.py
```

You can run with `ray` backend via:

```
python fashion_mnist.py --backend ray
```

To see the result figures after it finishes:

```
tensorboard --logdir=runs
```

Then open `https://localhost:6006`.


## Run on yarn cluster for yarn-client mode after pip install

```
export HADOOP_CONF_DIR=the directory of the hadoop and yarn configurations
python fashion_mnist.py --cluster_mode yarn
```

Then open `https://localhost:6006` on the local client machine to see the result figures.

The default backend is `spark`. You can also run with `ray` by specifying the backend.

## Results

**For "ray" and "spark" backend**

You can find the results of training and validation as follows:

```
Train stats: [{'num_samples': 60000, 'epoch': 1, 'batch_count': 15000, 'train_loss': 0.6387080065780457, 'last_train_loss': 0.17801283299922943}, {'num_samples': 60000, 'epoch': 2, 'batch_count': 15000, 'train_loss': 0.372230169281755, 'last_train_loss': 0.19179978966712952}, {'num_samples': 60000, 'epoch': 3, 'batch_count': 15000, 'train_loss': 0.32247564417196833, 'last_train_loss': 0.30726122856140137}, {'num_samples': 60000, 'epoch': 4, 'batch_count': 15000, 'train_loss': 0.2959285915141232, 'last_train_loss': 0.2786743640899658}, {'num_samples': 60000, 'epoch': 5, 'batch_count': 15000, 'train_loss': 0.27712880933261197, 'last_train_loss': 0.2697388529777527}]

Validation stats: {'num_samples': 10000, 'Accuracy': tensor(0.8788), 'val_loss': 0.34675604103680596}
```