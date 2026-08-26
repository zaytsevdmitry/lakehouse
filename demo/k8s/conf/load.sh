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
pwd
ls ./

CONFIG_URN="localhost:18081"
KEYCLOAK_URN="localhost:8085"

check_config_svc_ready() {
  healthz="http://${CONFIG_URN}/healthz"
  echo "Checking endpoint: $healthz"

  response=$(curl -sf -X GET "$healthz" || true)

  if [ -n "$response" ]; then
      echo "Config-SVC response: $response"
  else
      echo "Waiting Config-SVC: The request failed. Sleeping...zzZ"
      sleep 10
      echo "Retry Config-SVC"
      check_config_svc_ready
  fi
}


get_access_token() {
  token_url="http://${KEYCLOAK_URN}/realms/lakehouse/protocol/openid-connect/token"
  echo "Fetching access token from: $token_url"

  response=$(curl -s -X POST "$token_url" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "grant_type=client_credentials" \
    -d "client_id=lakehouse-internal-client" \
    -d "client_secret=super-secret-internal-key-987654321")


  ACCESS_TOKEN=$(echo "$response" | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p')

  if [ -z "$ACCESS_TOKEN" ] || [ "$ACCESS_TOKEN" = "$response" ]; then
      echo "Error: Failed to obtain access token from Keycloak!"
      echo "Keycloak response: $response"
      echo "Sleeping 10s and retrying..."
      sleep 10
      get_access_token
  else
      echo "Token obtained successfully! (Length: ${#ACCESS_TOKEN} chars)"
  fi
}

function curlPost() {
    PATH_URL=$1
    JSON_FILE=$2
    FULL_URL="http://${CONFIG_URN}${PATH_URL}"

    echo "Post to ${FULL_URL} file ${JSON_FILE}"

    response_code=$(curl -w "%{http_code}" -s -X POST "$FULL_URL" \
         -H "Authorization: Bearer $ACCESS_TOKEN" \
         -H "Content-Type: application/json" \
         --data-binary "@$JSON_FILE" -o /tmp/curl_json_resp.txt)

    if [ "$response_code" -ne 201 ] && [ "$response_code" -ne 200 ]; then
        echo -e "\n❌ Server returned error $response_code for URL: $FULL_URL"
        echo "Error message from server:"
        cat /tmp/curl_json_resp.txt
        echo -e "\n"
        exit 1
    fi
}


check_config_svc_ready
get_access_token

echo ">>> Uploading SQL scripts..."
find ./sql-scripts/ -type f -name "*.sql" | while read -r f; do
    clean_name="${f#./sql-scripts/}"
    scriptName="${clean_name//\//.}"

    echo "Uploading script: $scriptName"
    curl -f -i -X POST "http://${CONFIG_URN}/v1_0/configs/scripts/$scriptName" \
         -H "Authorization: Bearer $ACCESS_TOKEN" \
         -H "Content-Type: text/plain" \
         --data-binary "@$f"
done


CATEGORIES=(
    "nameSpaces"
    "drivers"
    "datasources"
    "taskexecutionservicegroups"
    "tasks"
    "datasets"
    "scenarios"
    "schedules"
    "quality/metrics"
)

for category in "${CATEGORIES[@]}"; do
    echo ">>> Processing category: $category"

    find "./$category" -maxdepth 2 -type f -name "*.json" 2>/dev/null | sort | while read -r json_file; do
        curlPost "/v1_0/configs/$category" "$json_file"
    done
done

# test
echo ">>> Checking effective initial schedule..."
curl -f -i -X GET "http://${CONFIG_URN}/v1_0/configs/effective/schedules/schedule/initial" \
     -H "Authorization: Bearer $ACCESS_TOKEN" \
     --show-error

echo -e "\e[37;42m All configurations loaded successfully! \e[0m"