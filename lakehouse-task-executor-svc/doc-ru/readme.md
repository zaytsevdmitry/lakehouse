# Сервис исполнителя задач (lakehouse-task-executor-svc)

Исполнитель задач — сервис, который получает задачи, поставленные планировщиком, блокирует их, запускает исполнение через процессор задач (TaskProcessor) и сообщает планировщику результат.

## Обзор

`lakehouse-task-executor-svc` — это сервис lakehouse, отвечающий за фактическое выполнение задач, запланированных `lakehouse-scheduler-svc`. Сервис не знает, когда и какие задачи должны выполняться — он лишь получает готовые к исполнению задачи из Kafka и выполняет их.

Схема работы:

1. `lakehouse-scheduler-svc` публикует сообщения о задачах, готовых к выполнению, в Kafka topic `scheduled_task_msg`.
2. Экземпляры `lakehouse-task-executor-svc` потребляют эти сообщения через `ScheduledTaskKafkaConsumerService`.
3. Сообщения фильтруются по группе исполнения (`taskExecutionServiceGroupName`): экземпляр обрабатывает только те задачи, группа которых совпадает с `group.id` его Kafka-потребителя.
4. Для каждой задачи сервис блокирует её в `lakehouse-scheduler-svc` через `lockTaskById`, получая полное описание задачи (`ScheduledTaskLockDTO`) — слияние шаблона задачи и перегруженных значений конкретной задачи.
5. На время исполнения запускается фоновый цикл heartbeat (`HeardBeatService`), который периодически подтверждает планировщику, что задача жива.
6. Задача исполняется выбранным процессором `TaskProcessor` (по имени, указанному в конфигурации задачи).
7. По завершении heartbeat останавливается, а планировщику через `lockRelease` отправляется результат (`TaskResultDTO`): `SUCCESS`, `FAILED` или `CONF_ERROR`.

Параллелизм выполнения ограничивается параметрами `concurrency` Kafka-потребителя и пулом потоков сервиса. Масштабирование описывается в [scaling.md](scaling.md).

## Архитектура

![Последовательность работы сервиса](uml/ServiceWorkSequence.png)

Сервис является точкой распараллеливания выполнения задач: он получает задание, адаптирует его для среды выполнения и передаёт среде, ожидая результата. Это не нагруженная, но блокирующая операция.

Внешние взаимодействия:

- **Kafka** — получение задач (`scheduled_task_msg`), десериализация `ScheduledTaskMsgDTO`.
- **lakehouse-scheduler-svc** — блокировка задачи (`lockTaskById`), heartbeat (`lockHeartBeat`), освобождение блокировки с результатом (`lockRelease`).
- **lakehouse-config-svc** — получение конфигурации источника/назначения (`SourceConfDTO`) по ключевому имени датасета.
- **lakehouse-state-svc** — работа со статусной моделью датасетов (требуется процессорами статусной модели).

Процессоры задач:

![Процессоры задач](uml/TaskProcessors.png)

- **Spark-процессоры** — запускают тело задачи на удалённом Spark standalone кластере через REST API `/v1/submissions`.
- **Процессоры статусной модели** — переводят интервал датасета в статус `Locked`/`Success`, проверяют зависимости.
- **JDBC-процессор** — исполняет `TaskProcessorBody` через JDBC-драйвер.

Тела задач (`TaskProcessorBody`):

![Тела задач](uml/TaskProcessorBody.png)

Процессоры, использующие шаблоны SQLTemplate, совместимы со Spark-задачами и JDBC-процессором. Подробно процессоры описаны в [processors.md](processors.md).

## Модули

### lakehouse-task-executor-svc

Сам сервис. Содержит:

- точку входа `TaskExecutorApplication`;
- Kafka-потребитель задач `ScheduledTaskKafkaConsumerService`;
- исполнитель `ExecuteService` — оркестрация: блокировка, рендеринг контекста Jinja, запуск процессора, отправка результата;
- `HeardBeatService` — фоновый цикл heartbeat по взятым блокировкам;
- процессоры задач (`processor`): Spark, статусная модель, JDBC;
- конфигурации (`configuration`): Kafka, REST-клиенты, фабрики, пул потоков.

