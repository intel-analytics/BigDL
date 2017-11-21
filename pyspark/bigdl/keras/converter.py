
import bigdl.nn.initialization_method as BInit
import numpy as np
import bigdl.nn.layer as BLayer
from bigdl.optim.optimizer import L1L2Regularizer as BRegularizer
import bigdl.optim.optimizer as boptimizer
import bigdl.nn.criterion as bcriterion
import bigdl.util.common as bcommon
import keras.optimizers as koptimizers
from keras.models import model_from_json
from keras.models import Sequential, Model
import keras
import warnings


def unsupport_exp(name):
    raise Exception("We don't support %s for now" % name)


class WeightLoader:

    @staticmethod
    def __load_weights_by_execution_seq(bmodel, kmodel):
        blayers = [l for l in bmodel.layers if l.is_with_weights()]
        klayers = [l for l in kmodel.layers if l.get_weights()]
        if len(blayers) != len(klayers):
            raise Exception(
                "keras with %s layers but bigdl with %s layers" % (len(klayers), len(blayers)))
        for b, k in zip(blayers, klayers):
            if b.name() != k.name:
                raise Exception("Found different layer in execution order, bigdl:%s, keras: %s" % (b.name(), k.name))  # noqa
            bigdl_weights = WeightsConverter.get_bigdl_weigths_from_keras(k)
            b.set_weights(bigdl_weights)

    # TODO: add more unitest
    @staticmethod
    def __load_weights_by_name(bmodel, kmodel, by_name=False):
        keras_name_to_layer = WeightLoader.__keras_name_to_Layers(kmodel, with_weights=True)
        bigdl_name_to_layer = WeightLoader.__bigdl_name_to_Layers(bmodel, with_weights=True)
        layers_not_in_keras = set(bigdl_name_to_layer.keys()) - set(keras_name_to_layer.keys())
        if layers_not_in_keras:
            raise Exception("Layers %s can be found in bigdl, but not in keras" % repr(layers_not_in_keras))  # noqa
        layers_not_in_bigdl = set(keras_name_to_layer.keys()) - set(bigdl_name_to_layer.keys())
        if layers_not_in_bigdl:
            if by_name:
                warnings.warn("Ignore weight of layers %s as it cannot be found in bigdl" % repr(layers_not_in_bigdl))  # noqa
            else:
                raise Exception("Layers %s can be found in bigdl, but not in keras" % repr(layers_not_in_keras))  # noqa
        for blayer in bigdl_name_to_layer.values():
            if blayer.name() in keras_name_to_layer:
                klayer = keras_name_to_layer[blayer.name()]
                bigdl_weights = WeightsConverter.get_bigdl_weigths_from_keras(klayer)
                blayer.set_weights(bigdl_weights)
                if isinstance(klayer, keras.layers.BatchNormalization):
                    blayer.set_running_mean(keras.backend.eval(klayer.running_mean))
                    blayer.set_running_std(keras.backend.eval(klayer.running_std))

    @staticmethod
    def load_weights_from_kmodel(bmodel, kmodel, by_name=False):
        """
        Load weights from kmodel to bmodel
        """
        if by_name:
            WeightLoader.__load_weights_by_name(bmodel, kmodel)
        else:
            WeightLoader.__load_weights_by_execution_seq(bmodel, kmodel)

    @staticmethod
    def load_weights_from_json_hdf5(def_json, weights_hdf5, by_name=False):
        with open(def_json, "r") as jp:
            kmodel = model_from_json(jp.read())
        bmodel = DefinitionLoader.from_json_path(def_json)
        WeightLoader.load_weights_from_hdf5(bmodel, kmodel, weights_hdf5, by_name)
        return bmodel

    @staticmethod
    def load_weights_from_hdf5(bmodel, kmodel, filepath, by_name=False):
        '''Loads all layer weights from a HDF5 save file.

        If `by_name` is False (default) weights are loaded
        based on the network's execution order topology,
        meaning layers in the execution seq should be exactly the same
        the architecture

        If `by_name` is True, weights are loaded into layers
        only if they share the same name. This is useful
        for fine-tuning or transfer-learning models where
        some of the layers have changed.
        '''
        kmodel.load_weights(filepath=filepath, by_name=by_name)
        WeightLoader.load_weights_from_kmodel(bmodel, kmodel, by_name=by_name)

    @staticmethod
    def __keras_name_to_Layers(model, with_weights=False):
        if with_weights:
            layers = [l for l in model.layers if l.get_weights()]
        else:
            layers = [l for l in model.layers]

        return dict([(layer.name, layer) for layer in layers])

    @staticmethod
    def __bigdl_name_to_Layers(model, with_weights=False):
        if with_weights:
            layers = [l for l in model.layers if l.is_with_weights()]
        else:
            layers = [l for l in model.layers]

        return dict([(layer.name(), layer) for layer in layers])


