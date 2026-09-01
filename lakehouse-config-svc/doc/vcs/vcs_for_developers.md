# VCS subsystem for platform developers

This document describes how the VCS (Configuration Versioning System) subsystem works,
how to extend it, and which parameters a developer needs to know about. It is aimed at
developers of the lakehouse platform. If you only manage configuration files in a Git
repository, see the [Git extension user guide](git_extension_user_guide.md) instead.

## 1. What VCS is

VCS is the configuration-as-code (GitOps) subsystem of `lakehouse-config-svc`. It
treats a Git repository as the **source of truth** for configuration metadata: the same
configuration DTOs that the REST API accepts can be written as Kubernetes-style YAML
files, and the subsystem periodically synchronizes them into the configuration database.

In contrast to the REST API, VCS gives:

- full history of every configuration change (Git commits);
- declarative, reviewable configuration;
- atomic application of a whole commit;
- automatic protection of VCS-managed constructs from accidental REST edits (see the
  `isVcsManaged` flag).

All VCS-specific code lives in the package `org.lakehouse.config.vcs` (plus the two
read-only controllers `VcsSyncLogController`/`VcsObjectLogController` in
`org.lakehouse.config.controller` and `VcsManagedException` in
`org.lakehouse.config.exception`).

## 2. How the sync works

```
┌──────────────────┐   fetch + diff   ┌────────────────────────────────┐
│  Git repository  │ ───────────────▶ │  GitOpsScheduler (poll)        │
│  (tracked branch)│                  │  ├─ pull()                     │
└──────────────────┘                  │  ├─ getCurrentCommitId()       │
                                      │  ├─ build diff vs last success │
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
                                      │  validate → apply → unmanage   │
                                      │  → record object logs           │
                                      │  → mark SUCCESS/FAILED          │
                                      └───────────────┬────────────────┘
                                                      ▼
                                      ┌────────────────────────────────┐
                                      │  PostgreSQL                     │
                                      │  vcs_sync_log + vcs_object_log  │
                                      └────────────────────────────────┘
```

The orchestrator is `GitOpsScheduler` (`org.lakehouse.config.vcs.component`). It is
registered only when `lakehouse.config.vcs.git.sync.enabled=true` and is driven by
`@Scheduled` with `fixedDelayString` / `initialDelayString` from the same property block.
The method `sync()` is `synchronized` and idempotent and can also be called directly,
for example from integration tests.

Per cycle:

1. Lazily `init()` the `VcsClient`.
2. `pull()` the tracked branch (fetch + hard reset to the remote branch ref).
3. Resolve the current commit id (HEAD).
4. Skip if the commit already has a row in `vcs_sync_log` (`existsByCommitId`) or if the
   last `SUCCESS` row already points to HEAD.
5. Compute the diff between the last successfully applied commit and HEAD. On an empty
   database the whole head is treated as a set of created files.
6. `GitOpsChangeSetBuilder` keeps only configuration files (extension `.yaml`, `.yml`,
   `.json`, name not starting with `.`), parses created/updated files from HEAD and
   deleted files from the last successful commit.
7. `GitOpsSynchronizer.sync(changeSet, head)` validates and applies everything in a
   **single transaction**. Exceptions become a `FAILED` sync-log row (recorded in a
   separate `REQUIRES_NEW` transaction so the failure survives the rollback).
8. Infrastructure failures (`VcsClientException`: repository unreachable, SSH failure,
   unreadable clone) are only logged and retried on the next cycle.

### Transaction and ordering rules

`GitOpsSynchronizer` (`org.lakehouse.config.vcs.service.GitOpsSynchronizer`):

- `applyAll` applies created/updated constructs in `ConfigKind.order()` order;
  datasets are additionally ordered by their `sources` dependencies
  (`orderDataSetsDependencyWise`, cyclic references fall back to declared order).
- `unmanageAll` clears the `isVcsManaged` flag of deleted constructs in **reverse**
  dependency order.
- After applying, every touched construct is recorded in `vcs_object_log`
  (`date_time_rec`, `object_name` = primary key, `kind`, `file_path`, `commit_id`).