### lakehouse-task-executor-api

Библиотека контрактов исполнения. Содержит:

- `TaskProcessor` — интерфейс процессора задач (`runTask`);
- `ProcessorBody` и реализации SQL-тел (`AppendSQLProcessorBody`, `MergeSQLProcessorBody`, `CreateTableSQLProcessorBody`, `CompactTableSQLProcessorBody`);
- абстракции работы с источниками данных: `DataSourceManipulator`, `DataSourceManipulatorFactory`, фабрики соединений и выполнения JDBC;
- `SQLTemplateResolver` / `SQLTemplateFactory`.

### lakehouse-task-executor-rest-client

REST-клиент для управления исполнителем задач. Содержит:

- `TaskExecutorRestClientApi` (контракт в `lakehouse-common-rest-client`);
- `TaskExecutorRestClientApiImpl` — реализация на базе `RestClientHelper`;
- `TaskExecutorRestClientConfiguration` — конфигурация подключения по `lakehouse.client.rest.taskexecutor.server.url`.

## API Endpoints

REST-контроллер сервиса (`TaskProcessorConfigController`) в текущей версии не содержит готовых эндпоинтов. Зарезервированный путь контракта клиента (`TaskExecutorRestClientApi.getScheduledTaskLockDTO`):

| Метод | Путь | Описание |
|---|---|---|
| GET | `/v1_0/taskexecutor/processor/config/lock/{id}` | Получение информации о блокировке задачи по `lockId` (зарезервировано) |

Health-check эндпоинты (из `lakehouse-common-health`):

| Метод | Путь | Описание |
|---|---|---|
| GET | `/healthz` | Liveness-проверка (`{"status":"UP"}`) |
| GET | `/readyz` | Readiness-проверка (`{"status":"READY"}`) |

Пути health-check настраиваются параметрами `lakehouse.health.liveness-path` и `lakehouse.health.readiness-path`.

## Конфигурация

Основные параметры (`src/main/resources/application.yml`):

```yaml
server:
  port: 8089
lakehouse:
  client:
    rest:
      state:
        server:
          url: http://127.0.0.1:8082
      config:
        server:
          url: http://127.0.0.1:8080
      scheduler:
        server:
          url: http://127.0.0.1:8081
  task-executor:
    service:
      heart-beat-initial-delaY-ms: 5000
      heart-beat-interval-ms: 5000
      max-lock-retries: 5
      max-lock-retries-duration-ms: 5
      id: first1
    scheduled:
      task:
        kafka:
          consumer:
            concurrency: 1
            properties:
              bootstrap.servers: 192.1.193.20:9092
              group.id: default
              auto.offset.reset: earliest
            topics: scheduled_task_msg
```

| Параметр | Описание |
|---|---|
| `server.port` | Порт сервиса |
| `lakehouse.client.rest.state.server.url` | URL `lakehouse-state-svc` |
| `lakehouse.client.rest.config.server.url` | URL `lakehouse-config-svc` |
| `lakehouse.client.rest.scheduler.server.url` | URL `lakehouse-scheduler-svc` |
| `lakehouse.task-executor.service.heart-beat-initial-delaY-ms` | Задержка перед началом отправки heartbeat |
| `lakehouse.task-executor.service.heart-beat-interval-ms` | Интервал отправки heartbeat. Должен быть меньше `lakehouse.scheduler.task.retry.delay-ms` в планировщике |
| `lakehouse.task-executor.service.max-lock-retries` | Число попыток взять блокировку при временной недоступности планировщика |
| `lakehouse.task-executor.service.max-lock-retries-duration-ms` | Задержка между попытками блокировки |
| `lakehouse.task-executor.service.id` | Идентификатор экземпляра в группе исполнителей (имя пода/хоста). Отправляется планировщику при блокировке |
| `lakehouse.task-executor.scheduled.task.kafka.consumer.concurrency` | Число потоков потребления задач |
| `...consumer.properties.bootstrap.servers` | Адрес Kafka-брокера |
| `...consumer.properties.group.id` | Группа исполнителя. Должна совпадать с `taskExecutionServiceGroupName` задачи, иначе задача игнорируется |
| `...consumer.properties.auto.offset.reset` | Стратегия смещения при отсутствии offset |
| `...consumer.topics` | Топик получения задач (по умолчанию `scheduled_task_msg`) |

