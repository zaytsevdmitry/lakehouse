# Git extension user guide

This guide is for users who manage `lakehouse-config-svc` configuration declaratively
through the built-in Git extension (the VCS subsystem). It explains the domain model and
repository format (YAML), the `isVcsManaged` flag, how to configure the synchronization,
and which error messages to expect.

For a developer-level description, see
[VCS subsystem for platform developers](vcs_for_developers.md).

## 1. What the Git extension does

`lakehouse-config-svc` can be configured either through the REST API or **declaratively**
from Git repositories. Configuration belongs to **domains**; every domain owns its own
repository. When the Git extension is enabled, the service periodically, for every domain:

1. pulls the configured branch of the domain repository;
2. diffs its HEAD against the last successfully applied commit of that domain;
3. parses the changed YAML/JSON files;
4. applies the whole commit atomically to the configuration database.

The Git repository of a domain is the **source of truth**: any change you commit there is
automatically applied, and every application is recorded in the synchronization log under
that domain.

## 2. How to enable and configure

All settings are under the `lakehouse.config.vcs.*` prefix. Set them in `application.yml`
or via environment variables. The domain list and names are **user-defined**: declare every
domain that owns a repository (here shown generically; nesting is optional):

```yaml
lakehouse:
  config:
    vcs:
      git:
        sync:
          enabled: ${LAKEHOUSE_CONFIG_GIT_SYNC_ENABLED:false}
          interval-ms: ${LAKEHOUSE_CONFIG_GIT_SYNC_INTERVAL_MS:30000}
          initial-delay-ms: ${LAKEHOUSE_CONFIG_GIT_SYNC_INITIAL_DELAY_MS:10000}
      domains:
        <domain>:
          priority: 0
          git:
            repository-url: ${LAKEHOUSE_CONFIG_GIT_<DOMAIN>_URL:}
            branch: ${LAKEHOUSE_CONFIG_GIT_<DOMAIN>_BRANCH:main}
            local-clone-path: ${LAKEHOUSE_CONFIG_GIT_<DOMAIN>_CLONE_PATH:}
            private-key-path: ${LAKEHOUSE_CONFIG_GIT_<DOMAIN>_PRIVATE_KEY_PATH:}
          domains:
            <nested-domain>:
              priority: 1
              git:
                repository-url: ${LAKEHOUSE_CONFIG_GIT_<NESTED_DOMAIN>_URL:}
                branch: ${LAKEHOUSE_CONFIG_GIT_<NESTED_DOMAIN>_BRANCH:main}
                local-clone-path: ${LAKEHOUSE_CONFIG_GIT_<NESTED_DOMAIN>_CLONE_PATH:}
                private-key-path: ${LAKEHOUSE_CONFIG_GIT_<NESTED_DOMAIN>_PRIVATE_KEY_PATH:}
```

### Parameters

| Property | Environment variable | Default | Description |
|---|---|---|---|
| `lakehouse.config.vcs.git.sync.enabled` | `LAKEHOUSE_CONFIG_GIT_SYNC_ENABLED` | `false` | Set `true` to enable the Git extension. |
| `lakehouse.config.vcs.git.sync.interval-ms` | `LAKEHOUSE_CONFIG_GIT_SYNC_INTERVAL_MS` | `30000` | Period of the synchronization cycle (ms). |
| `lakehouse.config.vcs.git.sync.initial-delay-ms` | `LAKEHOUSE_CONFIG_GIT_SYNC_INITIAL_DELAY_MS` | `10000` | Delay of the first cycle after startup (ms). |
| `lakehouse.config.vcs.domains.<name>.git.repository-url` | `LAKEHOUSE_CONFIG_GIT_<NAME>_URL` | *(empty)* | URL of the domain repository (`git://`, `ssh://`, `http(s)://` or local path). **Required** for the domain to be synchronized. |
| `lakehouse.config.vcs.domains.<name>.git.branch` | `LAKEHOUSE_CONFIG_GIT_<NAME>_BRANCH` | `main` | Branch to synchronize. |
| `lakehouse.config.vcs.domains.<name>.git.local-clone-path` | `LAKEHOUSE_CONFIG_GIT_<NAME>_CLONE_PATH` | *(empty)* | Local directory where the service keeps the domain clone. |
| `lakehouse.config.vcs.domains.<name>.git.private-key-path` | `LAKEHOUSE_CONFIG_GIT_<NAME>_PRIVATE_KEY_PATH` | *(empty)* | Path to an SSH private key; only needed for `ssh://` URLs. Leave empty for anonymous access. |
| `lakehouse.config.vcs.domains.<name>.priority` | - | lowest | Apply order of the domain: parents before nested domains, same-level domains ascending by this value. |
| `lakehouse.config.vcs.domains.<name>.domains` | - | - | Nested sub-domains. |

