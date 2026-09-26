# VCS subsystem for platform developers

This document describes how the VCS (Configuration Versioning System) subsystem works,
how to extend it, and which parameters a developer needs to know about. It is aimed at
developers of the lakehouse platform. If you only manage configuration files in Git
repositories, see the [Git extension user guide](git_extension_user_guide.md) instead.

## 1. What VCS is

VCS is the configuration-as-code (GitOps) subsystem of `lakehouse-config-svc`. It
treats Git repositories as the **source of truth** for configuration metadata: the same
configuration DTOs that the REST API accepts can be written as Kubernetes-style YAML
files, and the subsystem periodically synchronizes them into the configuration database.

Configuration is organized into **domains**. A domain owns its own Git repository and is
synchronized independently; a domain may contain nested domains. The hierarchy is declared
in `lakehouse.config.vcs.domains` (see section 6). Every construct applied from a domain
repository is stamped with the domain name (`domainKeyName`), which scopes constructs by
domain and allows a `Schedule` to reference only data sets of the same domain.

In contrast to the REST API, VCS gives:

- full history of every configuration change (Git commits);
- declarative, reviewable configuration;
- atomic application of a whole commit;
- automatic protection of VCS-managed constructs from accidental REST edits (see the
  `isVcsManaged` flag).

All VCS-specific code lives in the package `org.lakehouse.config.vcs` (plus the two
read-only controllers `VcsSyncLogController`/`VcsObjectLogController` in
`org.lakehouse.config.controller` and `VcsManagedException`/`DomainConflictException`/
`DataSetDomainConflictException` in `org.lakehouse.config.exception`).

## 2. How the sync works

```
┌──────────────────────┐   fetch + diff per domain  ┌────────────────────────────────┐
│  Git repositories    │ ──────────────────────────▶ │  GitOpsScheduler (poll)        │
│  (one per domain,    │   pull() per domain         │  ├─ priority-ordered domains  │
│  tracked branch)     │                             │  ├─ getCurrentCommitId()       │
└──────────────────────┘                             │  ├─ diff vs last success       │
                                                     │  └─ sync() in one transaction  │
                                                     └───────────────┬────────────────┘
                                                                     ▼
                                                     ┌────────────────────────────────┐
                                                     │  GitOpsChangeSetBuilder        │
                                                     │  └─ GitOpsYamlParser           │
                                                     └───────────────┬────────────────┘
                                                                     ▼
                                                     ┌────────────────────────────────┐
                                                     │  GitOpsSynchronizer            │
                                                     │  validate → stamp domain       │
                                                     │  → apply → unmanage            │
                                                     │  → record object logs          │
                                                     │  → mark SUCCESS/FAILED         │
                                                     └───────────────┬────────────────┘
                                                                     ▼
                                                     ┌────────────────────────────────┐
                                                     │  PostgreSQL                    │
                                                     │  vcs_sync_log + vcs_object_log │
                                                     └────────────────────────────────┘
```

The orchestrator is `GitOpsScheduler` (`org.lakehouse.config.vcs.component`). It is
registered only when `lakehouse.config.vcs.git.sync.enabled=true` and is driven by
`@Scheduled` with `fixedDelayString` / `initialDelayString` from the same property block.
The method `sync()` is `synchronized` and idempotent and can also be called directly,
for example from integration tests.

Per cycle the scheduler iterates `LakehouseVCSProperties.orderedDomains()`: domains are
traversed **depth-first, parents before their nested domains, same-level domains sorted by
`priority` ascending** (an absent priority is the lowest and is sorted last). Each domain
is synchronized sequentially, inside its own transaction:

1. Skip the domain if no repository is configured (`git.repository-url` blank).
2. Lazily `init()` the `VcsClient` for the domain (`GitVcsClientFactory.create`).
3. `pull()` the tracked branch (fetch + hard reset to the remote branch ref).
4. Resolve the current commit id (HEAD).
5. Skip if the `(commit_id, domain_key_name)` pair already has a row in `vcs_sync_log`
   (`existsByCommitIdAndDomainKeyName`) or if the last `SUCCESS` row of that domain
   already points to HEAD.
6. Load a `CurrentDomainContext` with the domain name (cleared in a `finally`).
7. Compute the diff between the last successfully applied commit of the domain and HEAD.
   On an empty database the whole head is treated as a set of created files.
