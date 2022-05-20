# -*- coding: utf-8 -*-
# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: fgboost_service.proto
"""Generated protocol buffer code."""
from google.protobuf import descriptor as _descriptor
from google.protobuf import descriptor_pool as _descriptor_pool
from google.protobuf import message as _message
from google.protobuf import reflection as _reflection
from google.protobuf import symbol_database as _symbol_database
# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()


import fl_base_pb2 as fl__base__pb2


DESCRIPTOR = _descriptor_pool.Default().AddSerializedFile(b'\n\x15\x66gboost_service.proto\x12\x07\x66gboost\x1a\rfl_base.proto\"U\n\x12UploadLabelRequest\x12\x12\n\nclientuuid\x18\x01 \x01(\t\x12\x18\n\x04\x64\x61ta\x18\x02 \x01(\x0b\x32\n.TensorMap\x12\x11\n\talgorithm\x18\x03 \x01(\t\"F\n\x14\x44ownloadLabelRequest\x12\x1b\n\x08metaData\x18\x01 \x01(\x0b\x32\t.MetaData\x12\x11\n\talgorithm\x18\x02 \x01(\t\"L\n\x10\x44ownloadResponse\x12\x18\n\x04\x64\x61ta\x18\x01 \x01(\x0b\x32\n.TensorMap\x12\x10\n\x08response\x18\x02 \x01(\t\x12\x0c\n\x04\x63ode\x18\x03 \x01(\x05\"R\n\x08TreeLeaf\x12\x0e\n\x06treeID\x18\x01 \x01(\t\x12\x11\n\tleafIndex\x18\x02 \x03(\x05\x12\x12\n\nleafOutput\x18\x03 \x03(\x02\x12\x0f\n\x07version\x18\x04 \x01(\x05\"P\n\x15UploadTreeLeafRequest\x12\x12\n\nclientuuid\x18\x01 \x01(\t\x12#\n\x08treeLeaf\x18\x02 \x01(\x0b\x32\x11.fgboost.TreeLeaf\"\xa8\x01\n\tDataSplit\x12\x0e\n\x06treeID\x18\x01 \x01(\t\x12\x0e\n\x06nodeID\x18\x02 \x01(\t\x12\x11\n\tfeatureID\x18\x03 \x01(\x05\x12\x12\n\nsplitValue\x18\x04 \x01(\x02\x12\x0c\n\x04gain\x18\x05 \x01(\x02\x12\x11\n\tsetLength\x18\x06 \x01(\x05\x12\x0f\n\x07itemSet\x18\x07 \x03(\x05\x12\x11\n\tclientUid\x18\x08 \x01(\t\x12\x0f\n\x07version\x18\t \x01(\x05\"0\n\x0eUploadResponse\x12\x10\n\x08response\x18\x01 \x01(\t\x12\x0c\n\x04\x63ode\x18\x02 \x01(\x05\"/\n\x0bTreePredict\x12\x0e\n\x06treeID\x18\x01 \x01(\t\x12\x10\n\x08predicts\x18\x02 \x03(\x08\"6\n\x0c\x42oostPredict\x12&\n\x08predicts\x18\x01 \x03(\x0b\x32\x14.fgboost.TreePredict\"4\n\tBoostEval\x12\'\n\tevaluates\x18\x01 \x03(\x0b\x32\x14.fgboost.TreePredict\"4\n\x0fRegisterRequest\x12\x12\n\nclientuuid\x18\x01 \x01(\t\x12\r\n\x05token\x18\x02 \x01(\t\"2\n\x10RegisterResponse\x12\x10\n\x08response\x18\x01 \x01(\t\x12\x0c\n\x04\x63ode\x18\x02 \x01(\x05\"b\n\x15UploadTreeEvalRequest\x12\x12\n\nclientuuid\x18\x01 \x01(\t\x12\x0f\n\x07version\x18\x02 \x01(\x05\x12$\n\x08treeEval\x18\x03 \x03(\x0b\x32\x12.fgboost.BoostEval\"o\n\x0f\x45valuateRequest\x12\x12\n\nclientuuid\x18\x01 \x01(\t\x12$\n\x08treeEval\x18\x02 \x03(\x0b\x32\x12.fgboost.BoostEval\x12\x0f\n\x07version\x18\x03 \x01(\x05\x12\x11\n\tlastBatch\x18\x04 \x01(\x08\"]\n\x10\x45valuateResponse\x12\x10\n\x08response\x18\x01 \x01(\t\x12\x18\n\x04\x64\x61ta\x18\x02 \x01(\x0b\x32\n.TensorMap\x12\x0c\n\x04\x63ode\x18\x03 \x01(\x05\x12\x0f\n\x07message\x18\x04 \x01(\t\"n\n\x0ePredictRequest\x12\x12\n\nclientuuid\x18\x01 \x01(\t\x12$\n\x08treeEval\x18\x02 \x03(\x0b\x32\x12.fgboost.BoostEval\x12\x0f\n\x07version\x18\x03 \x01(\x05\x12\x11\n\tlastBatch\x18\x04 \x01(\x08\"E\n\x0cSplitRequest\x12\x12\n\nclientuuid\x18\x01 \x01(\t\x12!\n\x05split\x18\x02 \x01(\x0b\x32\x12.fgboost.DataSplit\"K\n\x0fPredictResponse\x12\x10\n\x08response\x18\x01 \x01(\t\x12\x18\n\x04\x64\x61ta\x18\x02 \x01(\x0b\x32\n.TensorMap\x12\x0c\n\x04\x63ode\x18\x03 \x01(\x05\"R\n\rSplitResponse\x12!\n\x05split\x18\x01 \x01(\x0b\x32\x12.fgboost.DataSplit\x12\x10\n\x08response\x18\x02 \x01(\t\x12\x0c\n\x04\x63ode\x18\x03 \x01(\x05\x32\xf1\x03\n\x0e\x46GBoostService\x12\x45\n\x0buploadLabel\x12\x1b.fgboost.UploadLabelRequest\x1a\x17.fgboost.UploadResponse\"\x00\x12K\n\rdownloadLabel\x12\x1d.fgboost.DownloadLabelRequest\x1a\x19.fgboost.DownloadResponse\"\x00\x12\x38\n\x05split\x12\x15.fgboost.SplitRequest\x1a\x16.fgboost.SplitResponse\"\x00\x12\x41\n\x08register\x12\x18.fgboost.RegisterRequest\x1a\x19.fgboost.RegisterResponse\"\x00\x12K\n\x0euploadTreeLeaf\x12\x1e.fgboost.UploadTreeLeafRequest\x1a\x17.fgboost.UploadResponse\"\x00\x12\x41\n\x08\x65valuate\x12\x18.fgboost.EvaluateRequest\x1a\x19.fgboost.EvaluateResponse\"\x00\x12>\n\x07predict\x12\x17.fgboost.PredictRequest\x1a\x18.fgboost.PredictResponse\"\x00\x42\x42\n+com.intel.analytics.bigdl.ppml.fl.generatedB\x13\x46GBoostServiceProtob\x06proto3')



