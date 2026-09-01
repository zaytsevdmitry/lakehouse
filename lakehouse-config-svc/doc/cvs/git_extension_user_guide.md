# Git extension user guide

This guide is for users who manage `lakehouse-config-svc` configuration declaratively
through the built-in Git extension (the CVS subsystem). It explains the repository
format (YAML), the `isCvsManaged` flag, how to configure the synchronization, and which
error messages to expect.

For a developer-level description, see
[CVS subsystem for platform developers](cvs_for_developers.md).

## 1. What the Git extension does

`lakehouse-config-svc` can be configured either through the REST API or **declaratively**
from a Git repository. When the Git extension is enabled, the service periodically:

1. pulls the configured branch;
2. diffs its HEAD against the last successfully applied commit;
3. parses the changed YAML/JSON files;
4. applies the whole commit atomically to the configuration database.

The Git repository is the **source of truth**: any change you commit is automatically
applied, and every application is recorded in the synchronization log.

## 2. How to enable and configure

All settings are under the `lakehouse.config.cvs.*` prefix. Set them in `application.yml`
or via environment variables.

```yaml
lakehouse:
  config:
    cvs:
      git:
        repository-url: ${LAKEHOUSE_CONFIG_GIT_REPOSITORY_URL:}
        branch: ${LAKEHOUSE_CONFIG_GIT_BRANCH:main}
        local-clone-path: ${LAKEHOUSE_CONFIG_GIT_LOCAL_CLONE_PATH:}
        private-key-path: ${LAKEHOUSE_CONFIG_GIT_PRIVATE_KEY_PATH:}
        sync:
          enabled: ${LAKEHOUSE_CONFIG_GIT_SYNC_ENABLED:false}
          interval-ms: ${LAKEHOUSE_CONFIG_GIT_SYNC_INTERVAL_MS:30000}
          initial-delay-ms: ${LAKEHOUSE_CONFIG_GIT_SYNC_INITIAL_DELAY_MS:10000}
```

### Parameters

| Property | Environment variable | Default | Description |
|---|---|---|---|
| `lakehouse.config.cvs.git.repository-url` | `LAKEHOUSE_CONFIG_GIT_REPOSITORY_URL` | *(empty)* | URL of the configuration repository (`git://`, `ssh://`, `http(s)://` or local path). **Required** to enable the sync. |
| `lakehouse.config.cvs.git.branch` | `LAKEHOUSE_CONFIG_GIT_BRANCH` | `main` | Branch to synchronize. |
| `lakehouse.config.cvs.git.local-clone-path` | `LAKEHOUSE_CONFIG_GIT_LOCAL_CLONE_PATH` | *(empty)* | Local directory where the service keeps its clone. **Required** to enable the sync. |
| `lakehouse.config.cvs.git.private-key-path` | `LAKEHOUSE_CONFIG_GIT_PRIVATE_KEY_PATH` | *(empty)* | Path to an SSH private key; only needed for `ssh://` URLs. Leave empty for anonymous access. |
| `lakehouse.config.cvs.git.sync.enabled` | `LAKEHOUSE_CONFIG_GIT_SYNC_ENABLED` | `false` | Set `true` to enable the Git extension. |
| `lakehouse.config.cvs.git.sync.interval-ms` | `LAKEHOUSE_CONFIG_GIT_SYNC_INTERVAL_MS` | `30000` | Period of the synchronization cycle (ms). |
| `lakehouse.config.cvs.git.sync.initial-delay-ms` | `LAKEHOUSE_CONFIG_GIT_SYNC_INITIAL_DELAY_MS` | `10000` | Delay of the first cycle after startup (ms). |

### Example (environment variables)

```bash
LAKEHOUSE_CONFIG_GIT_REPOSITORY_URL=git://git-server:9418/config-repo.git
LAKEHOUSE_CONFIG_GIT_BRANCH=main
LAKEHOUSE_CONFIG_GIT_LOCAL_CLONE_PATH=/tmp/config-repo
LAKEHOUSE_CONFIG_GIT_SYNC_ENABLED=true
LAKEHOUSE_CONFIG_GIT_SYNC_INTERVAL_MS=30000
```

If the service has two mandatory settings (`repository-url`, `local-clone-path`) blank
while `sync.enabled=true`, the client refuses to start with a
`Git repository URL must be configured` / `Git local clone path must be configured`
error.

