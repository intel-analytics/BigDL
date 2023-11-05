# BigDL-LLM Transformers INT4 Optimization for HuggingFace Transformers Agent
In this example, we apply low-bit optimizations to [HuggingFace Transformers Agents](https://huggingface.co/docs/transformers/transformers_agents) using BigDL-LLM, which allows LLMs to use tools such as image generation, image captioning, text summarization, etc.

For illustration purposes, we utilize the [lmsys/vicuna-7b-v1.5](https://huggingface.co/lmsys/vicuna-7b-v1.5) as the reference model. We use [lmsys/vicuna-7b-v1.5](https://huggingface.co/lmsys/vicuna-7b-v1.5) to create an agent, and then ask the agent to generate the caption for an image from coco dataset, i.e. [demo.jpg](https://cocodataset.org/#explore?id=264959)

## 0. Requirements
To run this example with BigDL-LLM, we have some recommended requirements for your machine, please refer to [here](https://github.com/intel-analytics/BigDL/tree/main/python/llm/example/CPU/HF-Transformers-AutoModels/Model#recommended-requirements) for more information.


### 1. Install
We suggest using conda to manage environment:
```bash
conda create -n llm python=3.9
conda activate llm

pip install bigdl-llm[all] # install bigdl-llm with 'all' option
pip install pillow # additional package required for opening images
```

### 2. Run
```
python ./run_agent.py --repo-id-or-model-path REPO_ID_OR_MODEL_PATH --image-path IMAGE_PATH
```

Arguments info:
- `--repo-id-or-model-path REPO_ID_OR_MODEL_PATH`: argument defining the huggingface repo id for the Vicuna model (e.g. `lmsys/vicuna-7b-v1.5`) to be downloaded, or the path to the huggingface checkpoint folder. It is default to be `'lmsys/vicuna-7b-v1.5'`.
- `--image-path IMAGE_PATH`: argument defining the image to be infered.

> **Note**: When loading the model in 4-bit, BigDL-LLM converts linear layers in the model into INT4 format. In theory, a *X*B model saved in 16-bit will requires approximately 2*X* GB of memory for loading, and ~0.5*X* GB memory for further inference.
>
> Please select the appropriate size of the Vicuna model based on the capabilities of your machine.

#### 2.1 Client
On client Windows machine, it is recommended to run directly with full utilization of all cores:
```powershell
python ./run_agent.py --image-path IMAGE_PATH
```

#### 2.2 Server
For optimal performance on server, it is recommended to set several environment variables (refer to [here](../README.md#best-known-configuration-on-linux) for more information), and run the example with all the physical cores of a single socket.

E.g. on Linux,
```bash
# set BigDL-Nano env variables
source bigdl-nano-init

# e.g. for a server with 48 cores per socket
export OMP_NUM_THREADS=48
numactl -C 0-47 -m 0 python ./run_agent.py --image-path IMAGE_PATH
```

#### 2.3 Sample Output
#### [demo.jpg](https://cocodataset.org/#explore?id=264959)
<p align="center">
<img src="http://farm6.staticflickr.com/5268/5602445367_3504763978_z.jpg" alt="demo.jpg" width="400"/>
</p>

#### [lmsys/vicuna-7b-v1.5](https://huggingface.co/lmsys/vicuna-7b-v1.5)
```log
Image path: demo.jpg
== Prompt ==
Generate a caption for the 'image'
==Explanation from the agent==
I will use the following tool: `image_captioner` to generate a caption for the image.


==Code generated by the agent==
caption = image_captioner(image)


==Result==
a little girl holding a stuffed teddy bear
```