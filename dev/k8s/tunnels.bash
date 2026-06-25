#!/usr/bin/env bash

# "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
# Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
# 
#     This program is free software: you can redistribute it and/or modify
#     it under the terms of the GNU Affero General Public License as
#     published by the Free Software Foundation, either version 3 of the
#     License, or (at your option) any later version.
# 
#     This program is distributed in the hope that it will be useful,
#     but WITHOUT ANY WARRANTY; without even the implied warranty of
#     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#     GNU Affero General Public License for more details.
# 
#     You should have received a copy of the GNU Affero General Public License
#     along with this program.  If not, see <https://www.gnu.org/licenses/>.

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