## 3. Repository layout

A configuration repository is a **flat set of files, one configuration construct per
file**. Only `*.yaml`, `*.yml` and `*.json` files are treated as configuration; all other
files (e.g. `load.sh`) are ignored. File names starting with `.` (dotfiles) are ignored
as well.

Example layout:

```
config-repo/
├── nameSpaces/demo.yaml
├── drivers/postgres.yaml
├── datasources/processingdb.yaml
├── datasets/1_transaction_dds.yaml
├── sql-scripts/dataset-sql-model/transaction_dds.yaml
├── tasks/prepare-jdbc.yaml
├── schedules/regular.yaml
└── quality/metrics/transaction_dds_qm.yaml
```

Every configuration file starts with a `kind` field that selects the target construct
type. The rest of the file is bound to that type.

### Supported kinds

Applied in the order below; when you delete files, the reverse order is used.

| YAML `kind` | Example path | Primary key |
|---|---|---|
| `NameSpace` | `nameSpaces/demo.yaml` | `keyName` |
| `Driver` | `drivers/postgres.yaml` | `keyName` |
| `DataSource` | `datasources/processingdb.yaml` | `keyName` |
| `Script` | `sql-scripts/dq/non_zero_count.yaml` | `key` |
| `TaskExecutionServiceGroup` | `taskexecutionservicegroups/database.yaml` | `name` |
| `Task` | `tasks/prepare-jdbc.yaml` | `name` |
| `DataSet` | `datasets/1_transaction_dds.yaml` | `keyName` |
| `ScenarioActTemplate` | `scenarios/spark-dq.yaml` | `keyName` |
| `QualityMetricsConf` | `quality/metrics/transaction_dds_qm.yaml` | `keyName` |
| `Schedule` | `schedules/regular.yaml` | `keyName` |

## 4. YAML format

### Rules

- The `kind` field is **mandatory** and must be the first logical entry.
- `kind` matching is case-insensitive and tolerant of dashes/underscores/spaces:
  `DataSet`, `dataset` and `data-set` are the same kind.
- Enum values are case-insensitive (e.g. `postgresql` is the same as `POSTGRESQL`).
- Unknown properties are a **hard error**: the whole commit is rejected. Keep the file
  in sync with the DTO fields described by the REST API / Swagger.
- Datasets may reference other datasets in `sources`; the service applies datasets in
  dependency order, so source datasets are applied after their dependencies.

### DataSource example

```yaml
kind: DataSource
keyName: processingdb
description: Remote datastore processingdb
dataSourceType: database
databaseProtocol: postgresql
service:
  host: "172.20.193.10"
  port: "5432"
  urn: postgresDB
  properties:
    user: postgresUser
    fetchSize: "10000"
```

### NameSpace example

```yaml
kind: NameSpace
keyName: demo
description: Demo namespace
```

### Script example

Scripts store a global `key` (dots replace the directory path) and the script body as a
literal block `value`:

```yaml
kind: Script
key: dq.non_zero_count.sql
value: |
  select count(1) value
  from {{ refCat(targetDataSetKeyName) }}
```

### DataSet example

```yaml
kind: DataSet
keyName: transaction_dds
nameSpaceKeyName: DEMO
dataSourceKeyName: lakehousestorage
databaseSchemaName: default
tableName: transaction_dds
scripts:
  - key: dataset-sql-model.transaction_dds.sql
sources:
  client_processing:
    properties:
      fetchSize: "10000"
  transaction_processing:
    properties:
      fetchSize: "10000"
columnSchema:
  - name: id
    description: tx id
    dataType: bigint
    nullable: false
    order: 0
constraints:
  transaction_dds_pk:
    type: primary
    columns: id
    constraintLevelCheck: dataQuality
```

## 5. The `isCvsManaged` flag

Every construct applied from the Git repository is stored with `isCvsManaged = true`.
This flag distinguishes the source of the construct:

- constructs created through the REST API have `isCvsManaged = false`;
- constructs applied from Git have `isCvsManaged = true`.

### Consequences

- **REST API protection.** Any attempt to create, update or delete a CVS-managed
  construct through the REST API fails with HTTP `409 Conflict`. You must first change it
  in the repository and let the sync pick it up.
- **Deletion is two-step.** Deleting a YAML file from the repository does **not** delete
  the construct from the database. The sync only clears `isCvsManaged` on the
  corresponding entity. The user must then delete the construct through the REST API.