_UPLOADLABELREQUEST = DESCRIPTOR.message_types_by_name['UploadLabelRequest']
_DOWNLOADLABELREQUEST = DESCRIPTOR.message_types_by_name['DownloadLabelRequest']
_DOWNLOADRESPONSE = DESCRIPTOR.message_types_by_name['DownloadResponse']
_TREELEAF = DESCRIPTOR.message_types_by_name['TreeLeaf']
_UPLOADTREELEAFREQUEST = DESCRIPTOR.message_types_by_name['UploadTreeLeafRequest']
_DATASPLIT = DESCRIPTOR.message_types_by_name['DataSplit']
_UPLOADRESPONSE = DESCRIPTOR.message_types_by_name['UploadResponse']
_TREEPREDICT = DESCRIPTOR.message_types_by_name['TreePredict']
_BOOSTPREDICT = DESCRIPTOR.message_types_by_name['BoostPredict']
_BOOSTEVAL = DESCRIPTOR.message_types_by_name['BoostEval']
_REGISTERREQUEST = DESCRIPTOR.message_types_by_name['RegisterRequest']
_REGISTERRESPONSE = DESCRIPTOR.message_types_by_name['RegisterResponse']
_UPLOADTREEEVALREQUEST = DESCRIPTOR.message_types_by_name['UploadTreeEvalRequest']
_EVALUATEREQUEST = DESCRIPTOR.message_types_by_name['EvaluateRequest']
_EVALUATERESPONSE = DESCRIPTOR.message_types_by_name['EvaluateResponse']
_PREDICTREQUEST = DESCRIPTOR.message_types_by_name['PredictRequest']
_SPLITREQUEST = DESCRIPTOR.message_types_by_name['SplitRequest']
_PREDICTRESPONSE = DESCRIPTOR.message_types_by_name['PredictResponse']
_SPLITRESPONSE = DESCRIPTOR.message_types_by_name['SplitResponse']
UploadLabelRequest = _reflection.GeneratedProtocolMessageType('UploadLabelRequest', (_message.Message,), {
  'DESCRIPTOR' : _UPLOADLABELREQUEST,
  '__module__' : 'fgboost_service_pb2'
  # @@protoc_insertion_point(class_scope:fgboost.UploadLabelRequest)
  })
_sym_db.RegisterMessage(UploadLabelRequest)

