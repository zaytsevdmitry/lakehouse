#!/bin/sh

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

# Проверяем, что все переменные окружения переданы
if [ -z "$MINIO_ENDPOINT" ] || [ -z "$MINIO_ROOT_USER" ] || [ -z "$MINIO_ROOT_PASSWORD" ]; then
    echo "ERROR: MINIO_ENDPOINT, MINIO_ROOT_USER and MINIO_ROOT_PASSWORD must be set."
    exit 1
fi

echo "1. Waiting for MinIO API at $MINIO_ENDPOINT..."
until curl -sSf "$MINIO_ENDPOINT/minio/health/ready" > /dev/null 2>&1; do
    echo "MinIO is not ready yet. Sleeping 2s..."
    sleep 2
done
echo "MinIO API is up!"

echo "2. Configuring mc alias..."
mc alias set myminio "$MINIO_ENDPOINT" "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD"

# Функция для проверки и создания бакета/папки
ensure_structure() {
    local bucket=$1
    local folder=$2

    # Проверяем существование бакета
    if ! mc ls "myminio/$bucket" > /dev/null 2>&1; then
        echo "Bucket '$bucket' not found. Creating..."
        mc mb "myminio/$bucket"
    else
        echo "Bucket '$bucket' already exists."
    fi

    # В S3 папок как объектов нет, они существуют только если в них есть файл.
    # Проверяем наличие каталога по маркерному файлу .keep
    if ! mc ls "myminio/$bucket/$folder/.keep" > /dev/null 2>&1; then
        echo "Folder '$folder' or marker file not found in '$bucket'. Creating..."
        echo -n "" | mc pipe "myminio/$bucket/$folder/.keep"
    else
        echo "Folder '$folder' already exists in '$bucket'."
    fi
}

echo "3. Provisioning storage structures..."
ensure_structure "data" "warehouse"
ensure_structure "sparklogs" "eventlog"

echo "Storage provisioning completed successfully! Proceeding..."
