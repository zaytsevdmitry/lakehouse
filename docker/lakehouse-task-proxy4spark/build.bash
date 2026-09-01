#!/usr/bin/env bash

# "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
# Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
# 
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
#     https://www.apache.org/licenses/LICENSE-2.0.txt
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


set -e
export LH_VERSION=0.10.0
cp -f ../../lakehouse-task-proxy-for-spark/target/lakehouse-task-proxy-for-spark-0.10.0-jar-with-dependencies.jar ./

docker build  -t lakehouse-task-proxy4spark:$LH_VERSION ./
rm ./lakehouse*.jar
docker images | grep lakehouse