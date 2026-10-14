# Changelog

## [0.12.0] — 2026-10-13

Modelling access control moved from **global Keycloak roles to domain-driven RBAC**. Roles now
carry a domain prefix — `<domain>:<ROLE>` with `ROLE` in `VIEWER` / `EDITOR` / `CONTROL` — plus
a single global, non-domain `WORKSPACE_ADMIN` role.

### Changed

#### Domain-driven RBAC in `jarina-ui-svc`

- **Role model** (`org.jarina.ui.modeller.auth`): `ModellerRole` replaced by `DomainRole`
  (`VIEWER`/`EDITOR`/`CONTROL`; `VIEWER` implied by the other two, `EDITOR`/`CONTROL`
  orthogonal — `EDITOR` edits configs/repos but cannot stop tasks, `CONTROL` stops Spark tasks
  but cannot edit) and `ModellerRoleSupport` (parses `<domain>:<ROLE>`; the global
  `WORKSPACE_ADMIN` role is ignored by the domain map); `UserContext` exposes `domains` and
  `hasDomainRole`/`hasAnyDomainRole`/`hasWorkspaceAdmin`; new `DomainAccess` (allowed-domain
  resolution).
- **`GET /api/user` contract**: returns `{username, name, email, roles, domains}`; the
  `effectiveRole` string is gone. `domains` maps each domain to one display label with
  precedence `CONTROL > EDITOR > VIEWER`.
- **Server-side enforcement** stays authoritative and answers `403`: `EditorService`,
  `VcsService`, `ReviewService`, `SparkProxyController` (kill/killall → `CONTROL`),
  `AdminWorkspaceService`; `SSecurityConfig` keeps everything else `authenticated()` and gates
  the admin surface at `/api/workspace/admin/**` (`hasRole('WORKSPACE_ADMIN')`).
- **Domain filtering** of the monitoring data: `CatalogService`, `ScheduleService` and
  `VcsLogService` intersect the user's domains with the configured ones (`DomainAccess`) and
  drop rows outside them (empty result → `[]`).
- **Admin endpoints moved** from `/api/admin/**` to `/api/workspace/admin/**`
  (`AdminController`, frontend `AdminView`).
- **Frontend**: `ModellerSection` always shows the `My workspaces / Admin` tabs (a non-admin
  sees the backend `403` as a banner); `WorkspacePicker` disables **New branch** without an
  `EDITOR` role; `EditorView` derives `readOnly` per domain from `domains` (no longer from
  `effectiveRole` or `workspace.own`); the `effectiveRole`/`isAdmin`/`canEdit` helpers are gone.
- **Demo Keycloak realm** (`demo/compose/conf_infra/security/realms/jarina-realm.json` and the
  K8s configmap): realm roles are now domain roles for the configured domains
  (`platform`/`analytics`/`processing`) plus `WORKSPACE_ADMIN`; demo users `de_view` (viewer),
  `de_editor` (editor + control) and `de_admin` (workspace admin) updated accordingly. Colons in
  role names are supported by Keycloak.
- **Docs**: `jarina-ui-svc/doc/arch/architecture.md` (§9 rewritten, + `doc-ru` mirror),
  `jarina-ui-svc/doc/readme.md` (+ RU), `doc/security/security.md` (+ RU) and
  `SUMMARY_FEATURES.md` describe the domain model; the audit section states that
  user-initiated mutations are logged under the real Keycloak `username` and that writing them
  as `system` is forbidden.
- **Tests**: new `RbacEnforcementTest` (Spark kill / editor / review / admin 403s);
  `ModellerRoleSupportTest` and `SecurityContextBootTest` updated for domain roles. Frontend
  suite updated to `domains` fixtures (44 tests: `EditorView` per-domain `readOnly`,
  `WorkspacePicker` `New branch` enable/disable). Backend totals 107 tests.

## [0.11.0] — 2026-10-02

Configuration domains became first-class: **a domain is a Git repository**, and the domain stamp
(`domainKeyName`) now travels from the repository through config-svc, the scheduler, the UI
modeller and the task executor. On the UI side this arrived together with the **Modelling
workbench** — a Git-backed metadata editor with workspaces, branches and reviews.

### Added

#### Domains and per-domain repositories