The environment variable name embeds the **upper-cased** domain name: for the domain
`<domain>` they are `LAKEHOUSE_CONFIG_GIT_<DOMAIN>_URL`, `LAKEHOUSE_CONFIG_GIT_<DOMAIN>_BRANCH`,
`LAKEHOUSE_CONFIG_GIT_<DOMAIN>_CLONE_PATH` and `LAKEHOUSE_CONFIG_GIT_<DOMAIN>_PRIVATE_KEY_PATH`.

### Example (environment variables)

```bash
LAKEHOUSE_CONFIG_GIT_SYNC_ENABLED=true
LAKEHOUSE_CONFIG_GIT_MYDATA_URL=git://git-server:9418/mydata.git
LAKEHOUSE_CONFIG_GIT_MYDATA_CLONE_PATH=/tmp/config-mydata
LAKEHOUSE_CONFIG_GIT_SYNC_INTERVAL_MS=30000
```

A domain whose `repository-url` is blank is simply skipped (a warning is logged).

## 3. Domains and repository layout

Configuration is organized per **domain**. Each domain has its own repository whose files
are a **flat set, one configuration construct per file**. Only `*.yaml`, `*.yml` and
`*.json` files are treated as configuration; all other files (e.g. `load.sh`) are
ignored. File names starting with `.` (dotfiles) are ignored as well.

Every construct loaded from a domain repository is stored with `domainKeyName` = the
domain name, so the domain that owns a construct is known on the row. The key itself does
not change: `keyName` is the primary key and is global, so it is never reused by another
domain - two domains pointing at the same table use two distinct `keyName`s. Scripts are the
exception: they have no domain at all, being content-only and shared by key.

Example layout (domain names are arbitrary; the demo stack described in the readme uses
`platform`, `processing` and `analytics`):

```
domains/platform/
├── datasources/processingdb.yaml
├── drivers/postgres.yaml
├── sql-scripts/dq/non_zero_count.yaml
├── scenarios/spark.yaml
└── tasks/prepare-jdbc.yaml

domains/analytics/
├── datasets/transaction_dds.yaml
├── quality/metrics/transaction_dds_qm.yaml
└── schedules/regular.yaml

domains/processing/
├── datasets/client_processing.yaml
└── schedules/generateSource.yaml
```

Every configuration file starts with a `kind` field that selects the target construct
type. The rest of the file is bound to that type.

### Supported kinds

Applied in the order below; when you delete files, the reverse order is used.

| YAML `kind` | Example path | Primary key |
|---|---|---|
| `Driver` | `drivers/postgres.yaml` | `keyName` |
| `DataSource` | `datasources/processingdb.yaml` | `keyName` |
| `Script` | `sql-scripts/dq/non_zero_count.yaml` | `key` |
| `TaskExecutionServiceGroup` | `taskexecutionservicegroups/database.yaml` | `name` |
| `Task` | `tasks/prepare-jdbc.yaml` | `name` |
| `DataSet` | `datasets/1_transaction_dds.yaml` | `keyName` |
| `ScenarioActTemplate` | `scenarios/spark-dq.yaml` | `keyName` |
| `QualityMetricsConf` | `quality/metrics/transaction_dds_qm.yaml` | `keyName` |
| `Schedule` | `schedules/regular.yaml` | `keyName` |

The kinds `ERDiagram` and `DataLineageDiagram` are stored in the repository but are not
applied by the configuration service.

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
- The domain (`domainKeyName`) is **never** written into the YAML: it is derived at sync
  time from the repository that owns the file.

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

## 5. The `isVcsManaged` flag

Every construct applied from the Git repository is stored with `isVcsManaged = true`.
This flag distinguishes the source of the construct:

- constructs created through the REST API have `isVcsManaged = false`;
- constructs applied from Git have `isVcsManaged = true`.

### Consequences

- **REST API protection.** Any attempt to create, update or delete a VCS-managed
  construct through the REST API fails with HTTP `409 Conflict`. You must first change it
  in the repository and let the sync pick it up.
- **Deletion is two-step.** Deleting a YAML file from the repository does **not** delete
  the construct from the database. The sync only clears `isVcsManaged` on the
  corresponding entity. The user must then delete the construct through the REST API.
- **Taking ownership back.** Once the flag is cleared, the construct is again fully
  manageable via the REST API.
- **Domain ownership.** Constructs are additionally scoped by `domainKeyName`. The REST
  API rejects with `409 Conflict` (`DomainConflictException`) any change that would move
  a construct outside its domain (e.g. rebinding a data set of one domain to a data
  source of another).

The flag is never read from YAML; it is derived at runtime by the synchronization
process.

## 6. Synchronization semantics

- **Per domain.** Each domain repository is synchronized independently, in priority order
  (parents before nested domains). The synchronization log rows carry the domain name.
