# Changelog

## [0.9.0] — 2026-08-28

### Added

#### New modules — credential providers

- **`lakehouse-credential-providers-jdbc`** — shared, Spark-independent secret resolution library (no Spring dependency; Spring Boot artifacts are declared `provided`):
  - `SecretProvider` SPI and the `SecretResolver` helper (`hasProvider`, `resolvePassword`, `sanitize`, `resolveAndSanitize`)
  - HTTP clients `VaultHttp` (OpenBao/Vault; token via `VAULT_TOKEN` env or Kubernetes Service Account auth) and `LockboxHttp` (Yandex Cloud Lockbox; IAM token via Instance Metadata or authorized key file `YC_AUTH_KEY_PATH`)
  - `SecretCache` — in-memory cache, 5-minute TTL per JVM
  - JDBC providers `BaoJdbcSecretProvider` (KV v2) and `YcLockboxJdbcSecretProvider`
  - `LakehouseSecurityContext` for resolving secrets inside Spark closures on Executors
- **`lakehouse-credential-providers-spark`** — Spark-specific credentials, resolved on Driver and Executors:
  - `LakehouseSecureJDBCTableCatalog` — secure replacement for Spark's `JDBCTableCatalog`: resolves the DB password via `SecretResolver` at Driver + Executors and masks the original exception
  - S3 providers `BaoS3CredentialsProvider` / `YcLockboxS3CredentialsProvider` for Spark S3A
- **`lakehouse-spark-credential-providers`** split into the two modules above; the resulting jars (`lakehouse-credential-providers-jdbc-0.9.0.jar`, `lakehouse-credential-providers-spark-0.9.0.jar`) are copied into the Spark image

#### Runtime secret resolution

- **`JdbcConnectionFactory`** (`lakehouse-task-executor-api`) — resolves the DB password through `SecretResolver` when the datasource `service.properties` contains `secretProvider`; the security options are stripped before the map reaches the JDBC driver; unchanged when no provider is configured (backward compatible)
- **`lakehouse-task-executor-api`** now depends on `lakehouse-credential-providers-jdbc`

#### OpenBao / Vault integration in the demo

- **Docker Compose**: `openbao` service (`quay.io/openbao/openbao:2.0`) with the init script `demo/compose/conf_infra/openbao/init.sh`:
  - dev server, KV v2 secrets engine, seeding `kv/lakehouse/database` and `kv/infrastructure/minio`
  - read-only policy `lakehouse-spark-readonly`, scoped token `lakehouse-spark-token`
  - `VAULT_TOKEN` env added to `spark-master`, `spark-worker-1`, `spark-history`, `task-executor-svc-database`; healthcheck + `depends_on` wiring
- **`demo/compose/conf_infra/spark-defaults.conf`**:
  - `processingdb` catalog switched to `LakehouseSecureJDBCTableCatalog` + `BaoJdbcSecretProvider` (`secret-key: kv/data/lakehouse/database:password`) — plaintext PostgreSQL password removed
  - S3 credentials moved from hardcoded `spark_user`/`spark_pwd` to `BaoS3CredentialsProvider` reading `kv/data/infrastructure/minio` from OpenBao
- **Datasource configs** (`demo/compose/conf/datasources/processingdb.json`, `demo/k8s/conf/datasources/processingdb.json`) — `password` replaced by provider options (`secretProvider`, `secret-key`, `vault-url`)
- **Kubernetes umbrella chart** `lakehouse-management`:
  - `openbao:` section in `values.yaml` (image, env with `LAKEHOUSE_DB_PASSWORD`, `MINIO_ROOT_PASSWORD`, `BAO_DEV_ROOT_TOKEN_ID`, `BAO_TOKEN`; optional `valueFrom.secretKeyRef` support)
  - new templates: `openbao-configmap` (init script placeholder for `init.sh`), `openbao-deployment` (`BAO_ADDR` → `http://<release>-openbao:8200`, exec `bao status` probes), `openbao-service` (ClusterIP:8200)
- **Spark image** `docker/lakehouse-spark-aws` — provider jars added to `/opt/spark/jars`, version 0.9.0
- **lakehouse-ui-svc** — OpenBao added to the services topology (health-check `/v1/sys/health`, vertex, edge)

#### Documentation

- `doc/security/security.md` + `doc-ru/security/security.md` — new section "Secrets for data connections (credential providers)": option contract (`secretProvider`, `secret-key`, `vault-url`, `vault-role`, `vault-k8s-auth-path`, `secret-id`, `secret-version`), where to configure (`spark-defaults.conf` / datasource `service.properties`), OpenBao/Vault and Yandex Cloud Lockbox setup, security guarantees (secrets are never logged, masked errors, 5-minute cache); overview and configuration reference updated
- `lakehouse-credential-providers-spark` README (EN/RU) — configuration, deploying to Spark (`--jars`), caching / timeouts / troubleshooting
- `lakehouse-task-executor-svc` docs (EN/RU):
  - `processors.md` — Spark processor architecture updated to the actual code (deploy logic inlined, `deploy.clusterUrl` / `deploy.mainClass` / `deploy.appResource`), new "Secrets in Spark tasks" section
  - `properties.md` — spark processor properties (`maxWaitToRunningStateTimeoutMs`, `sparkJobStatusCheckIntervalMs`) and datasource provider options
  - `readme.md` — processor parameters, JDBC secret resolution, `VAULT_TOKEN` / `YC_AUTH_KEY_PATH` required settings
  - `TaskProcessors` UML diagram regenerated
- `demo/compose/README-INSTALL.md`, `demo/k8s/README-INSTALL.md` — micro-fixes (load.sh command, Keycloak section relocation)