class WeightsConverter:
    """
    Convert keras weights to bigdl weights
    The shape of weights would be changed if using different backend,
    so we only test against TensorFlow backend.
    TODO: Support th backend as well.
    """

    @staticmethod
    def get_converter(class_name):
        function_name = "convert_" + class_name.lower()
        if not hasattr(WeightsConverter, function_name):
            raise unsupport_exp(class_name)
        converter = getattr(WeightsConverter, function_name)
        return converter

    @staticmethod
    # weights is a list of ndarray or a ndarray
    # convert keras weights per layer to bigdl format
    def to_bigdl_weights(class_name, weights):
        return WeightsConverter.get_converter(class_name)(weights)

    @staticmethod
    def get_bigdl_weigths_from_keras(k):
        if isinstance(k, keras.engine.Model):
            return WeightsConverter.get_weights_from_kmodel(k)
        elif isinstance(k, keras.engine.Layer):
            return WeightsConverter.get_bigdl_weights_from_klayer(k)
        else:
            raise Exception("Unsupport type: %s", k)

    @staticmethod
    def get_bigdl_weights_from_klayer(klayer):
        # we should use get_weights instead of klayer.weights
        return WeightsConverter.to_bigdl_weights(klayer.__class__.__name__, klayer.get_weights())

    @staticmethod
    def get_weights_from_kmodel(kmodel):
        """
        Convert kmodel's weights to bigdl format.
        We are supposing the order is the same as the execution order.
        :param kmodel: keras model
        :return: list of ndarray
        """
        layers_with_weights = [layer for layer in kmodel.layers if layer.weights]
        bweights = []
        for klayer in layers_with_weights:
            # bws would be [weiths, bias] or [weights]
            bws = WeightsConverter.get_bigdl_weights_from_klayer(klayer)
            for w in bws:
                bweights.append(w)
        return bweights

    @staticmethod
    def convert_dense(weights):
        return [np.transpose(weights[0]), weights[1]]

    @staticmethod
    def convert_timedistributeddense(weights):
        return [np.transpose(weights[0]), weights[1]]

    @staticmethod
    def convert_batchnormalization(weights):
        gamma = weights[0]
        beta = weights[1]
        return [gamma, beta]

    @staticmethod
    def convert_atrousconvolution2d(weights):
        return weights

    @staticmethod
    def convert_atrousconvolution1d(weights):
        return [np.transpose(weights[0], (3, 2, 0, 1)), weights[1]]

    @staticmethod
    def convert_deconvolution2d(weights):
        w = np.transpose(weights[0], (1, 0, 2, 3))
        weight = np.expand_dims(w, 0)
        if len(weights) > 1:
            return [weight, weights[1]]
        else:
            return [weight]

    @staticmethod
    def convert_convolution2d(weights):
        weight = np.expand_dims(weights[0], 0)  # bigdl has a leading dim with value 1
        if len(weights) > 1:
            return [weight, weights[1]]
        else:
            return [weight]

    @staticmethod
    def convert_convolution1d(weights):
        return WeightsConverter.convert_convolution2d(weights)

    @staticmethod
    def convert_convolution3d(weights):
        return weights

    @staticmethod
    def convert_embedding(weights):
        return weights

    @staticmethod
    def convert_simplernn(weights):
        return [np.transpose(weights[0]), np.transpose(weights[1]), weights[2]]

    @staticmethod
    def convert_lstm(weights):
        w1 = np.concatenate((weights[0].T, weights[3].T, weights[6].T, weights[9].T))
        w2 = np.concatenate((weights[2], weights[5], weights[8], weights[11]))
        w3 = np.concatenate((weights[1].T, weights[4].T, weights[7].T, weights[10].T))
        return [w1, w2, w3]

    @staticmethod
    def convert_convlstm2d(weights):
        return [np.expand_dims(weights[6], 0), weights[8], np.expand_dims(weights[7], 0),
                np.expand_dims(weights[0], 0), weights[2], np.expand_dims(weights[1], 0),
                np.expand_dims(weights[3], 0), weights[5], np.expand_dims(weights[4], 0),
                np.expand_dims(weights[9], 0), weights[11], np.expand_dims(weights[10], 0)]

    @staticmethod
    def convert_gru(weights):
        w1 = np.concatenate((weights[3].T, weights[0].T, weights[6].T))
        w2 = np.concatenate((weights[5], weights[2], weights[8]))
        w3 = np.concatenate((weights[4].T, weights[1].T))
        w4 = weights[7].T
        return [w1, w2, w3, w4]


class DefinitionLoader:

    def __init__(self, kmodel):
        self.node_id_to_instance = {}
        self.node_id_to_layer = {}
        self.node_id_to_config_layer = {}
        self.kmodel = kmodel
        self.kconfig = self.kmodel.get_config()

        for layer in self.kmodel.layers:
            self.node_id_to_layer[layer.name] = layer

        if isinstance(self.kmodel, Sequential):
            for layer_config in self.kmodel.get_config():
                layer_name = layer_config["config"]["name"]
                self.node_id_to_config_layer[layer_name] = layer_config
        else:
            for layerConfig in self.kconfig["layers"]:
                self.node_id_to_config_layer[layerConfig["name"]] = layerConfig

    def __to_bigdl(self):
        if isinstance(self.kmodel, Sequential):
            bmodel = self._construct_bigdl_sequence()
        elif isinstance(self.kmodel, Model):
            bmodel = self._construct_bigdl_model()
        return bmodel

    @classmethod
    def from_kmodel(cls, kmodel):
        return cls(kmodel).__to_bigdl()

    @classmethod
    def from_json_path(cls, json_path):
        with open(json_path, "r") as jp:
            return DefinitionLoader.from_json_str(jp.read())

    @classmethod
    def from_json_str(cls, json_str):
        kmodel = model_from_json(json_str)
        return DefinitionLoader.from_kmodel(kmodel)

    def _do_create_node(self, layer, clayer):
        if clayer["class_name"] == "InputLayer":
            input = BLayer.Input()
            input.element().set_name(layer.name) # cannot set name for node?
            self.node_id_to_instance[layer.name] = input
            return input
        bigdl_in_nodes = []
        for node in clayer["inbound_nodes"]:
            for out in node:
                out_name = out[0]
                out_index = out[1]
                out_tensor_index = out[2]
                if out_name not in self.node_id_to_instance:
                    self._do_create_node(self.node_id_to_layer[out_name],
                                         self.node_id_to_config_layer[out_name])
                bigdl_in_nodes.append(self.node_id_to_instance[out_name])

        blayer = LayerConverter().create(layer, clayer)
        new_bnode = blayer(bigdl_in_nodes)
        self.node_id_to_instance[layer.name] = new_bnode
        return new_bnode

    def _construct_bigdl_model(self):
        for clayer in self.kconfig["layers"]:
            if clayer["name"] not in self.node_id_to_instance:

                self._do_create_node(self.node_id_to_layer[clayer["name"]],
                                     clayer)
        ins = []
        for input_layer in self.kconfig["input_layers"]:
            name = input_layer[0]
            ins.append(self.node_id_to_instance[name])
        outs = []
        for output_layer in self.kconfig["output_layers"]:
            name = output_layer[0]
            outs.append(self.node_id_to_instance[name])
        return BLayer.Model(inputs=ins, outputs=outs)

    def _construct_bigdl_sequence(self):
        bseq = BLayer.Sequential()
        layerConverter = LayerConverter()
        for layer in self.kmodel.layers:
            blayer = layerConverter.create(layer, self.node_id_to_config_layer[layer.name])
            bseq.add(blayer)
        return bseq

