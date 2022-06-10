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

from bigdl.ppml.api import *
import argparse


class PPMLContext(JavaValue):
    def __init__(self, jvalue=None, *args):
        self.bigdl_type = "float"
        super().__init__(jvalue, self.bigdl_type, *args)

    def load_keys(self, primary_key_path, data_key_path):
        callBigDlFunc(self.bigdl_type, "loadKeys", self.value, primary_key_path, data_key_path)

    def read(self, crypto_mode):
        df_reader = callBigDlFunc(self.bigdl_type, "read", self.value, crypto_mode)
        return EncryptedDataFrameReader(self.bigdl_type, df_reader)

    def write(self, dataframe, crypto_mode):
        df_writer = callBigDlFunc(self.bigdl_type, "write", self.value, dataframe, crypto_mode)
        return EncryptedDataFrameWriter(self.bigdl_type, df_writer)


class EncryptedDataFrameReader:
    def __init__(self, bigdl_type, df_reader):
        self.bigdl_type = bigdl_type
        self.df_reader = df_reader

    def option(self, key, value):
        self.df_reader = callBigDlFunc(self.bigdl_type, "option", self.df_reader, key, value)
        return self

    def csv(self, path):
        return callBigDlFunc(self.bigdl_type, "csv", self.df_reader, path)


class EncryptedDataFrameWriter:
    support_mode = {"overwrite", "append", "ignore", "error", "errorifexists"}

    def __init__(self, bigdl_type, df_writer):
        self.bigdl_type = bigdl_type
        self.df_writer = df_writer

    def option(self, key, value):
        self.df_writer = callBigDlFunc(self.bigdl_type, "option", self.df_writer, key, value)
        return self

    def mode(self, mode):
        if mode not in EncryptedDataFrameWriter.support_mode:
            raise ValueError("Unknown save mode: " + mode + "." +
                             "Accepted save modes are 'overwrite', 'append', 'ignore', 'error', 'errorifexists'.")
        self.df_writer = callBigDlFunc(self.bigdl_type, "mode", self.df_writer, mode)
        return self

    def csv(self, path):
        return callBigDlFunc(self.bigdl_type, "csv", self.df_writer, path)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument("--simple_app_id", type=str, required=True, help="simple app id")
    parser.add_argument("--simple_app_key", type=str, required=True, help="simple app key")
    parser.add_argument("--primary_key_path", type=str, required=True, help="primary key path")
    parser.add_argument("--data_key_path", type=str, required=True, help="data key path")
    parser.add_argument("--input_encrypt_mode", type=str, required=True, help="input encrypt mode")
    parser.add_argument("--output_encrypt_mode", type=str, required=True, help="output encrypt mode")
    parser.add_argument("--input_path", type=str, required=True, help="input path")
    parser.add_argument("--output_path", type=str, required=True, help="output path")
    parser.add_argument("--kms_type", type=str, default="SimpleKeyManagementService",
                        help="SimpleKeyManagementService or EHSMKeyManagementService")
    args = parser.parse_args()
    arg_dict = vars(args)

    sc = PPMLContext(None, 'testApp', arg_dict)
    df = sc.read(args.input_encrypt_mode) \
        .option("header", "true") \
        .csv(args.input_path)

    df.select("name").count()

    df.select(df["name"], df["age"] + 1).show()

    developers = df.filter((df["job"] == "Developer") & df["age"].between(20, 40)).toDF("name", "age", "job")

    sc.write(developers, args.output_encrypt_mode) \
        .mode('overwrite') \
        .option("header", True) \
        .csv(args.output_path)