- **Taking ownership back.** Once the flag is cleared, the construct is again fully
  manageable via the REST API.

The flag is never read from YAML; it is derived at runtime by the synchronization
process.

## 6. Synchronization semantics

- **Atomicity.** A commit is applied inside a single transaction: all created/updated
  constructs, all deletions, object log entries, and the `SUCCESS` marker. On any error
  the whole commit is rolled back.
- **Idempotency.** Commits whose id already has a `cvs_sync_log` row are skipped. If the
  last `SUCCESS` already points to HEAD, nothing is done.
- **First sync.** When the database has no successful commit yet, the whole repository
  HEAD is treated as a set of created files.
- **Renames.** A file rename is treated as delete + create, because constructs are
  identified by their content (primary key), not by the file name.
- **Failure handling.** A commit that fails to parse, validate or bind is recorded as
  `FAILED` with the error message and is **not retried**. A later fixing commit rolls the
  corrected content in as part of a new diff.
- **Infrastructure errors** (repository unreachable, missing clone, SSH failure) are only
  logged and retried on the next cycle.
- **Progress is visible** in the UI panel "CVS → CVSLog": `SUCCESS` rows store the
  applied commit id, `FAILED` rows store the error message; the object log lists every
  touched object per commit.

## 7. Checking the synchronization state

Read-only REST endpoints (also shown in the UI):

- `GET /v1_0/configs/cvs/logs?from=...&to=...&status=...&commitId=...`
  Returns the sync-log history (`id`, `commitId`, `syncDateTime`, `status`, `errorMessage`).
  `from` and `to` are required; `status` (`SUCCESS`/`FAILED`) and `commitId` are optional.
- `GET /v1_0/configs/cvs/objectlogs?commitId=...&kind=...&from=...&to=...&filePath=...&objectName=...`
  Returns the per-object log (`id`, `dateTimeRec`, `objectName`, `kind`, `filePath`,
  `commitId`). Either `commitId` or both `from` and `to` must be supplied.

## 8. Error messages

### Configuration errors (logged as `FAILED` with the message in `cvs_sync_log`)

| Message | Meaning |
|---|---|
| `Missing required field 'kind'` | The YAML file has no `kind` field. |
| `YAML document is empty` | The file is empty or blank. |
| `YAML document cannot be parsed as a configuration map` | Invalid YAML syntax or the root is not a mapping. |
| `Configuration kind must not be blank` | `kind` is present but empty. |
| `Unknown configuration kind: <value>` | The `kind` value does not match any supported construct. |
| `Cannot bind YAML document to <kind>` | The remaining fields cannot be bound to the DTO, e.g. an unknown property or an invalid value. |

Because unknown properties fail binding, most validation problems surface as
`Cannot bind YAML document to <kind>`. The underlying Jackson message is included as the
cause.

### Infrastructure errors (logged, retried on the next cycle)

| Message | Meaning |
|---|---|
| `Git repository URL must be configured` | `repository-url` is blank while the sync is enabled. |
| `Git local clone path must be configured` | `local-clone-path` is blank while the sync is enabled. |
| `Cannot init CVS client for repository <url>` | Clone/open or SSH setup failed. |
| `Cannot pull repository <url>` | Fetch or reset failed. |
| `No reachable commit on branch <branch>` | The branch has no commits. |
| `Cannot resolve current commit on branch <branch>` | Reading the branch ref failed. |
| `Cannot compute diff against <baseCommitId>` | Diff computation failed. |
| `Cannot read file <path> at commit <commitId>` | Reading a configuration file blob failed. |
| `CVS client is not initialized; call init() first` | A method was called before `init()`. |
| `SSH private key is not readable: <key>` | The configured private key does not exist or cannot be read. |
| `Cannot create SSH session factory for key <key>` | SSH setup failed for the key. |

### REST API errors

| HTTP status | Message pattern | Meaning |
|---|---|---|
| `409 Conflict` | `Configuration construct '<keyName>' is managed via CVS (git) and cannot be <created or updated|deleted> through the REST API. Remove it from the configuration repository (git) first.` | A user tries to modify or delete a CVS-managed construct via REST. |
| `400 Bad Request` | `Either commitId or both from and to must be provided` | The object-log query supplied neither a `commitId` nor the `from`+`to` pair. |