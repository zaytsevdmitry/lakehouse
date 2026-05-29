#!/bin/sh
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