DownloadLabelRequest = _reflection.GeneratedProtocolMessageType('DownloadLabelRequest', (_message.Message,), {
  'DESCRIPTOR' : _DOWNLOADLABELREQUEST,
  '__module__' : 'fgboost_service_pb2'
  # @@protoc_insertion_point(class_scope:fgboost.DownloadLabelRequest)
  })
_sym_db.RegisterMessage(DownloadLabelRequest)

DownloadResponse = _reflection.GeneratedProtocolMessageType('DownloadResponse', (_message.Message,), {
  'DESCRIPTOR' : _DOWNLOADRESPONSE,
  '__module__' : 'fgboost_service_pb2'
  # @@protoc_insertion_point(class_scope:fgboost.DownloadResponse)
  })
_sym_db.RegisterMessage(DownloadResponse)

TreeLeaf = _reflection.GeneratedProtocolMessageType('TreeLeaf', (_message.Message,), {
  'DESCRIPTOR' : _TREELEAF,
  '__module__' : 'fgboost_service_pb2'
  # @@protoc_insertion_point(class_scope:fgboost.TreeLeaf)
  })
_sym_db.RegisterMessage(TreeLeaf)

UploadTreeLeafRequest = _reflection.GeneratedProtocolMessageType('UploadTreeLeafRequest', (_message.Message,), {
  'DESCRIPTOR' : _UPLOADTREELEAFREQUEST,
  '__module__' : 'fgboost_service_pb2'
  # @@protoc_insertion_point(class_scope:fgboost.UploadTreeLeafRequest)
  })
_sym_db.RegisterMessage(UploadTreeLeafRequest)

DataSplit = _reflection.GeneratedProtocolMessageType('DataSplit', (_message.Message,), {
  'DESCRIPTOR' : _DATASPLIT,
  '__module__' : 'fgboost_service_pb2'
  # @@protoc_insertion_point(class_scope:fgboost.DataSplit)
  })
_sym_db.RegisterMessage(DataSplit)

UploadResponse = _reflection.GeneratedProtocolMessageType('UploadResponse', (_message.Message,), {
  'DESCRIPTOR' : _UPLOADRESPONSE,
  '__module__' : 'fgboost_service_pb2'
  # @@protoc_insertion_point(class_scope:fgboost.UploadResponse)
  })
_sym_db.RegisterMessage(UploadResponse)

TreePredict = _reflection.GeneratedProtocolMessageType('TreePredict', (_message.Message,), {
  'DESCRIPTOR' : _TREEPREDICT,
  '__module__' : 'fgboost_service_pb2'
  # @@protoc_insertion_point(class_scope:fgboost.TreePredict)
  })
_sym_db.RegisterMessage(TreePredict)

BoostPredict = _reflection.GeneratedProtocolMessageType('BoostPredict', (_message.Message,), {
  'DESCRIPTOR' : _BOOSTPREDICT,
  '__module__' : 'fgboost_service_pb2'
  # @@protoc_insertion_point(class_scope:fgboost.BoostPredict)
  })
_sym_db.RegisterMessage(BoostPredict)

BoostEval = _reflection.GeneratedProtocolMessageType('BoostEval', (_message.Message,), {
  'DESCRIPTOR' : _BOOSTEVAL,
  '__module__' : 'fgboost_service_pb2'
  # @@protoc_insertion_point(class_scope:fgboost.BoostEval)
  })
_sym_db.RegisterMessage(BoostEval)

RegisterRequest = _reflection.GeneratedProtocolMessageType('RegisterRequest', (_message.Message,), {
  'DESCRIPTOR' : _REGISTERREQUEST,
  '__module__' : 'fgboost_service_pb2'
  # @@protoc_insertion_point(class_scope:fgboost.RegisterRequest)
  })
_sym_db.RegisterMessage(RegisterRequest)

RegisterResponse = _reflection.GeneratedProtocolMessageType('RegisterResponse', (_message.Message,), {
  'DESCRIPTOR' : _REGISTERRESPONSE,
  '__module__' : 'fgboost_service_pb2'
  # @@protoc_insertion_point(class_scope:fgboost.RegisterResponse)
  })
_sym_db.RegisterMessage(RegisterResponse)

UploadTreeEvalRequest = _reflection.GeneratedProtocolMessageType('UploadTreeEvalRequest', (_message.Message,), {
  'DESCRIPTOR' : _UPLOADTREEEVALREQUEST,
  '__module__' : 'fgboost_service_pb2'
  # @@protoc_insertion_point(class_scope:fgboost.UploadTreeEvalRequest)
  })
_sym_db.RegisterMessage(UploadTreeEvalRequest)

