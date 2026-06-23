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





