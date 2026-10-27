# Конфигурация (RU)

Интеграция использует стандартные **Connection** Airflow. Секреты хранятся только
в Connection (или во внешнем secret backend), но не в коде DAG.

## Connection

| ID Connection | Host | Login | Password | Extra |
|:--------------|:-----|:------|:---------|:------|
| `yarina_state_default` | `http://yarina-state-svc:8082` | `yarina-internal-client` | `KEYCLOAK_INTERNAL_CLIENT_SECRET` | `{"auth_url": "http://keycloak:8085/realms/yarina/protocol/openid-connect/token"}` |
| `yarina_config_default` | `http://yarina-config-svc:8080` | `yarina-internal-client` | `KEYCLOAK_INTERNAL_CLIENT_SECRET` | тот же `auth_url` |
| `yarina_spark_proxy_default` | `http://yarina-task-proxy-for-spark:8090` | `yarina-internal-client` | `KEYCLOAK_INTERNAL_CLIENT_SECRET` | тот же `auth_url` |

> **Важно:** `auth_url` - это **токен-эндпоинт**, а не issuer. Он должен
> заканчиваться на `/protocol/openid-connect/token`. Ответ 404 от Keycloak почти
> всегда означает, что вместо токен-эндпоинта указан issuer.

### Создание через Airflow CLI

```bash
export KEYCLOAK_ISSUER_URI="http://keycloak:8085/realms/yarina"
export KEYCLOAK_INTERNAL_CLIENT_SECRET="<client-secret>"

airflow connections add yarina_state_default \
  --conn-type http \
  --conn-host "http://yarina-state-svc:8082" \
  --conn-login "yarina-internal-client" \
  --conn-password "${KEYCLOAK_INTERNAL_CLIENT_SECRET}" \
  --conn-extra "{\"auth_url\": \"${KEYCLOAK_ISSUER_URI}/protocol/openid-connect/token\"}"

airflow connections add yarina_config_default \
  --conn-type http \
  --conn-host "http://yarina-config-svc:8080" \
  --conn-login "yarina-internal-client" \
  --conn-password "${KEYCLOAK_INTERNAL_CLIENT_SECRET}" \
  --conn-extra "{\"auth_url\": \"${KEYCLOAK_ISSUER_URI}/protocol/openid-connect/token\"}"

airflow connections add yarina_spark_proxy_default \
  --conn-type http \
  --conn-host "http://yarina-task-proxy-for-spark:8090" \
  --conn-login "yarina-internal-client" \
  --conn-password "${KEYCLOAK_INTERNAL_CLIENT_SECRET}" \
  --conn-extra "{\"auth_url\": \"${KEYCLOAK_ISSUER_URI}/protocol/openid-connect/token\"}"
```

### Поля Connection, используемые hook'ами

| Поле | Обязательно | Описание |
|:-----|:------------|:---------|
| `host` | да | Базовый URL сервиса со схемой и портом. Для Spark-прокси используется порт `8090`. |
| `login` | да (при включённой безопасности) | Client id Keycloak, обычно `yarina-internal-client` |
| `password` | да (при включённой безопасности) | Client secret Keycloak |
| `extra.auth_url` | да (при включённой безопасности) | Абсолютный токен-эндпоинт |
| `extra.security_enabled` | нет, по умолчанию `true` | `false` отключает OAuth2 (только локальная разработка) |

## Переменные окружения

Значения ниже - конвенция развёртывания; сам Airflow их не читает, если этого не
делает Connection/secret backend.

| Переменная | Назначение |
|:-----------|:-----------|
| `KEYCLOAK_ISSUER_URI` | Issuer URI realm, например `http://keycloak:8085/realms/yarina` |
| `KEYCLOAK_INTERNAL_CLIENT_SECRET` | Секрет service account `yarina-internal-client` |

В production предпочтителен secret backend Airflow (Vault/OpenBao, Kubernetes
Secrets, облачный secret manager), на который ссылается Connection.

## Локальная разработка без безопасности

`yarina-state-svc` и `yarina-config-svc` можно запустить с отключённой
безопасностью (`yarina.security.enabled=false`). В этом режиме укажите в
Connection `extra`:

