# Changelog

## [Unreleased]

### lakehouse-task-proxy-for-spark — major refactoring & new schedulers

- **`/clear` endpoint redesigned**: now `claimAllTasks(10000)` locks all non-locked rows, kills running tasks (SUBMITTED/RUNNING), calls `clearCompleted()` for terminal tasks, deletes all from DB, then calls `adapter.postClear()`
- **Three independent schedulers** added (all `ScheduledExecutorService`-based):
  - `SubmissionScheduler` — claims WAITING tasks via `claimNextTask()`, dispatches `spark-submit` via `SparkLauncher`, records metrics
  - `ClusterStatusScheduler` — inspects non-terminal tasks via `claimIncompleteTasks()`, calls `adapter.getSubmissionStatus()`, syncs DB with cluster state
  - `CleanupScheduler` — claims terminal records older than `retentionSeconds` via `claimForCleanup()`, calls `adapter.clearCompleted()`, bulk DELETE
- **`clearCompleted()` moved to `SparkAdapterBase`** as stub returning `success=true`. Override kept only in `KubernetesSparkAdapter` (deletes driver pod). Removed overrides from `StandaloneSparkAdapter`, `YarnSparkAdapter`, `MesosSparkAdapter`
- **`postClear()` added** to `SparkAdapter` interface as `default` empty method. `StandaloneSparkAdapter` overrides it to call real standalone REST `/v1/submissions/clear` (404/405 tolerated)
- **`SubmissionScheduler.claimNextTask()`** — fixed return type handling (JPA native query can return `Object[]` or `Object[][]`)
- **`SparkMetrics`** — custom Prometheus metrics (Counter, Timer with p50/p95/p99)
- **`MetricsConfig`** — `MeterRegistryCustomizer` + `sparkLauncherExecutor` (virtual threads)
- **Regex patterns externalized** to `application.yml` via `submission-id-pattern` config per adapter (Standalone, YARN, K8s)
- **`SparkAdapterBase.buildSparkLauncher()`** — `setMainClass()` before `setConf()` so `--conf spark.app.name=...` takes priority
- **`KubernetesSparkAdapter.extractSubmissionId()`** — regex `(?:driver\s+)?pod name:\s+([a-zA-Z0-9\-]+-driver)` with `CASE_INSENSITIVE`, works with Spark 3.5.8 (`pod name:`) and older (`driver pod name:`) formats
- **`KubernetesSparkAdapter.findDriverPodName()`** — switched from `labelSelector` to `readNamespacedPod()` to avoid K8s 63-char label value limit; `submissionId` IS the pod name
- **Output logging** in `extractSubmissionId()` on extraction failure (first 2000 chars)
- **`SparkConfUtil.unSparkConf()`** — fix ordering: `extractSparkConFromTaskConf()` called **before** `unSparkConf()` in `SparkStandAloneClusterTaskProcessor` to preserve task-level `spark.*` properties
- **`SparkSubmissionRepository`** — added `claimAllTasks(batchSize)`, `claimIncompleteTasks(batchSize)`, `claimForCleanup(batchSize, retentionSeconds)`, `deleteAllIds(ids)`
- **`SparkSubmission` entity** — added `RUNNING`, `KILLED`, `ERROR` statuses; `isFinalStatus()` helper
- **`SubmissionResponse`** — renamed from `CreateSubmissionResponse`, added `success()` field
- **`ExternalStatus`** — added `fromInternal()` for cluster status mapping
- **`GlobalExceptionHandler`** — unified error handling for `CreateErrorException`
- **Helm chart** `demo/k8s/lakehouse-management-helm-charts/lakehouse-task-proxy4spark/` — new chart for the proxy service
- **Dockerfile** `docker/lakehouse-task-proxy4spark/` — multi-stage build with `jar-with-dependencies`
- **README** (EN/RU) — rewritten with scheduler descriptions, architecture updates
- **PlantUML diagrams** — `sequence-cleanup.puml`, `activity-inspectStatus.puml` added; existing diagrams updated

### lakehouse-task-executor-svc — cleanup & fixes

