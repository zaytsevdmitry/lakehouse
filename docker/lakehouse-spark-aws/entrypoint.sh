#!/usr/bin/env sh
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
  check_s3_ready
  bucket_add_if_not_exists "sparklogs"
  bucket_add_if_not_exists "data"
  upload_if_missing "/tmp/create-stub.log" "sparklogs" "eventlog/create-stub.log"

  echo "Starting application..."
  if [ $SPARK_MODE = "master" ];then
    $SPARK_HOME/sbin/start-master.sh --host $SPARK_MASTER_HOSTNAME
  elif [ $SPARK_MODE = "worker" ];then
    $SPARK_HOME/sbin/start-worker.sh $SPARK_MASTER_URL --host $SPARK_WORKER_HOSTNAME
  elif [ $SPARK_MODE = "history" ];then
     # sleep 40 #waiting fot start minio service and maked up buket
      $SPARK_HOME/sbin/start-history-server.sh
  else
    echo "SPARK_MODE value '$SPARK_MODE' is not in master, worker, history"
    exit 1
  fi
  echo "Started application..."

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
trap cleanup SIGTERM SIGINT
# Your main application or process here
start
sleep infinity &
wait $! # Wait for the background process to finish or be interrupted