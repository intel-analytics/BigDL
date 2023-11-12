from bigdl.llm.transformers.low_bit_linear import ggml_convert_qtype
from bigdl.llm.transformers.low_bit_linear import ggml_q_format_convet_cpu2xpu
from bigdl.llm.ggml.quantize import ggml_tensor_qtype
import torch
# import intel_extension_for_pytorch as ipex

NF4_MAP = [-1.0, -0.6961928009986877, -0.5250730514526367, -0.39491748809814453,
-0.28444138169288635, -0.18477343022823334, -0.09105003625154495, 0.0,
0.07958029955625534, 0.16093020141124725, 0.24611230194568634, 0.33791524171829224,
0.44070982933044434, 0.5626170039176941, 0.7229568362236023, 1.0]

# nf4_map = None
from operator import mul
from functools import reduce
def torch_dequant_nf4(quant_weight, scales, original_shape=(4096, 4096), dtype=torch.bfloat16):
    quants = quant_weight
    quants = quants.reshape(-1, 32)
    quants_0 = quants.bitwise_right_shift(4)
    quants_1 = quants.bitwise_and(0x0f)

    quants = torch.cat([quants_0, quants_1], dim=-1)

    # global nf4_map
    # if nf4_map is None:
    nf4_map = torch.tensor(NF4_MAP, dtype=dtype, device=quant_weight.device).reshape(-1, 1)

    dequants = torch.nn.functional.embedding(quants.int(), nf4_map)

    # print(dequants_0.shape)
    # print(scales.shape)
    dequants = dequants * scales.reshape(-1, 1, 1)

    return dequants.reshape(original_shape)

right_shift_tensor = torch.tensor(4, dtype=torch.uint8, device="hpu")
and_tensor = torch.tensor(0x0f, dtype=torch.uint8, device="hpu")
eight_tensor = torch.tensor(8, dtype=torch.bfloat16, device="hpu")

def torch_dequant_int4(quant_weight, scales, original_shape=(4096, 4096), dtype=torch.bfloat16):
    quants = quant_weight
    quants = quants.reshape(-1, 32)
    quants_0 = quants.bitwise_right_shift(right_shift_tensor)
    quants_1 = quants.bitwise_and(and_tensor)

    quants = torch.cat([quants_1, quants_0], dim=-1)

    # global nf4_map
    # if nf4_map is None:
    # nf4_map = torch.tensor(NF4_MAP, dtype=dtype, device=quant_weight.device).reshape(-1, 1)

    # dequants = torch.nn.functional.embedding(quants.int(), nf4_map)

    # print(dequants_0.shape)
    # print(scales.shape)
    quants_int = quants.to(dtype) - eight_tensor
    dequants = (quants_int.to(dtype)) * scales.reshape(-1, 1)

    return dequants.reshape(original_shape)

nf4_map = torch.tensor(NF4_MAP, dtype=torch.bfloat16, device="hpu").reshape(-1, 1)
right_shift = torch.tensor([4, 0], dtype=torch.uint8, device="hpu").reshape(1, 2, 1)

def torch_dequant_nf4_2(quant_weight, scales, original_shape=(4096, 4096), dtype=torch.bfloat16):
    quants = quant_weight
    quants = quants.reshape(-1, 1, 32).expand(-1, 2, -1)
    # print(quants.shape)
    # print("before right shift")
    quants = quants.bitwise_right_shift(right_shift)
    # print("after right shift")
    # print(quants.shape)
    quants = quants.bitwise_and(0x0f)
    # nf4_map = torch.tensor(NF4_MAP, dtype=dtype, device=quant_weight.device).reshape(-1, 1)
    dequants = torch.nn.functional.embedding(quants.int(), nf4_map)
    # print("after embedding")
    # print(dequants.shape)
    dequants = dequants.reshape(-1, 64) * scales.reshape(-1, 1)
    return dequants.reshape(original_shape)
nf4_map = torch.tensor(NF4_MAP, dtype=torch.bfloat16, device="hpu")
right_shift = torch.tensor([4, 0], dtype=torch.uint8, device="hpu").reshape(1, 2, 1)
def torch_dequant_nf4_3(quant_weight, scales, original_shape=(4096, 4096), dtype=torch.bfloat16):
    quants = quant_weight
    quants = quants.reshape(-1, 1, 32).expand(-1, 2, -1)
    quants = quants.bitwise_right_shift(right_shift)
    quants = quants.bitwise_and(0x0f)

    # dequants = torch.nn.functional.embedding(quants.int(), nf4_map)
    # dequants = torch.gather(nf4_map, 0, quants)
    dequants = nf4_map[quants.int()]
    # print("after embedding")
    # print(dequants.shape)
    dequants = dequants.reshape(-1, 64) * scales.reshape(-1, 1)
    return dequants.reshape(original_shape)

# @torch.no_grad()
# def get_scales(quant_weight, num_elements, dtype):
#     assert quant_weight.dtype == torch.uint8
#     scales = quant_weight[num_elements//2:]

#     # fp16_tensor = torch.empty(0, dtype=torch.float16, device=quant_weight.device)
#     # fp16_tensor.set_(scales.untyped_storage(),
#     #                  scales.storage_offset()//2,
#     #                  size=(num_elements//64,),
#     #                  stride=(1,))
#     fp16_tensor = scales.view(torch.float16)
#     # assert scales.storage_offset() == num_elements // 2
#     # assert num_elements // 64 * 2== (quant_weight.numel() - num_elements // 2), \
#     #     f"{num_elements // 64 * 2} != {(quant_weight.numel() - num_elements // 2)}"
#     return fp16_tensor.to(dtype)