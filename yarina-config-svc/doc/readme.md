# yarina-config-svc

Metadata management service - a single storage for all yarina configurations. It is the source of truth for metadata on the basis of which the other services (scheduler-svc, task-executor-svc, state-svc) perform data processing.

## Overview

`yarina-config-svc` stores and serves yarina metadata:

- **Domains** - hierarchy of configuration environments; every domain is backed by its own Git repository and stamped onto all the objects loaded from it as `domainKeyName` (see [Domains](#domains))
- **Drivers** - connection settings for compute clusters
- **Data sources** - connections to external storages (JDBC/Spark)
- **Datasets** - table, column and constraint descriptions
- **Schedules** - data processing periodicity (intervals, scenario acts, tasks)
- **Data quality metrics** - DQ checks
- **Scripts and SQL templates** - query templates with Jinjava substitutions
- **Data lineage** - data provenance relationships
- **TaskExecutionServiceGroups** - task executor groups
- **Declarative configuration from Git (GitOps/VCS)** - the same configuration DTOs can be defined as YAML files in a Git repository and synchronized into the database automatically by the VCS subsystem (see [GitOps: declarative configuration from a Git repository](#gitops-declarative-configuration-from-git-repositories-vcs))

Configurations are defined as DTOs, stored in PostgreSQL and exposed via REST API. Configuration changes are published to Kafka (topic `configuration_changes`) as `ConfigurationChangeDTO` (kind/keyName/action/object), so that scheduler-svc builds actual schedule instances.

## Architecture

```
┌───────────────────────┐     REST (CRUD)      ┌───────────────────────────┐
│  Admin / UI / CLI     │ ────────────────────▶│   yarina-config-svc    │
└───────────────────────┘                      │   (REST API /v1_0/configs)│
                                               │                           │
┌───────────────────────┐     REST (read)      │  ┌─────────────────────┐  │
│  scheduler-svc        │ ────────────────────▶│  │ ConfigService       │  │
│  task-executor-svc    │                      │  │ (CRUD + merge DTO)  │  │
│  state-svc            │                      │  └─────────────────────┘  │
└───────────────────────┘                      │           │               │
                                               │           ▼               │
                                               │  ┌─────────────────────┐  │
                                               │  │ PostgreSQL          │  │
                                               │  │ (schema yarina_  │  │
                                               │  │       config)       │  │
                                               │  └─────────────────────┘  │
                                               │           │               │
                                               │           ▼  Kafka        │
                                               │  InternalScheduler        │
                                               │  schedule_effective_      │
                                               │  changes                  │
                                               └───────────────────────────┘
```

- **Controller** - REST CRUD endpoints for each metadata type plus compound endpoints for derived objects.
- **Service** - business logic: validation, DTO/entity mapping, merging of template and concrete configurations via `DtoMergeUtils`.
- **Repository (JPA/Hibernate)** - persistence in PostgreSQL.
- **InternalScheduler** - periodic publishing of schedule changes to Kafka.
- Metadata is organized hierarchically (domain → datasource → dataset → ...); the domain hierarchy is defined in `yarina.domains.*` by the shared `DomainSettings` of `yarina-domain-api` (see [Domains](#domains)) and the dependency scheme is described in [content_configuration](content_configuration/content_configuration.md).

## GitOps: declarative configuration from Git repositories (VCS)

In addition to the REST API, `yarina-config-svc` can manage configuration declaratively: the same metadata DTOs are written as YAML files into Git repositories and the VCS (Configuration Versioning System) subsystem synchronizes them into the database on a schedule. The repositories become the source of truth and keep the full history of every configuration change (GitOps style).

Configuration is organized into **domains**. Every domain owns its own Git repository and is synchronized independently. A domain may contain **nested domains** (e.g. `platform` ⊃ `processing` ⊃ `analytics`); domains are applied in the priority order (ascending, parents before children). Every construct loaded from a domain repository is stamped with the domain name (`domainKeyName`), which scopes it and makes cross-domain references verifiable.

The domain model itself (tree, ordering, ownership, isolation rules, the
`yarina.domains.*` namespace and per-domain credentials) is **owned by
`yarina-domain-api`** and described canonically in
[yarina-domain-api/doc/readme.md](../../yarina-domain-api/doc/readme.md); this readme only
summarizes it, and [content_configuration/domains.md](content_configuration/domains.md)
describes the reference of the configuration objects themselves.

Detailed documentation: [VCS subsystem for platform developers](vcs/vcs_for_developers.md) (internals, the `VcsClient` abstraction, extension points, developer parameters) and [Git extension user guide](vcs/git_extension_user_guide.md) (YAML format, `isVcsManaged`, configuration and error messages).

```
┌──────────────────────┐   fetch + diff per domain  ┌───────────────────────────────┐
│  Git repos           │ ──────────────────────────▶ │  GitOpsScheduler             │
│  (one per domain,    │    pull → build change set  │  domains in priority order   │
│  branch main)        │    → apply in one txn       └───────────────┬───────────────┘
└──────────────────────┘                                            │
                                                                     ▼
                                                     ┌───────────────────────────────┐
                                                     │  ConfigService layer          │
                                                     │  (apply/delete DTOs)          │
                                                     └───────────────┬───────────────┘
                                                                     ▼
                                                     ┌───────────────────────────────┐
                                                     │  PostgreSQL                   │
                                                     │  + vcs_sync_log (SUCCESS/FAILED)│
                                                     │  + vcs_object_log (per object) │
                                                     └───────────────────────────────┘
```

### Domains and repository layout

The demo stack serves one bare repository per domain on `git://git-server:9418/<domain>.git`. A domain repository contains a flat set of YAML files, one configuration construct per file. Each file starts with a Kubernetes-style `kind` field that selects the target DTO; the rest of the file is bound to that DTO (unknown properties are an error, enum values are case-insensitive).

Demo layout (`demo/compose/conf_git/domains/`):

```
domains/
├── platform/
│   ├── datasources/yarinastorage.yaml
│   ├── drivers/postgres.yaml
│   ├── sql-scripts/sql-template-postgres/insertDML.yaml
│   └── scenarios/spark.yaml
├── analytics/
│   ├── datasets/transaction_dds.yaml
│   ├── quality/metrics/transaction_dds_qm.yaml
│   └── schedules/regular.yaml
└── processing/
    ├── datasets/client_processing.yaml
    └── schedules/generateSource.yaml
```

The three directories are three sibling Git repositories; the demo *declares* them as a
nested chain (`platform` ⊃ `processing` ⊃ `analytics`) - see the `domains` block in
[Domains](#domains).

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

SQL scripts are stored the same way with `kind: Script` and two fields - the global script `key` (dots replace the directory path) and the script body as a literal `value`. Scripts are content-only and are shared between domains by key: their DTO is not stamped with a domain, and the domain stored on the `SQLTemplate` row is derived from the `Driver` or `Task` that references the script:

```yaml
kind: Script
key: dq.non_zero_count.sql
value: |
  select count(1) value
  from {{ refCat(targetDataSetKeyName) }}
```

### Supported kinds

Applied in dependency order (delete happens in the reverse order):

| kind | File example | Primary key |
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

The kinds `ERDiagram` (`erdiagrams/`) and `DataLineageDiagram` (`datalineagediagrams/`) are stored in the repository but are **not** applied by the configuration service.

### Sync semantics

- Every cycle iterates the configured domains in priority order (parents before nested domains, same-level domains by `priority` ascending) and synchronizes each domain repository independently: pull → diff against the last **successfully** applied commit of that domain → apply in one transaction. On an empty database the whole head of a domain is treated as a set of created files. Sub-domains have a hierarchical dependency on their parent: when synchronization of a domain fails, its whole sub-tree is skipped until the parent succeeds again.
- The first application of a commit is done inside a **single transaction**: created and updated constructs are applied in the `kind` order above (datasets additionally in their `sources` dependency order), deleted constructs in the reverse order, and only then the `SUCCESS` marker is written. Any failure rolls the whole commit back.
- Each construct applied from a domain repository is stored with `domainKeyName` = the domain name, so its owner is known on the row. The key stays global: `keyName` is the primary key, so it is never reused across domains (two domains pointing at the same table use two distinct `keyName`s). Scripts are the exception (no domain at all, shared by key; the domain stored on the `SQLTemplate` row is inherited from the referencing driver or task).
- Every construct touched by a commit is recorded in `vcs_object_log` (`date_time_rec`, `object_name` from `keyName`, `kind`, `file_path` relative to the repository root, `commit_id`, `domain_key_name`), for both applied and un-managed files. Sync rows in `vcs_sync_log` carry `domain_key_name` too.
- A commit that fails YAML parsing, validation or a database constraint is recorded as `FAILED` (with `domain_key_name` and the error message) and is **not retried**; a later fixing commit simply rolls the failed content in as part of a new diff.
- A `Schedule` may only reference data sets of its own domain: otherwise the commit fails with `DataSetDomainConflictException` and is recorded as `FAILED`.
- Infrastructure failures (unreachable repository, missing local clone) are only logged and retried on the next cycle. Domains without a configured repository are skipped.
- Commits whose `(commit_id, domain_key_name)` pair already has a `vcs_sync_log` row are skipped. Renames are treated as delete + create. Only `*.yaml`, `*.yml` and `*.json` files are configuration files; everything else (e.g. `load.sh`) is ignored.

### VCS management flag

Every construct loaded from a repository gets `isVcsManaged=true`; it stays false for constructs created through the REST API.

- Deleting a YAML file from the repository **does not delete the construct** - the service only clears `isVcsManaged` on the corresponding entity. The actual deletion has to be done by the user through the REST API afterwards.
- Any REST `POST`/`PUT`/`DELETE` on a VCS-managed construct is rejected with `409 Conflict` (`VcsManagedException`): to change or delete a managed construct via the REST API, first remove it from the repository. The REST API additionally rejects changes that move a construct outside its domain (`DomainConflictException`).

### Configuration

Two independent prefixes are involved: the **sync cadence** of this service stays under
`yarina.config.vcs.sync.*`, while the **domains themselves** are declared under
`yarina.domains.*` and processed by the shared `DomainSettings` of `yarina-domain-api`
(see [yarina-domain-api/doc/readme.md](../../yarina-domain-api/doc/readme.md); also
[appconf/service_configuration.md](appconf/service_configuration.md)).

The service accesses every domain repository through a single Git integration: a local
clone is kept per domain via JGit and synchronized on every cycle. Credentials are
declared **per domain** in `yarina.domains.<name>.git.credentials.*` — there is no global
account and no merge: a domain either carries its own credentials or speaks to its
repository anonymously, so different domains can authenticate with different accounts. The
repository URL accepts any Git form (`git://`, `ssh://`, `http(s)://` or a local path).
Setup in detail: [Git extension user guide](vcs/git_extension_user_guide.md).

| Property | Environment variable | Default | Description |
|---|---|---|---|
| `yarina.config.vcs.sync.enabled` | `YARINA_CONFIG_GIT_SYNC_ENABLED` | `false` | Enables the VCS scheduler bean |
| `yarina.config.vcs.sync.interval-ms` | `YARINA_CONFIG_GIT_SYNC_INTERVAL_MS` | `30000` | Cycle period |
| `yarina.config.vcs.sync.initial-delay-ms` | `YARINA_CONFIG_GIT_SYNC_INITIAL_DELAY_MS` | `10000` | Delay of the first cycle after startup |
| `yarina.domains.<name>.git.repository-url` | `YARINA_DOMAINS_<NAME>_GIT_REPOSITORY_URL` | - | URL of the domain repository (any Git URL: `git://`, `ssh://`, `http(s)://` or local path); **required** for the domain to be active |
| `yarina.domains.<name>.git.branch` | `YARINA_DOMAINS_<NAME>_GIT_BRANCH` | `main` | Branch to synchronize |
| `yarina.domains.<name>.git.local-clone-path` | `YARINA_DOMAINS_<NAME>_GIT_LOCAL_CLONE_PATH` | - | Local path where the service keeps the domain clone (must be unique per domain) |
| `yarina.domains.<name>.git.credentials.auth-type` | - | inferred | `ssh` (when `ssh-private-key-path` is set), `token` (when any credential is set) or `basic`; empty credentials mean anonymous access |
| `yarina.domains.<name>.git.credentials.ssh-private-key-path` | `YARINA_DOMAINS_<NAME>_GIT_CREDENTIALS_SSH_PRIVATE_KEY_PATH` | - | Path to an SSH private key (only for `ssh://` URLs) |
| `yarina.domains.<name>.git.credentials.username` / `.token` / `.password` | - | - | Per-domain credentials for `basic` / `token` auth |
| `yarina.domains.<name>.priority` | - | lowest | Position of the domain in the apply order (ascending, parents before children) |
| `yarina.domains.<name>.domains` | - | - | Nested sub-domains |

Per-domain environment variables follow Spring relaxed binding of the same namespace: for a
domain named e.g. `platform` they are `YARINA_DOMAINS_PLATFORM_GIT_REPOSITORY_URL`,
`YARINA_DOMAINS_PLATFORM_GIT_BRANCH` and `YARINA_DOMAINS_PLATFORM_GIT_LOCAL_CLONE_PATH`.

The shipped `application.yml` declares an empty `yarina.domains` map and the
`yarina.config.vcs.sync.*` block, so the domain tree is supplied by the deployment - see
[Domains](#domains).

### Demo

The `demo/compose` stack runs a lightweight git server (`git-server`, image `alpine/git` with the `git-daemon` package) that serves **one bare repository per domain** under a persistent volume. On start it imports every directory of `demo/compose/conf_git/domains` into its own same-named repository (`platform`, `analytics`, `processing`) — a root commit on first start, only the differences on later starts — and exposes them over `git://`. `yarina-config-svc` is configured with the three domain repositories (`git://git-server:9418/{platform,analytics,processing}.git`), branch `main`, and `sync.enabled=true`, so on startup it applies the whole demo configuration from git instead of the REST `load.sh` bootstrap.

The demo declares the domains as a **nested chain** `platform` ⊃ `processing` ⊃ `analytics` even
though the three repositories are siblings from Git's point of view, which demonstrates that
nesting is a configuration choice, not a storage constraint:

```
-Dyarina.config.vcs.sync.enabled=true
-Dyarina.domains.platform.priority=0
-Dyarina.domains.platform.git.repository-url=git://git-server:9418/platform.git
-Dyarina.domains.platform.domains.processing.priority=0
-Dyarina.domains.platform.domains.processing.git.repository-url=git://git-server:9418/processing.git
-Dyarina.domains.platform.domains.processing.domains.analytics.priority=0
-Dyarina.domains.platform.domains.processing.domains.analytics.git.repository-url=git://git-server:9418/analytics.git
```

## Domains

A **domain** is the unit of isolation of the service. It is at the same time a logical scope
for metadata and a Git repository: one repository per domain, one independent synchronization
cycle per repository, and the domain name stamped onto every construct loaded from it as
`domainKeyName`. The domain is never part of the YAML content - it is derived from the
repository the file comes from, so the same file means different things in different
domains.

The canonical description of the shared model - namespace, ordering, credentials - is
[yarina-domain-api/doc/readme.md](../../yarina-domain-api/doc/readme.md); this section only
summarizes what this service does with it.

```
yarina.domains  ──▶  DomainSettings.orderedDomains()  ──▶  for each domain
                                                             fetch + diff
                                                             apply in one txn
                                                             stamp domainKeyName
                                                                 │
                                                                 ▼
                                          DataSet / DataSource / Driver / Task /
                                          ScenarioActTemplate / Schedule /
                                          QualityMetricsConf / TaskExecutionServiceGroup
                                          (identity = keyName, ownership = domainKeyName)
```

### Tree, order and ownership

Domains form a tree: `yarina.domains.<name>.domains` declares nested sub-domains.
`DomainSettings.orderedDomains()` walks the tree depth-first, so a parent is always applied
before its children; within one level the order is `priority` ascending, domains without a
`priority` go last, and equal priorities are broken by name. Nesting is a **configuration**
relation, not a storage one - the same repositories may be declared as siblings or as a
chain. Its only behavioural consequence is failure propagation: when a domain fails, its
whole sub-tree is skipped until the parent succeeds again, because a child may reference
constructs of its parent. A domain without `git.repository-url` is inactive: this service
skips it entirely, while `yarina-ui-svc` fails when such a domain is used.

The domain is an ownership attribute, not a part of the object identity: `keyName` is the
sole primary key and stays global, so the same `keyName` cannot exist in two domains. Scripts
(`kind: Script`) have no domain at all - they are content-only and shared, and the domain
stored on the `SQLTemplate` row is inherited from the `Driver` or `Task` that references
them.

### Isolation rules

- A `Schedule` may reference only data sets of its own domain. A reference to a data set of
  another domain, or to a manually created data set that belongs to no domain, fails the
  whole commit with `DataSetDomainConflictException`.
- The REST API rejects an update that would overwrite a construct of one domain with a
  construct of another: `DomainConflictException` → `409 Conflict`. A construct whose stored
  or incoming domain is `null` is not checked.
- Only the `Save`/`Delete` REST paths of the domain-scoped kinds are affected; see
  [VCS management flag](#vcs-management-flag).

### How domains are configured

The domains themselves are bound by the shared `DomainSettings` of `yarina-domain-api` under
the `yarina.domains` prefix; this service only delivers the raw properties:

```yaml
yarina:
  domains:
    platform:
      priority: 0
      git:
        repository-url: git://git-server:9418/platform.git
        branch: main
        local-clone-path: /tmp/platform
        credentials:                      # per-domain, never merged
          auth-type: ssh
          ssh-private-key-path: /keys/platform_ed25519
      domains:                            # nested sub-domains
        processing:
          priority: 0
          git:
            repository-url: git://git-server:9418/processing.git
            branch: main
            local-clone-path: /tmp/processing
```

`local-clone-path` must be unique per domain, since each domain keeps its own clone there.
The per-domain property table and the environment-variable pattern
(`YARINA_DOMAINS_<NAME>_GIT_REPOSITORY_URL`, `..._GIT_BRANCH`, `..._GIT_LOCAL_CLONE_PATH`,
`..._GIT_CREDENTIALS_SSH_PRIVATE_KEY_PATH`) are in [Configuration](#configuration) above.
The pre-domains properties `yarina.config.vcs.git.*` and the synthesized `default` domain
are gone - see [yarina-domain-api/doc/readme.md](../../yarina-domain-api/doc/readme.md#3-a-domain-is-active-iff-gitrepository-url-is-set).
The scheduler itself (`yarina.config.vcs.sync.*`) is global and is not declared per domain.

### Observability: which domain a change came from

Both VCS log tables carry the domain, and both read-only REST endpoints accept it as a
filter:

| Table / endpoint | Column / parameter |
|---|---|
| `vcs_sync_log` | `domain_key_name` |
| `vcs_object_log` | `domain_key_name` |
| `GET /v1_0/configs/vcs/logs` | `domainKeyName` (optional; `from`/`to` required, `status`, `commitId` optional) |
| `GET /v1_0/configs/vcs/objectlogs` | `domainKeyName` (optional; `commitId` or both `from` and `to` required) |

A `(commit_id, domain_key_name)` pair that already has a sync row is skipped, so the same
commit id applied by two different domains is tracked separately.

`domainKeyName` also leaves this service: it is part of the REST representation of the
domain-scoped DTOs and of the sync log DTOs, and is consumed by `yarina-ui-svc` (read-only
filter of the VCS logs) and `yarina-scheduler-svc` (domain check of a
`TaskExecutionServiceGroup`).

The domain reference of the configuration objects themselves is
[content_configuration/domains.md](content_configuration/domains.md); the internals of the
tree resolution are in [VCS subsystem for platform developers](vcs/vcs_for_developers.md#6-developer-parameters).

## Modules

### yarina-config-svc

Spring Boot application that implements the REST API and the metadata storage. Entry point: `org.yarina.config.YarinaConfigApplication`.

### yarina-config-rest-client

Java client (`ConfigRestClientApi`/`ConfigRestClientApiImpl`) for accessing `yarina-config-svc` from other services (scheduler-svc, task-executor-svc, etc.). It performs typed requests to the `/v1_0/configs/...` endpoints through `RestClientHelper`. The base URL is set by the property `yarina.client.rest.config.server.url`.

## API Endpoints

The description of the endpoint structure and metadata configurations is in the [content_configuration](content_configuration/content_configuration.md) section.

## Configuration

Application parameters (datasource, JPA, Kafka schedule publishing settings, health endpoints) are described in [appconf/service_configuration.md](appconf/service_configuration.md).

## Security

`yarina-config-svc` is protected with OAuth 2.0 / OIDC and uses Keycloak as the identity provider (realm `yarina`). Spring Security is configured as an **OAuth2 resource server**: every request must carry a valid JWT issued by the realm, otherwise the service returns `401`.

### Authentication

- **User requests** (UI BFF, CLI, direct API calls) - the JWT is validated via `spring.security.oauth2.resourceserver.jwt.issuer-uri`. Roles from the `realm_access.roles` claim are converted to `ROLE_<NAME>` authorities by `KeycloakRoleConverter` and can be used with `@PreAuthorize`.
- **Service-to-service calls** (`BearerTokenClientHttpRequestInterceptor`) - when a request originates from a background task (no user JWT in the `SecurityContext`), the outgoing `RestClient` obtains a `client_credentials` token through the `OAuth2AuthorizedClientManager` using the `keycloak-internal` registration (client `yarina-internal-client`) and attaches it as `Authorization: Bearer`. When a user JWT is present, it is propagated unchanged.

### Audit logging

`AuditLoggingFilter` writes one line per request to the `AUDIT_LOG` logger (file `logs/audit.log`, override with `yarina.security.audit.log-file`):

```
User ID: <subject>, Username: <preferred_username>, Method: <method>, URI: <uri>, HTTP status: <status>
```

Tokens obtained with the service account are logged with the configured `yarina.security.audit.service-account-name` (default `system`) instead of the username.

### Required settings

| Property / env | Default | Description |
|---|---|---|
| `KEYCLOAK_ISSUER_URI` | `http://yarina-auth-svc:8080/realms/yarina` | Keycloak realm URL |
| `KEYCLOAK_INTERNAL_CLIENT_SECRET` | `super-secret-internal-key-987654321` | Secret of `yarina-internal-client` |
| `yarina.security.enabled` | `true` | Set `false` to disable security completely |
| `yarina.security.audit.service-account-name` | `system` | Username logged for service account tokens |
| `yarina.security.audit.log-file` | `logs/audit.log` | Audit log file path (created on start-up, relative paths are resolved against the working directory) |
| `yarina.security.oauth2.internal-client-id` | `yarina-internal-client` | Client identifying service account tokens (`azp` claim) |
| `yarina.security.oauth2.client-registration-id` | `keycloak-internal` | OAuth2 client registration used by the interceptor |

`spring.security.oauth2.resourceserver.jwt.issuer-uri` and the `spring.security.oauth2.client` block (registration `keycloak-internal`) are preconfigured in `src/main/resources/application.yml`.

Whitelisted paths (no token required): `/healthz`, `/readyz`, `/actuator/**`, `/v3/api-docs/**`, `/swagger-ui/**`. The Swagger paths are active only while Swagger is enabled; under the `prod` profile they are fully disabled (`springdoc.api-docs.enabled: false`, `springdoc.swagger-ui.enabled: false`).

### Keycloak realm

The `yarina` realm must contain:

- **`yarina-internal-client`** - confidential client with *Service Accounts Enabled* (service-to-service calls);
- **`yarina-ui-client`** - confidential client with *Standard Flow Enabled* (user login via the UI BFF);
- Realm roles `USER` / `ADMIN` (optional, used with `@PreAuthorize`).

The reference realm import is in `demo/compose/conf_infra/security/realms/yarina-realm.json`.