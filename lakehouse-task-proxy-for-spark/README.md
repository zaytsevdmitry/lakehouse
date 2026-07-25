# Lakehouse Task Proxy for Spark

REST API proxy for Spark Submit requests with a PostgreSQL-backed task queue, supporting multiple cluster types (Local, Standalone, Kubernetes, YARN, MESOS) and concurrent execution across multiple service instances.

## Overview

The service acts as a proxy between clients and Apache Spark clusters. It accepts Spark submission requests via a REST API (compatible with the [Spark Standalone REST API](https://spark.apache.org/docs/latest/submitting-applications.html#spark-submit-programmatic-interface)), queues them in PostgreSQL, and a scheduler dispatches them to the appropriate cluster using the adapter pattern.

**Key features:**

- REST API compatible with the Spark Standalone REST API format
- PostgreSQL-backed task queue with `FOR UPDATE SKIP LOCKED` for multi-instance concurrency
- Single adapter selected at startup via configuration
- Adapter pattern for cluster-specific operations (kill, status, clear)
- External status model following the Spark REST API standard

## Architecture

```
┌─────────────┐     ┌──────────────┐     ┌────────────┐     ┌──────────────┐
│   Client    │────▶│  Controller  │────▶│   Service  │────▶│  PostgreSQL  │
│(spark-submit)│   │  /v1/submit  │     │            │     │   (queue)    │
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

## Modules

| Module | Description |
|--------|-------------|
| `lakehouse-task-proxy-for-spark-api` | DTOs (`CreateSubmissionRequest`, `CreateSubmissionResponse`, `ExternalStatus`) |
| `lakehouse-task-proxy-for-spark` | Service implementation (controller, service, adapters, scheduler) |

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
- **`realSubmissionId`** (internal) — actual Spark driver ID (`spark_submissions.submission_id`, e.g. `driver-abc-123`), stored internally and used for communication with the cluster.

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

If the task is still in queue (QUEUED/CLAIMED/SUBMITTED), returns `WAITING`. Once the task is in the cluster (COMPLETED/FAILED), queries the cluster directly for the actual status.

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
| `QUEUED` | Task is in queue, not yet claimed by scheduler |
| `CLAIMED` | Scheduler has claimed the task, about to launch |
| `SUBMITTED` | `spark-submit` completed successfully (exit code 0) |
| `COMPLETED` | Cluster reports success |
| `FAILED` | `spark-submit` failed or cluster reports failure |

### Internal → External mapping

| Internal | External |
|----------|----------|
| `QUEUED` | `WAITING` |
| `CLAIMED` | `WAITING` |
| `SUBMITTED` | `WAITING` |
| `COMPLETED` | `FINISHED` |
| `FAILED` | `FAILED` |

### Cluster-specific status mapping

**Local** (process state → external):

| Process State | External |
|---------------|----------|
| `process.isAlive()` | `RUNNING` |
| exit code 0 | `FINISHED` |
| exit code != 0 | `FAILED` |

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
| `local` | `LocalSparkClusterAdapter` |
| `standalone` | `StandaloneSparkClusterAdapter` |
| `k8s` / `kubernetes` | `KubernetesSparkClusterAdapter` |
| `yarn` | `YarnSparkClusterAdapter` |
| `mesos` | `MesosSparkClusterAdapter` |

## Adapters

| Adapter | Implementation | Cluster API | URL Configuration |
|---------|---------------|-------------|-------------------|
| `LocalSparkClusterAdapter` | ProcessBuilder (background) | Local JVM process | N/A (no cluster API) |
| `StandaloneSparkClusterAdapter` | REST (RestClient) | Spark Master REST API | `lakehouse.task.proxy4spark.standalone.rest-url` |
| `KubernetesSparkClusterAdapter` | Kubernetes Java client 27.0.0 | K8s API (pods) | `lakehouse.task.proxy4spark.k8s.rest-url` |
| `YarnSparkClusterAdapter` | REST (RestClient) | YARN ResourceManager REST API | `lakehouse.task.proxy4spark.yarn.rest-url` |
| `MesosSparkClusterAdapter` | Stub (not implemented) | — | — |

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

## Scheduler

The scheduler polls PostgreSQL every 5 seconds (configurable via `lakehouse.task.proxy4spark.scheduler.poll-interval-ms`):

1. `claimTask()` — claims the oldest `QUEUED` task using `FOR UPDATE SKIP LOCKED`
2. `createSubmission()` — builds and runs the `spark-submit` command via `ProcessBuilder`
3. `completeTask()` — updates the task status to `SUBMITTED` (success) or `FAILED` (error)

Multiple service instances can run simultaneously — database-level locking prevents duplicate claims.

## spark-submit Execution

The service launches `spark-submit` as a local process via `ProcessBuilder`. The command is built from the request parameters:

```
spark-submit --conf spark.master=... --conf spark.app.name=... --class com.example.Main /path/to/app.jar arg1 arg2
```

### Environment Requirements

The service host must have the following environment variables configured:

- **`SPARK_HOME`** — path to the Spark installation directory (e.g., `/opt/spark`)
- **`PATH`** — must include `$SPARK_HOME/bin` so that `spark-submit` is available

Example:

```bash
export SPARK_HOME=/opt/spark
export PATH=$SPARK_HOME/bin:$PATH
```

Without these variables, the scheduler will fail to locate and execute `spark-submit`.

## Configuration

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

## Tech Stack

- Java 23
- Spring Boot (Web, Data JPA)
- PostgreSQL
- Kubernetes Java client 27.0.0
- Jackson (JSON)

## Project Structure

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
      SparkClusterAdapter.java        (interface)
      SparkClusterAdapterBase.java    (abstract base)
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
