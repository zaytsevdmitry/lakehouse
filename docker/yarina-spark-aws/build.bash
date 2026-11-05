#!/usr/bin/env bash

# "Yarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
# Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/yarina
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
export LH_VERSION=1.1.0
cp -f ../../yarina-task-executor-spark-dataset-app/target/yarina-task-executor-spark-dataset-app-${LH_VERSION}-jar-with-dependencies.jar ./
cp -f ../../yarina-task-executor-spark-dq-app/target/yarina-task-executor-spark-dq-app-${LH_VERSION}-jar-with-dependencies.jar ./
cp -f ../../yarina-task-proxy-for-spark/target/yarina-task-proxy-for-spark-${LH_VERSION}-jar-with-dependencies.jar ./
cp -f ../../yarina-credential-providers-jdbc/target/yarina-credential-providers-jdbc-${LH_VERSION}.jar ./
cp -f ../../yarina-credential-providers-spark/target/yarina-credential-providers-spark-${LH_VERSION}.jar ./
docker build  -t yarina-spark-aws:$LH_VERSION ./
rm ./yarina*.jar
docker images | grep yarina