# lakehouse-config-svc

Metadata management service - a single storage for all lakehouse configurations. It is the source of truth for metadata on the basis of which the other services (scheduler-svc, task-executor-svc, state-svc) perform data processing.

## Overview

`lakehouse-config-svc` stores and serves lakehouse metadata:

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
│  Admin / UI / CLI     │ ────────────────────▶│   lakehouse-config-svc    │
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
                                               │  │ (schema lakehouse_  │  │
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
- Metadata is organized hierarchically (domain → datasource → dataset → ...); the domain hierarchy is defined in `lakehouse.config.vcs.domains` (see the GitOps section) and the dependency scheme is described in [content_configuration](content_configuration/content_configuration.md).

## GitOps: declarative configuration from Git repositories (VCS)

In addition to the REST API, `lakehouse-config-svc` can manage configuration declaratively: the same metadata DTOs are written as YAML files into Git repositories and the VCS (Configuration Versioning System) subsystem synchronizes them into the database on a schedule. The repositories become the source of truth and keep the full history of every configuration change (GitOps style).

Configuration is organized into **domains**. Every domain owns its own Git repository and is synchronized independently. A domain may contain **nested domains** (e.g. `platform` ⊃ `processing` ⊃ `analytics`); domains are applied in the priority order (ascending, parents before children). Every construct loaded from a domain repository is stamped with the domain name (`domainKeyName`), which scopes it and makes cross-domain references verifiable.

The domain model itself (tree, ownership, isolation rules, configuration) is described in
[Domains](#domains) and in [content_configuration/domains.md](content_configuration/domains.md).

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
│   ├── datasources/lakehousestorage.yaml
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

All settings live under the `lakehouse.config.vcs.*` prefix (see also [appconf/service_configuration.md](appconf/service_configuration.md)):

| Property | Environment variable | Default | Description |
|---|---|---|---|
| `lakehouse.config.vcs.git.sync.enabled` | `LAKEHOUSE_CONFIG_GIT_SYNC_ENABLED` | `false` | Enables the VCS scheduler bean |
| `lakehouse.config.vcs.git.sync.interval-ms` | `LAKEHOUSE_CONFIG_GIT_SYNC_INTERVAL_MS` | `30000` | Cycle period |
| `lakehouse.config.vcs.git.sync.initial-delay-ms` | `LAKEHOUSE_CONFIG_GIT_SYNC_INITIAL_DELAY_MS` | `10000` | Delay of the first cycle after startup |
| `lakehouse.config.vcs.domains.<name>.git.repository-url` | `LAKEHOUSE_CONFIG_GIT_<NAME>_URL` | - | URL of the domain repository (supports `git://`, `ssh://` and `http(s)://`) |
| `lakehouse.config.vcs.domains.<name>.git.branch` | `LAKEHOUSE_CONFIG_GIT_<NAME>_BRANCH` | `main` | Branch to synchronize |
| `lakehouse.config.vcs.domains.<name>.git.local-clone-path` | `LAKEHOUSE_CONFIG_GIT_<NAME>_CLONE_PATH` | - | Local path where the service keeps the domain clone |
| `lakehouse.config.vcs.domains.<name>.git.private-key-path` | `LAKEHOUSE_CONFIG_GIT_<NAME>_PRIVATE_KEY_PATH` | - | Path to an SSH private key (only for `ssh://` URLs) |
| `lakehouse.config.vcs.domains.<name>.priority` | - | lowest | Position of the domain in the apply order (ascending, parents before children) |
| `lakehouse.config.vcs.domains.<name>.domains` | - | - | Nested sub-domains |

For a domain named e.g. `platform` the environment variables are `LAKEHOUSE_CONFIG_GIT_PLATFORM_URL`, `LAKEHOUSE_CONFIG_GIT_PLATFORM_BRANCH`, `LAKEHOUSE_CONFIG_GIT_PLATFORM_CLONE_PATH` and `LAKEHOUSE_CONFIG_GIT_PLATFORM_PRIVATE_KEY_PATH`.

The shipped `application.yml` declares only the legacy `lakehouse.config.vcs.git.*` block, so the domain tree is supplied by the deployment - see [Domains](#domains).

### Demo

The `demo/compose` stack runs a lightweight git server (`git-server`, image `alpine/git` with the `git-daemon` package) that serves **one bare repository per domain** under a persistent volume. On start it imports every directory of `demo/compose/conf_git/domains` into its own same-named repository (`platform`, `analytics`, `processing`) — a root commit on first start, only the differences on later starts — and exposes them over `git://`. `lakehouse-config-svc` is configured with the three domain repositories (`git://git-server:9418/{platform,analytics,processing}.git`), branch `main`, and `sync.enabled=true`, so on startup it applies the whole demo configuration from git instead of the REST `load.sh` bootstrap.

The demo declares the domains as a **nested chain** `platform` ⊃ `processing` ⊃ `analytics` even
though the three repositories are siblings from Git's point of view, which demonstrates that
nesting is a configuration choice, not a storage constraint:

```
-Dlakehouse.config.vcs.git.sync.enabled=true
-Dlakehouse.config.vcs.domains.platform.priority=0
-Dlakehouse.config.vcs.domains.platform.git.repository-url=git://git-server:9418/platform.git
-Dlakehouse.config.vcs.domains.platform.domains.processing.priority=0
-Dlakehouse.config.vcs.domains.platform.domains.processing.git.repository-url=git://git-server:9418/processing.git
-Dlakehouse.config.vcs.domains.platform.domains.processing.domains.analytics.priority=0
-Dlakehouse.config.vcs.domains.platform.domains.processing.domains.analytics.git.repository-url=git://git-server:9418/analytics.git
```

## Domains

A **domain** is the unit of isolation of the service. It is at the same time a logical scope
for metadata and a Git repository: one repository per domain, one independent synchronization
cycle per repository, and the domain name stamped onto every construct loaded from it as
`domainKeyName`. The domain is never part of the YAML content - it is derived from the
repository the file comes from, so the same file means different things in different
domains.

```
lakehouse.config.vcs.domains  ──▶  orderedDomains()  ──▶  for each domain
                                                             fetch + diff
                                                             apply in one txn
                                                             stamp domainKeyName
                                                                 │
                                                                 ▼
                                          DataSet / DataSource / Driver / Task /
                                          ScenarioActTemplate / Schedule /
                                          QualityMetricsConf / TaskExecutionServiceGroup
                                          (identity = domainKeyName + keyName)
```

### Tree, order and ownership

Domains form a tree: `lakehouse.config.vcs.domains.<name>.domains` declares nested
sub-domains. `LakehouseVCSProperties.orderedDomains()` walks the tree depth-first, so a
parent is always applied before its children; within one level the order is `priority`
ascending, domains without a `priority` go last, and equal priorities are broken by name.
Nesting is a **configuration** relation, not a storage one - the same repositories may be
declared as siblings or as a chain. Its only behavioural consequence is failure
propagation: when a domain fails, its whole sub-tree is skipped until the parent succeeds
again, because a child may reference constructs of its parent. A domain without
`git.repository-url` is skipped entirely.

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

Everything is bound by `LakehouseVCSProperties` under the `lakehouse.config.vcs` prefix:

```yaml
lakehouse:
  config:
    vcs:
      domains:
        platform:
          priority: 0
          git:
            repository-url: git://git-server:9418/platform.git
            branch: main
            local-clone-path: /tmp/platform
            private-key-path:            # only for ssh:// URLs
          domains:                        # nested sub-domains
            processing:
              priority: 0
              git:
                repository-url: git://git-server:9418/processing.git
                branch: main
                local-clone-path: /tmp/processing
```

`local-clone-path` must be unique per domain, since each domain keeps its own clone there.
The per-domain property table and the environment-variable pattern
(`LAKEHOUSE_CONFIG_GIT_<NAME>_URL`, `..._<NAME>_BRANCH`, `..._<NAME>_CLONE_PATH`,
`..._<NAME>_PRIVATE_KEY_PATH`) are in [Configuration](#configuration) above.

**Legacy single-repository configuration.** The pre-domains properties
`lakehouse.config.vcs.git.repository-url` / `.branch` / `.local-clone-path` /
`.private-key-path` are still honoured: when `domains` is **empty** and
`git.repository-url` is not blank, that single repository is exposed as one domain named
`default`. As soon as `domains` contains at least one entry the legacy block is ignored - it
does not become an additional `default` domain. The scheduler itself (`git.sync.*`) is
global and is not declared per domain.

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
domain-scoped DTOs and of the sync log DTOs, and is consumed by `lakehouse-ui-svc` (read-only
filter of the VCS logs) and `lakehouse-scheduler-svc` (domain check of a
`TaskExecutionServiceGroup`).

The domain reference of the configuration objects themselves is
[content_configuration/domains.md](content_configuration/domains.md); the internals of the
tree resolution are in [VCS subsystem for platform developers](vcs/vcs_for_developers.md#6-developer-parameters).

## Modules

### lakehouse-config-svc

Spring Boot application that implements the REST API and the metadata storage. Entry point: `org.lakehouse.config.LakehouseConfigApplication`.

### lakehouse-config-rest-client

Java client (`ConfigRestClientApi`/`ConfigRestClientApiImpl`) for accessing `lakehouse-config-svc` from other services (scheduler-svc, task-executor-svc, etc.). It performs typed requests to the `/v1_0/configs/...` endpoints through `RestClientHelper`. The base URL is set by the property `lakehouse.client.rest.config.server.url`.

## API Endpoints

The description of the endpoint structure and metadata configurations is in the [content_configuration](content_configuration/content_configuration.md) section.

## Configuration

Application parameters (datasource, JPA, Kafka schedule publishing settings, health endpoints) are described in [appconf/service_configuration.md](appconf/service_configuration.md).

## Security

`lakehouse-config-svc` is protected with OAuth 2.0 / OIDC and uses Keycloak as the identity provider (realm `lakehouse`). Spring Security is configured as an **OAuth2 resource server**: every request must carry a valid JWT issued by the realm, otherwise the service returns `401`.

### Authentication

- **User requests** (UI BFF, CLI, direct API calls) - the JWT is validated via `spring.security.oauth2.resourceserver.jwt.issuer-uri`. Roles from the `realm_access.roles` claim are converted to `ROLE_<NAME>` authorities by `KeycloakRoleConverter` and can be used with `@PreAuthorize`.
- **Service-to-service calls** (`BearerTokenClientHttpRequestInterceptor`) - when a request originates from a background task (no user JWT in the `SecurityContext`), the outgoing `RestClient` obtains a `client_credentials` token through the `OAuth2AuthorizedClientManager` using the `keycloak-internal` registration (client `lakehouse-internal-client`) and attaches it as `Authorization: Bearer`. When a user JWT is present, it is propagated unchanged.

### Audit logging

`AuditLoggingFilter` writes one line per request to the `AUDIT_LOG` logger (file `logs/audit.log`):

```
User ID: <subject>, Username: <preferred_username>, Method: <method>, URI: <uri>, HTTP status: <status>
```

Tokens obtained with the service account are logged with the configured `lakehouse.security.audit.service-account-name` (default `system`) instead of the username.

### Required settings

| Property / env | Default | Description |
|---|---|---|
| `KEYCLOAK_ISSUER_URI` | `http://lakehouse-auth-svc:8080/realms/lakehouse` | Keycloak realm URL |
| `KEYCLOAK_INTERNAL_CLIENT_SECRET` | `super-secret-internal-key-987654321` | Secret of `lakehouse-internal-client` |
| `lakehouse.security.enabled` | `true` | Set `false` to disable security completely |
| `lakehouse.security.audit.service-account-name` | `system` | Username logged for service account tokens |
| `lakehouse.security.oauth2.internal-client-id` | `lakehouse-internal-client` | Client identifying service account tokens (`azp` claim) |
| `lakehouse.security.oauth2.client-registration-id` | `keycloak-internal` | OAuth2 client registration used by the interceptor |

`spring.security.oauth2.resourceserver.jwt.issuer-uri` and the `spring.security.oauth2.client` block (registration `keycloak-internal`) are preconfigured in `src/main/resources/application.yml`.

Whitelisted paths (no token required): `/healthz`, `/readyz`, `/actuator/**`, `/v3/api-docs/**`, `/swagger-ui/**`. The Swagger paths are active only while Swagger is enabled; under the `prod` profile they are fully disabled (`springdoc.api-docs.enabled: false`, `springdoc.swagger-ui.enabled: false`).

### Keycloak realm

The `lakehouse` realm must contain:

- **`lakehouse-internal-client`** - confidential client with *Service Accounts Enabled* (service-to-service calls);
- **`lakehouse-ui-client`** - confidential client with *Standard Flow Enabled* (user login via the UI BFF);
- Realm roles `USER` / `ADMIN` (optional, used with `@PreAuthorize`).

The reference realm import is in `demo/compose/conf_infra/security/realms/lakehouse-realm.json`.