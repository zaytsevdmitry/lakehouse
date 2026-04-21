#!/usr/bin/env sh
start() {
  echo "Starting application..."
  FILE="${HIVE_HOME}/schema_installed.txt"

  if [ -e "$FILE" ]; then
      echo "File exists. Show:"
      cat $FILE
  else
      echo "File does not exist."
      ${HIVE_HOME}/bin/schematool -initSchema -dbType ${DATABASE_TYPE}
      ${HIVE_HOME}/bin/schematool -dbType ${DATABASE_TYPE} -info> $FILE
  fi

  bash ${HIVE_HOME}/bin/start-metastore
  echo "Started application..."

}
cleanup() {
  echo "Stopping application"
 # todo add script to shutdown # bash ${HIVE_HOME}/bin/stop-metastore
  echo "Stopped application"
  exit 0
}

# Trap SIGTERM and SIGINT
trap cleanup SIGTERM SIGINT
# Your main application or process here
start
sleep infinity &
wait $! # Wait for the background process to finish or be interrupted