class LayerConverter:

    def __check_is_share_weights(self, kclayer):
        # For Merge layer len(kclayer["inbound_nodes"]) is equal to 1
        # "inbound_nodes": [
        #                      [
        #                          [
        #                              "batchnormalization_194",
        #                              0,
        #                              0
        #                          ],
        #                          [
        #                              "batchnormalization_196",
        #                              0,
        #                              0
        #                          ],
        #                          [
        #                              "batchnormalization_199",
        #                              0,
        #                              0
        #                          ],
        #                          [
        #                              "batchnormalization_200",
        #                              0,
        #                              0
        #                          ]
        #                      ]
        #                  ],
        if "inbound_nodes" in kclayer and len(kclayer["inbound_nodes"]) > 1:
            raise Exception(
                "%s doesn't support multiple inputs with shared weights" % kclayer["class_name"])

    def create(self, klayer, kclayer):
        class_name = kclayer["class_name"]

        self.__check_is_share_weights(kclayer)

        if (hasattr(klayer, "b_constraint") and klayer.b_constraint) or \
           (hasattr(klayer, "W_constraint") and klayer.W_constraint):
            raise Exception("We don't support constraint for now")

        if (hasattr(klayer, "activity_regularizer") and klayer.activity_regularizer):
            raise Exception("We don't support activity_regularizer for now")

        function_name = "create_" + class_name.lower()
        if not hasattr(self, function_name):
            raise Exception("We don't support layer: %s for now" % class_name )

        api = getattr(self, function_name)
        blayer = api(klayer, kclayer)
        return blayer.set_name(klayer.name)

    def create_model(self, klayer, kclyer):
        return DefinitionLoader.from_kmodel(klayer)

    def create_inputlayer(self, klayer, kclyer):
        return BLayer.Identity()

    def create_dense(self, klayer, kclayer):
        config = kclayer["config"]
        # Multiple inputs should share the same input_dim for Dense layer
        # We don't need to respect the tensor index for method `get_input_shape_at`
        # which is internal implementation and `get_input_shape_at` has hided that for us,
        # What we need to use is the input index, not node index, not tensor index.
        input_shape = klayer.get_input_shape_at(0)
        blayer = BLayer.Linear(
            input_size=int(input_shape[1]),
            output_size=config["output_dim"],
            with_bias=config["bias"],
            wRegularizer=self.to_bigdl_reg(config["W_regularizer"]),
            bRegularizer=self.to_bigdl_reg(config["b_regularizer"])
        )
        return self.combo_parameter_layer(blayer, config)

    def create_timedistributeddense(self, klayer, kclayer):
        config = kclayer["config"]
        input_shape = klayer.get_input_shape_at(0)
        blayer = BLayer.TimeDistributed(BLayer.Linear(
            input_size=int(input_shape[2]),
            output_size=config["output_dim"],
            with_bias=config["bias"],
            wRegularizer=self.to_bigdl_reg(config["W_regularizer"]),
            bRegularizer=self.to_bigdl_reg(config["b_regularizer"])
        ))
        return self.combo_parameter_layer(blayer, config)

    def create_embedding(self, klayer, kclayer):
        config = kclayer["config"]
        input_shape = klayer.get_input_shape_at(0) # batch, seq_len
        seq_len = int(input_shape[1])
        if klayer.input_length and klayer.input_length != seq_len:
            raise Exception(
                "The input_length doesn't match: %s vs %s" % (seq_len, klayer.input_length))

        if (hasattr(klayer, "dropout") and klayer.dropout != 0):
            raise Exception("We don't support dropout for now")

        if (hasattr(klayer, "mask_zero") and klayer.mask_zero != False):
            raise Exception("We don't support mask_zero for now")

        bseq = BLayer.Sequential()
        blayer = BLayer.LookupTable(
                 n_index = klayer.input_dim,
                 n_output = klayer.output_dim,
                 padding_value=0.0,
                 norm_type=2.0,
                 should_scale_grad_by_freq=False,
                 wRegularizer= self.to_bigdl_reg(config["W_regularizer"]),
                 bigdl_type="float")
        bseq.add(BLayer.AddConstant(1.0, inplace=True)) # Add 1 as BigDL is one-based index
        bseq.add(blayer)
        return bseq

    def create_activation(self, klayer, kclayer):
        config = kclayer["config"]
        return self.to_bigdl_activation(config["activation"], klayer.name)

    def create_dropout(self, klayer, kclayer):
        return BLayer.Dropout(klayer.p)

    def create_flatten(self, klayer, kclayer):
        self.__check_is_share_weights(kclayer)
        input_shape = klayer.input_shape
        blayer = BLayer.Reshape([int(np.prod(input_shape[1:]))], None)
        return blayer

    def create_permute(self, klayer, kclayer):
        swaps = self.__perm_to_pair(list(klayer.dims))
        swaps.reverse()
        swaps = map(lambda pair: (pair[0]+1, pair[1]+1), swaps)
        return BLayer.Transpose(swaps)

    def __perm_to_pair(self, perm):
        # perm: a list as a permutation of [1..n], eg [3, 1, 2] for n=3.
        # return a list of tuples that needs to be swapped to obtain the input `perm`.
        pairs = []

        def sort(arr, low, high):
            i = low
            j = high
            pivot = arr[low + int((high - low) / 2)]
            while i <= j:
                while arr[i] < pivot:
                    i += 1
                while arr[j] > pivot:
                    j -= 1
                if i <= j:
                    exchangeNumbers(arr, i, j)
                    i += 1
                    j -= 1
            if low < j:
                sort(arr, low, j)
            if i < high:
                sort(arr, i, high)

        def exchangeNumbers(arr, i, j):
            temp = arr[i]
            arr[i] = arr[j]
            arr[j] = temp
            pairs.append((i + 1, j + 1))

        sort(perm, 0, len(perm) - 1)

        return filter(lambda pair: pair[0] != pair[1], pairs)

    def create_reshape(self, klayer, kclayer):
        self.__check_is_share_weights(kclayer)
        blayer = BLayer.Reshape(klayer.target_shape, None)
        return blayer

    def create_repeatvector(self, klayer, kclayer):
        return BLayer.Replicate(n_features=klayer.n,
                                n_dim=1,
                                bigdl_type="float")

    def create_merge(self, klayer, kclayer):
        self.__check_is_share_weights(kclayer)
        input_shape = klayer.get_input_shape_at(0)
        if klayer.output_shape and not isinstance(klayer.output_shape, tuple):
            raise Exception("Only output_shape=None or a shape tuple is supported for now")
        if klayer.node_indices:
            unsupport_exp("node_indices")
        if klayer.output_mask:
            unsupport_exp("output_mask")
        if klayer.mode == "concat":
            blayer = BLayer.JoinTable(
                dimension=klayer.concat_axis,
                n_input_dims=len(input_shape[0]) - 1,
                bigdl_type="float")
        elif klayer.mode == "sum":
            blayer = BLayer.CAddTable(
                inplace=False,
                bigdl_type="float")
        elif klayer.mode == "mul":
            blayer = BLayer.CMulTable(bigdl_type="float")
        elif klayer.mode == "max":
            blayer = BLayer.CMaxTable(bigdl_type="float")
        elif klayer.mode == "dot":
            if len(input_shape[0]) >= 3:
                raise Exception("For merge mode dot, 3D input or above is not supported for now.")
            if klayer.dot_axes != [1, 1]:
                raise Exception("For merge mode dot, only dot_axes=1 is supported for now.")
            model = BLayer.Sequential()
            blayer = model.add(BLayer.DotProduct(bigdl_type="float"))\
                .add(BLayer.Reshape([1], True))
        elif klayer.mode == "ave":
            blayer = BLayer.CAveTable(
                inplace=False,
                bigdl_type="float")
        elif klayer.mode in ['cos']:
            raise Exception("Merge mode `%s` not supported for now" % klayer.mode)
        else:  # invalid mode or lambda functions
            raise Exception("Invalid merge mode: `%s`. Lambda/function as merge mode is not supported for now." % klayer.mode)
        return blayer

    def create_elu(self, klayer, kclayer):
        return BLayer.ELU(alpha=float(klayer.alpha),
                          inplace=False,
                          bigdl_type="float")

    def create_prelu(self, klayer, kclayer):
        return BLayer.PReLU(n_output_plane=0,
                            bigdl_type="float")

    def create_leakyrelu(self, klayer, kclayer):
        return BLayer.LeakyReLU(negval=float(klayer.alpha),
                                inplace=False,
                                bigdl_type="float")

    def create_parametricsoftplus(self, klayer, kclayer):
        alpha = float(klayer.alpha_init)
        beta = float(klayer.beta_init)
        if klayer.shared_axes != [None]:
            unsupport_exp("shared_axes")
        if round(alpha * beta, 4) == 1.0:
            return BLayer.SoftPlus(beta=beta,
                                   bigdl_type="float")
        else:
            raise Exception("Only alpha_init = 1/beta_init is supported for now")

    def create_thresholdedrelu(self, klayer, kclayer):
        return BLayer.Threshold(th=float(klayer.theta),
                                v=0.0,
                                ip=False,
                                bigdl_type="float")

    def __generate_zeropadding1d(self, pad_top, pad_bottom):
        return BLayer.SpatialZeroPadding(pad_left=0,
                                         pad_right=0,
                                         pad_top=pad_top,
                                         pad_bottom=pad_bottom,
                                         bigdl_type="float")

    def create_zeropadding1d(self, klayer, kclayer):
        padding = klayer.padding
        if isinstance(padding, int):
            return self.__generate_zeropadding1d(padding, padding)
        elif isinstance(padding, dict):
            return self.__generate_zeropadding1d(padding.get('left_pad', 0), padding.get('right_pad', 0))
        else:  # tuple of int (length 2)
            padding = tuple(padding)
            return self.__generate_zeropadding1d(padding[0], padding[1])

    def __generate_zeropadding2d(self, dim1, dim2, n_input_dim, pad1, pad2, pad3, pad4):
        model = BLayer.Sequential()
        paddinglayer1 = BLayer.Padding(dim=dim1,
                                       pad=pad1,
                                       n_input_dim=n_input_dim,
                                       value=0.0,
                                       n_index=1,
                                       bigdl_type="float")
        paddinglayer2 = BLayer.Padding(dim=dim1,
                                       pad=pad2,
                                       n_input_dim=n_input_dim,
                                       value=0.0,
                                       n_index=1,
                                       bigdl_type="float")
        paddinglayer3 = BLayer.Padding(dim=dim2,
                                       pad=pad3,
                                       n_input_dim=n_input_dim,
                                       value=0.0,
                                       n_index=1,
                                       bigdl_type="float")
        paddinglayer4 = BLayer.Padding(dim=dim2,
                                       pad=pad4,
                                       n_input_dim=n_input_dim,
                                       value=0.0,
                                       n_index=1,
                                       bigdl_type="float")
        model.add(paddinglayer1)
        model.add(paddinglayer2)
        model.add(paddinglayer3)
        model.add(paddinglayer4)
        return model

    # NB: zeropadding doesn't serialize dim_ording to json file
    def create_zeropadding2d(self, klayer, kclayer):
        padding = klayer.padding
        input_shape = klayer.get_input_shape_at(0)
        dim = 1
        if klayer.dim_ordering == "th":
            dim = 2
        if isinstance(padding, dict):  # dictionary
            return self.__generate_zeropadding2d(dim, dim+1, len(input_shape) - 1,
                                                 -padding.get('top_pad', 0), padding.get('bottom_pad', 0),
                                                 -padding.get('left_pad', 0), padding.get('right_pad', 0))
        else:  # tuple of int
            padding = tuple(padding)
            if len(padding) == 2:
                return self.__generate_zeropadding2d(dim, dim+1, len(input_shape) - 1,
                                                     -padding[0], padding[0], -padding[1], padding[1])
            elif len(padding) == 4:
                return self.__generate_zeropadding2d(dim, dim+1, len(input_shape) - 1,
                                                     -padding[0], padding[1], -padding[2], padding[3])

    # NB: zeropadding doesn't serialize dim_ording to json file
    def create_zeropadding3d(self, klayer, kclayer):
        padding = tuple(klayer.padding)
        input_shape = klayer.get_input_shape_at(0)
        dim = 1
        if klayer.dim_ordering == "th":
            dim = 2
        model = BLayer.Sequential()
        paddinglayer1 = BLayer.Padding(dim=dim,
                                       pad=-padding[0],
                                       n_input_dim=len(input_shape) - 1,
                                       value=0.0,
                                       n_index=1,
                                       bigdl_type="float")
        paddinglayer2 = BLayer.Padding(dim=dim,
                                       pad=padding[0],
                                       n_input_dim=len(input_shape) - 1,
                                       value=0.0,
                                       n_index=1,
                                       bigdl_type="float")
        paddinglayer3 = BLayer.Padding(dim=dim+1,
                                       pad=-padding[1],
                                       n_input_dim=len(input_shape) - 1,
                                       value=0.0,
                                       n_index=1,
                                       bigdl_type="float")
        paddinglayer4 = BLayer.Padding(dim=dim+1,
                                       pad=padding[1],
                                       n_input_dim=len(input_shape) - 1,
                                       value=0.0,
                                       n_index=1,
                                       bigdl_type="float")
        paddinglayer5 = BLayer.Padding(dim=dim+2,
                                       pad=-padding[2],
                                       n_input_dim=len(input_shape) - 1,
                                       value=0.0,
                                       n_index=1,
                                       bigdl_type="float")
        paddinglayer6 = BLayer.Padding(dim=dim+2,
                                       pad=padding[2],
                                       n_input_dim=len(input_shape) - 1,
                                       value=0.0,
                                       n_index=1,
                                       bigdl_type="float")
        model.add(paddinglayer1)
        model.add(paddinglayer2)
        model.add(paddinglayer3)
        model.add(paddinglayer4)
        model.add(paddinglayer5)
        model.add(paddinglayer6)
        return model

    def create_cropping1d(self, klayer, kclayer):
        cropping = tuple(klayer.cropping)
        return BLayer.SpatialZeroPadding(0, 0, -cropping[0], -cropping[1])

    def __return_sequences(self, return_sequences, blayer):
        # For recurrent layers, handle whether to return the last output sentence or the full sequence.
        if return_sequences:
            return blayer
        else:
            model = BLayer.Sequential()
            model.add(blayer)
            model.add(BLayer.Select(2, -1))
            return model

    def create_simplernn(self, klayer, kclayer):
        rec = BLayer.Recurrent()
        input_shape = klayer.get_input_shape_at(0)
        config = kclayer["config"]
        self.check_constraint_in_config(config)
        activation = self.to_bigdl_activation(config["activation"],
                                              "%s_%s" % (config["name"], config["activation"]))
        rnn = BLayer.RnnCell(input_size=int(input_shape[2]),
                             hidden_size=klayer.output_dim,
                             activation=activation,
                             isInputWithBias=False,
                             wRegularizer=self.to_bigdl_reg(config["W_regularizer"]),
                             uRegularizer=self.to_bigdl_reg(config["U_regularizer"]),
                             bRegularizer=self.to_bigdl_reg(config["b_regularizer"]),
                             bigdl_type="float")
        return self.__return_sequences(klayer.return_sequences, rec.add(rnn))

    def create_lstm(self, klayer, kclayer):
        rec = BLayer.Recurrent()
        input_shape = klayer.get_input_shape_at(0)
        config = kclayer["config"]
        self.check_constraint_in_config(config)
        activation = self.to_bigdl_activation(config["activation"],
                                              "%s_%s" % (config["name"], config["activation"]))
        if not isinstance(activation, BLayer.Tanh):
            raise Exception("For activation, only tanh is supported for now.")
        inner_activation = self.to_bigdl_activation(config["inner_activation"],
                                              "%s_%s" % (config["name"], config["inner_activation"]))
        if not isinstance(inner_activation, BLayer.Sigmoid):
            raise Exception("For inner_activation, only sigmond is supported for now.")
        lstm = BLayer.LSTM(input_size=int(input_shape[2]),
                           hidden_size=klayer.output_dim,
                           p=0.0,
                           wRegularizer=self.to_bigdl_reg(config["W_regularizer"]),
                           uRegularizer=self.to_bigdl_reg(config["U_regularizer"]),
                           bRegularizer=self.to_bigdl_reg(config["b_regularizer"]),
                           bigdl_type="float")
        return self.__return_sequences(klayer.return_sequences, rec.add(lstm))

    def create_convlstm2d(self, klayer, kclayer):
        rec = BLayer.Recurrent()
        input_shape = klayer.get_input_shape_at(0)
        config = kclayer["config"]
        self.check_constraint_in_config(config)
        activation = self.to_bigdl_activation(config["activation"],
                                              "%s_%s" % (config["name"], config["activation"]))
        if not isinstance(activation, BLayer.Tanh):
            raise Exception("For activation, only tanh is supported for now.")
        inner_activation = self.to_bigdl_activation(config["inner_activation"],
                                                    "%s_%s" % (config["name"], config["inner_activation"]))
        if not isinstance(inner_activation, BLayer.Sigmoid):
            raise Exception("For inner_activation, only sigmond is supported for now.")

        #TODO: border_mode = 'valid'
        if config["border_mode"] != 'same':
            raise Exception("Unsupported border_mode: valid")

        if config["nb_row"] != config["nb_col"]:
            raise Exception("Only square kernel is supported for now. Please set nb_row=nb_col.")
        if klayer.subsample[0] != klayer.subsample[1]:
            raise Exception("Only equal stride is supported for now. "
                            "Please set subsample to be a tuple with equal values.")

        blayer = BLayer.ConvLSTMPeephole(input_size=input_shape[2],
                                         output_size=config["nb_filter"],
                                         kernel_i=config["nb_col"],
                                         kernel_c=config["nb_row"],
                                         # NB: ConvLSTM doesn't serialize subsample to json file
                                         stride=klayer.subsample[0],
                                         padding=-1,
                                         # NB: ConvLSTM doesn't serialize regularizers to json file
                                         # wRegularizer=self.to_bigdl_reg(config["W_regularizer"]),
                                         # uRegularizer=self.to_bigdl_reg(config["U_regularizer"]),
                                         # bRegularizer=self.to_bigdl_reg(config["b_regularizer"]),
                                         cRegularizer=None,
                                         with_peephole=False,
                                         bigdl_type="float")
        return self.__return_sequences(klayer.return_sequences, rec.add(blayer))

    def create_gru(self, klayer, kclayer):
        rec = BLayer.Recurrent()
        input_shape = klayer.get_input_shape_at(0)
        config = kclayer["config"]
        self.check_constraint_in_config(config)
        activation = self.to_bigdl_activation(config["activation"],
                                              "%s_%s" % (config["name"], config["activation"]))
        if not isinstance(activation, BLayer.Tanh):
            raise Exception("For activation, only `tanh` is supported for now.")
        inner_activation = self.to_bigdl_activation(config["inner_activation"],
                                                    "%s_%s" % (config["name"], config["inner_activation"]))
        if not isinstance(inner_activation, BLayer.Sigmoid):
            raise Exception("For inner_activation, only `sigmond` is supported for now.")
        gru = BLayer.GRU(input_size=int(input_shape[2]),
                         hidden_size=klayer.output_dim,
                         p=0.0,
                         wRegularizer=self.to_bigdl_reg(config["W_regularizer"]),
                         uRegularizer=self.to_bigdl_reg(config["U_regularizer"]),
                         bRegularizer=self.to_bigdl_reg(config["b_regularizer"]),
                         bigdl_type="float")
        return self.__return_sequences(klayer.return_sequences, rec.add(gru))

    def create_batchnormalization(self, klayer, kclayer):
        config = kclayer["config"]

        self.__check_is_share_weights(kclayer)
        if keras.backend.image_dim_ordering() != "th" or klayer.axis != 1:
            raise Exception("""For BatchNormalization, we only support th image ordering (i.e. NCHW) """ +
                            """with axis = 1 for now, but the current order is %s and axis is %s
            """ % (keras.backend.image_dim_ordering(), klayer.axis))  # noqa
        if klayer.mode != 0:
            raise Exception(
                "Only support mode = 0 for now, but the current mode is: %s", klayer.mode)

        if config["gamma_regularizer"]:
            raise Exception("We don't support gamma_regularizer for now")

        if config["beta_regularizer"]:
            raise Exception("We don't support beta_regularizer for now")

        input_shape = klayer.get_input_shape_at(0)
        n_input_channel = int(input_shape[klayer.axis])  # default is -1 which is channel-last

        # init gamma and beta
        # TODO: replace this with to_bigdl_init in the future
        gamma = self.get_value_from_init(klayer.gamma_init.__name__, (n_input_channel,))
        beta = self.get_value_from_init(klayer.beta_init.__name__, (n_input_channel,))

        blayer = BLayer.SpatialBatchNormalization(
                 n_output=n_input_channel,
                 eps=klayer.epsilon,
                 momentum=klayer.momentum,
                 affine=True,
                 init_weight=gamma,
                 init_bias=beta,
                 init_grad_weight=None,
                 init_grad_bias=None,
                 bigdl_type="float")

        k_running_mean = keras.backend.eval(klayer.running_mean)
        k_running_std = keras.backend.eval(klayer.running_std)
        blayer.set_running_mean(k_running_mean)
        blayer.set_running_std(k_running_std)
        return blayer

    def get_bdim_order(self, kclayer):
        return self.to_bigdl_2d_ordering(self.get_kdim_order(kclayer))

    def get_kdim_order(self, kclayer):
        config = kclayer["config"]
        if "dim_ordering" in config:
            return config["dim_ordering"]
        else:
            warnings.warn("Cannot find dim_ordering from json definition. Use default instead.")
            return keras.backend.image_dim_ordering()

    def to_bigdl_2d_ordering(self, order):
        if order == "tf":
            return "NHWC"
        elif order == "th":
            return "NCHW"
        else:
            raise Exception("Unsupport ordering: %s" % order)

    def to_bigdl_3d_padding(self, border_mode):
        if border_mode == "valid":
            return 0, 0, 0
        # TODO: border_mode=`same`
        else:
            raise Exception("Unsupported border mode: %s" % border_mode)

    def to_bigdl_2d_padding(self, border_mode):
        if border_mode == "same":
            return -1, -1
        elif border_mode == "valid":
            return 0, 0
        else:
            raise Exception("Unsupported border mode: %s" % border_mode)

    def to_bigdl_1d_padding(self, border_mode, kernel_w):
        if border_mode == "same":
            raise Exception("We don't support padding for now")
            # TODO: support padding
            # return int((kernel_w -1) / 2)
        elif border_mode == "valid":
            return 0
        else:
            raise Exception("Unsupported border mode: %s" % border_mode)

    def create_convolution1d(self, klayer, kclayer):
        config = kclayer["config"]
        input_shape = klayer.get_input_shape_at(0)
        # batch, steps, dim, batch is None here, so you cannot use it directly.
        stack_size = int(input_shape[2])

        bpadW, bpadH = self.to_bigdl_2d_padding(klayer.border_mode)
        seq = BLayer.Sequential()
        seq.add(BLayer.Reshape([int(input_shape[1]), 1, int(input_shape[2])], True))
        blayer = BLayer.SpatialConvolution(
                 n_input_plane=stack_size,
                 n_output_plane=klayer.nb_filter,
                 kernel_w=1,
                 kernel_h=klayer.filter_length,
                 stride_w=1,
                 stride_h=klayer.subsample_length,
                 pad_w=bpadW,
                 pad_h=bpadH,
                 n_group=1,
                 propagate_back=True,
                 wRegularizer=self.to_bigdl_reg(config["W_regularizer"]),
                 bRegularizer=self.to_bigdl_reg(config["b_regularizer"]),
                 init_weight=None,
                 init_bias=None,
                 init_grad_weight=None,
                 init_grad_bias=None,
                 with_bias=config["bias"],
                 data_format="NHWC",
                 bigdl_type="float")
        seq.add(blayer)
        seq.add(BLayer.Squeeze(3))
        return self.combo_parameter_layer(seq, config)

    def create_convolution2d(self, klayer, kclayer):
        config = kclayer["config"]
        bigdl_order = self.get_bdim_order(kclayer)
        input_shape = klayer.get_input_shape_at(0)

        if bigdl_order == "NCHW":
            stack_size = int(input_shape[1])
        elif bigdl_order == "NHWC":
            stack_size = int(input_shape[3])

        bpadW, bpadH = self.to_bigdl_2d_padding(klayer.border_mode)
        blayer = BLayer.SpatialConvolution(
                 n_input_plane=stack_size,
                 n_output_plane=klayer.nb_filter,
                 kernel_w=klayer.nb_col,
                 kernel_h=klayer.nb_row,
                 stride_w=klayer.subsample[0],
                 stride_h=klayer.subsample[1],
                 pad_w=bpadW,
                 pad_h=bpadH,
                 n_group=1,
                 propagate_back=True,
                 wRegularizer=self.to_bigdl_reg(config["W_regularizer"]),
                 bRegularizer=self.to_bigdl_reg(config["b_regularizer"]),
                 init_weight=None,
                 init_bias=None,
                 init_grad_weight=None,
                 init_grad_bias=None,
                 with_bias=config["bias"],
                 data_format=bigdl_order,
                 bigdl_type="float")

        return self.combo_parameter_layer(blayer, config)

    def create_convolution3d(self, klayer, kclayer):
        config = kclayer["config"]
        if klayer.dim_ordering != "th":
            raise Exception("Please use `th` for `dim_ordering`. `%s` is not supported for now." % klayer.dim_ordering)
        input_shape = klayer.get_input_shape_at(0)

        bpadT, bpadW, bpadH = self.to_bigdl_3d_padding(klayer.border_mode)
        blayer = BLayer.VolumetricConvolution(
            n_input_plane=input_shape[1],
            n_output_plane=klayer.nb_filter,
            k_t=klayer.kernel_dim1,
            k_w=klayer.kernel_dim3,
            k_h=klayer.kernel_dim2,
            d_t=klayer.subsample[0],
            d_w=klayer.subsample[2],
            d_h=klayer.subsample[1],
            pad_t=bpadT,
            pad_w=bpadW,
            pad_h=bpadH,
            with_bias=config["bias"],
            wRegularizer=self.to_bigdl_reg(config["W_regularizer"]),
            bRegularizer=self.to_bigdl_reg(config["b_regularizer"]),
            bigdl_type="float")

        return self.combo_parameter_layer(blayer, config)

    def create_atrousconvolution1d(self, klayer, kclayer):
        config = kclayer["config"]
        if not config["bias"]:
            raise Exception("Please set `bias=True` for AtrousConvolution1D")
        input_shape = klayer.get_input_shape_at(0)

        # TODO: border_mode=`same`
        if klayer.border_mode == "same":
            raise Exception("Unsupported border mode: %s" % klayer.border_mode)
        bpadW, bpadH = self.to_bigdl_2d_padding(klayer.border_mode)
        seq = BLayer.Sequential()
        seq.add(BLayer.Transpose([(2, 3)]))
        seq.add(BLayer.Reshape([input_shape[2], input_shape[1], 1], True))
        blayer = BLayer.SpatialDilatedConvolution(
            n_input_plane=input_shape[2],
            n_output_plane=config["nb_filter"],
            kw=1,
            kh=config["filter_length"],
            dw=1,
            dh=config["subsample_length"],
            pad_w=bpadW,
            pad_h=bpadH,
            dilation_w=1,
            dilation_h=config["atrous_rate"],
            wRegularizer=self.to_bigdl_reg(config["W_regularizer"]),
            bRegularizer=self.to_bigdl_reg(config["b_regularizer"]),
            bigdl_type="float")

        seq.add(blayer)
        seq.add(BLayer.Transpose([(2, 3)]))
        seq.add(BLayer.Squeeze(4))
        return self.combo_parameter_layer(seq, config)

    def create_atrousconvolution2d(self, klayer, kclayer):
        config = kclayer["config"]
        if klayer.dim_ordering != "th":
            raise Exception("Please use `th` for `dim_ordering`. `%s` is not supported for now." % klayer.dim_ordering)
        if not config["bias"]:
            raise Exception("Please set `bias=True` for AtrousConvolution2D")
        input_shape = klayer.get_input_shape_at(0)

        # TODO: border_mode=`same`
        if klayer.border_mode == "same":
            raise Exception("Unsupported border mode: %s" % klayer.border_mode)
        bpadW, bpadH = self.to_bigdl_2d_padding(klayer.border_mode)
        blayer = BLayer.SpatialDilatedConvolution(
            n_input_plane=input_shape[1],
            n_output_plane=config["nb_filter"],
            kw=config["nb_col"],
            kh=config["nb_row"],
            dw=config["subsample"][1],
            dh=config["subsample"][0],
            pad_w=bpadW,
            pad_h=bpadH,
            dilation_w=config["atrous_rate"][1],
            dilation_h=config["atrous_rate"][0],
            wRegularizer=self.to_bigdl_reg(config["W_regularizer"]),
            bRegularizer=self.to_bigdl_reg(config["b_regularizer"]),
            bigdl_type="float")

        return self.combo_parameter_layer(blayer, config)

    def create_deconvolution2d(self, klayer, kclayer):
        config = kclayer["config"]
        if klayer.dim_ordering != "th":
            raise Exception("Please use `th` for `dim_ordering`. `%s` is not supported for now." % klayer.dim_ordering)
        input_shape = klayer.get_input_shape_at(0)

        bpadW, bpadH = self.to_bigdl_2d_padding(klayer.border_mode)
        blayer = BLayer.SpatialFullConvolution(
            n_input_plane=input_shape[1],
            n_output_plane=klayer.nb_filter,
            kw=klayer.nb_col,
            kh=klayer.nb_row,
            dw=klayer.subsample[1],
            dh=klayer.subsample[0],
            pad_w=bpadW,
            pad_h=bpadH,
            adj_w=0,
            adj_h=0,
            n_group=1,
            no_bias=False,
            wRegularizer=self.to_bigdl_reg(config["W_regularizer"]),
            bRegularizer=self.to_bigdl_reg(config["b_regularizer"]),
            bigdl_type="float")

        return self.combo_parameter_layer(blayer, config)

    def create_maxpooling3d(self, klayer, kclayer):
        if klayer.dim_ordering != "th":
            raise Exception("Please use `th` for `dim_ordering`. `%s` is not supported for now." % klayer.dim_ordering)
        bpadT, bpadW, bpadH = self.to_bigdl_3d_padding(klayer.border_mode)
        blayer = BLayer.VolumetricMaxPooling(
                k_t=klayer.pool_size[0],
                k_w=klayer.pool_size[2],
                k_h=klayer.pool_size[1],
                d_t=klayer.strides[0],
                d_w=klayer.strides[2],
                d_h=klayer.strides[1],
                pad_t=bpadT,
                pad_w=bpadW,
                pad_h=bpadH,
                bigdl_type="float")
        return blayer

    def create_maxpooling2d(self, klayer, kclayer):
        bigdl_order = self.get_bdim_order(kclayer)
        bpadW, bpadH = self.to_bigdl_2d_padding(klayer.border_mode)
        blayer = BLayer.SpatialMaxPooling(
                 kw=klayer.pool_size[1],
                 kh=klayer.pool_size[0],
                 dw=klayer.strides[1],
                 dh=klayer.strides[0],
                 pad_w=bpadW,
                 pad_h=bpadH,
                 to_ceil=False,
                 format=bigdl_order,
                 bigdl_type="float")
        return blayer

    def create_globalmaxpooling3d(self, klayer, kclayer):
        input_shape = klayer.get_input_shape_at(0)
        if klayer.dim_ordering == "th":
            b_kt = input_shape[2]
            b_kw = input_shape[4]
            b_kh = input_shape[3]
        else:
            raise Exception("Please use `th` for dim_ordering. `%s` is not supported for now." % klayer.dim_ordering)

        seq = BLayer.Sequential()
        blayer = BLayer.VolumetricMaxPooling(
                k_t=b_kt,
                k_w=b_kw,
                k_h=b_kh,
                d_t=1,
                d_w=1,
                d_h=1,
                pad_t=0,
                pad_w=0,
                pad_h=0,
                bigdl_type="float"
        )
        seq.add(blayer)
        seq.add(BLayer.Squeeze(5))
        seq.add(BLayer.Squeeze(4))
        seq.add(BLayer.Squeeze(3))

        return seq

    def create_globalaveragepooling3d(self, klayer, kclayer):
        input_shape = klayer.get_input_shape_at(0)
        if klayer.dim_ordering == "th":
            b_kt = input_shape[2]
            b_kw = input_shape[4]
            b_kh = input_shape[3]
        else:
            raise Exception("Please use `th` for dim_ordering. `%s` is not supported for now." % klayer.dim_ordering)

        seq = BLayer.Sequential()
        blayer = BLayer.VolumetricAveragePooling(
                k_t=b_kt,
                k_w=b_kw,
                k_h=b_kh,
                d_t=1,
                d_w=1,
                d_h=1,
                pad_t=0,
                pad_w=0,
                pad_h=0,
                count_include_pad=False,
                bigdl_type="float"
        )
        seq.add(blayer)
        seq.add(BLayer.Squeeze(5))
        seq.add(BLayer.Squeeze(4))
        seq.add(BLayer.Squeeze(3))

        return seq

    def create_averagepooling2d(self, klayer, kclayer):
        bigdl_order = self.get_bdim_order(kclayer)
        bpadW, bpadH = self.to_bigdl_2d_padding(klayer.border_mode)
        blayer = BLayer.SpatialAveragePooling(
            kw=klayer.pool_size[1],
            kh=klayer.pool_size[0],
            dw=klayer.strides[1],
            dh=klayer.strides[0],
            pad_w=bpadW,
            pad_h=bpadH,
            global_pooling=False,
            ceil_mode=False,
            count_include_pad=False,
            divide=True,
            format=bigdl_order,
            bigdl_type="float"
        )
        return blayer

    def create_averagepooling3d(self, klayer, kclayer):
        if klayer.dim_ordering != "th":
            raise Exception("Please use `th` for `dim_ordering`. `%s` is not supported for now." % klayer.dim_ordering)
        bpadT, bpadW, bpadH = self.to_bigdl_3d_padding(klayer.border_mode)
        blayer = BLayer.VolumetricAveragePooling(
                k_t=klayer.pool_size[0],
                k_w=klayer.pool_size[2],
                k_h=klayer.pool_size[1],
                d_t=klayer.strides[0],
                d_w=klayer.strides[2],
                d_h=klayer.strides[1],
                pad_t=bpadT,
                pad_w=bpadW,
                pad_h=bpadH,
                count_include_pad=False,
                bigdl_type="float")
        return blayer

    def create_globalmaxpooling2d(self, klayer, kclayer):
        bigdl_order = self.get_bdim_order(kclayer)
        input_shape = klayer.get_input_shape_at(0)
        if bigdl_order == "NCHW":
            b_kw = int(input_shape[3])
            b_kh = int(input_shape[2])
        else:
            b_kw = int(input_shape[2])
            b_kh = int(input_shape[1])

        seq = BLayer.Sequential()
        blayer = BLayer.SpatialMaxPooling(
            kw=b_kw,
            kh=b_kh,
            dw=b_kw,
            dh=b_kh,
            pad_w=0,
            pad_h=0,
            to_ceil=False,
            format=bigdl_order,
            bigdl_type="float"
        )
        seq.add(blayer)
        if bigdl_order == "NCHW":
            seq.add(BLayer.Squeeze(3, num_input_dims=3))
            seq.add(BLayer.Squeeze(2, num_input_dims=2))
        else:
            seq.add(BLayer.Squeeze(2, num_input_dims=3))
            seq.add(BLayer.Squeeze(1, num_input_dims=2))
        return seq

    def create_globalmaxpooling1d(self, klayer, kclayer):
        input_shape = klayer.get_input_shape_at(0) # batch, step, dim
        b_kw = 1
        b_kh = int(input_shape[1])

        seq = BLayer.Sequential()
        seq.add(BLayer.View([int(input_shape[1]), 1, int(input_shape[2])], num_input_dims=2))
        blayer = BLayer.SpatialMaxPooling(
            kw=b_kw,
            kh=b_kh,
            dw=0,
            dh=0,
            pad_w=0,
            pad_h=0,
            to_ceil=False,
            format="NHWC",
            bigdl_type="float"
        )
        seq.add(blayer)
        seq.add(BLayer.Squeeze(2, num_input_dims=2))
        seq.add(BLayer.Squeeze(1, num_input_dims=1))
        return seq

    def create_globalaveragepooling1d(self, klayer, kclayer):
        input_shape = klayer.get_input_shape_at(0) # batch, step, dim
        b_kw = 1
        b_kh = int(input_shape[1])

        seq = BLayer.Sequential()
        seq.add(BLayer.View([int(input_shape[1]), 1, int(input_shape[2])], num_input_dims=2))
        blayer = BLayer.SpatialAveragePooling(
            kw=b_kw,
            kh=b_kh,
            dw=0,
            dh=0,
            pad_w=0,
            pad_h=0,
            global_pooling=False,
            ceil_mode=False,
            count_include_pad=False,
            divide=True,
            format="NHWC",
            bigdl_type="float"
        )
        seq.add(blayer)
        seq.add(BLayer.Squeeze(2, num_input_dims=2)) # the index start from one but without batch
        seq.add(BLayer.Squeeze(1, num_input_dims=1))

        return seq

    def create_maxpooling1d(self, klayer, kclayer):
        input_shape = klayer.get_input_shape_at(0)  # batch, steps, dim
        bpadW, bpadH = self.to_bigdl_2d_padding(klayer.border_mode)

        seq = BLayer.Sequential()
        seq.add(BLayer.Reshape([int(input_shape[1]), 1, int(input_shape[2])], True))
        blayer = BLayer.SpatialMaxPooling(
            kw=1,
            kh=klayer.pool_length,
            dw=1,
            dh=klayer.stride,
            pad_w=bpadW,
            pad_h=bpadH,
            to_ceil=False,
            format="NHWC",
            bigdl_type="float"
        )
        seq.add(blayer)
        seq.add(BLayer.Squeeze(3))
        return seq

    def create_averagepooling1d(self, klayer, kclayer):
        input_shape = klayer.get_input_shape_at(0)  # batch, steps, dim
        bpadW, bpadH = self.to_bigdl_2d_padding(klayer.border_mode)

        seq = BLayer.Sequential()
        seq.add(BLayer.Reshape([int(input_shape[1]), 1, int(input_shape[2])], True))
        blayer = BLayer.SpatialAveragePooling(
            kw=1,
            kh=klayer.pool_length,
            dw=1,
            dh=klayer.stride,
            pad_w=bpadW,
            pad_h=bpadH,
            global_pooling=False,
            ceil_mode=False,
            count_include_pad=False,
            divide=True,
            format="NHWC",
            bigdl_type="float"
        )
        seq.add(blayer)
        seq.add(BLayer.Squeeze(3))
        return seq

    def create_globalaveragepooling2d(self, klayer, kclayer):
        bigdl_order = self.get_bdim_order(kclayer)
        input_shape = klayer.get_input_shape_at(0)
        if bigdl_order == "NCHW":
            b_kw = int(input_shape[3])
            b_kh = int(input_shape[2])
        else:
            b_kw = int(input_shape[2])
            b_kh = int(input_shape[1])

        seq = BLayer.Sequential()
        blayer = BLayer.SpatialAveragePooling(
            kw=b_kw,
            kh=b_kh,
            dw=b_kw,
            dh=b_kh,
            pad_w=0,
            pad_h=0,
            global_pooling=False,
            ceil_mode=False,
            count_include_pad=False,
            divide=True,
            format=bigdl_order,
            bigdl_type="float"
        )
        seq.add(blayer)
        if bigdl_order == "NCHW":
            seq.add(BLayer.Squeeze(3, num_input_dims=3))
            seq.add(BLayer.Squeeze(2, num_input_dims=2))
        else:
            seq.add(BLayer.Squeeze(2, num_input_dims=3))
            seq.add(BLayer.Squeeze(1, num_input_dims=2))
        return seq

    def check_constraint_in_config(self, config):
        if "W_constraint" in config:
            if config["W_constraint"]:
                raise Exception("W_constraint is not supported for now")
        if "b_constraint" in config:
            if config["b_constraint"]:
                raise Exception("b_constraint is not supported for now")

    def combo_parameter_layer(self, blayer, config):
        self.check_constraint_in_config(config)

        blayer.set_name(config["name"])
        if hasattr(blayer, "set_init_method"):
            blayer.set_init_method(self.to_bigdl_init(config["init"]),
                                   BInit.Zeros())  # Keras always set this to be zeros
        # "linear" meaning do nothing
        if config["activation"] != "linear":
            activation = self.to_bigdl_activation(config["activation"],
                                                  "%s_%s" % (config["name"], config["activation"]))
            return self.fuse(blayer, activation)
        else:
            return blayer

    def to_bigdl_activation(self, activation_name, activation_id):
        activation = None
        if activation_name == "tanh":
            activation = BLayer.Tanh()
        elif activation_name == "sigmoid":
            activation = BLayer.Sigmoid()
        elif activation_name == "hard_sigmoid":
            activation = BLayer.HardSigmoid()
        elif activation_name == "relu":
            activation = BLayer.ReLU()
        elif activation_name == "softmax":
            activation = BLayer.SoftMax()
        elif activation_name == "softplus":
            activation = BLayer.SoftPlus(beta=1.0)
        elif activation_name == "softsign":
            activation = BLayer.SoftSign()
        elif activation_name == "linear":
            activation = BLayer.Identity()
        else:
            raise Exception("Unsupported activation type: %s" % activation_name)
        activation.set_name(activation_id)
        return activation

    def get_value_from_init(self, kinit_method, shape):
        if kinit_method == "zero":
            return np.zeros(shape)
        elif kinit_method == "one":
            return np.ones(shape)
        else:
            raise Exception("We don't support % for now", kinit_method)

    def to_bigdl_init(self, kinit_method):  # kinit_method is a string
        init = None
        if kinit_method == "glorot_uniform":
            init = BInit.Xavier()
        elif kinit_method == "one":
            init = BInit.Ones()
        elif kinit_method == "zero":
            init = BInit.Zeros()
        else:
            raise Exception("Unsupported init type: %s" % init)
        return init

    def to_bigdl_reg(self, reg):  # reg is a dict
        if reg:
            return BRegularizer(reg['l1'], reg['l2'])
        else:
            return None

    def fuse(self, src_blayer, activation):  # activation is a layer
        seq = BLayer.Sequential()
        seq.add(src_blayer)
        seq.add(activation)
        seq.set_name(src_blayer.name())
        return seq