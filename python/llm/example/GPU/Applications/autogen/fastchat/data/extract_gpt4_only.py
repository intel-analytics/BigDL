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
# Copyright 2023 The FastChat team. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
"""
Extract the conversations generated by GPT-4 only.

Usage: python3 -m fastchat.data.extract_gpt4_only --in sharegpt.json
"""
import argparse
import json


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--in-file", type=str, required=True)
    parser.add_argument("--out-file", type=str)
    parser.add_argument("--begin", type=int)
    parser.add_argument("--end", type=int)
    args = parser.parse_args()

    content = json.load(open(args.in_file, "r"))
    content = content[args.begin : args.end]
    new_content = []
    for c in content:
        model = c.get("model", None)
        if model == "gpt4" or model is None:
            new_content.append(c)

    if args.out_file:
        out_file = args.out_file
    else:
        out_file = args.in_file.replace(".json", "_gpt4.json")

    print(f"#in: {len(content)}, #out: {len(new_content)}")
    json.dump(new_content, open(out_file, "w"), indent=2, ensure_ascii=False)
