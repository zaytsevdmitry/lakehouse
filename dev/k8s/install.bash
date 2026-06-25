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

set -e
set -v
kubectl get pods -A

echo build helm dependencies
helm dependency update ./lakehouse-management-helm-charts/lakehouse-management

echo -e "\e[34mecho copy image lakehouse:0.5.0 ~ 2-3 minutes\e[0m"
minikube image load lakehouse:0.5.0 --daemon --alsologtostderr -v=1

echo -e "\e[34mecho copy image lakehouse-s3-check  ~ 2-3 minutes\e[0m"
minikube image load lakehouse-s3-check:0.5.0 --daemon  --alsologtostderr -v=1

echo -e "\e[34mecho copy image lakehouse-hms:0.5.0  ~ 2-3 minutes\e[0m"
minikube image load lakehouse-hms:0.5.0  --daemon --alsologtostderr -v=1

echo -e "\e[34mecho lakehouse-spark-aws:0.5.0  ~ 2-3 minutes\e[0m"
minikube image load lakehouse-spark-aws:0.5.0 --daemon  --alsologtostderr -v=1

echo -e "\e[34mecho All lakehouse images loaded\e[0m"

echo install lakehouse-release
helm install lakehouse-release ./lakehouse-management-helm-charts/lakehouse-management --create-namespace  --namespace lakehouse-management


echo -e "\e[37;42m All services installed to namespace lakehouse-management \e[0m"