#!/usr/bin/env sh

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