- **Domain = Git repository** (`jarina-config-svc`): `JarinaVCSProperties` binds
  `jarina.config.vcs.domains.<name>` — a tree of domains, each with its own
  `git.repository-url` / `branch` / `local-clone-path` / `private-key-path`, a `priority` and
  nested `domains`. Each domain is fetched and applied on its own; the apply order is
  depth-first with parents before children (`orderedDomains()`), `priority` ascending inside one
  level, `Integer.MAX_VALUE` and last otherwise, ties broken by domain name. A domain without
  `repository-url` is skipped entirely; a failing domain skips its whole sub-tree until the
  parent succeeds. Per-domain env vars: `JARINA_CONFIG_GIT_<NAME>_URL|_BRANCH|_CLONE_PATH|_PRIVATE_KEY_PATH`.
- **Legacy single-repository mode kept**: when `domains` is empty and `git.repository-url` is
  set, that repository is exposed as one domain named `default` (`rootDomains()`); as soon as
  `domains` has an entry the legacy block is ignored and does not become an extra `default`.
  The sync cycle itself stays global: `jarina.config.vcs.git.sync.{enabled,interval-ms,initial-delay-ms}`.
- **`domainKeyName` stamping and identity rules**: `GitOpsSynchronizer.stampDomainOn(...)` stamps
  every managed kind except `Script`; `SQLTemplate` rows are content-only and shared between
  domains, their domain derived from the referencing `Driver`/`Task`
  (`SQLTemplateEntitySpecifier.domainOf()`). `keyName` stays the global primary key and is **not**
  part of a composite key with `domainKeyName`. REST-created constructs have `domainKeyName = null`.
- **Domain isolation rules**: a `Schedule` may only reference data sets of its own domain — a
  foreign or domain-less data set fails the whole commit with the new
  `DataSetDomainConflictException` (`GitOpsSynchronizer.checkScheduleDatasetDomains()`); the REST
  API refuses to overwrite a construct of one domain with a construct of another —
  `DomainConflictException` → `409 Conflict` via `rejectDomainConflict(...)` in the services of
  `DataSet`, `DataSource`, `Driver`, `Task`, `ScenarioActTemplate`, `Schedule`,
  `QualityMetricsConf` and `TaskExecutionServiceGroup`. `domain_key_name` was added to
  `vcs_sync_log` and `vcs_object_log`, and `domainKeyName` became an optional filter of
  `GET /v1_0/configs/vcs/logs` and `.../vcs/objectlogs`.
- **Domain-scoped executor configuration** (`jarina-task-executor-svc`):
  `DomainDataSourceServiceProperties` binds
  `jarina.task-executor.domains.<domainKeyName>.<dataSourceKeyName>.service-properties`
  (free-form map). `ExecuteService.prepareProperties(...)` looks it up per data source and merges
  it into `DataSourceDTO.getService().getProperties()` (`putAll` — the local value overrides the
  repository-declared one), so per-domain connection settings and secret-provider options reach
  `JdbcConnectionFactory` without being committed to Git.
- **Domain check in the scheduler** (`jarina-scheduler-svc`):
  `ScheduleTaskInstanceService.checkDomain(...)` compares the schedule domain against
  `allowedDomains ∪ {domainKeyName}` of the task's `TaskExecutionServiceGroup` (new fields
  `domainKeyName` and `allowedDomains`, the latter in the `task_execution_service_group_domains`
  collection table). A mismatch stores the `ScheduleTaskInstance` as `CONF_ERROR`, deletes the
  pending producer message and publishes nothing; a missing group is only a warning. Since
  executors are grouped solely by `taskExecutionServiceGroupName` (the Kafka `group.id`), this
  check is what keeps a foreign domain out of an executor group.
- **Per-domain repositories in the UI modeller** (`jarina-ui-svc`):
  `jarina.modeller.domains.<name>.{repository-url,branch-main}` — `GET /api/vcs/branches`
  returns one `DomainBranchesResponse {domain, branches[], branchMain}` per domain (the branch
  panel renders a domain tree), `POST /api/vcs/branch` creates a branch inside a named domain,
  and a workspace is a set of `(domain, branch)` pairs (`POST /api/vcs/workspace`) checked out
  into per-domain folders `<domain> (<branch>)` with id
  `md5(username + "|" + sorted "domain=branch")`. Review is submitted **per domain**
  (`POST /api/vcs/review/{workspaceId}`): files are grouped by their covering folder, each group
  is committed and pushed to its own domain branch and turned into an MR/PR against that
  domain's `branch-main`; an empty diff yields `NO_CHANGES`. `domainKeyName` is excluded from
  every form schema (`SchemaService.DERIVED_PROPERTIES`) — the domain comes from the repository,
  never from the file content. Resolution falls back to the legacy single repository as the
  domain `default` (`ModellerProperties.domainNames()/domainRemoteUrl()/domainBranchMain()`).
