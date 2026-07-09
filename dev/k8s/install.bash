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

# Function to check and load images into Minikube only if they are missing
load_image_if_missing() {
    local IMAGE=$1
    # Check if the image already exists inside minikube
    if minikube image list | grep -q "${IMAGE}"; then
        echo -e "\e[32mImage ${IMAGE} is already present in Minikube. Skipping.\e[0m"
    else
        echo -e "\e[34mLoading image ${IMAGE} ~ 2-3 minutes\e[0m"
        minikube image load "${IMAGE}" --daemon --alsologtostderr -v=1
    fi
}

# Load only missing images
load_image_if_missing "lakehouse:0.5.0"
load_image_if_missing "lakehouse-s3-check:0.5.0"
load_image_if_missing "lakehouse-hms:0.5.0"
load_image_if_missing "lakehouse-spark-aws:0.5.0"

echo -e "\e[34mAll lakehouse images processed\e[0m"

echo install lakehouse-release
helm install lakehouse-release ./lakehouse-management-helm-charts/lakehouse-management --create-namespace  --namespace lakehouse-management

echo -e "\e[37;42m All services installed to namespace lakehouse-management \e[0m"