- Only when the whole commit succeeded is the `SUCCESS` row written to `vcs_sync_log`.

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
and is built on **JGit**.

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
to a DTO:

- the file must start with a `kind` field (Kubernetes style);
- the `kind` value selects the target DTO class (`ConfigKind` enum);
- `kind` matching is case-insensitive and tolerant of dashes/underscores/spaces, so
  `DataSet`, `dataset` and `data-set` are all accepted;
- enum fields are deserialized case-insensitively (e.g. `postgresql` == `POSTGRESQL`);
- unknown properties are a **hard error** to keep the declarative description strict.

`ConfigKind` (`org.lakehouse.config.vcs.yaml.ConfigKind`) defines the recognized kinds
with their YAML value, DTO class and dependency `order`:

| ConfigKind | YAML `kind` | DTO | order |
|---|---|---|---|
| `NAME_SPACE` | `NameSpace` | `NameSpaceDTO` | 1 |
| `DRIVER` | `Driver` | `DriverDTO` | 2 |
| `DATA_SOURCE` | `DataSource` | `DataSourceDTO` | 3 |
| `SCRIPT` | `Script` | `ScriptContent` | 4 |
| `TASK_EXECUTION_SERVICE_GROUP` | `TaskExecutionServiceGroup` | `TaskExecutionServiceGroupDTO` | 5 |
| `TASK` | `Task` | `TaskDTO` | 6 |
| `DATA_SET` | `DataSet` | `DataSetDTO` | 7 |
| `SCENARIO_ACT_TEMPLATE` | `ScenarioActTemplate` | `ScenarioActTemplateDTO` | 8 |
| `QUALITY_METRICS_CONF` | `QualityMetricsConf` | `QualityMetricsConfDTO` | 9 |
| `SCHEDULE` | `Schedule` | `ScheduleDTO` | 10 |

The primary key per kind is extracted by `GitOpsYamlParser.resolveKey()` (e.g.
`keyName`, `name` or `key`). `ScriptContent` is a special record `(String key, String
value)` for scripts whose content is an inline literal.

## 5. The `isVcsManaged` management contract

Every configuration concrete entity carries a boolean `isVcsManaged`
(`@Column(nullable=false)`, default `false`). It marks constructs owned by VCS:

`NameSpace`, `Schedule`, `TaskExecutionServiceGroup`, `SQLTemplate`, `Script`, `Task`,
`DataSet`, `TemplateScenarioAct`, `Driver`, `DataSource`, `QualityMetricsConf`.

Each entity service implements a **three-way contract** (see `NameSpaceService`,
`ScriptService`, `TaskService`, `DataSetService`, `DriverService`,
`DataSourceService`, `ScheduleService`, `ScenarioActTemplateService`,
`TaskExecutionServiceGroupService`, `QualityMetricsConfService`):

1. User-facing `save(...)` / `deleteById(...)` call `rejectIfVcsManaged(key, operation)`
   and throw `VcsManagedException` (HTTP `409 Conflict`) when the construct is managed.
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

All VCS parameters live under the `lakehouse.config.vcs.*` prefix and are bound by
`GitVcsConfigurationProperties` (`org.lakehouse.config.vcs.configuration`). They are
wired into the `GitVcsClient` bean by `GitVcsConfiguration` (guarded by
`@ConditionalOnProperty(lakehouse.config.vcs.git.sync.enabled=true)`).

