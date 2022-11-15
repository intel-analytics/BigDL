# set -x
rm /etc/sgx_default_qcnl.conf
#echo "PCCS_URL=$PCCS_URL" > /etc/sgx_default_qcnl.conf
echo 'PCCS_URL='${PCCS_URL}'/sgx/certification/v3/' > /etc/sgx_default_qcnl.conf
echo "USE_SECURE_CERT=FALSE" >> /etc/sgx_default_qcnl.conf
EHSM_URL=${ATTESTATION_URL}
EHSM_KMS_IP=${EHSM_URL%:*}
EHSM_KMS_PORT=${EHSM_URL#*:}
export KMS_TYPE=ehsm
if [ "$action" = "enroll" ]; then
  # no support
	if [ "$KMS_TYPE" = "ehsm" ]; then
		cd /home/ehsm/out/ehsm-kms_enroll_app/
		./ehsm-kms_enroll_app -a http://$EHSM_KMS_IP:$EHSM_KMS_PORT/ehsm/
	elif [ "$KMS_TYPE" = "simple" ]; then
		echo "Simple KMS is dummy. You can choose any appid and apikey. If you want to generate the corresponding primarykey and datakey, the appid must be 12 characters long."
	elif [ "$KMS_TYPE" = "azure" ]; then
	    keyVaultName=$2
	    id=$3
		az keyvault set-policy --name $keyVaultName --object-id $id \
		--secret-permissions all --key-permissions all --certificate-permissions all
	else
		echo "Wrong KMS_TYPE! KMS_TYPE can be (1) ehsm, (2) simple, (3) azure"
		return -1
	fi
elif [ "$action" = "generatekeys" ]; then
	if [ "$KMS_TYPE" = "ehsm" ]; then
	    appid=$2
	    apikey=$3
		java -cp $BIGDL_HOME/jars/bigdl-ppml-spark_${SPARK_VERSION}-${BIGDL_VERSION}.jar:$SPARK_HOME/jars/*:$SPARK_HOME/examples/jars/*:$BIGDL_HOME/jars/* \
		com.intel.analytics.bigdl.ppml.examples.GenerateKeys \
		--primaryKeyPath /home/key1/ehsm_encrypted_primary_key \
		--dataKeyPath /home/key1/ehsm_encrypted_data_key \
		--kmsType EHSMKeyManagementService \
		--kmsServerIP $EHSM_KMS_IP \
		--kmsServerPort $EHSM_KMS_PORT \
		--ehsmAPPID $appid \
		--ehsmAPIKEY $apikey
	elif [ "$KMS_TYPE" = "simple" ]; then
	    appid=$2
	    apikey=$3
		java -cp $BIGDL_HOME/jars/bigdl-ppml-spark_${SPARK_VERSION}-${BIGDL_VERSION}.jar:$SPARK_HOME/jars/*:$SPARK_HOME/examples/jars/*:$BIGDL_HOME/jars/* \
		com.intel.analytics.bigdl.ppml.examples.GenerateKeys \
		--primaryKeyPath /home/key/simple_encrypted_primary_key \
		--dataKeyPath /home/key/simple_encrypted_data_key \
		--kmsType SimpleKeyManagementService \
		--simpleAPPID $appid \
		--simpleAPIKEY $apikey
	elif [ "$KMS_TYPE" = "azure" ]; then
	    keyVaultName=$2
		java -cp $BIGDL_HOME/jars/bigdl-ppml-spark_${SPARK_VERSION}-${BIGDL_VERSION}.jar:$SPARK_HOME/jars/*:$SPARK_HOME/examples/jars/*:$BIGDL_HOME/jars/* \
		com.intel.analytics.bigdl.ppml.examples.GenerateKeys \
		--primaryKeyPath /home/key/simple_encrypted_primary_key \
		--dataKeyPath /home/key/simple_encrypted_data_key \
		--kmsType AzureKeyManagementService \
		--vaultName $keyVaultName
	else
		echo "Wrong KMS_TYPE! KMS_TYPE can be (1) ehsm, (2) simple, (3) azure"
		return -1
	fi
elif [ "$action" = "encrypt" ]; then
	appid=$2
	apikey=$3
	input_path=$4
	if [ "$KMS_TYPE" = "ehsm" ]; then
	    appid=$2
	    apikey=$3
		java -cp $BIGDL_HOME/jars/bigdl-ppml-spark_${SPARK_VERSION}-${BIGDL_VERSION}.jar:$SPARK_HOME/jars/*:$SPARK_HOME/examples/jars/*:$BIGDL_HOME/jars/* \
		com.intel.analytics.bigdl.ppml.examples.Encrypt \
		--inputPath $input_path \
		--primaryKeyPath /home/key/ehsm_encrypted_primary_key \
                --dataKeyPath /home/key/ehsm_encrypted_data_key \
                --kmsType EHSMKeyManagementService \
		--kmsServerIP $EHSM_KMS_IP \
                --kmsServerPort $EHSM_KMS_PORT \
                --ehsmAPPID $appid \
                --ehsmAPIKEY $apikey
	elif [ "$KMS_TYPE" = "simple" ]; then
	    appid=$2
	    apikey=$3
		java -cp $BIGDL_HOME/jars/bigdl-ppml-spark_${SPARK_VERSION}-${BIGDL_VERSION}.jar:$SPARK_HOME/jars/*:$SPARK_HOME/examples/jars/*:$BIGDL_HOME/jars/* \
		com.intel.analytics.bigdl.ppml.examples.Encrypt \
                --inputPath $input_path \
                --primaryKeyPath /home/key/simple_encrypted_primary_key \
                --dataKeyPath /home/key/simple_encrypted_data_key \
                --kmsType SimpleKeyManagementService \
		--simpleAPPID $appid \
                --simpleAPIKEY $apikey
    elif [ "$KMS_TYPE" = "azure" ]; then
        keyVaultName=$2
		java -cp $BIGDL_HOME/jars/bigdl-ppml-spark_${SPARK_VERSION}-${BIGDL_VERSION}.jar:$SPARK_HOME/jars/*:$SPARK_HOME/examples/jars/*:$BIGDL_HOME/jars/* \
		com.intel.analytics.bigdl.ppml.examples.Encrypt \
                --inputPath $input_path \
                --primaryKeyPath /home/key/simple_encrypted_primary_key \
                --dataKeyPath /home/key/simple_encrypted_data_key \
                --kmsType AzureKeyManagementService \
		--vaultName $keyVaultName
	else
		echo "Wrong KMS_TYPE! KMS_TYPE can be (1) ehsm, (2) simple, (3) azure"
                return -1
        fi
elif [ "$action" = "encryptwithrepartition" ]; then
	if [ "$KMS_TYPE" = "ehsm" ]; then
	    appid=$2
            apikey=$3
	    input_path=$4
	    output_path=$input_path.encrypted
		java -cp $BIGDL_HOME/jars/bigdl-ppml-spark_${SPARK_VERSION}-${BIGDL_VERSION}.jar:$SPARK_HOME/jars/*:$SPARK_HOME/examples/jars/*:$BIGDL_HOME/jars/* \
		com.intel.analytics.bigdl.ppml.examples.EncryptWithRepartition   \
		--inputPath $input_path \
		--outputPath $output_path \
		--inputEncryptModeValue plain_text \
                --outputEncryptModeValue AES/CBC/PKCS5Padding \
		--outputPartitionNum 4 \
		--primaryKeyPath /home/key/ehsm_encrypted_primary_key \
		--dataKeyPath /home/key/ehsm_encrypted_data_key \
		--kmsType EHSMKeyManagementService \
		--kmsServerIP $EHSM_KMS_IP \
                --kmsServerPort $EHSM_KMS_PORT \
                --ehsmAPPID $appid \
                --ehsmAPIKEY $apikey
	elif [ "$KMS_TYPE" = "simple" ]; then
	    appid=$2
            apikey=$3
	    input_path=$4
	    output_path=$input_path.encrypted
		java -cp $BIGDL_HOME/jars/bigdl-ppml-spark_${SPARK_VERSION}-${BIGDL_VERSION}.jar:$SPARK_HOME/jars/*:$SPARK_HOME/examples/jars/*:$BIGDL_HOME/jars/* \
		com.intel.analytics.bigdl.ppml.examples.EncryptWithRepartition   \
		--inputPath $input_path \
		--outputPath $output_path \
		--inputEncryptModeValue plain_text \
                --outputEncryptModeValue AES/CBC/PKCS5Padding \
		--outputPartitionNum 4 \
                --primaryKeyPath /home/key/simple_encrypted_primary_key \
                --dataKeyPath /home/key/simple_encrypted_data_key \
		--kmsType SimpleKeyManagementService \
                --simpleAPPID $appid \
                --simpleAPIKEY $apikey
    elif [ "$KMS_TYPE" = "azure" ]; then
        keyVaultName=$2
        input_path=$3
	    output_path=$input_path.encrypted
		java -cp $BIGDL_HOME/jars/bigdl-ppml-spark_${SPARK_VERSION}-${BIGDL_VERSION}.jar:$SPARK_HOME/jars/*:$SPARK_HOME/examples/jars/*:$BIGDL_HOME/jars/* \
		com.intel.analytics.bigdl.ppml.examples.EncryptWithRepartition   \
		--inputPath $input_path \
		--outputPath $output_path \
		--inputEncryptModeValue plain_text \
                --outputEncryptModeValue AES/CBC/PKCS5Padding \
		--outputPartitionNum 4 \
                --primaryKeyPath /home/key/simple_encrypted_primary_key \
                --dataKeyPath /home/key/simple_encrypted_data_key \
		--kmsType AzureKeyManagementService \
                --vaultName $keyVaultName
	else
                echo "Wrong KMS_TYPE! KMS_TYPE can be (1) ehsm, (2) simple, (3) azure"
                return -1
        fi
elif [ "$action" = "decrypt" ]; then
	if [ "$KMS_TYPE" = "ehsm" ]; then
	appid=$2
        apikey=$3
        input_path=$4
		java -cp $BIGDL_HOME/jars/bigdl-ppml-spark_${SPARK_VERSION}-${BIGDL_VERSION}.jar:$SPARK_HOME/jars/*:$SPARK_HOME/examples/jars/*:$BIGDL_HOME/jars/* \
		com.intel.analytics.bigdl.ppml.examples.Decrypt \
		--inputPath $input_path \
		--inputPartitionNum 8 \
		--outputPartitionNum 8 \
		--inputEncryptModeValue AES/CBC/PKCS5Padding \
		--outputEncryptModeValue plain_text \
		--primaryKeyPath /home/key/ehsm_encrypted_primary_key \
		--dataKeyPath /home/key/ehsm_encrypted_data_key \
		--kmsType EHSMKeyManagementService \
		--kmsServerIP $EHSM_KMS_IP \
                --kmsServerPort $EHSM_KMS_PORT \
                --ehsmAPPID $appid \
                --ehsmAPIKEY $apikey
	elif [ "$KMS_TYPE" = "simple" ]; then
	appid=$2
        apikey=$3
        input_path=$4
		java -cp $BIGDL_HOME/jars/bigdl-ppml-spark_${SPARK_VERSION}-${BIGDL_VERSION}.jar:$SPARK_HOME/jars/*:$SPARK_HOME/examples/jars/*:$BIGDL_HOME/jars/* \
		com.intel.analytics.bigdl.ppml.examples.Decrypt \
                --inputPath $input_path \
                --inputPartitionNum 8 \
                --outputPartitionNum 8 \
                --inputEncryptModeValue AES/CBC/PKCS5Padding \
                --outputEncryptModeValue plain_text \
                --primaryKeyPath /home/key/simple_encrypted_primary_key \
                --dataKeyPath /home/key/simple_encrypted_data_key \
                --kmsType SimpleKeyManagementService \
                --simpleAPPID $appid \
                --simpleAPIKEY $apikey
    elif [ "$KMS_TYPE" = "azure" ]; then
        keyVaultName=$2
        input_path=$3
		java -cp $BIGDL_HOME/jars/bigdl-ppml-spark_${SPARK_VERSION}-${BIGDL_VERSION}.jar:$SPARK_HOME/jars/*:$SPARK_HOME/examples/jars/*:$BIGDL_HOME/jars/* \
		com.intel.analytics.bigdl.ppml.examples.Decrypt \
                --inputPath $input_path \
                --inputPartitionNum 8 \
                --outputPartitionNum 8 \
                --inputEncryptModeValue AES/CBC/PKCS5Padding \
                --outputEncryptModeValue plain_text \
                --primaryKeyPath /home/key/simple_encrypted_primary_key \
                --dataKeyPath /home/key/simple_encrypted_data_key \
                --kmsType AzureKeyManagementService \
                --vaultName $keyVaultName
	else
                echo "Wrong KMS_TYPE! KMS_TYPE can be (1) ehsm, (2) simple, (3) azure"
                return -1
        fi
else
	echo "Wrong action! Action can be (1) enroll, (2) generatekeys, (3) encrypt, (4) decrypt, and (5) encryptwithrepartition."
fi
