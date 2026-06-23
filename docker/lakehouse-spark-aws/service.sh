#!/usr/bin/env sh

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

check_s3_ready() {
  if curl -sf -X GET "$S3_TEST_URL"; then
      echo "s3 ready!"
  else
      echo "Waiting s3: The request failed. Sleeping...zzZ"
      sleep 10
      check_s3_ready
  fi
}

bucket_add_if_not_exists() {
    sleep 3
    echo look bucket $1 for endpoint-url $S3_ENDPOINT
    if aws --endpoint-url $S3_ENDPOINT s3api head-bucket --bucket "$1" 2>/dev/null; then
        echo "Bucket '$1' available."
    else
        echo "Bucket '$1' not available."
        aws s3 mb s3://$1 --endpoint-url $S3_ENDPOINT  || bucket_add_if_not_exists $1
    fi
}

upload_if_missing() {
    local_file="$1"
    bucket="$2"
    s3_key="$3"

    if ! aws --endpoint-url "$S3_ENDPOINT" s3api head-object --bucket "$bucket" --key "$s3_key" >/dev/null 2>&1; then
        echo "" > $local_file
        if aws --endpoint-url "$S3_ENDPOINT" s3 cp "$local_file" "s3://$bucket/$s3_key"; then
            echo "success"
        else
            echo "Error when load file $3"
            return 1
        fi
    else
        echo "File $s3_key already exists."
    fi
}

start() {
  echo "Starting application..."
  echo "Prepare remote storage..."

  check_s3_ready
  bucket_add_if_not_exists "sparklogs"
  bucket_add_if_not_exists "data"
  upload_if_missing "/tmp/create-stub.log" "sparklogs" "eventlog/create-stub.log"

  echo "Infrastructure is ready. Checking SPARK_MODE='$SPARK_MODE'..."

  if [ "$SPARK_MODE" = "application" ]; then
      echo "Passing control strictly to Spark Operator (exec)..."
      exec "$SPARK_HOME/kubernetes/dockerfiles/spark/entrypoint.sh" "$@"
  else
      # Обработка всех инфраструктурных режимов для Docker Compose / Локального кластера
      if [ "$SPARK_MODE" = "master" ]; then
          echo "Starting Spark Master..."
          $SPARK_HOME/sbin/start-master.sh --host $SPARK_MASTER_HOSTNAME
      elif [ "$SPARK_MODE" = "worker" ]; then
          echo "Starting Spark Worker..."
          $SPARK_HOME/sbin/start-worker.sh $SPARK_MASTER_URL --host $SPARK_WORKER_HOSTNAME
      elif [ "$SPARK_MODE" = "history" ]; then
          echo "Starting History Server..."
          $SPARK_HOME/sbin/start-history-server.sh
      else
          echo "SPARK_MODE value '$SPARK_MODE' is not in master, worker, history, application"
          exit 1
      fi
      echo "Service started. Entering infinite sleep to keep container alive..."
      sleep infinity &
      wait $! # Wait for the background process to finish or be interrupted
  fi
}

cleanup() {
  echo "Stopping application"
    if [ $SPARK_MODE == "master" ];then
        $SPARK_HOME/sbin/stop-master.sh
      elif [ $SPARK_MODE == "worker" ];then
        $SPARK_HOME/sbin/stop-worker.sh
      elif [ $SPARK_MODE = "history" ];then
        echo "Stoping history not implemented"
      else
        echo "SPARK_MODE value '$SPARK_MODE' is not in master,worker,history"
        exit 1
      fi
      echo "Stopped application"
    exit 0
}

# Trap SIGTERM and SIGINT
trap cleanup 15 2
start
