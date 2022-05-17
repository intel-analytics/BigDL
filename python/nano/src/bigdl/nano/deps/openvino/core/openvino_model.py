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
from pathlib import Path
from tempfile import TemporaryDirectory
from openvino.runtime import Core
from bigdl.nano.utils.log4Error import invalidInputError
from openvino.tools.pot.graph import load_model, save_model
from openvino.tools.pot.engines.ie_engine import IEEngine
from openvino.tools.pot.pipeline.initializer import create_pipeline
from openvino.tools.pot.graph.model_utils import compress_model_weights
from .openvino_utils import save
from .openvino_utils import validate_dataloader
import copy


class OpenVINOModel:
    def __init__(self, ie_network: str):
        self.ie_network = None
        self.read_network(ie_network)

    def forward_step(self, *inputs):
        return self.infer_request.infer(list(inputs))

    def read_network(self, model: str):
        core = Core()
        self.ie_network = core.read_model(model=model)
        self.exec_model = core.compile_model(model=self.ie_network, device_name='CPU')
        self.infer_request = self.exec_model.create_infer_request()

    def _save_model(self, path):
        """
        Save PytorchOpenVINOModel to local as xml and bin file

        :param path: Path to save the model.
        """
        path = Path(path)
        invalidInputError(self.ie_network,
                          "self.ie_network shouldn't be None.")
        invalidInputError(path.suffix == ".xml",
                          "Path of openvino model must be with '.xml' suffix.")
        path.mkdir(exist_ok=True)
        xml_path = path / self.status['xml_path']
        save(self.ie_network, xml_path)

    def pot(self,
            dataloader,
            metric=None,
            drop_type="relative",
            maximal_drop=0.999,
            max_iter_num=1,
            n_requests=None,
            sample_size=300):

        # set batch as 1 if it's dynaminc or larger than 1
        orig_shape = dict()
        static_shape = dict()
        for i, input_obj in enumerate(self.ie_network.inputs):
            orig_shape[i] = input_obj.get_partial_shape()
            shape = input_obj.get_partial_shape()
            # modify dynamic axis to 1 if it's batch dimension
            shape[0] = 1
            static_shape[i] = shape
        self.ie_network.reshape(static_shape)

        # pot has its own model format, so we need to save and reload by pot
        with TemporaryDirectory() as dir:
            dir = Path(dir)
            save(self.ie_network, str(dir / 'model.xml'))

            # Convert model back to original shape
            self.ie_network.reshape(orig_shape)

            model_config = {
                "model_name": "model",
                "model": str(dir / 'model.xml'),
                "weights": str(dir / 'model.bin')
            }
            model = load_model(model_config)

        engine_config = {"device": "CPU",
                         "stat_requests_number": n_requests,
                         "eval_requests_number": n_requests}
        engine = IEEngine(config=engine_config, data_loader=dataloader, metric=metric)

        algorithms = [
            {
                "name": "DefaultQuantization",
                "params": {
                    "target_device": "CPU",
                    "preset": "performance",
                    "stat_subset_size": sample_size,
                },
            }
        ]
        if metric:
            algorithms = [
                {
                    "name": "AccuracyAwareQuantization",
                    "params": {
                        "target_device": "CPU",
                        "preset": "performance",
                        "stat_subset_size": sample_size,
                        "maximal_drop": maximal_drop,
                        "max_iter_num": max_iter_num,
                        "drop_type": drop_type,
                    },
                }
            ]

        pipeline = create_pipeline(algorithms, engine)
        compressed_model = pipeline.run(model=model)
        compress_model_weights(model=compressed_model)

        # To use runtime, we need to save and reload
        # returned a list of paths, but for now there is only one model path in list
        dir = "optimized_model"
        compressed_model_paths = save_model(
            model=compressed_model,
            save_path=dir,
            model_name='model'
        )
        # set batch for compressed model
        model_path = compressed_model_paths[0]['model']
        model = Core().read_model(model_path)
        model.reshape(orig_shape)
        save(model, model_path)
        return model_path