8. `GitOpsChangeSetBuilder` keeps only configuration files (extension `.yaml`, `.yml`,
   `.json`, name not starting with `.`), parses created/updated files from HEAD and
   deleted files from the last successful commit.
9. `GitOpsSynchronizer.sync(changeSet, head)` validates and applies everything in a
   **single transaction**. Exceptions become a `FAILED` sync-log row for the domain (the
   row is recorded via `GitOpsFailureRecorder` in a separate `REQUIRES_NEW` transaction
   so the failure survives the rollback).
10. Infrastructure failures (`VcsClientException`: repository unreachable, SSH failure,
    unreadable clone) are only logged and retried on the next cycle.
11. Sub-domains have a hierarchical dependency on their parent: when step 3, 9 or the
    infrastructure step fails for a domain, its whole sub-tree is skipped this cycle. The
    traversal is recursive (`rootDomains()` → `nestedDomains(...)`), so nested children are
    only reached when the parent domain succeeded (or had nothing to sync).

### Transaction and ordering rules

`GitOpsSynchronizer` (`org.lakehouse.config.vcs.service.GitOpsSynchronizer`):

- `validateAll` runs the kind-specific validation (Schedule / ScenarioActTemplate / Task)
  **before** anything is written, so an invalid commit touches nothing.
- `stampDomain` forces the current domain (`CurrentDomainContext`) onto every construct of
  the commit (`setDomainKeyName`). The domain is **never** read from YAML; it is derived
  from the repository being synchronized and wins over anything in the parsed DTOs.
  Scripts are the exception: they are content-only entities shared between domains by key,
  so they are not stamped.
- `applyAll` applies created/updated constructs in `YamlMetadataKind.order()` order;
  datasets are additionally ordered by their `sources` dependencies
  (`orderDataSetsDependencyWise`, cyclic references fall back to declared order).
- `unmanageAll` clears the `isVcsManaged` flag of deleted constructs in **reverse**
  dependency order.
- `validateAll` also runs `checkScheduleDatasetDomains`: every `Schedule` may reference
  only data sets whose `domainKeyName` equals the schedule domain (or data sets that are
  not domain-scoped at all). A mismatch throws `DataSetDomainConflictException`, which
  rolls the whole commit back and records `FAILED`.
- After applying, every touched construct is recorded in `vcs_object_log`
  (`date_time_rec`, `object_name` = primary key, `kind`, `file_path`, `commit_id`,
  `domain_key_name`).
- Only when the whole commit succeeded is the `SUCCESS` row (with `domain_key_name`)
  written to `vcs_sync_log`.

## 3. The VCS abstraction

The core abstraction is the interface `VcsClient` (`org.lakehouse.config.vcs.VcsClient`):

| Method | Description |
|---|---|
| `void init()` | Make sure the local copy exists and points at the configured remote. |
| `void pull()` | Fetch the tracked branch and hard-reset the local checkout to it. |
| `String getCurrentCommitId()` | Current HEAD commit id after `pull()`. |
| `List<VcsDiffEntry> getDiff(String baseCommitId)` | Files changed between `baseCommitId` and HEAD. |
| `Optional<String> readFileContent(String commitId, String path)` | File content at a given commit. |

Supporting value types in the same package:

- `VcsDiffEntry` — `record VcsDiffEntry(String path, VcsChangeType type)`.
- `VcsChangeType` — `enum { CREATED, UPDATED, DELETED }`.
- `VcsClientException` — runtime exception for **infrastructure** failures; it is not
  treated as a failed sync, so the cycle is retried later.

### The bundled Git implementation

`GitVcsClient` (`org.lakehouse.config.vcs.client`) is the only bundled implementation
and is built on **JGit**. A client is created **per domain** by `GitVcsClientFactory`
(`org.lakehouse.config.vcs.configuration`) with the domain name and its `git.*` settings.

- `init()` applies SSH settings (only when `privateKeyPath` is set) and clones the
  remote if there is no local `.git`, otherwise it opens the local repository.
- `pull()` fetches `+refs/heads/*:refs/remotes/origin/*` and resets the local checkout
  to the fetched branch ref.
- `getDiff()` uses `DiffFormatter` with rename detection enabled. **A rename is reported
  as DELETE + CREATE**, because configuration constructs are identified by their content,
  not by their file path.
