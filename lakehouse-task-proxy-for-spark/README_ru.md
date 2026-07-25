# Lakehouse Task Proxy for Spark

REST API прокси для запросов Spark Submit с очередью задач на PostgreSQL. Поддерживает несколько типов кластеров (Local, Standalone, Kubernetes, YARN, MESOS) и параллельное выполнение на нескольких экземплярах сервиса.

## Обзор

Сервис выступает прокси между клиентами и кластерами Apache Spark. Он принимает запросы на отправку задач Spark через REST API (совместимый с [Spark Standalone REST API](https://spark.apache.org/docs/latest/submitting-applications.html#spark-submit-programmatic-interface)), очередяет их в PostgreSQL, а планировщик (scheduler) dispatch'ит их на нужный кластер через паттерн Adapter.

**Ключевые возможности:**

- REST API совместимый с форматом Spark Standalone REST API
- Очередь задач на PostgreSQL с `FOR UPDATE SKIP LOCKED` для мног instantiated concurrency
- Один адаптер выбирается при старте сервиса через конфигурацию
- Паттерн Adapter для кластер-специфичных операций (kill, status, clear)
- Внешняя модель статусов по стандарту Spark REST API

## Архитектура

```
┌─────────────┐     ┌──────────────┐     ┌────────────┐     ┌──────────────┐
│   Client    │────▶│  Controller  │────▶│   Service  │────▶│  PostgreSQL  │
│(spark-submit)│   │  /v1/submit  │     │            │     │   (очередь)  │
└─────────────┘     └──────────────┘     └──────┬─────┘     └──────────────┘
                                                │
                                                ▼
                                         ┌──────────────┐
                                         │  Scheduler   │
                                         │ (@Scheduled) │
                                         └──────┬───────┘
                                                │
                                                ▼
                                         ┌──────────────┐     ┌─────────────┐
                                         │   Adapter    │────▶│   Cluster   │
                                         │(Local/Standalone/│ │   (API)     │
                                         │  K8s/YARN/…) │     └─────────────┘
                                         └──────────────┘
```

## Модули

| Модуль | Описание |
|--------|----------|
| `lakehouse-task-proxy-for-spark-api` | DTO (`CreateSubmissionRequest`, `CreateSubmissionResponse`, `ExternalStatus`) |
| `lakehouse-task-proxy-for-spark` | Реализация сервиса (controller, service, adapters, scheduler) |

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
- **`realSubmissionId`** (внутренний) — реальный Spark driver ID (`spark_submissions.submission_id`, например `driver-abc-123`), хранится внутри и используется для общения с кластером.

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

Если задача ещё в очереди (QUEUED/CLAIMED/SUBMITTED), возвращает `WAITING`. Когда задача в кластере (COMPLETED/FAILED), запрашивает статус напрямую у кластера.

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
| `QUEUED` | Задача в очереди, ещё не забрана планировщиком |
| `CLAIMED` | Планировщик забрал задачу, готовится к запуску |
| `SUBMITTED` | `spark-submit` успешно завершился (exit code 0) |
| `COMPLETED` | Кластер сообщает об успехе |
| `FAILED` | `spark-submit` завершился с ошибкой или кластер сообщает о сбое |

### Маппинг внутренних → внешних

| Внутренний | Внешний |
|-----------|---------|
| `QUEUED` | `WAITING` |
| `CLAIMED` | `WAITING` |
| `SUBMITTED` | `WAITING` |
| `COMPLETED` | `FINISHED` |
| `FAILED` | `FAILED` |

### Маппинг статусов кластеров

**Local** (состояние процесса → внешний статус):

| Состояние процесса | Внешний статус |
|--------------------|---------------|
| `process.isAlive()` | `RUNNING` |
| exit code 0 | `FINISHED` |
| exit code != 0 | `FAILED` |

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
| `local` | `LocalSparkClusterAdapter` |
| `standalone` | `StandaloneSparkClusterAdapter` |
| `k8s` / `kubernetes` | `KubernetesSparkClusterAdapter` |
| `yarn` | `YarnSparkClusterAdapter` |
| `mesos` | `MesosSparkClusterAdapter` |

## Адаптеры

| Адаптер | Реализация | API кластера | Конфигурация URL |
|---------|-----------|--------------|------------------|
| `LocalSparkClusterAdapter` | ProcessBuilder (фоновый) | Локальный JVM процесс | N/A (нет API кластера) |
| `StandaloneSparkClusterAdapter` | REST (RestClient) | Spark Master REST API | `lakehouse.task.proxy4spark.standalone.rest-url` |
| `KubernetesSparkClusterAdapter` | Kubernetes Java client 27.0.0 | K8s API (pods) | `lakehouse.task.proxy4spark.k8s.rest-url` |
| `YarnSparkClusterAdapter` | REST (RestClient) | YARN ResourceManager REST API | `lakehouse.task.proxy4spark.yarn.rest-url` |
| `MesosSparkClusterAdapter` | Заглушка (не реализован) | — | — |

### SparkClusterAdapter Interface

```java
public interface SparkClusterAdapter {
    String createSubmission(CreateSubmissionRequest request);
    CreateSubmissionResponse killSubmission(String submissionId);
    CreateSubmissionResponse killAllSubmissions();
    CreateSubmissionResponse getSubmissionStatus(String submissionId);
    CreateSubmissionResponse clearCompleted();
}
```

## Планировщик

Планировщик опрашивает PostgreSQL каждые 5 секунд (настраивается через `lakehouse.task.proxy4spark.scheduler.poll-interval-ms`):

1. `claimTask()` — забирает самую старую `QUEUED` задачу через `FOR UPDATE SKIP LOCKED`
2. `createSubmission()` — строит и запускает команду `spark-submit` через `ProcessBuilder`
3. `completeTask()` — обновляет статус задачи на `SUBMITTED` (успех) или `FAILED` (ошибка)

Несколько экземпляров сервиса могут работать одновременно — блокировка на уровне базы данных предотвращает повторное взятие задачи.

## Запуск spark-submit

Сервис запускает `spark-submit` как локальный процесс через `ProcessBuilder`. Команда формируется из параметров запроса:

```
spark-submit --conf spark.master=... --conf spark.app.name=... --class com.example.Main /path/to/app.jar arg1 arg2
```

### Требования к переменным окружения

На хосте, где работает сервис, должны быть настроены следующие переменные окружения:

- **`SPARK_HOME`** — путь к каталогу установки Spark (например, `/opt/spark`)
- **`PATH`** — должен включать `$SPARK_HOME/bin`, чтобы команда `spark-submit` была доступна

Пример:

```bash
export SPARK_HOME=/opt/spark
export PATH=$SPARK_HOME/bin:$PATH
```

Без этих переменных планировщик не сможет найти и выполнить `spark-submit`.

## Конфигурация

`application.yml`:

```yaml
server:
  port: 8090

lakehouse:
  task:
    proxy4spark:
      adapter: local
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

spring:
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
```

## Технологический стек

- Java 23
- Spring Boot (Web, Data JPA)
- PostgreSQL
- Kubernetes Java client 27.0.0
- Jackson (JSON)

## Структура проекта

```
lakehouse-task-proxy-for-spark-api/
  src/main/java/.../dto/
    CreateSubmissionRequest.java
    CreateSubmissionResponse.java
    ExternalStatus.java

lakehouse-task-proxy-for-spark/
  src/main/java/.../
    controller/
      SparkProxyController.java
      GlobalExceptionHandler.java
    service/
      SparkProxyService.java
    entity/
      SparkSubmission.java
    repository/
      SparkSubmissionRepository.java
    adapter/
      SparkClusterAdapter.java        (интерфейс)
      SparkClusterAdapterBase.java    (абстрактный базовый)
      LocalSparkClusterAdapter.java
      StandaloneSparkClusterAdapter.java
      KubernetesSparkClusterAdapter.java
      YarnSparkClusterAdapter.java
      MesosSparkClusterAdapter.java
    scheduler/
      SparkSubmissionScheduler.java
    config/
      AdapterConfig.java
      ProxyConfig.java
  src/main/resources/
    application.yml
  diagrams/
    activity-*.puml
    sequence-*.puml
```