| Property | Environment variable | Default | Meaning |
|---|---|---|---|
| `lakehouse.config.vcs.git.repository-url` | `LAKEHOUSE_CONFIG_GIT_REPOSITORY_URL` | *(empty)* | Remote repository URL (`git://`, `ssh://`, `http(s)://`, local) |
| `lakehouse.config.vcs.git.branch` | `LAKEHOUSE_CONFIG_GIT_BRANCH` | `main` | Branch to track |
| `lakehouse.config.vcs.git.local-clone-path` | `LAKEHOUSE_CONFIG_GIT_LOCAL_CLONE_PATH` | *(empty)* | Local directory owned by the service for its clone |
| `lakehouse.config.vcs.git.private-key-path` | `LAKEHOUSE_CONFIG_GIT_PRIVATE_KEY_PATH` | *(empty)* | SSH private key path, used only for `ssh://` |
| `lakehouse.config.vcs.git.sync.enabled` | `LAKEHOUSE_CONFIG_GIT_SYNC_ENABLED` | `false` | Master switch of the VCS scheduler |
| `lakehouse.config.vcs.git.sync.interval-ms` | `LAKEHOUSE_CONFIG_GIT_SYNC_INTERVAL_MS` | `30000` | Scheduler fixed delay between cycles |
| `lakehouse.config.vcs.git.sync.initial-delay-ms` | `LAKEHOUSE_CONFIG_GIT_SYNC_INITIAL_DELAY_MS` | `10000` | Delay of the first cycle after startup |

Reference wiring lives in `src/main/resources/application.yml`; the demo stack declares
these values in `demo/k8s`/`demo/compose` (a lightweight `git-server` serving
`git://git-server:9418/config-repo.git`, branch `main`, `sync.enabled=true`).

## 7. Calling the scheduler manually

`GitOpsScheduler.sync()` is public and `synchronized`, so it can be invoked directly
(e.g. by integration tests or an admin trigger). It:

- skips commits that already have a `vcs_sync_log` row;
- records configuration/validation errors as `FAILED` (so the offending commit is not
  retried forever);
- leaves infrastructure `VcsClientException` errors to be retried on the next cycle.

## 8. How to extend the VCS abstraction

### 8.1 Add a new VCS backend

Implement the five `VcsClient` methods (`init`, `pull`, `getCurrentCommitId`,
`getDiff`, `readFileContent`), reuse `VcsDiffEntry`/`VcsChangeType`, and throw
`VcsClientException` for transient infrastructure failures. Optionally expose a
`VcsClient` `@Bean` in a `@Configuration` guarded by `@ConditionalOnProperty`, exactly
like `GitVcsConfiguration`. The rest of the pipeline (change-set builder,
synchronizer, scheduler, persistence) is transport-agnostic.

### 8.2 Add a new configuration construct kind

1. Add an entry to the `ConfigKind` enum with a YAML value, the target DTO class and a
   dependency `order`.
2. Make sure the corresponding entity has an `isVcsManaged` boolean field with getter and
   setter.
3. Implement the three-way contract in the service: `save(...)`/`deleteById(...)` that
   call `rejectIfVcsManaged(...)` and throw `VcsManagedException`; `saveVcs(...)` that
   sets the flag; `unmanage(...)` that clears it.
4. In `GitOpsSynchronizer` add `case <KIND> -> <xService>.saveVcs(...)` in `apply()` and
   the matching `<xService>.unmanage(key)` in `unmanage()`.
5. In `GitOpsYamlParser.resolveKey()` add the primary-key extraction for the new kind.
6. Optionally add a `ConfigKind`-specific validation invoked from
   `GitOpsSynchronizer.validate()`.
7. If the kind needs special ordering (like datasets by their `sources`), extend the
   ordering logic in `applyAll()` / `orderDataSetsDependencyWise()`.

### 8.3 Tests and references

Reference tests live in `src/test/java/org/lakehouse/config/vcs/`:
`GitOpsIntegrationTest`, `GitVcsClientTest`, `GitOpsChangeSetBuilderTest`,
`GitOpsSchedulerUnitTest`, `GitOpsYamlParserTest`, and `TestGitRepository` helper.

## 9. Read-only REST endpoints

Consumed by the UI (`lakehouse-ui-svc`), the endpoints return synchronization history:

- `GET /v1_0/configs/vcs/logs` — `VcsSyncLogController` (`VcsSyncLogDTO`): `from`, `to`
  (required), optional `status`, `commitId`. A `SUCCESS` row carries the applied commit
  id, a `FAILED` row carries the error message.
- `GET /v1_0/configs/vcs/objectlogs` — `VcsObjectLogController` (`VcsObjectLogDTO`):
  optional `commitId`, `kind`, `from`, `to`, `filePath`, `objectName`; either `commitId`
  or both `from` and `to` must be provided.