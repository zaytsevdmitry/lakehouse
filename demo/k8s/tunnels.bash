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


for svc in \
  "spark-history 18080:18080" \
  "lakehouse-release-trino 9090:8080" \
  "minio 9001:9001" \
  "minio 9000:9000" \
  "lakehouse-management-config-service 8080:8080" \
  "lakehouse-management-state-service 8081:8081" \
  "db-dev 5432:5432" \
  "broker 9092:9092"
do
  set -- $svc
  kubectl port-forward svc/$1 $2 -n lakehouse-management &
done