- **Atomicity.** A commit is applied inside a single transaction: all created/updated
  constructs, all deletions, object log entries, and the `SUCCESS` marker. On any error
  the whole commit is rolled back.
- **Idempotency.** Commits whose `(commit_id, domain)` pair already has a `vcs_sync_log`
  row are skipped. If the last `SUCCESS` of the domain already points to HEAD, nothing is
  done.
- **First sync.** When the database has no successful commit for a domain yet, the whole
  HEAD of that domain is treated as a set of created files.
- **Renames.** A file rename is treated as delete + create, because constructs are
  identified by their content (primary key), not by the file name.
- **Cross-domain validation.** A `Schedule` may reference only data sets of its own
  domain (`domainKeyName` equal) or domain-less data sets. Otherwise the commit fails
  with `DataSetDomainConflictException` and is recorded as `FAILED`.
- **Failure handling.** A commit that fails to parse, validate or bind is recorded as
  `FAILED` (with the domain and the error message) and is **not retried**. A later fixing
  commit rolls the corrected content in as part of a new diff.
- **Infrastructure errors** (repository unreachable, missing clone, SSH failure) are only
  logged and retried on the next cycle.
- **Progress is visible** in the UI panel "VCS → VCSLog": `SUCCESS` rows store the
  applied commit id, `FAILED` rows store the error message; the object log lists every
  touched object per commit; both track the domain.

## 7. Checking the synchronization state

Read-only REST endpoints (also shown in the UI):

- `GET /v1_0/configs/vcs/logs?from=...&to=...&status=...&commitId=...&domainKeyName=...`
  Returns the sync-log history (`id`, `commitId`, `syncDateTime`, `status`, `domainKeyName`,
  `errorMessage`). `from` and `to` are required; `status` (`SUCCESS`/`FAILED`), `commitId`
  and `domainKeyName` are optional.
- `GET /v1_0/configs/vcs/objectlogs?commitId=...&kind=...&from=...&to=...&filePath=...&objectName=...&domainKeyName=...`
  Returns the per-object log (`id`, `dateTimeRec`, `objectName`, `kind`, `filePath`,
  `commitId`, `domainKeyName`). Either `commitId` or both `from` and `to` must be supplied;
  `domainKeyName` filters the entries of a single domain.

## 8. Error messages

### Configuration errors (logged as `FAILED` with the message in `vcs_sync_log`)

| Message | Meaning |
|---|---|
| `Missing required field 'kind'` | The YAML file has no `kind` field. |
| `YAML document is empty` | The file is empty or blank. |
| `YAML document cannot be parsed as a configuration map` | Invalid YAML syntax or the root is not a mapping. |
| `Configuration kind must not be blank` | `kind` is present but empty. |
| `Unknown configuration kind: <value>` | The `kind` value does not match any supported construct. |
| `Cannot bind YAML document to <kind>` | The remaining fields cannot be bound to the DTO, e.g. an unknown property or an invalid value. |
| `Schedule '<schedule>' refers to data set '<dataset>' of domain '<other>' instead of '<domain>'` | A schedule references a data set owned by another domain (`DataSetDomainConflictException`). |

Because unknown properties fail binding, most validation problems surface as
`Cannot bind YAML document to <kind>`. The underlying Jackson message is included as the
cause.

### Infrastructure errors (logged, retried on the next cycle)

| Message | Meaning |
|---|---|
| `Cannot init VCS client for repository <url>` | Clone/open or SSH setup failed. |
| `Cannot pull repository <url>` | Fetch or reset failed. |
| `No reachable commit on branch <branch>` | The branch has no commits. |
| `Cannot resolve current commit on branch <branch>` | Reading the branch ref failed. |
| `Cannot compute diff against <baseCommitId>` | Diff computation failed. |
| `Cannot read file <path> at commit <commitId>` | Reading a configuration file blob failed. |
| `VCS client is not initialized; call init() first` | A method was called before `init()`. |
| `SSH private key is not readable: <key>` | The configured private key does not exist or cannot be read. |
| `Cannot create SSH session factory for key <key>` | SSH setup failed for the key. |

### REST API errors

| HTTP status | Message pattern | Meaning |
|---|---|---|
| `409 Conflict` | `Configuration construct '<keyName>' is managed via VCS (git) and cannot be <created or updated|deleted> through the REST API. Remove it from the configuration repository (git) first.` | A user tries to modify or delete a VCS-managed construct via REST (`VcsManagedException`). |
| `409 Conflict` | `Configuration construct '<keyName>' belongs to domain '<domain>' and cannot be <reason> through the REST API.` | A request would move a construct outside its domain (`DomainConflictException`). |
| `400 Bad Request` | `Either commitId or both from and to must be provided` | The object-log query supplied neither a `commitId` nor the `from`+`to` pair. |