### Changed

- **Version 0.8.0 → 0.9.0** for all modules, Docker images and Helm chart image tags

### Fixed

- **Stale documentation** referencing the removed `AbstractSparkDeployTaskProcessor` / `SparkRestDeployFactory` — processors.md (EN/RU) and the `TaskProcessors` UML diagram corrected to the current architecture

---

## [0.8.0] — 2026-08-26

### Changed

#### Framework upgrade
- **Spring Boot 3.4.2 → 4.1.1** — major version bump across all modules
- **JUnit 4.13.2 → JUnit Jupiter 6.1.3** — migrated from `junit:junit` to `org.junit.jupiter:junit-jupiter-api`; commented-out vintage engine for backward compatibility
- **SLF4J** — version now managed by Spring Boot parent (removed explicit `2.0.16`)

#### New dependencies
- `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.0` — OpenAPI / Swagger UI (config-svc, scheduler-svc)
- `net.logstash.logback:logstash-logback-encoder:8.1` — structured JSON logging (config-svc, scheduler-svc, state-svc, task-executor-svc, task-proxy-for-spark)
- `org.testcontainers:testcontainers-bom:1.20.4` — BOM import for integration tests
- `org.apache.kafka:kafka-testcontainers:1.19.3` — Kafka testcontainers version property

#### Removed
- `junit:junit:4.13.2` — replaced by JUnit Jupiter
- `log4j.properties` files (config-svc, scheduler-svc, task-executor-svc) — replaced by `logback-spring.xml`
- `AbstractSparkDeployTaskProcessor` — logic inlined into `SparkStandAloneClusterTaskProcessor`

### Added

#### Keycloak security — OAuth2 Resource Server / Client
All backend services now authenticate via Keycloak (OAuth2 Bearer token validation):

- **`lakehouse-common-health`** — shared security infrastructure:
  - `AuditLoggingFilter` — logs incoming request details
  - `BearerTokenClientHttpRequestInterceptor` — attaches OAuth2 access token to outbound REST calls
  - `KeycloakRoleConverter` — maps Keycloak realm roles to Spring Security authorities
  - `RestClientSecurityConfiguration` — configures `RestClient` with token relay
  - `TestSecurityConfiguration` — test-only security config for health controller tests
  - `KeycloakRoleConverterTest`
- **`SecurityConfig`** added to: `lakehouse-config-svc`, `lakehouse-scheduler-svc`, `lakehouse-state-svc`, `lakehouse-task-executor-svc`, `lakehouse-task-proxy-for-spark`, `lakehouse-ui-svc`
- **`SecurityConfigTest`** added to: `lakehouse-config-svc`, `lakehouse-scheduler-svc`

#### OpenAPI
- **`OpenApiConfig`** — Swagger UI endpoints added to `lakehouse-config-svc`, `lakehouse-scheduler-svc`

#### lakehouse-ui-svc
- **`UserController`** — `/api/user` endpoint returning authenticated user info (name, email, roles)

#### lakehouse-config-svc
- **`SparkRestClientConfiguration`** — REST client config for Spark Proxy communication

#### Logging
- **`logback-spring.xml`** added to all services (config-svc, scheduler-svc, state-svc, task-executor-svc, task-proxy-for-spark), replacing `log4j.properties`

#### Security documentation
- `doc/security/security.md` — interservice security architecture (EN)
- `doc-ru/security/security.md` — русская версия
- PlantUML diagrams: `interservice-security.puml`, `ui-security.puml`, `spark-apps-security.puml`

#### Demo & infrastructure
- **Docker Compose** — Keycloak service added (image `quay.io/keycloak/keycloak:26.0`, import realm, healthcheck)
- **Keycloak realm** — `demo/compose/conf_infra/security/realms/lakehouse-realm.json` (clients: `lakehouse-ui-client`, `lakehouse-internal-client`; roles: `USER`, `ADMIN`; demo users: `de_view`, `de_editor`)
- **Keycloak schema init** — `demo/compose/conf_infra/security/init_keycloak_schema.sql`
- **K8s Helm charts** for Keycloak:
  - `keycloak-deployment` — Keycloak pod with `start-dev --import-realm`
  - `keycloak-service` — ClusterIP:8085
  - `keycloak-db-init-configmap` — `CREATE SCHEMA IF NOT EXISTS keycloak`
  - `keycloak-realm-configmap` — realm import via `.Files.Get`
- **K8s services** — wait-for-keycloak initContainer + `KEYCLOAK_ISSUER_URI` / `KEYCLOAK_INTERNAL_CLIENT_SECRET` env vars on all backend deployments
- **Spark driver env** — `spark.kubernetes.driverEnv.KEYCLOAK_ISSUER_URI` + `KEYCLOAK_INTERNAL_CLIENT_SECRET` in `spark-defaults-configmap`
- **`tunnels.bash`** — added `keycloak 8085:8085` port-forward
- **`README-INSTALL.md`** (k8s) — Keycloak setup instructions (`/etc/hosts`, admin console, demo users)
- **Demo screenshots** — `demo/img/*.png` (lineage, relations, schedules, pipeline DAG, spark jobs, states, topology-state)
- **`README-SCENARIO.md`** — updated with screenshot references
- **`.utils/kafka/topic-get-messages.bash`** — utility for reading Kafka topic messages

#### lakehouse-task-executor-svc
- **`SparkStandAloneClusterTaskProcessor`** rewritten — expanded submission handling with improved error recovery and status synchronization

### Fixed
- **Health controller tests** — added `TestSecurityConfiguration` to `HealthControllerTest` and `HealthControllerCustomPathTest` to resolve missing security context in tests

---

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
