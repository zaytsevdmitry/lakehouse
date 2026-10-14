#!/usr/bin/env bash

# "Jarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
# Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/jarina
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

kubectl get pods -A

echo build helm dependencies
helm dependency update ./jarina-management-helm-charts/jarina-management

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
load_image_if_missing "jarina:0.12.0"
load_image_if_missing "jarina-s3-check:0.12.0"
load_image_if_missing "jarina-hms:0.12.0"
load_image_if_missing "jarina-spark-aws:0.12.0"

echo -e "\e[34mAll jarina images processed\e[0m"

echo install jarina-release
helm install jarina-release ./jarina-management-helm-charts/jarina-management --create-namespace  --namespace jarina-management

kubectl -n jarina-management get pods -o custom-columns="NAME:.metadata.name,STATUS:.status.phase,TASK-NAME:.metadata.annotations.jarina-management-task"

bash ./bootstrap-configs.bash
/etc/profile.d via Read/Glob.


kubectl -n jarina-management get pods -o custom-columns="NAME:.metadata.name,STATUS:.status.phase,TASK-NAME:.metadata.annotations.jarina-management-task"

bash ./tunnels.bash

kubectl -n jarina-management get pods -o custom-columns="NAME:.metadata.name,STATUS:.status.phase,TASK-NAME:.metadata.annotations.jarina-management-task"

echo -e "\e[37;42m All services installed to namespace jarina-management \e[0m"


