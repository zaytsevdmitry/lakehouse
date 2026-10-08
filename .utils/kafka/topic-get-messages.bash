#!/bin/bash

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


if [ -z "$1" ]; then
  echo "Ошибка: Укажите имя топика."
  echo "Пример: bash topic-get-messages.bash metric_status"
  exit 1
fi

CONTAINER_NAME="broker"
TOPIC_NAME="$1"

echo "Подключение к контейнеру $CONTAINER_NAME..."
echo "Получение записей из топика: $TOPIC_NAME"
echo "----------------------------------------"

# Выполняем hostname -i прямо внутри контейнера и сохраняем в переменную
CONTAINER_IP=$(docker exec "$CONTAINER_NAME" hostname -i | awk '{print $1}')

# Передаем полученный IP в утилиту
# Чтение с форматированием JSON (требуется установленный jq на хосте)
docker exec -it "$CONTAINER_NAME" /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server "$CONTAINER_IP:9092" \
  --topic "$TOPIC_NAME" \
  --from-beginning | jq .


