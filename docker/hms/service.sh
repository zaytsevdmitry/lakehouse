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


 # bash ${HIVE_HOME}/bin/start-metastore
 "${HIVE_HOME}/bin/hive" --skiphadoopversion --skiphbasecp --service metastore &
  HIVE_PID=$!
  echo "Started application with PID $HIVE_PID..."

}
cleanup() {
  echo "Stopping application..."
  # Корректно завершаем процесс Hive при остановке контейнера
  if [ -n "$HIVE_PID" ]; then
      kill -TERM "$HIVE_PID"
      wait "$HIVE_PID"
  fi
  echo "Stopped application"
  exit 0
}

# Trap SIGTERM and SIGINT
trap cleanup SIGTERM SIGINT
# Your main application or process here
start

wait $HIVE_PID