- When `baseCommitId` is blank the whole tree is reported as CREATED.
- SSH transport supports `publickey` authentication with a single private key; the key
  is only consulted when `privateKeyPath` is configured.

Because the whole pipeline above the client only consumes the `VcsClient` abstraction,
exchanging the transport (SVN, Mercurial, a REST service, ...) needs no changes to the
synchronizer, change-set builder, scheduler or persistence.

## 4. Declarative YAML parsing

`GitOpsYamlParser` (`org.lakehouse.config.vcs.yaml.GitOpsYamlParser`) binds a YAML file
to a DTO in **two stages**:

1. `parsePreliminary(content)` returns a `PreliminaryConfig(kind, body)` — it only reads
   the `kind` field.
2. `parseFull(PreliminaryConfig)` binds the remaining fields to the DTO.

Rules:

- the file must start with a `kind` field (Kubernetes style);
- the `kind` value selects the target DTO class (`YamlMetadataKind` enum);
- `kind` matching is case-insensitive and tolerant of dashes/underscores/spaces, so
  `DataSet`, `dataset` and `data-set` are all accepted;
- enum fields are deserialized case-insensitively (e.g. `postgresql` == `POSTGRESQL`);
- unknown properties are a **hard error** to keep the declarative description strict.

`YamlMetadataKind` (in `lakehouse-common`, `org.lakehouse.client.api.constant.YamlMetadataKind`)
defines the recognized kinds with their YAML value, DTO class, identifying field and the
dependency `order`. `isConfig()` tells whether the kind is a configuration object applied
by the config service; kinds such as `ERDiagram`/`DataLineageDiagram` are stored in the
repository but are **not** applied (`isConfig() == false`):

| YamlMetadataKind | YAML `kind` | DTO | order | Applied |
|---|---|---|---|---|
| `DRIVER` | `Driver` | `DriverDTO` | 2 | yes |
| `DATA_SOURCE` | `DataSource` | `DataSourceDTO` | 3 | yes |
| `SCRIPT` | `Script` | `ScriptDTO` | 4 | yes |
| `TASK_EXECUTION_SERVICE_GROUP` | `TaskExecutionServiceGroup` | `TaskExecutionServiceGroupDTO` | 5 | yes |
| `TASK` | `Task` | `TaskDTO` | 6 | yes |
| `DATA_SET` | `DataSet` | `DataSetDTO` | 7 | yes |
| `SCENARIO_ACT_TEMPLATE` | `ScenarioActTemplate` | `ScenarioActTemplateDTO` | 8 | yes |
| `QUALITY_METRICS_CONF` | `QualityMetricsConf` | `QualityMetricsConfDTO` | 9 | yes |
| `SCHEDULE` | `Schedule` | `ScheduleDTO` | 10 | yes |
| `ER_DIAGRAM` | `ERDiagram` | `ERDiagramDTO` | 11 | no |
| `METRIC_DQ` | `MetricDQ` | `MetricDQStatusDTO` | 12 | yes |
| `DATA_LINEAGE_DIAGRAM` | `DataLineageDiagram` | `DataLineageDiagramDTO` | 13 | no |

The primary key per kind is extracted by `GitOpsYamlParser.resolveKey()` (e.g.
`keyName`, `name` or `key`). `ParsedConfig` is the bound record `(YamlMetadataKind kind,
Object dto)`.

## 5. The `isVcsManaged` management contract

Every configuration concrete entity carries a boolean `isVcsManaged`
(`@Column(nullable=false)`, default `false`). It marks constructs owned by VCS:

`Schedule`, `TaskExecutionServiceGroup`, `SQLTemplate`, `Script`, `Task`, `DataSet`,
`TemplateScenarioAct`, `Driver`, `DataSource`, `QualityMetricsConf`.

Entities additionally carry `domainKeyName` (the domain that owns the construct, stamped
during sync); `TaskExecutionServiceGroup` and `Script` are the exceptions and drop the
domain — scripts are shared content, task executor groups are a platform-level setting.

Each entity service implements a **three-way contract** (see `ScriptService`,
`TaskService`, `DataSetService`, `DriverService`, `DataSourceService`, `ScheduleService`,
`ScenarioActTemplateService`, `TaskExecutionServiceGroupService`,
`QualityMetricsConfService`):

