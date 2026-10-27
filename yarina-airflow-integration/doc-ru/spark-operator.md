# Оператор Spark Submit (RU)

`YarinaSparkSubmitOperator` отправляет Spark-приложение через
`yarina-task-proxy-for-spark` и ждёт его терминального статуса. Connection описан в
[configuration.md](configuration.md); полные шаблоны DAG доступны в
[examples/2.x/yarina_spark_submit_dag.py](../examples/2.x/yarina_spark_submit_dag.py)
и [examples/3.x/yarina_spark_submit_dag.py](../examples/3.x/yarina_spark_submit_dag.py).

## Поток выполнения

1. Оператор собирает `CreateSubmissionRequest` из своих параметров.
2. Вызывает `POST /v1/submissions/create` через `YarinaSparkProxyHook`.
3. Проверяет `success == true` и непустой proxy `submissionId`.
4. Кладёт `spark_submission_id` в XCom.
5. Опрашивает `GET /v1/submissions/status/{submissionId}` каждые
   `poll_interval_seconds`.
6. При `FINISHED` возвращает `{"submissionId": "...", "message": "FINISHED"}`.
7. При `FAILED`, `KILLED` или таймауте выбрасывает `AirflowException`.

Прокси асинхронен: `/create` возвращает `WAITING` после сохранения задачи в очередь
PostgreSQL. Позже scheduler запускает `spark-submit`, поэтому `WAITING` может длиться
долго. Закладывайте `timeout_seconds` на время в очереди плюс время работы
приложения.

## Статусы и реакция оператора

Прокси возвращает состояние в поле **`message`**.

| Статус / ответ | Реакция оператора |
|:---------------|:------------------|
| `WAITING` | Нетерминальный: сбросить счётчик unknown, sleep, опрос снова. |
| `SUBMITTED` | Нетерминальный: сбросить счётчик unknown, sleep, опрос снова. |
| `RUNNING` | Нетерминальный: сбросить счётчик unknown, sleep, опрос снова. |
| `FINISHED` | Успех: вернуть `{"submissionId": id, "message": "FINISHED"}`. |
| `FAILED` | Немедленная ошибка `AirflowException`. |
| `KILLED` | Немедленная ошибка `AirflowException`. |
| `UNKNOWN` | Увеличить счётчик подряд идущих unknown; ошибка при `max_consecutive_unknown`. |
| HTTP 404 на status | Немедленная ошибка: неизвестный proxy id не станет известным. |
| HTTP 5xx на status | Трактуется как `UNKNOWN`; увеличивает тот же счётчик. |
| Прочие non-2xx | Ошибка с HTTP-кодом и телом ответа. |
| Таймаут | Ошибка с `submissionId` и URL для ручной проверки статуса. |

Нетерминальный статус после одного или нескольких `UNKNOWN` сбрасывает счётчик.
Счётчик живёт в рамках одного выполнения оператора и не сохраняется.

## Proxy submissionId

Оператор работает только с **proxy** `submissionId`, например `"42"`. Этот id
возвращает `/create`, и его принимает `/status/{submissionId}`.

Реальный Spark driver-id (`driver-...`) разрешается внутри
`yarina-task-proxy-for-spark` и никогда не передаётся в Airflow. Не пытайтесь передать
driver-id в оператор или в `/status`.

## Модель ответа: `message`, а не `driverState`

Используйте поле `message` из `SubmissionStatusResponse`:

```json
{
  "action": "StatusResponse",
  "message": "RUNNING",
  "submissionId": "42",
  "success": true
}
```

`driverState` относится к прямому Spark Standalone REST API. Модель `StatusResponse`
из `yarina-spark-rest-client` для прокси этим модулем **не используется**. Чтение
`driverState` было бы неверным для контракта прокси.

## Отсутствие kill-эндпоинтов

Оператор ни при каких обстоятельствах не вызывает `/kill`, `/killall` или `/clear`.
Hook предоставляет только `create_submission` и `get_status`; kill-методов в нём нет.

Если `timeout_seconds` истёк, **Spark-приложение может продолжать выполняться в
кластере**. В исключении есть proxy `submissionId`, поэтому статус можно проверить
или остановить задачу вручную вне Airflow:

```bash
curl -H "Authorization: Bearer <token>" \
  "http://yarina-task-proxy-for-spark:8090/v1/submissions/status/42"
```

После возникновения таймаута дополнительные HTTP-вызовы не выполняются.

## Настройка таймаута и опроса

| Параметр | По умолчанию | Значение |
|:---------|:-------------|:---------|
| `timeout_seconds` | `3600` | Общий бюджет времени на очередь и выполнение. |
| `poll_interval_seconds` | `10` | Пауза между запросами статуса. |
| `max_consecutive_unknown` | `5` | Число подряд идущих `UNKNOWN`/HTTP 5xx до ошибки. |

Рекомендации:

* Ставьте `timeout_seconds` больше максимального ожидания в очереди плюс времени
  работы Spark-задачи. Ожидание в очереди PostgreSQL тоже расходует этот бюджет.
* Не опрашивайте слишком часто. Обычно достаточно `10-30` секунд.
* Увеличьте `max_consecutive_unknown`, если прокси временно перегружен или статус
  кластера нестабилен. Каждый успешный нетерминальный статус сбрасывает счётчик.
* Retry задачи Airflow создаёт **новую** submission. Прокси не дедуплицирует повторы,
  поэтому настраивайте `retries` осознанно.

## Ошибки и диагностика

* **Create отклонён:** исключение содержит `message` прокси и полный payload запроса.
* **FAILED/KILLED:** исключение содержит `submissionId` и терминальный статус.
* **Таймаут:** исключение содержит `submissionId` и
  `GET /v1/submissions/status/{id}`. Задача может всё ещё выполняться.
* **HTTP 404 на status:** исключение сообщает, что submission не найден (404); retry
  не выполняется.
* **HTTP 5xx на status:** пишется warning и статус считается `UNKNOWN`.
* **Keycloak 404:** исключение сообщает, что `auth_url` должен быть
  `{issuer}/protocol/openid-connect/token`, а не issuer.

## Логирование

* `INFO` - создание submission, полученный `submissionId`, каждый опрос статуса и
  финальный `FINISHED`.
* `WARNING` - `UNKNOWN` или неожиданные статусы, приближение таймаута.
* `ERROR` - через исключения: отказ при создании, `FAILED`/`KILLED`, таймаут.

Токены и пароли никогда не логируются.