EvaluateRequest = _reflection.GeneratedProtocolMessageType('EvaluateRequest', (_message.Message,), {
  'DESCRIPTOR' : _EVALUATEREQUEST,
  '__module__' : 'fgboost_service_pb2'
  # @@protoc_insertion_point(class_scope:fgboost.EvaluateRequest)
  })
_sym_db.RegisterMessage(EvaluateRequest)

EvaluateResponse = _reflection.GeneratedProtocolMessageType('EvaluateResponse', (_message.Message,), {
  'DESCRIPTOR' : _EVALUATERESPONSE,
  '__module__' : 'fgboost_service_pb2'
  # @@protoc_insertion_point(class_scope:fgboost.EvaluateResponse)
  })
_sym_db.RegisterMessage(EvaluateResponse)

PredictRequest = _reflection.GeneratedProtocolMessageType('PredictRequest', (_message.Message,), {
  'DESCRIPTOR' : _PREDICTREQUEST,
  '__module__' : 'fgboost_service_pb2'
  # @@protoc_insertion_point(class_scope:fgboost.PredictRequest)
  })
_sym_db.RegisterMessage(PredictRequest)

SplitRequest = _reflection.GeneratedProtocolMessageType('SplitRequest', (_message.Message,), {
  'DESCRIPTOR' : _SPLITREQUEST,
  '__module__' : 'fgboost_service_pb2'
  # @@protoc_insertion_point(class_scope:fgboost.SplitRequest)
  })
_sym_db.RegisterMessage(SplitRequest)

PredictResponse = _reflection.GeneratedProtocolMessageType('PredictResponse', (_message.Message,), {
  'DESCRIPTOR' : _PREDICTRESPONSE,
  '__module__' : 'fgboost_service_pb2'
  # @@protoc_insertion_point(class_scope:fgboost.PredictResponse)
  })
_sym_db.RegisterMessage(PredictResponse)

SplitResponse = _reflection.GeneratedProtocolMessageType('SplitResponse', (_message.Message,), {
  'DESCRIPTOR' : _SPLITRESPONSE,
  '__module__' : 'fgboost_service_pb2'
  # @@protoc_insertion_point(class_scope:fgboost.SplitResponse)
  })
_sym_db.RegisterMessage(SplitResponse)

_FGBOOSTSERVICE = DESCRIPTOR.services_by_name['FGBoostService']
if _descriptor._USE_C_DESCRIPTORS == False:

  DESCRIPTOR._options = None
  DESCRIPTOR._serialized_options = b'\n+com.intel.analytics.bigdl.ppml.fl.generatedB\023FGBoostServiceProto'
  _UPLOADLABELREQUEST._serialized_start=49
  _UPLOADLABELREQUEST._serialized_end=134
  _DOWNLOADLABELREQUEST._serialized_start=136
  _DOWNLOADLABELREQUEST._serialized_end=206
  _DOWNLOADRESPONSE._serialized_start=208
  _DOWNLOADRESPONSE._serialized_end=284
  _TREELEAF._serialized_start=286
  _TREELEAF._serialized_end=368
  _UPLOADTREELEAFREQUEST._serialized_start=370
  _UPLOADTREELEAFREQUEST._serialized_end=450
  _DATASPLIT._serialized_start=453
  _DATASPLIT._serialized_end=621
  _UPLOADRESPONSE._serialized_start=623
  _UPLOADRESPONSE._serialized_end=671
  _TREEPREDICT._serialized_start=673
  _TREEPREDICT._serialized_end=720
  _BOOSTPREDICT._serialized_start=722
  _BOOSTPREDICT._serialized_end=776
  _BOOSTEVAL._serialized_start=778
  _BOOSTEVAL._serialized_end=830
  _REGISTERREQUEST._serialized_start=832
  _REGISTERREQUEST._serialized_end=884
  _REGISTERRESPONSE._serialized_start=886
  _REGISTERRESPONSE._serialized_end=936
  _UPLOADTREEEVALREQUEST._serialized_start=938
  _UPLOADTREEEVALREQUEST._serialized_end=1036
  _EVALUATEREQUEST._serialized_start=1038
  _EVALUATEREQUEST._serialized_end=1149
  _EVALUATERESPONSE._serialized_start=1151
  _EVALUATERESPONSE._serialized_end=1244
  _PREDICTREQUEST._serialized_start=1246
  _PREDICTREQUEST._serialized_end=1356
  _SPLITREQUEST._serialized_start=1358
  _SPLITREQUEST._serialized_end=1427
  _PREDICTRESPONSE._serialized_start=1429
  _PREDICTRESPONSE._serialized_end=1504
  _SPLITRESPONSE._serialized_start=1506
  _SPLITRESPONSE._serialized_end=1588
  _FGBOOSTSERVICE._serialized_start=1591
  _FGBOOSTSERVICE._serialized_end=2088
# @@protoc_insertion_point(module_scope)
