#!/bin/bash
set -x

export ATTESTATION_URL=your_attestation_url
export ATTESTATION_TYPE=your_attestation_service_type
export APP_ID=your_app_id
export API_KEY=your_api_key
export CHALLENGE=your_challenge_string
export JARS=/ppml/jars/*:

if [ "$ATTESTATION_URL" = "your_attestation_url" ]; then
    echo "[ERROR] ATTESTATION_URL is not set!"
    echo "[INFO] PPML Application Exit!"
    exit 1
fi
if [ "$ATTESTATION_TYPE" = "your_attestation_service_type" ]; then
    ATTESTATION_TYPE="EHSMAttestationService"
fi
if [ "$APP_ID" = "your_app_id" ]; then
    echo "[ERROR] APP_ID is not set!"
    echo "[INFO] PPML Application Exit!"
    exit 1
fi
if [ "$API_KEY" = "your_api_key" ]; then
    echo "[ERROR] API_KEY is not set!"
    echo "[INFO] PPML Application Exit!"
    exit 1
fi
if [ "$CHALLENGE" = "your_challenge_string" ]; then
    echo "[ERROR] CHALLENGE is not set!"
    echo "[INFO] PPML Application Exit!"
    exit 1
fi

# Set PCCS conf
if [ "$PCCS_URL" != "" ] ; then
    echo 'PCCS_URL='${PCCS_URL}'/sgx/certification/v3/' > /etc/sgx_default_qcnl.conf
    echo 'USE_SECURE_CERT=FALSE' >> /etc/sgx_default_qcnl.conf
fi

java -cp $JARS com.intel.analytics.bigdl.ppml.attestation.VerificationCLI -i $APP_ID -k $API_KEY -c $CHALLENGE -u $ATTESTATION_URL -t $ATTESTATION_TYPE
