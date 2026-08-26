#!/bin/bash

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

