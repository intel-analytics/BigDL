#!/bin/bash

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

#set -x
DB_FILE_PAHT=/ppml/data/kms.db

if [! -f $DB_FILE_PAHT ]
then
  echo "[INFO] db does not exist, creating..."
  sqlite3 $DB_FILE_PAHT "create table user (name TEXT PRIMARY KEY,token TEXT);"
  sqlite3 $DB_FILE_PAHT "create table key (user TEXT,name TEXT,data TEXT, PRIMARY KEY (user, name));"
else
  echo "[INFO] db file has existed, start from it..."
fi

HTTPS_KEY_STORE_TOKEN=`openssl rsautl -inkey /ppml/password/key.txt -decrypt </ppml/password/output.bin`
HTTPS_KEY_STORE_PAHT=/ppml/keys/keystore.pkcs12

java \
  -Xms2g \
  -Xmx10g \
  -Dcom.intel.analytics.zoo.shaded.io.netty.tryReflectionSetAccessible=true \
  -cp /ppml/${SPARK_VERSION}-${BIGDL_VERSION}-jar-with-dependencies.jar \
  com.intel.analytics.bigdl.ppml.kms.EasyKeyManagementServer \
  --httpsKeyStorePath "${HTTPS_KEY_STORE_PAHT}" \
  --httpsKeyStoreToken "${HTTPS_KEY_STORE_TOKEN}" | tee ./easy-kms-server.log