```json
{"security_enabled": false}
```

Тогда `auth_url`, login и password не нужны. **Этот режим только для локальной
разработки.**

## Примеры DAG

### Автоматический резолв источников (рекомендуется)

```python
YarinaStateCheckOperator(
    task_id="check_dependencies",
    dataset_key_name="transaction_dds",
    interval_start="{{ data_interval_start.isoformat() }}",
    interval_end="{{ data_interval_end.isoformat() }}",
    lock_source="airflow-run-{{ run_id }}",
    auto_resolve_sources=True,
    config_http_conn_id="yarina_config_default",
    state_http_conn_id="yarina_state_default",
)
```

Оператор вызывает `GET /v1_0/configs/datasets/{keyName}` и берёт
`DataSetDTO.sources` минус сам датасет.

### Явный список источников

```python
YarinaStateCheckOperator(
    task_id="check_dependencies",
    dataset_key_name="transaction_dds",
    interval_start="{{ data_interval_start.isoformat() }}",
    interval_end="{{ data_interval_end.isoformat() }}",
    lock_source="airflow-run-{{ run_id }}",
    source_datasets=["client_processing", "transaction_processing"],
    auto_resolve_sources=False,
    state_http_conn_id="yarina_state_default",
)
```

Явный список нужен, когда config-svc недоступен из Airflow worker или когда DAG
должен фиксировать зависимости независимо от изменений метаданных.

## Настройка проверки

| Параметр | По умолчанию | Эффект |
|:---------|:-------------|:-------|
| `fail_on_foreign_lock` | `True` | Падать, если целевой датасет `LOCKED` чужим lockSource |
| `fail_on_holes` | `False` | Считать «дыры» (интервалы без `SUCCESS`) в источниках ошибкой |
| `auto_resolve_sources` | `True` | Читать источники из config-svc, если `source_datasets` не задан |

## Параметры оператора Spark Submit

| Параметр | Тип | По умолчанию | Описание |
|:---------|:----|:-------------|:---------|
| `app_resource` | `str` | обязателен | `appResource`, передаваемый прокси (JAR, Python-файл и т.п.) |
| `main_class` | `str` | `""` | `mainClass` для Java/Scala-приложений |
| `app_args` | `list[str]` | `[]` | Список `appArgs`; Jinja-шаблоны рендерятся |
| `spark_properties` | `dict[str, str]` | `{}` | `sparkProperties`, например `spark.executor.instances` |
| `environment_variables` | `dict[str, str]` | `{}` | `environmentVariables` для Spark driver |
| `client_spark_version` | `str` | `""` | `clientSparkVersion` |
| `http_conn_id` | `str` | `yarina_spark_proxy_default` | Id Airflow Connection |
| `poll_interval_seconds` | `int` | `10` | Пауза между запросами статуса; больше 0 |
| `timeout_seconds` | `int` | `3600` | Общий бюджет ожидания; больше 0 |
| `terminal_fail_states` | `Iterable[str]` | `("FAILED", "KILLED")` | Статусы, немедленно завершающие задачу ошибкой |
| `transient_states` | `Iterable[str]` | `("WAITING", "SUBMITTED", "RUNNING")` | Нетерминальные статусы, сбрасывающие счётчик unknown |
| `max_consecutive_unknown` | `int` | `5` | Допустимое число подряд идущих `UNKNOWN`/HTTP 5xx до ошибки |

`app_resource`, `main_class`, `app_args`, `spark_properties`,
`environment_variables` и `client_spark_version` являются template-полями, поэтому
Jinja-выражения вроде `{{ ds }}` и `{{ var.value.name }}` работают. Модель статусов
и поведение при таймауте описаны в [spark-operator.md](spark-operator.md).

## Гигиена секретов

* Не помещайте client secret или токен в DAG, default args или XCom.
* Не логируйте токены: hook'и пишут только client id и ключевые шаги.
* Для `environment_variables`, подставляемых из DAG, используйте Airflow Variables
  или secrets backend вместо хардкода; оператор логирует только app resource и main
  class, но не отрендеренные переменные.
* Ротация секрета выполняется обновлением клиента в Keycloak / secret backend;
  правка DAG не требуется.
