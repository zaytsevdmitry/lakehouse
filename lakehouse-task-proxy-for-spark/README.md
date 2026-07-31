# Lakehouse Task Proxy for Spark

REST API proxy for Spark Submit requests with a PostgreSQL-backed task queue, supporting multiple cluster types (Standalone, Kubernetes, YARN, MESOS) and concurrent execution across multiple service instances.

## Overview

The service acts as a proxy between clients and Apache Spark clusters. It accepts Spark submission requests via a REST API (compatible with the [Spark Standalone REST API](https://spark.apache.org/docs/latest/submitting-applications.html#spark-submit-programmatic-interface)), queues them in PostgreSQL, and a scheduler dispatches them to the appropriate cluster using the adapter pattern.

**Key features:**

- REST API compatible with the Spark Standalone REST API format
- PostgreSQL-backed task queue with `FOR UPDATE SKIP LOCKED` for multi-instance concurrency
- Single adapter selected at startup via configuration
- Adapter pattern for cluster-specific operations (kill, status, clear)
- External status model following the Spark REST API standard
- [Spark Launcher](https://mvnrepository.com/artifact/org.apache.spark/spark-launcher_2.12/3.5.8) for spark-submit execution
- Virtual Threads for non-blocking spark-submit execution (`spring.threads.virtual.enabled=true`)
- OpenMetrics (Prometheus) metrics via `spring-boot-starter-actuator` + `micrometer-registry-prometheus`

## Architecture

```
┌─────────────┐     ┌──────────────┐     ┌────────────┐     ┌──────────────┐
│   Client    │────▶│  Controller  │────▶│   Service  │────▶│  PostgreSQL  │
│(spark-submit)│   │  /v1/submit  │     │            │     │   (queue)    │
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

## Modules

| Module | Description |
|--------|-------------|
| `lakehouse-task-proxy-for-spark-api` | DTOs (`CreateSubmissionRequest`, `CreateSubmissionResponse`, `SubmissionStatusResponse`, `ExternalStatus`) |
| `lakehouse-task-proxy-for-spark` | Service implementation (controller, service, adapters, scheduler, metrics) |

## API Endpoints

Base path: `/v1/submissions`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/create` | Submit a new Spark job (saves to DB, returns `WAITING`) |
| `GET` | `/status/{submissionId}` | Get task status by proxy ID (returns cluster status if submitted) |
| `POST` | `/kill/{submissionId}` | Kill a specific submission |
| `POST` | `/killall` | Kill all queued/claimed submissions |
| `POST` | `/clear` | Clear all completed submissions |

### ID Model

The service uses a dual-ID model:

- **`submissionId`** (external) — proxy ID (`spark_submissions.id`), returned to the client in all API responses. This is what the client uses to query status, kill, etc.
- **`realSubmissionId`** (internal) — actual Spark driver ID (`spark_submissions.submission_id`, e.g. `driver-abc-123`), stored internally and used for communication with the cluster. Parsed from spark-submit stdout/stderr output via adapter-specific `extractSubmissionId()` regex.

The client always works with the proxy `submissionId`. The real Spark submission ID is resolved internally by the adapter layer.

## Request / Response Format

### POST /create

```json
// Request
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

// Response
{
  "action": "CreateSubmissionResponse",
  "message": "WAITING",
  "submissionId": "42",
  "success": true
}
```

### GET /status/{submissionId}

If the task is still in queue (WAITING/SUBMITTED), returns `WAITING`. Once the task is in the cluster (FINISHED/FAILED), queries the cluster directly for the actual status.

```json
// While in queue
{
  "action": "StatusResponse",
  "message": "WAITING",
  "submissionId": "42",
  "success": true
}

// After cluster reports status
{
  "action": "StatusResponse",
  "message": "RUNNING",
  "submissionId": "42",
  "success": true
}
```

## Status Model

### External statuses (API responses — Spark REST API standard)

| Status | Description |
|--------|-------------|
| `WAITING` | Application registered on Master but waiting for resources (workers/cores), or scheduler hasn't reached it yet, or is currently processing it |
| `RUNNING` | Application is running and actively executing tasks |
| `FINISHED` | Application completed successfully |
| `FAILED` | Application failed |
| `KILLED` | Application was forcibly stopped |
| `UNKNOWN` | Application state is unknown |

### Internal statuses (database / scheduler)

| Status | Description |
|--------|-------------|
| `WAITING` | Task is in queue, not yet claimed by scheduler |
| `SUBMITTED` | `spark-submit` completed successfully (exit code 0) |
| `FINISHED` | Cluster reports success |
| `FAILED` | `spark-submit` failed or cluster reports failure |

### Internal → External mapping

| Internal | External |
|----------|----------|
| `WAITING` | `WAITING` |
| `SUBMITTED` | `SUBMITTED` |
| `FINISHED` | `FINISHED` |
| `FAILED` | `FAILED` |

### Cluster-specific status mapping

**Kubernetes** (pod phase → external):

| Pod Phase | External |
|-----------|----------|
| `Pending` | `WAITING` |
| `Running` | `RUNNING` |
| `Succeeded` | `FINISHED` |
| `Failed` | `FAILED` |

**YARN** (application state → external):

| YARN State | External |
|------------|----------|
| `NEW`, `NEW_SAVING`, `SUBMITTED`, `ACCEPTED` | `WAITING` |
| `RUNNING` | `RUNNING` |
| `FINISHED` | `FINISHED` |
| `FAILED` | `FAILED` |
| `KILLED` | `KILLED` |

**Standalone** — mapped via `fromStandaloneState()` (driver state from Spark Master REST API).

## Adapter Selection

The adapter is selected at startup via `lakehouse.task.proxy4spark.adapter`:

| Value | Adapter |
|-------|---------|
| `standalone` | `StandaloneSparkAdapter` |
| `k8s` / `kubernetes` | `KubernetesSparkAdapter` |
| `yarn` | `YarnSparkAdapter` |
| `mesos` | `MesosSparkAdapter` |

## Adapters

| Adapter | Implementation | Cluster API | URL Configuration |
|---------|---------------|-------------|-------------------|
| `StandaloneSparkAdapter` | SparkLauncher + REST (RestClient) | Spark Master REST API | `lakehouse.task.proxy4spark.standalone.rest-url` |
| `KubernetesSparkAdapter` | SparkLauncher + Kubernetes Java client 27.0.0 | K8s API (pods) | `lakehouse.task.proxy4spark.k8s.rest-url` |
| `YarnSparkAdapter` | SparkLauncher + REST (RestClient) | YARN ResourceManager REST API | `lakehouse.task.proxy4spark.yarn.rest-url` |
| `MesosSparkAdapter` | Stub (not implemented) | — | — |

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

All adapters extend `SparkAdapterBase`, which provides:
- `defaultCreateSubmission(request)` — builds a `SparkLauncher`, calls `launch()`, reads stdout/stderr in parallel threads, waits for process with timeout, parses submissionId via adapter-specific `extractSubmissionId(output)`
- `buildSparkLauncher(request)` — configures `SparkLauncher` with master, deploy mode, spark properties, main class, app resource, and app args

### SparkLauncher Execution Flow

1. `SparkAdapterBase.buildSparkLauncher()` configures the launcher (master, deploy mode, spark properties, main class, app resource, app args)
2. `launcher.launch()` starts the spark-submit process
3. Two parallel threads read stdout and stderr
4. `process.waitFor(timeoutSeconds, SECONDS)` waits for completion
5. If timed out: `process.destroyForcibly()` + `CreateErrorException`
6. If exit code 0: `extractSubmissionId(output)` parses the real Spark submission ID from output using adapter-specific regex
7. If exit code != 0: `CreateErrorException` with output

### Adapter-Specific submissionId Extraction

| Adapter | Regex | Example |
|---------|-------|---------|
| Standalone | `(driver-\d{14}-\d{4})` | `driver-20240101120000-0001` |
| YARN | `Submitted application (application_\d+_\d+) to YARN` | `application_20240101_0001` |
| Kubernetes | `pods/(spark-\S+)` (fallback: `driver-\S+`) | `spark-driver-abc` |
| Mesos | throws `CreateErrorException` (not implemented) | — |

## Schedulers

The service uses three independent schedulers running in parallel, each backed by a `ScheduledExecutorService`.

### 1. SubmissionScheduler — Task Dispatch

Polls PostgreSQL every N ms (`lakehouse.task.proxy4spark.scheduler.poll-interval-ms`, default `5000`) with a configurable thread pool size:

1. `claimNextTask()` — SELECT the oldest `WAITING` task with `FOR UPDATE SKIP LOCKED` (returns when no tasks)
2. Deserializes `sparkProperties` and `appArgs` from JSON columns
3. Calls `adapter.createSubmission(request)` — runs `spark-submit` via `SparkLauncher`
4. Records SparkMetrics (Counter + Timer with p50/p95/p99)
5. On success: `repository.completeTask(id, submissionId, "SUBMITTED", ...)`
6. On failure: `repository.completeTask(id, null, "ERROR", message)`

```java
ScheduledExecutorService — poolSize threads, scheduleWithFixedDelay
    ↓ claimNextTask() (FOR UPDATE SKIP LOCKED)
    ↓ adapter.createSubmission(request)  ← spark-submit process
    ↓ completeTask() / markFailed()
```

Multiple service instances can run simultaneously — database-level locking prevents duplicate claims.

### 2. ClusterStatusScheduler — Status Inspection

Polls PostgreSQL every N ms (`lakehouse.task.proxy4spark.inspection.poll-interval-ms`, default `10000`):

1. `claimIncompleteTasks(batchSize)` — SELECT non-terminal tasks (status NOT IN `FINISHED`, `KILLED`, `FAILED`, `ERROR`) with `FOR UPDATE SKIP LOCKED`
2. For each row with a non-null `submissionId`: calls `adapter.getSubmissionStatus(submissionId)`
3. Maps the cluster response to an external status via `ExternalStatus.fromInternal(driverState)`
4. Updates the task status in the DB: `repository.updateStatus(id, newStatus, message)`
5. On error (exception): sets status to `UNKNOWN`

This keeps the DB in sync with the actual cluster state for SUBMITTED/RUNNING tasks.

### 3. CleanupScheduler — Garbage Collection

Polls PostgreSQL every N ms (`lakehouse.task.proxy4spark.cleanup.poll-interval-ms`, default `60000`):

1. `claimForCleanup(batchSize, retentionSeconds)` — SELECT terminal tasks (`FINISHED`, `KILLED`, `FAILED`, `ERROR`) whose `updated_at` is older than `retentionSeconds`, with `FOR UPDATE SKIP LOCKED`
2. For each row: calls `adapter.clearCompleted(submissionId)`:
   - **Standalone / YARN / Mesos**: stub — returns success immediately (no cluster-side API per submission)
   - **Kubernetes**: finds the driver pod via `readNamespacedPod`, deletes it via `deleteNamespacedPod`; if pod not found, logs WARN but returns success
3. On success: adds ID to the delete list
4. On failure: skips the row (will retry on next cycle)
5. `repository.deleteAllIds(toDelete)` — bulk DELETE remaining records

Configuration (`application.yml`):
```yaml
lakehouse:
  task:
    proxy4spark:
      cleanup:
        poll-interval-ms: 60000
        pool-size: 1
        batch-size: 50
        retention-seconds: 3600  # only clean records older than 1h
```

## Metrics

The service exposes OpenMetrics (Prometheus) metrics via Spring Boot Actuator.

### Prometheus Endpoint

```
GET /actuator/prometheus
```

Enabled in `application.yml`:
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

### Custom Metrics (SparkMetrics)

| Metric | Type | Tags | Description |
|--------|------|------|-------------|
| `lakehouse_task_proxy4spark_submission_requests_total` | Counter | `backend` | Total number of spark submission requests |
| `lakehouse_task_proxy4spark_submission_result_total` | Counter | `backend`, `status` (success/failed/timeout) | Total completed submissions by result |
| `lakehouse_task_proxy4spark_submission_duration_seconds` | Timer | `backend` | Time from spark-submit launch to submissionId capture (p50/p95/p99 histogram) |

### MetricsConfig

`MetricsConfig` configures:
1. **`MeterRegistryCustomizer`** — global naming convention that replaces dots with underscores for OpenMetrics compatibility
2. **`sparkLauncherExecutor`** — virtual thread executor for spark-submit work

## Configuration

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

## Tech Stack

- Java 23
- Spring Boot (Web, Data JPA, Actuator)
- PostgreSQL
- Kubernetes Java client 27.0.0
- Spark Launcher (`spark-launcher_2.12:3.5.8`)
- Micrometer + Prometheus (`micrometer-registry-prometheus`)
- Jackson (JSON)
- Virtual Threads (Java 21+)

## Project Structure

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
      SparkAdapter.java           (interface)
      SparkAdapterBase.java       (abstract base — SparkLauncher logic)
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
