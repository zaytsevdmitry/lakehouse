#!/usr/bin/env sh
start() {
  echo "Starting application..."
  if [ $SPARK_MODE = "master" ];then
    $SPARK_HOME/sbin/start-master.sh
  elif [ $SPARK_MODE = "worker" ];then
    $SPARK_HOME/sbin/start-worker.sh $SPARK_MASTER_URL
  else
    echo "SPARK_MODE value '$SPARK_MODE' is not in master,worker"
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
      else
        echo "SPARK_MODE value '$SPARK_MODE' is not in master,worker"
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