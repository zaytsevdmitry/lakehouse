# Changelog

## [0.7.0] — 2026-08-19

### Added

#### New modules
- **`lakehouse-ui-svc`** — web UI service
  - Spring Boot backend (`/api/catalog`, `/api/schedules`, `/api/services`, `/api/spark-proxy`, `/api/states`) aggregating config-svc, scheduler-svc, state-svc and task-proxy-for-spark
  - React frontend served from static resources (Vite build): Catalog tree, dataset lineage, model/DDL scripts, relations (ER) view, schedules + pipeline DAG, services topology, spark submissions, dataset state control
  - Configurable service graph via `lakehouse.ui.services/edges/vertices` (`UiServiceProperties`)
  - `HealthChecker` for services availability, `GlobalExceptionHandler`
- **`lakehouse-common-health`** — shared liveness/readiness endpoints `/healthz` `/readyz` (paths configurable via `lakehouse.health.liveness-path` / `readiness-path`), applied to all services
- **`lakehouse-task-proxy-for-spark-rest-client`** — REST client for the spark proxy submission query API

#### lakehouse-task-proxy-for-spark — submission querying
- **`SparkSubmissionQueryController`** `/api/v1/spark-proxy-submissions` — list submissions filtered by `status`, `dateFrom`/`dateTo`, cursor pagination via `lastId` + `limit` (max 100), plus per-submission spark-properties endpoint
- **`SparkProxyService`** — `getSubmissions`, `getStatus`, `getSparkProperties`, `kill`, `killAll`, `clear` (locks all rows, kills running, deletes DB records)
- **DTOs** — `SparkProxySubmissionDTO`, `SparkProxySubmissionPropertiesDTO`, `SparkProxySubmissionsRequest`, `SparkProxySubmissionsMeta`, `SparkProxySubmissionsResponse`
- **`SparkSubmissionRepository.findSubmissions`** — native query with `TypedParameterValue` for status/date-range/cursor filters
- `SparkSubmission` entity updates (message, submissionId handling)

#### Config service
- **Data lineage** — `DataLineageController` (`/v1_0/configs/lineage/datasets/{keyName}`), `DataSetLineageService` (BFS over dataset dependencies), `DataSetLineageDTO`
- **Task templates** — `TaskTemplateController` CRUD (`/v1_0/configs/tasks`), `TaskTemplate` entity rework
- **`DatabaseProtocol`** enum added to `DataSourceDTO`/datasource configuration
- **Schedule headers** — `ScheduleHeaderDTO` support in `ScheduleService`
- **`TaskDTOValidator`** in `lakehouse-validators`
- Docs: `lineage.md`, `metadata_relationships.puml` diagram, updated content_configuration docs

#### Scheduler service
- **`ScheduleInstanceDAGController`** + **`ScheduleInstanceDAGService`** — DAG of a schedule instance (`/v1_0/schedule/dag/id={id}`)
- **Max retries control** — `ScheduleTaskInstance.maxRetries` (default -1), passed from `TaskDTO` via `ScheduleTaskInstanceFactory`; `ScheduleTaskInstanceService` stops retrying when `reTryNum >= maxRetries`
- **`SchedulerTaskRetryProperties`** — `lag-when-failed` (10s), `lag-when-config-failed` (240s) configurable retry delays

#### REST clients
- `SchedulerRestClientApi` — `getAllByInterval`, `getScheduleInstanceDAGDTOById`
- `SparkProxyRestClientApi` — `createSubmission`, `getStatus`, `getSubmissions`, `getSparkProperties`, `killSubmission`, `killAllSubmissions`, `clearCompleted`
- `ConfigRestClientApi`, `StateRestClientApi` — updated endpoints

#### Common
- New DTOs — `ScheduleInstanceDAGDTO`, `ScheduleScenarioActInstanceDTO`, `ScheduleTaskInstanceDTO`, `DataSetLineageDTO`
- **`DtoMergeUtils`** — field-level merge for config DTOs
- `DriverDTO` moved to `configs.schedule` package
- `DataSetStateResponseDTO` renamed to `DataSetWrongStateResponseDTO`
- `SQLTemplateFactory` moved from `lakehouse-common` to `lakehouse-task-executor-api`
- `Endpoint` constants for lineage, task templates, schedule DAG

#### Demo & infrastructure
- New Helm chart `lakehouse-management-ui-svc`
- Demo task templates: `begin.json` (`lockedStateTaskProcessor`), `check.json` (`dependencyCheckStateTaskProcessor`), `prepare-jdbc.json` (`jdbcTaskProcessor` + `createTableSQLProcessorBody`)
- Demo configs updated (datasources with `databaseProtocol`, schedules, scenario-act-templates) for compose and k8s
- Docker: images bumped to 0.7.0, `build.bash` version checks, updated spark-aws image
- `doc/nexttimedev/openmetadata` — OpenMetadata integration diagrams (classCurrent, omdClass, sendtoomd)

### Changed
- `lakehouse-config-svc` — task storage refactored: `TaskAbstract`/`ScenarioActTask` replaced by `Task` + `TaskProcessorArg` entities (`TaskRepository`, `TaskService`, `TaskProcessorArgRepository`, `TaskNotFoundException`); `Driver` entity removed in favor of DTO/driver service
- `lakehouse-scheduler-svc` — `ScheduleTaskInstanceService` retry semantics driven by `maxRetries`; `ScheduleInstanceController` interval queries
- All services — health check configuration (`lakehouse.health.*`)

### Fixed
- Proxy `getStatus` returns `NOT_FOUND` for missing submissions
- Proxy kill/clear of queued (submissionId == null) tasks — removed from DB instead of failing on cluster
- `SparkSubmission` status/message deserialization in query DTOs

---

## [0.6.0] 2026-07-30

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

## [0.5.0] — 2026-06-23

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

## [0.4.0] — 2026-06-20

### Added
- **Spark DQ module** (`lakehouse-task-executor-spark-dq-app`) — data quality validation as Spark job
- **Spark Dataset module** (`lakehouse-task-executor-spark-dataset-app`) — dataset processing as Spark job
- **`ScenarioActTemplate`** configuration in config-svc

### Changed
- Spark version bumped to 3.5.8
- Refactored `SparkRestClientApi` — unified status response handling

---

## [0.3.0] — 2026-04-23

### Added
- **Spark K8s native task processor** — initial (later removed)
- **Spark REST client** (`lakehouse-spark-rest-client`) — client for Spark Standalone REST API
- **Demo helm charts** for all services

### Fixed
- Status response deserialization in `SparkRestClientApi`

---

## [0.2.0] — 2025-07-13

### Added
- **State service** (`lakehouse-state-svc`) — manages dataset increment states (LOCKED, SUCCESS, etc.)
- **State processors**: `LockedStateTaskProcessor`, `SuccessStateTaskProcessor`, `DependencyCheckStateTaskProcessor`
- **State REST client** (`lakehouse-state-rest-client`)

### Changed
- `AbstractSparkDeployTaskProcessor.deploy()` — waits for RUNNING state with 2-minute timeout, then for final status

---

## [0.1.1] — 2025-05-06

### Fixed
- Documentation updates

---

## [0.1.0] — 2024-06-16

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