1. User-facing `save(...)` / `deleteById(...)` call `rejectIfVcsManaged(key, operation)`
   and throw `VcsManagedException` (HTTP `409 Conflict`) when the construct is managed.
   Domain-aware services additionally call `rejectDomainConflict(...)` and throw
   `DomainConflictException` (HTTP `409 Conflict`) when a request would move a construct
   outside its domain (e.g. rebinding a data set to a different domain's data source).
2. `saveVcs(...)` stores the construct and sets `isVcsManaged = true`. This is the method
   called by `GitOpsSynchronizer.apply()`.
3. `unmanage(...)` clears the flag to `false`. Called by `GitOpsSynchronizer.unmanage()`
   when a YAML file is deleted from the repository.

`Task` and `Driver` additionally cascade the flag to their related `SQLTemplate`s via
`SQLTemplateService.markTaskManaged` / `markDriverManaged`.

The flag is **runtime-derived**, it is never read from YAML: YAML only toggles whether
the whole sync is enabled. It is also the mechanism that protects managed constructs
against accidental REST API modifications.

## 6. Developer parameters

The configuration hierarchy is bound by `LakehouseVCSProperties`
(`org.lakehouse.config.vcs.LakehouseVCSProperties`, prefix `lakehouse.config.vcs`).
`orderedDomains()` returns the flat, dependency-ordered list of domains: parents first,
same-level domains by `priority` ascending (absent priority treated as the lowest).
`DomainProperties.isRepositoryConfigured()` tells whether a domain has a
`repository-url`; per-domain `VcsClient` beans are created on demand by
`GitVcsClientFactory`.

| Property | Environment variable | Default | Meaning |
|---|---|---|---|
| `lakehouse.config.vcs.git.sync.enabled` | `LAKEHOUSE_CONFIG_GIT_SYNC_ENABLED` | `false` | Master switch of the VCS scheduler |
| `lakehouse.config.vcs.git.sync.interval-ms` | `LAKEHOUSE_CONFIG_GIT_SYNC_INTERVAL_MS` | `30000` | Scheduler fixed delay between cycles |
| `lakehouse.config.vcs.git.sync.initial-delay-ms` | `LAKEHOUSE_CONFIG_GIT_SYNC_INITIAL_DELAY_MS` | `10000` | Delay of the first cycle after startup |
| `lakehouse.config.vcs.domains.<name>.priority` | - | lowest | Position in the apply order (ascending, parents before children) |
| `lakehouse.config.vcs.domains.<name>.git.repository-url` | `LAKEHOUSE_CONFIG_GIT_<NAME>_URL` | *(empty)* | Remote repository URL (`git://`, `ssh://`, `http(s)://`, local) |
| `lakehouse.config.vcs.domains.<name>.git.branch` | `LAKEHOUSE_CONFIG_GIT_<NAME>_BRANCH` | `main` | Branch to track |
| `lakehouse.config.vcs.domains.<name>.git.local-clone-path` | `LAKEHOUSE_CONFIG_GIT_<NAME>_CLONE_PATH` | *(empty)* | Local directory owned by the service for the domain clone |
| `lakehouse.config.vcs.domains.<name>.git.private-key-path` | `LAKEHOUSE_CONFIG_GIT_<NAME>_PRIVATE_KEY_PATH` | *(empty)* | SSH private key path, used only for `ssh://` |
| `lakehouse.config.vcs.domains.<name>.domains` | - | - | Nested sub-domains of the domain |

Environment variables for a domain `platform`: `LAKEHOUSE_CONFIG_GIT_PLATFORM_URL`,
`LAKEHOUSE_CONFIG_GIT_PLATFORM_BRANCH`, `LAKEHOUSE_CONFIG_GIT_PLATFORM_CLONE_PATH`,
`LAKEHOUSE_CONFIG_GIT_PLATFORM_PRIVATE_KEY_PATH`.

The shipped `src/main/resources/application.yml` declares only the **legacy** single
`lakehouse.config.vcs.git.*` block, so out of the box the service has no domain
configured and a non-blank `git.repository-url` is exposed as the single domain `default`.
The `demo/compose` stack therefore wires the domain tree explicitly as system properties
(`docker-compose.yaml`), nesting `platform` (`priority: 0`) with a child `processing`
(`priority: 0`) which in turn nests `analytics` (`priority: 0`):

```
-Dlakehouse.config.vcs.git.sync.enabled=true
-Dlakehouse.config.vcs.domains.platform.priority=0
-Dlakehouse.config.vcs.domains.platform.git.repository-url=git://git-server:9418/platform.git
-Dlakehouse.config.vcs.domains.platform.git.branch=main
-Dlakehouse.config.vcs.domains.platform.git.local-clone-path=/tmp/platform
-Dlakehouse.config.vcs.domains.platform.domains.processing.priority=0
-Dlakehouse.config.vcs.domains.platform.domains.processing.git.repository-url=git://git-server:9418/processing.git
...
```

All three repositories are served by a lightweight `git-server`
(`git://git-server:9418/{platform,processing,analytics}.git`, branch `main`).

## 7. Calling the scheduler manually

`GitOpsScheduler.sync()` is public and `synchronized`, so it can be invoked directly
(e.g. by integration tests or an admin trigger). It:

- iterates all configured domains in priority order and skips the ones without a
  configured repository;
- skips `(commit_id, domain_key_name)` pairs that already have a `vcs_sync_log` row;
- records configuration/validation errors as `FAILED` for the failing domain (so the
  offending commit is not retried forever);
- leaves infrastructure `VcsClientException` errors to be retried on the next cycle.

## 8. How to extend the VCS abstraction

### 8.1 Add a new VCS backend

Implement the five `VcsClient` methods (`init`, `pull`, `getCurrentCommitId`,
`getDiff`, `readFileContent`), reuse `VcsDiffEntry`/`VcsChangeType`, and throw
`VcsClientException` for transient infrastructure failures. Register the client per
domain in `GitVcsClientFactory` (or expose a `VcsClient` `@Bean` in a `@Configuration`
guarded by `@ConditionalOnProperty`). The rest of the pipeline (change-set builder,
synchronizer, scheduler, persistence) is transport-agnostic.

### 8.2 Add a new configuration construct kind

1. Add an entry to the `YamlMetadataKind` enum in `lakehouse-common` with a YAML value,
   the target DTO class and a dependency `order`.
2. Make sure the corresponding entity has an `isVcsManaged` boolean field with getter and
   setter, and, if the construct is domain-scoped, a `domainKeyName` field.
3. Implement the three-way contract in the service: `save(...)`/`deleteById(...)` that
   call `rejectIfVcsManaged(...)` and throw `VcsManagedException`; `saveVcs(...)` that
   sets the flag and the domain; `unmanage(...)` that clears it.
4. In `GitOpsSynchronizer.apply()` add the `case <KIND> -> <xService>.saveVcs(...)` and
   the matching `<xService>.unmanage(key)` in `unmanage()`. When the construct carries a
   domain, add it to `stampDomainOn(...)`.
5. In `GitOpsYamlParser.resolveKey()` add the primary-key extraction for the new kind.
6. Optionally add a kind-specific validation invoked from `GitOpsSynchronizer.validate()`.
7. If the kind needs special ordering (like datasets by their `sources`), extend the
   ordering logic in `applyAll()` / `orderDataSetsDependencyWise()`.

### 8.3 Tests and references

Reference tests live in `src/test/java/org/lakehouse/config/vcs/`:
`GitOpsIntegrationTest`, `GitVcsClientTest`, `GitOpsChangeSetBuilderTest`,
`GitOpsSchedulerUnitTest`, `GitOpsYamlParserTest`, and `TestGitRepository` helper.

## 9. Read-only REST endpoints

Consumed by the UI (`lakehouse-ui-svc`), the endpoints return synchronization history:

- `GET /v1_0/configs/vcs/logs` — `VcsSyncLogController` (`VcsSyncLogDTO`): `from`, `to`
  (required), optional `status`, `commitId`, `domainKeyName`. A row carries
  `domainKeyName`; a `SUCCESS` row carries the applied commit id, a `FAILED` row carries
  the error message.
- `GET /v1_0/configs/vcs/objectlogs` — `VcsObjectLogController` (`VcsObjectLogDTO`):
  optional `commitId`, `kind`, `from`, `to`, `filePath`, `objectName`, `domainKeyName`;
  either `commitId` or both `from` and `to` must be provided. Every row carries
  `domainKeyName`.