# Lakehouse Task Proxy for Spark

REST API прокси для запросов Spark Submit с очередью задач на PostgreSQL. Поддерживает несколько типов кластеров (Standalone, Kubernetes, YARN, MESOS) и параллельное выполнение на нескольких экземплярах сервиса.

## Обзор

Сервис выступает прокси между клиентами и кластерами Apache Spark. Он принимает запросы на отправку задач Spark через REST API (совместимый с [Spark Standalone REST API](https://spark.apache.org/docs/latest/submitting-applications.html#spark-submit-programmatic-interface)), формирует очередь в PostgreSQL, а планировщик (scheduler) отправляет их на нужный кластер через паттерн Adapter.

**Ключевые возможности:**

- REST API совместимый с форматом Spark Standalone REST API
- Очередь задач на PostgreSQL с `FOR UPDATE SKIP LOCKED` для мног instantiated concurrency
- Один адаптер выбирается при старте сервиса через конфигурацию
- Паттерн Adapter для кластер-специфичных операций (kill, status, clear)
- Внешняя модель статусов по стандарту Spark REST API
- [Spark Launcher](https://mvnrepository.com/artifact/org.apache.spark/spark-launcher_2.12/3.5.8) для выполнения spark-submit
- Virtual Threads для неблокирующего выполнения spark-submit (`spring.threads.virtual.enabled=true`)
- OpenMetrics (Prometheus) метрики через `spring-boot-starter-actuator` + `micrometer-registry-prometheus`

## Архитектура

```
┌─────────────┐     ┌──────────────┐     ┌────────────┐     ┌──────────────┐
│   Client    │────▶│  Controller  │────▶│   Service  │────▶│  PostgreSQL  │
│(spark-submit)│   │  /v1/submit  │     │            │     │   (очередь)  │
└─────────────┘     └──────────────┘     └──────┬─────┘     └──────────────┘
                                                │
                                                ▼
                                         ┌──────────────┐     ┌────────────┐
                                         │  Scheduler   │────▶│ SparkMetrics│
                                         │ (@Scheduled) │     │ (Prometheus)│
                                         └──────┬───────┘     └────────────┘
                                                │
                                                ▼
                                         ┌──────────────────┐
                                         │sparkLauncherExecutor│
                                         │ (Virtual Threads) │
                                         └────────┬─────────┘
                                                  │
                                                  ▼
                                         ┌──────────────┐     ┌─────────────┐
                                         │   Adapter    │────▶│   Cluster   │
                                         │(Standalone/  │     │   (API)     │
                                         │  K8s/YARN/…) │     └─────────────┘
                                         └──────────────┘
```

## Модули

| Модуль | Описание |
|--------|----------|
| `lakehouse-task-proxy-for-spark-api` | DTO (`CreateSubmissionRequest`, `CreateSubmissionResponse`, `SubmissionStatusResponse`, `ExternalStatus`) |
| `lakehouse-task-proxy-for-spark` | Реализация сервиса (controller, service, adapters, scheduler, metrics) |

## API Endpoints

Базовый путь: `/v1/submissions`

| Метод | Endpoint | Описание |
|-------|----------|----------|
| `POST` | `/create` | Отправить новую задачу Spark (сохраняет в БД, возвращает `WAITING`) |
| `GET` | `/status/{submissionId}` | Получить статус задачи по proxy ID (возвращает статус кластера если задача в кластере) |
| `POST` | `/kill/{submissionId}` | Остановить конкретную задачу |
| `POST` | `/killall` | Остановить все queued/claimed задачи |
| `POST` | `/clear` | Очистить все завершённые задачи |

### Модель идентификаторов

Сервис использует двойную модель ID:

- **`submissionId`** (внешний) — proxy ID (`spark_submissions.id`), возвращается клиенту во всех ответах API. Клиент использует его для запроса статуса, kill и т.д.
- **`realSubmissionId`** (внутренний) — реальный Spark driver ID (`spark_submissions.submission_id`, например `driver-abc-123`), хранится внутри и используется для общения с кластером. Парсится из stdout/stderr spark-submit через адаптер-специфичный `extractSubmissionId()` regex.

Клиент всегда работает с proxy `submissionId`. Реальный Spark submission ID разрешается внутри через слой Adapter.

## Формат запроса / ответа

### POST /create

```json
// Запрос
{
  "action": "CreateSubmissionRequest",
  "appResource": "s3a://bucket/app.jar",
  "mainClass": "com.example.Main",
  "sparkProperties": {
    "spark.app.name": "my-app",
    "spark.executor.instances": "4"
  },
  "appArgs": ["--date", "2024-01-01"],
  "environmentVariables": {
    "AWS_ACCESS_KEY_ID": "..."
  }
}

// Ответ
{
  "action": "CreateSubmissionResponse",
  "message": "WAITING",
  "submissionId": "42",
  "success": true
}
```

### GET /status/{submissionId}

Если задача ещё в очереди (WAITING/SUBMITTED), возвращает `WAITING`. Когда задача в кластере (FINISHED/FAILED), запрашивает статус напрямую у кластера.

```json
// Пока в очереди
{
  "action": "StatusResponse",
  "message": "WAITING",
  "submissionId": "42",
  "success": true
}

// После отчёта кластера
{
  "action": "StatusResponse",
  "message": "RUNNING",
  "submissionId": "42",
  "success": true
}
```

## Модель статусов

### Внешние статусы (ответы API — стандарт Spark REST API)

| Статус | Описание |
|--------|----------|
| `WAITING` | Приложение зарегистрировано на Мастере, но ожидает выделения ресурсов (воркеров/ядер) для старта. Также возвращается если планировщик не добрался до запуска или запускает в данный момент |
| `RUNNING` | Приложение запущено и выполняет задачи |
| `FINISHED` | Приложение успешно завершило работу |
| `FAILED` | Выполнение приложения завершилось аварийно |
| `KILLED` | Приложение было принудительно остановлено |
| `UNKNOWN` | Состояние приложения неизвестно |

### Внутренние статусы (база данных / планировщик)

| Статус | Описание |
|--------|----------|
| `WAITING` | Задача в очереди, ещё не забрана планировщиком |
| `SUBMITTED` | `spark-submit` успешно завершился (exit code 0) |
| `FINISHED` | Кластер сообщает об успехе |
| `FAILED` | `spark-submit` завершился с ошибкой или кластер сообщает о сбое |

### Маппинг внутренних → внешних

| Внутренний | Внешний |
|-----------|---------|
| `WAITING` | `WAITING` |
| `SUBMITTED` | `SUBMITTED` |
| `FINISHED` | `FINISHED` |
| `FAILED` | `FAILED` |

### Маппинг статусов кластеров

**Kubernetes** (фаза пода → внешний статус):

| Pod Phase | Внешний статус |
|-----------|---------------|
| `Pending` | `WAITING` |
| `Running` | `RUNNING` |
| `Succeeded` | `FINISHED` |
| `Failed` | `FAILED` |

**YARN** (состояние приложения → внешний статус):

| YARN State | Внешний статус |
|------------|---------------|
| `NEW`, `NEW_SAVING`, `SUBMITTED`, `ACCEPTED` | `WAITING` |
| `RUNNING` | `RUNNING` |
| `FINISHED` | `FINISHED` |
| `FAILED` | `FAILED` |
| `KILLED` | `KILLED` |

**Standalone** — маппинг через `fromStandaloneState()` (состояние driver из Spark Master REST API).

## Выбор адаптера

Адаптер выбирается при старте сервиса через `lakehouse.task.proxy4spark.adapter`:

| Значение | Адаптер |
|----------|---------|
| `standalone` | `StandaloneSparkAdapter` |
| `k8s` / `kubernetes` | `KubernetesSparkAdapter` |
| `yarn` | `YarnSparkAdapter` |
| `mesos` | `MesosSparkAdapter` |

## Адаптеры

| Адаптер | Реализация | API кластера | Конфигурация URL |
|---------|-----------|--------------|------------------|
| `StandaloneSparkAdapter` | SparkLauncher + REST (RestClient) | Spark Master REST API | `lakehouse.task.proxy4spark.standalone.rest-url` |
| `KubernetesSparkAdapter` | SparkLauncher + Kubernetes Java client 27.0.0 | K8s API (pods) | `lakehouse.task.proxy4spark.k8s.rest-url` |
| `YarnSparkAdapter` | SparkLauncher + REST (RestClient) | YARN ResourceManager REST API | `lakehouse.task.proxy4spark.yarn.rest-url` |
| `MesosSparkAdapter` | Заглушка (не реализован) | — | — |

### SparkAdapter Interface

```java
public interface SparkAdapter {
    String createSubmission(CreateSubmissionRequest request) throws CreateErrorException;
    CreateSubmissionResponse killSubmission(String submissionId);
    CreateSubmissionResponse killAllSubmissions();
    SubmissionStatusResponse getSubmissionStatus(String submissionId);
    CreateSubmissionResponse clearCompleted();
}
```

Все адаптеры наследуют `SparkAdapterBase`, который предоставляет:
- `defaultCreateSubmission(request)` — создаёт `SparkLauncher`, вызывает `launch()`, параллельно читает stdout/stderr, ждёт завершения процесса с таймаутом, парсит submissionId через адаптер-специфичный `extractSubmissionId(output)`
- `buildSparkLauncher(request)` — конфигурирует `SparkLauncher` (master, deploy mode, spark properties, main class, app resource, app args)

### SparkLauncher Execution Flow

1. `SparkAdapterBase.buildSparkLauncher()` конфигурирует лаунчер (master, deploy mode, spark properties, main class, app resource, app args)
2. `launcher.launch()` запускает процесс spark-submit
3. Два параллельных потока читают stdout и stderr
4. `process.waitFor(timeoutSeconds, SECONDS)` ждёт завершения
5. При таймауте: `process.destroyForcibly()` + `CreateErrorException`
6. При exit code 0: `extractSubmissionId(output)` парсит реальный Spark submission ID из вывода через адаптер-специфичный regex
7. При exit code != 0: `CreateErrorException` с выводом

### Адаптер-специфичный извлечение submissionId

| Адаптер | Regex | Пример |
|---------|-------|--------|
| Standalone | `(driver-\d{14}-\d{4})` | `driver-20240101120000-0001` |
| YARN | `Submitted application (application_\d+_\d+) to YARN` | `application_20240101_0001` |
| Kubernetes | `pods/(spark-\S+)` (fallback: `driver-\S+`) | `spark-driver-abc` |
| Mesos | бросает `CreateErrorException` (не реализовано) | — |

## Планировщики

Сервис использует три независимых планировщика, работающих параллельно. Каждый запущен на `ScheduledExecutorService`.

### 1. SubmissionScheduler — Диспетчеризация задач

Опрашивает PostgreSQL каждые N мс (`lakehouse.task.proxy4spark.scheduler.poll-interval-ms`, по умолчанию `5000`) с настраиваемым пулом потоков:

1. `claimNextTask()` — SELECT самой старой `WAITING` задачи с `FOR UPDATE SKIP LOCKED` (возвращает null если задач нет)
2. Десериализует `sparkProperties` и `appArgs` из JSON-колонок
3. Вызывает `adapter.createSubmission(request)` — запускает `spark-submit` через `SparkLauncher`
4. Записывает SparkMetrics (Counter + Timer с p50/p95/p99)
5. При успехе: `repository.completeTask(id, submissionId, "SUBMITTED", ...)`
6. При ошибке: `repository.completeTask(id, null, "ERROR", message)`

```java
ScheduledExecutorService — poolSize потоков, scheduleWithFixedDelay
    ↓ claimNextTask() (FOR UPDATE SKIP LOCKED)
    ↓ adapter.createSubmission(request)  ← процесс spark-submit
    ↓ completeTask() / markFailed()
```

Несколько экземпляров сервиса могут работать одновременно — блокировка на уровне БД предотвращает повторное взятие задачи.

### 2. ClusterStatusScheduler — Инспекция статусов

Опрашивает PostgreSQL каждые N мс (`lakehouse.task.proxy4spark.inspection.poll-interval-ms`, по умолчанию `10000`):

1. `claimIncompleteTasks(batchSize)` — SELECT незавершённых задач (status NOT IN `FINISHED`, `KILLED`, `FAILED`, `ERROR`) с `FOR UPDATE SKIP LOCKED`
2. Для каждой строки с непустым `submissionId`: вызывает `adapter.getSubmissionStatus(submissionId)`
3. Маппит ответ кластера во внешний статус через `ExternalStatus.fromInternal(driverState)`
4. Обновляет статус задачи в БД: `repository.updateStatus(id, newStatus, message)`
5. При ошибке (exception): устанавливает статус `UNKNOWN`

Это держит БД синхронизированной с реальным состоянием кластера для задач в статусе SUBMITTED/RUNNING.

### 3. CleanupScheduler — Сборка мусора

Опрашивает PostgreSQL каждые N мс (`lakehouse.task.proxy4spark.cleanup.poll-interval-ms`, по умолчанию `60000`):

1. `claimForCleanup(batchSize, retentionSeconds)` — SELECT завершённых задач (`FINISHED`, `KILLED`, `FAILED`, `ERROR`), чей `updated_at` старше `retentionSeconds`, с `FOR UPDATE SKIP LOCKED`
2. Для каждой строки: вызывает `adapter.clearCompleted(submissionId)`:
   - **Standalone / YARN / Mesos**: заглушка — сразу возвращает успех (нет кластерного API на одну задачу)
   - **Kubernetes**: находит pod драйвера через `readNamespacedPod`, удаляет через `deleteNamespacedPod`; если pod не найден — WARN, но возвращает успех
3. При успехе: ID добавляется в список на удаление
4. При неудаче: строка пропускается (повтор на следующем цикле)
5. `repository.deleteAllIds(toDelete)` — bulk DELETE оставшихся записей

Конфигурация (`application.yml`):
```yaml
lakehouse:
  task:
    proxy4spark:
      cleanup:
        poll-interval-ms: 60000
        pool-size: 1
        batch-size: 50
        retention-seconds: 3600  # только записи старше 1ч
```

## Метрики

Сервис предоставляет OpenMetrics (Prometheus) метрики через Spring Boot Actuator.

### Prometheus Endpoint

```
GET /actuator/prometheus
```

Включается в `application.yml`:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    prometheus:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
        step: 1m
    tags:
      application: lakehouse-task-proxy4spark
```

### Пользовательские метрики (SparkMetrics)

| Метрика | Тип | Теги | Описание |
|---------|-----|------|----------|
| `lakehouse_task_proxy4spark_submission_requests_total` | Counter | `backend` | Общее количество запросов на отправку spark |
| `lakehouse_task_proxy4spark_submission_result_total` | Counter | `backend`, `status` (success/failed/timeout) | Завершённые отправки по результату |
| `lakehouse_task_proxy4spark_submission_duration_seconds` | Timer | `backend` | Время от запуска spark-submit до захвата submissionId (p50/p95/p99 гистограмма) |

### MetricsConfig

`MetricsConfig` конфигурирует:
1. **`MeterRegistryCustomizer`** — глобальная конвенция именования, заменяющая точки на подчёркивания для совместимости с OpenMetrics
2. **`sparkLauncherExecutor`** — virtual thread executor для работы spark-submit

## Конфигурация

`application.yml`:

```yaml
server:
  port: 8090

spring:
  threads:
    virtual:
      enabled: true
  datasource:
    url: jdbc:postgresql://localhost:5432/postgresDB?ApplicationName=TaskProxy4Spark
    username: postgresUser
    password: postgresPW
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    prometheus:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
        step: 1m
    tags:
      application: lakehouse-task-proxy4spark

lakehouse:
  task:
    proxy4spark:
      adapter: standalone
      spark-master: "local[*]"
      standalone:
        rest-url: "http://localhost:6066"
      yarn:
        rest-url: "http://localhost:8088"
      k8s:
        namespace: "default"
        rest-url: "http://kubernetes.default.svc"
      scheduler:
        poll-interval-ms: 5000
        pool-size: 2
      metrics:
        submission-timeout-seconds: 30
```

## Технологический стек

- Java 23
- Spring Boot (Web, Data JPA, Actuator)
- PostgreSQL
- Kubernetes Java client 27.0.0
- Spark Launcher (`spark-launcher_2.12:3.5.8`)
- Micrometer + Prometheus (`micrometer-registry-prometheus`)
- Jackson (JSON)
- Virtual Threads (Java 21+)

## Структура проекта

```
lakehouse-task-proxy-for-spark-api/
  src/main/java/.../dto/
    CreateSubmissionRequest.java
    CreateSubmissionResponse.java
    SubmissionStatusResponse.java
    ExternalStatus.java

lakehouse-task-proxy-for-spark/
  src/main/java/.../
    controller/
      SparkProxyController.java
      GlobalExceptionHandler.java
    service/
      SparkProxyService.java
      SparkMetrics.java
    entity/
      SparkSubmission.java
    repository/
      SparkSubmissionRepository.java
    adapter/
      SparkAdapter.java           (интерфейс)
      SparkAdapterBase.java       (абстрактный базовый — логика SparkLauncher)
      StandaloneSparkAdapter.java
      KubernetesSparkAdapter.java
      YarnSparkAdapter.java
      MesosSparkAdapter.java
    scheduler/
      SubmissionScheduler.java
      ClusterStatusScheduler.java
      CleanupScheduler.java
    config/
      AdapterConfig.java
      ProxyConfig.java
      MetricsConfig.java
    exception/
      CreateErrorException.java
  src/main/resources/
    application.yml
  diagrams/
    activity-*.puml
    sequence-*.puml
```

## Безопасность

`lakehouse-task-proxy-for-spark` защищен по OAuth 2.0 / OIDC с Keycloak в качестве identity provider (realm `lakehouse`). Spring Security настроен как **OAuth2 resource server**: каждый запрос должен содержать валидный JWT, выпущенный этим realm, иначе сервис возвращает `401`.

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

Пути из белого списка (токен не требуется): `/healthz`, `/readyz`, `/actuator/**` (включая метрики Prometheus), `/v3/api-docs/**`, `/swagger-ui/**`.

### Realm Keycloak

В realm `lakehouse` должны быть:

- **`lakehouse-internal-client`** - confidential-клиент с включенными *Service Accounts* (межсервисные вызовы);
- роли realm'а `USER` / `ADMIN` (опционально, используются в `@PreAuthorize`).

Эталонный realm для импорта: `demo/compose/conf_infra/security/realms/lakehouse-realm.json`.