- **Tests**: `ScheduleTaskInstanceServiceDomainCheckTest` (foreign domain rejected and the task
  not published, a domain in `allowedDomains` accepted, the group's own domain accepted) and
  `DomainDataSourceServicePropertiesTest` (dynamic-map binding through `Environment`, per-entry
  isolation, same singleton exposed by `ExecuteService`).

#### Modelling workbench in `jarina-ui-svc`

- **Workspaces from a Git branch**: `WorkspaceStorage` SPI — `LocalFsWorkspaceStorage`
  (`jarina.modeller.storage.root-directory`) and `S3WorkspaceStorage` (S3 with a minimal
  built-in AWS SigV4 signer); `WorkspaceManager`, `WorkspaceSeeder`, background
  `WorkspaceCleanupTask` (inactivity TTL, default 4 h) and
  `jarina.modeller.session.inactivity-minutes`.
- **VCS provider SPI**: `LocalGitVcsProvider` (JGit), `GitLabApiVcsProvider`,
  `GitHubAppVcsProvider`, `DisabledVcsProvider` plus a factory; system technical account
  (`jarina.modeller.vcs-system-account`, auth type `ssh`/`token`/`basic`, GitHub App options);
  declarative matrix in `jarina.modeller.*` (`vcs-provider`, `auth-strategy`
  `jwt-rbac`/`token-exchange`).
- **File CRUD and editors**: `EditorController` (tree/dirs, create by `kind`+`keyName`+`directory`,
  read, save via `YamlEditorService`, rename/move/delete of files and directories, restore from
  the VCS state), schema-driven forms (`SchemaController`, `KindSchema`/`FieldSchema`,
  `EnumOptionsService`) with a form ↔ raw-YAML toggle, and React Flow visual editors —
  `ErDiagramEditor` (`kind: ERDiagram`), `DataLineageDiagramEditor`
  (`kind: DataLineageDiagram`) and a generic DAG editor. `ERDiagram`/`DataLineageDiagram` are
  non-configurational (`isConfig=false`): stored in the repository but not applied by config-svc.
- **Review, RBAC and observability**: submission to review with a comment
  (`ReviewService`), restore, modeller roles `VIEWER < EDITOR < ADMIN` with
  ownership-based edit rights and `readOnly` from `effectiveRole`, admin surface
  (`AdminController`: all workspaces, force-delete, cleanup TTL, sync-log tail), sync-log
  capacity (`jarina.modeller.logging.sync-log-capacity`).
- **New shared kinds** `ER_DIAGRAM` (order 11) and `DATA_LINEAGE_DIAGRAM` (order 13) in
  `YamlMetadataKind`, with directories `erdiagrams` / `datalineagediagrams`.
- **Frontend test suite**: Vitest 3 + React Testing Library (jsdom), configured in
  `vite.config.js` (`test/setup.js` polyfills the React Flow browser APIs) — 41 tests across
  `DataLineageDiagramEditor` (24), `FormEditor` (7), `EditorView` (5, incl. the multi-domain
  leave-on-review flow) and `WorkspacePicker` (5, incl. per-domain branch preselection).

#### Documentation and repository housekeeping

- New `jarina-config-svc/doc/content_configuration/domains.md` (+ `doc-ru` mirror): the tree
  and apply order, ownership and identity rules, isolation rules, the full property table, the
  legacy single-repository block, observability and object fields.
- New `jarina-ui-svc/doc/arch/architecture.md` (frontend architecture review with PlantUML
  sources under `doc/arch/diagrams/`) and `jarina-ui-svc/doc/doc_requirements.md`.
- Domain chapters and configuration references added to
  `jarina-ui-svc/doc/readme.md` (+ RU), `jarina-scheduler-svc/doc/readme.md` (+ RU),
  `jarina-task-executor-svc/doc/readme.md` and `properties.md` (+ RU mirrors) and
  `jarina-config-svc/doc/readme.md` (+ RU).
- `taskexecutionservicegroups.md` (EN/RU) documents `domainKeyName` and `allowedDomains`.
- **License headers**: the Apache-2.0 notice from `NOTICE` was added to every `.java`, `.bash`
  and `.sh` file that did not carry a copyright notice (via `.utils/autonotice.bash`).
- Russian VCS docs moved from `doc-ru/cvs/` to `doc-ru/vcs/`, fixing the `cvs`/`vcs` path typo;
  `content_configuration/namespaces.md` (EN/RU) removed together with the `NameSpace` kind.
- `SUMMARY_FEATURES.md` extended with a dedicated "Домены и репозитории" section and
  per-service cross-references.

### Changed

- **`ConfigurationProduceResolver` SPI replaces the `ConfigKind` enum**
  (`jarina-config-svc`): one `@Component` per kind (`Driver`, `DataSource`, `Task`,
  `TaskExecutionServiceGroup`, `DataSet`, `Schedule`, `ScenarioActTemplate`, `Script`,
  `QualityMetricsConf`) each declaring `getKind()` and `resolve(keyName)`, collected by
  `ConfigurationProduceResolverRegistry.findByKind(...)` (duplicate kinds fail fast). Kinds are
  resolved through services instead of a hard-coded DTO/order table, which is what the
  `NameSpace` kind removal and the new domain stamp needed.
- `GitVcsConfigurationProperties` replaced by `JarinaVCSProperties`; the client factory moved
  to `GitVcsClientFactory`; `GitOpsChangeSetBuilder`, `GitOpsSynchronizer`,
  `GitOpsFailureRecorder`, `GitOpsScheduler`, `VcsClient`, `GitVcsClient` and the log
  services/repositories became domain-aware.
- `CurrentDomainContext` (`org.jarina.config.vcs`) carries the domain being applied through
  the stamping and validation path.
- **`jarina-scheduler-svc`**: `ScheduleConfigConsumerService` → `ConfigurationChangeConsumerService`
  with `ConfigurationChangeKafkaConfiguration` /
  `ConfigurationChangeConsumerKafkaConfigurationProperties`, consuming the
  `configuration_changes` topic; added `TaskExecutionServiceGroupConfigService` as an in-memory
  cache of executor-group configuration (populated by Kafka change events, fetched on miss over
  REST).
- **Demo**: the configuration repository is now laid out per domain
  (`demo/conf_git/domains/{platform,processing,analytics}`), and the REST-loaded demo
  configurations moved to `demo/conf/`.
- Project and module versions bumped `0.10.0` → `0.11.0`.

### New dependencies

None. All libraries required by the Modelling workbench (`org.eclipse.jgit`,
`org.eclipse.jgit.ssh.apache`, `tools.jackson.dataformat:jackson-dataformat-yaml`,
`spring-boot-starter-oauth2-resource-server`) were already declared by the project at tag
`0.10.0` — `jarina-ui-svc` only started consuming them — so `THIRD-PARTY-NOTICES` is
unchanged.

## [0.10.0] — 2026-09-01

### Added

#### VCS — Configuration Versioning System (GitOps) in `jarina-config-svc`

Git repository as the **source of truth** for configuration metadata — the same
configuration DTOs as the REST API can be written as Kubernetes-style YAML/JSON files and
are periodically synchronized into the configuration database.

- **`VcsClient` abstraction** (`org.jarina.config.vcs`): `init` / `pull` /
  `getCurrentCommitId` / `getDiff` / `readFileContent`; supporting value types
  `VcsDiffEntry`, `VcsChangeType`, `VcsClientException` (infrastructure failures are
  retried, not marked FAILED).
- **`GitVcsClient`** (JGit): clone/open, `+refs/heads/*:refs/remotes/origin/*` fetch +
  hard reset, `DiffFormatter` with rename detection (rename reported as delete+create),
  SSH `publickey` auth via `org.eclipse.jgit.ssh.apache`.
- **`GitOpsScheduler`** — `@Scheduled` orchestrator (guarded by
  `jarina.config.vcs.git.sync.enabled=true`); `sync()` is `synchronized` and
  idempotent; skips already-applied commits, records config errors as `FAILED`, retries
  infrastructure errors.
- **`GitOpsChangeSetBuilder` + `GitOpsYamlParser` + `ConfigKind`**: only config files
  (`.yaml`/`.yml`/`.json`, no dotfiles) are considered; mandatory `kind` field;
  case-/separator-insensitive kind matching; case-insensitive enums; unknown properties
  are a hard error. 10 recognized kinds with dependency `order`.
- **`GitOpsSynchronizer`** — single-transaction atomic apply (`applyAll` in kind order,
  datasets ordered by `sources` dependencies) / `unmanageAll` in reverse order; records
  every touched construct in `vcs_object_log`; a whole commit succeeds only when the
  `SUCCESS` row is written to `vcs_sync_log`.
- **`isVcsManaged` contract** on all configuration entities: REST create/update/delete of
  a VCS-managed construct fails with HTTP `409 Conflict` (`VcsManagedException`); a
  deleted YAML file only clears the flag (two-step delete); three-way service contract
  `save(...)` / `saveVcs(...)` / `unmanage(...)`.
- **Read-only REST endpoints for the UI**: `GET /v1_0/configs/vcs/logs` (sync history
  with status/error/commitId) and `GET /v1_0/configs/vcs/objectlogs` (per-object log).
- **Configuration properties** `jarina.config.vcs.git.*`: `repository-url`, `branch`,
  `local-clone-path`, `private-key-path`, `sync.enabled` / `sync.interval-ms` /
  `sync.initial-delay-ms` (mapped to `JARINA_CONFIG_GIT_*` env vars).
- **Tests**: `GitOpsIntegrationTest`, `GitVcsClientTest`, `GitOpsChangeSetBuilderTest`,
  `GitOpsSchedulerUnitTest`, `GitOpsYamlParserTest`, `TestGitRepository` helper.

#### UI — VCSLog panel

- **`jarina-ui-svc`**: new "VCS" panel (`VCSLog` / objects search as tabs) showing the
  sync history (`SUCCESS` rows with applied commit id, `FAILED` rows with error message)
  and the per-object log; backed by `VcsLogService` / `VcsLogController`, VCS added to the
  frontend routing and API client.
- SPA assets rebuilt (`index.html`, hashed `assets/index-*`).

#### Demo — Git-sourced configs (`conf_git`) and bootstrap

- **`demo/k8s`**: configuration moved out of the `git-server` Helm chart into a dedicated
  `demo/k8s/conf_git/` tree (one config construct per YAML file, K8s-style `kind`), with
  new `bootstrap-configs.bash` to seed the `git-server` config repo that `config-svc`
  VCS syncs from.
- New/renamed task templates (`beginTemplate`, `checkTemplate`, `prepareJdbc`,
  `sparkTemplate`, `sparkDQTemplate`), a default `taskexecutionservicegroups/default.yaml`,
  and VCS wiring (repo `git://git-server:9418/config-repo.git`, branch `main`,
  `sync.enabled=true`) in `demo/k8s`/`demo/compose`.

#### Documentation

- `jarina-config-svc/doc/vcs/vcs_for_developers.md` + `doc-ru/vcs/vcs_for_developers.md`
  — VCS architecture, sync pipeline, `VcsClient` abstraction, YAML parsing, `isVcsManaged`
  contract, developer parameters, extension guide.
- `jarina-config-svc/doc/vcs/git_extension_user_guide.md` + `doc-ru/vcs/git_extension_user_guide.md`
  — repository layout, YAML format, enablement, sync semantics, error messages.
- `jarina-config-svc/doc/readme.md`, `doc-ru/readme.md`, `service_configuration.md`
  (EN/RU) — VCS section and links to the new docs.
- `demo/compose/git-server-config-repo.md` — demo VCS config repo layout.

#### New dependencies

- `org.eclipse.jgit:org.eclipse.jgit` and `org.eclipse.jgit.ssh.apache` (JGit 6.7.0) —
  Git transport for `GitVcsClient`.
- `tools.jackson.dataformat:jackson-dataformat-yaml` — YAML parsing for VCS config files.
- `THIRD-PARTY-NOTICES` updated accordingly.

### Changed

- **Version 0.9.0 → 0.10.0** for all modules, Docker images and Helm chart image tags.
- **`jarina-ui-svc`** `VcsSection.jsx` — redesigned to tabbed layout (VCSLog / object
  logs), vertical splitter and error column.

---

## [0.9.0] — 2026-08-28

### Added

#### New modules — credential providers

- **`jarina-credential-providers-jdbc`** — shared, Spark-independent secret resolution library (no Spring dependency; Spring Boot artifacts are declared `provided`):
  - `SecretProvider` SPI and the `SecretResolver` helper (`hasProvider`, `resolvePassword`, `sanitize`, `resolveAndSanitize`)
  - HTTP clients `VaultHttp` (OpenBao/Vault; token via `VAULT_TOKEN` env or Kubernetes Service Account auth) and `LockboxHttp` (Yandex Cloud Lockbox; IAM token via Instance Metadata or authorized key file `YC_AUTH_KEY_PATH`)
  - `SecretCache` — in-memory cache, 5-minute TTL per JVM
  - JDBC providers `BaoJdbcSecretProvider` (KV v2) and `YcLockboxJdbcSecretProvider`
  - `JarinaSecurityContext` for resolving secrets inside Spark closures on Executors
- **`jarina-credential-providers-spark`** — Spark-specific credentials, resolved on Driver and Executors:
  - `JarinaSecureJDBCTableCatalog` — secure replacement for Spark's `JDBCTableCatalog`: resolves the DB password via `SecretResolver` at Driver + Executors and masks the original exception
  - S3 providers `BaoS3CredentialsProvider` / `YcLockboxS3CredentialsProvider` for Spark S3A
- **`jarina-spark-credential-providers`** split into the two modules above; the resulting jars (`jarina-credential-providers-jdbc-0.9.0.jar`, `jarina-credential-providers-spark-0.9.0.jar`) are copied into the Spark image

#### Runtime secret resolution

- **`JdbcConnectionFactory`** (`jarina-task-executor-api`) — resolves the DB password through `SecretResolver` when the datasource `service.properties` contains `secretProvider`; the security options are stripped before the map reaches the JDBC driver; unchanged when no provider is configured (backward compatible)
- **`jarina-task-executor-api`** now depends on `jarina-credential-providers-jdbc`

#### OpenBao / Vault integration in the demo

- **Docker Compose**: `openbao` service (`quay.io/openbao/openbao:2.0`) with the init script `demo/compose/conf_infra/openbao/init.sh`:
  - dev server, KV v2 secrets engine, seeding `kv/jarina/database` and `kv/infrastructure/minio`
  - read-only policy `jarina-spark-readonly`, scoped token `jarina-spark-token`
  - `VAULT_TOKEN` env added to `spark-master`, `spark-worker-1`, `spark-history`, `task-executor-svc-database`; healthcheck + `depends_on` wiring
- **`demo/compose/conf_infra/spark-defaults.conf`**:
  - `processingdb` catalog switched to `JarinaSecureJDBCTableCatalog` + `BaoJdbcSecretProvider` (`secret-key: kv/data/jarina/database:password`) — plaintext PostgreSQL password removed
  - S3 credentials moved from hardcoded `spark_user`/`spark_pwd` to `BaoS3CredentialsProvider` reading `kv/data/infrastructure/minio` from OpenBao
- **Datasource configs** (`demo/compose/conf/datasources/processingdb.json`, `demo/k8s/conf/datasources/processingdb.json`) — `password` replaced by provider options (`secretProvider`, `secret-key`, `vault-url`)
- **Kubernetes umbrella chart** `jarina-management`:
  - `openbao:` section in `values.yaml` (image, env with `JARINA_DB_PASSWORD`, `MINIO_ROOT_PASSWORD`, `BAO_DEV_ROOT_TOKEN_ID`, `BAO_TOKEN`; optional `valueFrom.secretKeyRef` support)
  - new templates: `openbao-configmap` (init script placeholder for `init.sh`), `openbao-deployment` (`BAO_ADDR` → `http://<release>-openbao:8200`, exec `bao status` probes), `openbao-service` (ClusterIP:8200)
- **Spark image** `docker/jarina-spark-aws` — provider jars added to `/opt/spark/jars`, version 0.9.0
- **jarina-ui-svc** — OpenBao added to the services topology (health-check `/v1/sys/health`, vertex, edge)

#### Documentation

- `doc/security/security.md` + `doc-ru/security/security.md` — new section "Secrets for data connections (credential providers)": option contract (`secretProvider`, `secret-key`, `vault-url`, `vault-role`, `vault-k8s-auth-path`, `secret-id`, `secret-version`), where to configure (`spark-defaults.conf` / datasource `service.properties`), OpenBao/Vault and Yandex Cloud Lockbox setup, security guarantees (secrets are never logged, masked errors, 5-minute cache); overview and configuration reference updated
- `jarina-credential-providers-spark` README (EN/RU) — configuration, deploying to Spark (`--jars`), caching / timeouts / troubleshooting
- `jarina-task-executor-svc` docs (EN/RU):
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

- **`jarina-common-health`** — shared security infrastructure:
  - `AuditLoggingFilter` — logs incoming request details
  - `BearerTokenClientHttpRequestInterceptor` — attaches OAuth2 access token to outbound REST calls
  - `KeycloakRoleConverter` — maps Keycloak realm roles to Spring Security authorities
  - `RestClientSecurityConfiguration` — configures `RestClient` with token relay
  - `TestSecurityConfiguration` — test-only security config for health controller tests
  - `KeycloakRoleConverterTest`
- **`SecurityConfig`** added to: `jarina-config-svc`, `jarina-scheduler-svc`, `jarina-state-svc`, `jarina-task-executor-svc`, `jarina-task-proxy-for-spark`, `jarina-ui-svc`
- **`SecurityConfigTest`** added to: `jarina-config-svc`, `jarina-scheduler-svc`

#### OpenAPI
- **`OpenApiConfig`** — Swagger UI endpoints added to `jarina-config-svc`, `jarina-scheduler-svc`

#### jarina-ui-svc
- **`UserController`** — `/api/user` endpoint returning authenticated user info (name, email, roles)

#### jarina-config-svc
- **`SparkRestClientConfiguration`** — REST client config for Spark Proxy communication

#### Logging
- **`logback-spring.xml`** added to all services (config-svc, scheduler-svc, state-svc, task-executor-svc, task-proxy-for-spark), replacing `log4j.properties`

#### Security documentation
- `doc/security/security.md` — interservice security architecture (EN)
- `doc-ru/security/security.md` — русская версия
- PlantUML diagrams: `interservice-security.puml`, `ui-security.puml`, `spark-apps-security.puml`

#### Demo & infrastructure
- **Docker Compose** — Keycloak service added (image `quay.io/keycloak/keycloak:26.0`, import realm, healthcheck)
- **Keycloak realm** — `demo/compose/conf_infra/security/realms/jarina-realm.json` (clients: `jarina-ui-client`, `jarina-internal-client`; roles: `USER`, `ADMIN`; demo users: `de_view`, `de_editor`)
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

#### jarina-task-executor-svc
- **`SparkStandAloneClusterTaskProcessor`** rewritten — expanded submission handling with improved error recovery and status synchronization

### Fixed
- **Health controller tests** — added `TestSecurityConfiguration` to `HealthControllerTest` and `HealthControllerCustomPathTest` to resolve missing security context in tests

---

## [0.7.0] — 2026-08-19

### Added

#### New modules
- **`jarina-ui-svc`** — web UI service
  - Spring Boot backend (`/api/catalog`, `/api/schedules`, `/api/services`, `/api/spark-proxy`, `/api/states`) aggregating config-svc, scheduler-svc, state-svc and task-proxy-for-spark
  - React frontend served from static resources (Vite build): Catalog tree, dataset lineage, model/DDL scripts, relations (ER) view, schedules + pipeline DAG, services topology, spark submissions, dataset state control
  - Configurable service graph via `jarina.ui.services/edges/vertices` (`UiServiceProperties`)
  - `HealthChecker` for services availability, `GlobalExceptionHandler`
- **`jarina-common-health`** — shared liveness/readiness endpoints `/healthz` `/readyz` (paths configurable via `jarina.health.liveness-path` / `readiness-path`), applied to all services
- **`jarina-task-proxy-for-spark-rest-client`** — REST client for the spark proxy submission query API

#### jarina-task-proxy-for-spark — submission querying
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
- **`TaskDTOValidator`** in `jarina-validators`
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
- `SQLTemplateFactory` moved from `jarina-common` to `jarina-task-executor-api`
- `Endpoint` constants for lineage, task templates, schedule DAG

#### Demo & infrastructure
- New Helm chart `jarina-management-ui-svc`
- Demo task templates: `begin.json` (`lockedStateTaskProcessor`), `check.json` (`dependencyCheckStateTaskProcessor`), `prepare-jdbc.json` (`jdbcTaskProcessor` + `createTableSQLProcessorBody`)
- Demo configs updated (datasources with `databaseProtocol`, schedules, scenario-act-templates) for compose and k8s
- Docker: images bumped to 0.7.0, `build.bash` version checks, updated spark-aws image
- `doc/nexttimedev/openmetadata` — OpenMetadata integration diagrams (classCurrent, omdClass, sendtoomd)

### Changed
- `jarina-config-svc` — task storage refactored: `TaskAbstract`/`ScenarioActTask` replaced by `Task` + `TaskProcessorArg` entities (`TaskRepository`, `TaskService`, `TaskProcessorArgRepository`, `TaskNotFoundException`); `Driver` entity removed in favor of DTO/driver service
- `jarina-scheduler-svc` — `ScheduleTaskInstanceService` retry semantics driven by `maxRetries`; `ScheduleInstanceController` interval queries
- All services — health check configuration (`jarina.health.*`)

### Fixed
- Proxy `getStatus` returns `NOT_FOUND` for missing submissions
- Proxy kill/clear of queued (submissionId == null) tasks — removed from DB instead of failing on cluster
- `SparkSubmission` status/message deserialization in query DTOs

---

## [0.6.0] 2026-07-30

### jarina-task-proxy-for-spark — major refactoring & new schedulers

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
- **Helm chart** `demo/k8s/jarina-management-helm-charts/jarina-task-proxy4spark/` — new chart for the proxy service
- **Dockerfile** `docker/jarina-task-proxy4spark/` — multi-stage build with `jar-with-dependencies`
- **README** (EN/RU) — rewritten with scheduler descriptions, architecture updates
- **PlantUML diagrams** — `sequence-cleanup.puml`, `activity-inspectStatus.puml` added; existing diagrams updated

### jarina-task-executor-svc — cleanup & fixes

- **Removed `K8sSparkNativeTaskProcessor`** and all k8s-native classes (`K8sClientService`, `K8sConfigService`, `K8sPodStatusWatcher`, `PodPhase`, `PodUtilService`) — not used
- **Removed `SourceConfUtil`** from jarina-jinjava
- **`ExecuteService`** — fixed NPE on uninitialized `JinJavaUtils`
- **`processors.md`** — updated to reflect current architecture (removed references to non-existent processors, added `AbstractSparkDeployTaskProcessor` and `SparkRestDeployFactory` descriptions)

### Demo & infrastructure

- **Helm charts**: all services updated to 0.5.0; `jarina-task-proxy4spark` chart added
- **Docker Compose**: updated Spark templates, proxy port mappings
- **Dockerfiles**: `build.bash` for all images; fixed `jarina-spark-aws` base image to `spark:3.5.8-scala2.12-java17-ubuntu`
- **K8s scripts**: `install.bash`, `setup-default.bash`, `tunnels.bash`, `remove_images.bash` updated

---

## [0.5.0] — 2026-06-23

### Added
- **`jarina-task-proxy-for-spark`** — new module: REST API proxy for Spark Submit with PostgreSQL-backed task queue
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
- **Spark DQ module** (`jarina-task-executor-spark-dq-app`) — data quality validation as Spark job
- **Spark Dataset module** (`jarina-task-executor-spark-dataset-app`) — dataset processing as Spark job
- **`ScenarioActTemplate`** configuration in config-svc

### Changed
- Spark version bumped to 3.5.8
- Refactored `SparkRestClientApi` — unified status response handling

---

## [0.3.0] — 2026-04-23

### Added
- **Spark K8s native task processor** — initial (later removed)
- **Spark REST client** (`jarina-spark-rest-client`) — client for Spark Standalone REST API
- **Demo helm charts** for all services

### Fixed
- Status response deserialization in `SparkRestClientApi`

---

## [0.2.0] — 2025-07-13

### Added
- **State service** (`jarina-state-svc`) — manages dataset increment states (LOCKED, SUCCESS, etc.)
- **State processors**: `LockedStateTaskProcessor`, `SuccessStateTaskProcessor`, `DependencyCheckStateTaskProcessor`
- **State REST client** (`jarina-state-rest-client`)

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
  - `jarina-common` — shared DTOs, utils (`SparkConfUtil`, `Coalesce`, etc.)
  - `jarina-jinjava` — Jinja template engine integration
  - `jarina-common-rest-client`, `jarina-config-rest-client`, `jarina-scheduler-rest-client`, `jarina-task-executor-rest-client`
- **Configuration service** (`jarina-config-svc`) — manages datasources, datasets, drivers, scenario templates
- **Scheduler service** (`jarina-scheduler-svc`) — manages schedules and task delivery via Kafka
- **Task executor service** (`jarina-task-executor-svc`) — executes tasks with JDBC and Spark processors
- **Spark API** (`jarina-task-executor-spark-api`) — shared Spark session configuration
- **CLI** (`jarina-cli`) — command-line interface for configuration management
- **UI** (`jarina-ui-svc`) — web UI placeholder
- **Validators** (`jarina-validators`) — validation utilities
- **Docker Compose** (`demo/compose/`) — development environment with PostgreSQL, MinIO, Hive Metastore
- **K8s helm charts** (`demo/k8s/jarina-management-helm-charts/`) — deployment charts for all services
- **PlantUML diagrams** in each module