- **Removed `K8sSparkNativeTaskProcessor`** and all k8s-native classes (`K8sClientService`, `K8sConfigService`, `K8sPodStatusWatcher`, `PodPhase`, `PodUtilService`) — not used
- **Removed `SourceConfUtil`** from lakehouse-jinjava
- **`ExecuteService`** — fixed NPE on uninitialized `JinJavaUtils`
- **`processors.md`** — updated to reflect current architecture (removed references to non-existent processors, added `AbstractSparkDeployTaskProcessor` and `SparkRestDeployFactory` descriptions)

### Demo & infrastructure

- **Helm charts**: all services updated to 0.5.0; `lakehouse-task-proxy4spark` chart added
- **Docker Compose**: updated Spark templates, proxy port mappings
- **Dockerfiles**: `build.bash` for all images; fixed `lakehouse-spark-aws` base image to `spark:3.5.8-scala2.12-java17-ubuntu`
- **K8s scripts**: `install.bash`, `setup-default.bash`, `tunnels.bash`, `remove_images.bash` updated

---

## [0.5.0] — 2026-07-28

### Added
- **`lakehouse-task-proxy-for-spark`** — new module: REST API proxy for Spark Submit with PostgreSQL-backed task queue
  - Four adapters: Standalone, Kubernetes, YARN, MESOS
  - `SubmissionScheduler` with `ScheduledExecutorService`, `FOR UPDATE SKIP LOCKED`
  - `SparkLauncher`-based spark-submit execution
  - Prometheus metrics via Micrometer
  - Virtual threads (`spring.threads.virtual.enabled=true`)
- **Apache 2.0 license headers** applied across all source files
- **THIRD-PARTY-NOTICES** — third-party dependency notices

### Changed
- License from AGPL-3.0 to Apache 2.0

---

## [0.4.0] — 2026-07-22

### Added
- **Spark DQ module** (`lakehouse-task-executor-spark-dq-app`) — data quality validation as Spark job
- **Spark Dataset module** (`lakehouse-task-executor-spark-dataset-app`) — dataset processing as Spark job
- **`ScenarioActTemplate`** configuration in config-svc

### Changed
- Spark version bumped to 3.5.8
- Refactored `SparkRestClientApi` — unified status response handling

---

## [0.3.0] — 2026-07-15

### Added
- **Spark K8s native task processor** — initial (later removed)
- **Spark REST client** (`lakehouse-spark-rest-client`) — client for Spark Standalone REST API
- **Demo helm charts** for all services

### Fixed
- Status response deserialization in `SparkRestClientApi`

---

## [0.2.0] — 2026-07-08

### Added
- **State service** (`lakehouse-state-svc`) — manages dataset increment states (LOCKED, SUCCESS, etc.)
- **State processors**: `LockedStateTaskProcessor`, `SuccessStateTaskProcessor`, `DependencyCheckStateTaskProcessor`
- **State REST client** (`lakehouse-state-rest-client`)

### Changed
- `AbstractSparkDeployTaskProcessor.deploy()` — waits for RUNNING state with 2-minute timeout, then for final status

---

## [0.1.1] — 2026-07-01

### Fixed
- Documentation updates

---

## [0.1.0] — 2026-06-25

### Added
- Initial project structure
- **Core modules**:
  - `lakehouse-common` — shared DTOs, utils (`SparkConfUtil`, `Coalesce`, etc.)
  - `lakehouse-jinjava` — Jinja template engine integration
  - `lakehouse-common-rest-client`, `lakehouse-config-rest-client`, `lakehouse-scheduler-rest-client`, `lakehouse-task-executor-rest-client`
- **Configuration service** (`lakehouse-config-svc`) — manages datasources, datasets, drivers, scenario templates
- **Scheduler service** (`lakehouse-scheduler-svc`) — manages schedules and task delivery via Kafka
- **Task executor service** (`lakehouse-task-executor-svc`) — executes tasks with JDBC and Spark processors
- **Spark API** (`lakehouse-task-executor-spark-api`) — shared Spark session configuration
- **CLI** (`lakehouse-cli`) — command-line interface for configuration management
- **UI** (`lakehouse-ui-svc`) — web UI placeholder
- **Validators** (`lakehouse-validators`) — validation utilities
- **Docker Compose** (`demo/compose/`) — development environment with PostgreSQL, MinIO, Hive Metastore
- **K8s helm charts** (`demo/k8s/lakehouse-management-helm-charts/`) — deployment charts for all services
- **PlantUML diagrams** in each module