Полное описание параметров с комментариями: [properties.md](properties.md).

Вопросы масштабирования (вертикальное, горизонтальное, сегментирование): [scaling.md](scaling.md).

## Безопасность

`lakehouse-task-executor-svc` защищен по OAuth 2.0 / OIDC с Keycloak в качестве identity provider (realm `lakehouse`). Spring Security настроен как **OAuth2 resource server**: каждый запрос должен содержать валидный JWT, выпущенный этим realm, иначе сервис возвращает `401`.

### Аутентификация

- **Запросы пользователей** (UI BFF, CLI, прямые вызовы API) - JWT проверяется по `spring.security.oauth2.resourceserver.jwt.issuer-uri`. Роли из claim'а `realm_access.roles` конвертируются в authorities `ROLE_<ИМЯ>` классом `KeycloakRoleConverter` и могут использоваться в `@PreAuthorize`.
- **Межсервисные вызовы** (`BearerTokenClientHttpRequestInterceptor`) - если запрос инициирован фоновой задачей (в `SecurityContext` нет пользовательского JWT), исходящий `RestClient` получает токен `client_credentials` через `OAuth2AuthorizedClientManager` по регистрации `keycloak-internal` (клиент `lakehouse-internal-client`) и добавляет его как `Authorization: Bearer`. При наличии пользовательского JWT он пробрасывается без изменений.
- Безопасность можно полностью отключить свойством `lakehouse.security.enabled=false` (все запросы становятся анонимными).

### Аудит

`AuditLoggingFilter` пишет одну строку на каждый запрос в логгер `AUDIT_LOG` (файл `logs/audit.log`):

```
User ID: <subject>, Username: <preferred_username>, Method: <method>, URI: <uri>, HTTP status: <status>
```

Для токенов, полученных через service account, вместо имени пользователя подставляется значение `lakehouse.security.audit.service-account-name` (по умолчанию `system`).

### Необходимые настройки

| Свойство / env | По умолчанию | Описание |
|---|---|---|
| `KEYCLOAK_ISSUER_URI` | `http://lakehouse-auth-svc:8080/realms/lakehouse` | URL realm'а Keycloak |
| `KEYCLOAK_INTERNAL_CLIENT_SECRET` | `super-secret-internal-key-987654321` | Секрет клиента `lakehouse-internal-client` |
| `lakehouse.security.enabled` | `true` | `false` полностью отключает безопасность |
| `lakehouse.security.audit.service-account-name` | `system` | Имя пользователя в аудите для service account токенов |
| `lakehouse.security.oauth2.internal-client-id` | `lakehouse-internal-client` | Клиент, идентифицирующий service account токены (claim `azp`) |
| `lakehouse.security.oauth2.client-registration-id` | `keycloak-internal` | OAuth2-регистрация, используемая интерцептором |

`spring.security.oauth2.resourceserver.jwt.issuer-uri` и блок `spring.security.oauth2.client` (регистрация `keycloak-internal`) преднастроены в `src/main/resources/application.yml`.

Пути из белого списка (токен не требуется): `/healthz`, `/readyz`, `/actuator/**`, `/v3/api-docs/**`, `/swagger-ui/**`.

### Realm Keycloak

В realm `lakehouse` должны быть:

- **`lakehouse-internal-client`** - confidential-клиент с включенными *Service Accounts* (межсервисные вызовы);
- роли realm'а `USER` / `ADMIN` (опционально, используются в `@PreAuthorize`).

Эталонный realm для импорта: `demo/compose/conf_infra/security/realms/lakehouse-realm.json`.