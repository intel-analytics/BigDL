echo "inference_address=https://0.0.0.0:$INFERENCE_PORT" >> /ppml/config.yaml
echo "management_address=https://0.0.0.0:$MANAGEMENT_PORT" >> /ppml/config.yaml
echo "metrics_address=https://0.0.0.0:$METRICS_PORT" >> /ppml/config.yaml
echo "grpc_inference_port=7070" >> /ppml/config.yaml
echo "grpc_management_port=7071" >> /ppml/config.yaml
echo "model_store=/tmp/model/torchserve" >> /ppml/config.yaml
echo "load_models=$MODEL_NAME.mar" >> /ppml/config.yaml
echo "enable_metrics_api=true" >> /ppml/config.yaml
echo "models={ \"$MODEL_NAME\": { \"1.0\": {\"defaultVersion\": true,\"marName\": \"$MODEL_NAME\",\"workers\": $BACKEND_NUM,\"batchSize\": 1,\"maxBatchDelay\": 100,\"responseTimeout\": 1200}}}" >> /ppml/config.yaml
