# lakehouse-config-svc

Metadata management service - a single storage for all lakehouse configurations. It is the source of truth for metadata on the basis of which the other services (scheduler-svc, task-executor-svc, state-svc) perform data processing.

## Overview

`lakehouse-config-svc` stores and serves lakehouse metadata:

- **Namespaces** - logical separation of environments
- **Drivers** - connection settings for compute clusters
- **Data sources** - connections to external storages (JDBC/Spark)
- **Datasets** - table, column and constraint descriptions
- **Schedules** - data processing periodicity (intervals, scenario acts, tasks)
- **Data quality metrics** - DQ checks
- **Scripts and SQL templates** - query templates with Jinjava substitutions
- **Data lineage** - data provenance relationships
- **TaskExecutionServiceGroups** - task executor groups
- **Declarative configuration from Git (GitOps/CVS)** - the same configuration DTOs can be defined as YAML files in a Git repository and synchronized into the database automatically by the CVS subsystem (see [GitOps: declarative configuration from a Git repository](#gitops-declarative-configuration-from-a-git-repository-cvs))

Configurations are defined as DTOs, stored in PostgreSQL and exposed via REST API. Schedule changes are published to Kafka (topic `schedule_effective_changes`) so that scheduler-svc builds actual schedule instances.

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
- Metadata is organized hierarchically (namespace → datasource → dataset → ...); the dependency scheme is described in [content_configuration](content_configuration/content_configuration.md).

## GitOps: declarative configuration from a Git repository (CVS)

In addition to the REST API, `lakehouse-config-svc` can manage configuration declaratively: the same metadata DTOs are written as YAML files into a Git repository and the CVS (Configuration Versioning System) subsystem synchronizes them into the database on a schedule. This makes the repository the source of truth and gives a full history of every configuration change (GitOps style).

```
┌──────────────────┐   fetch + diff   ┌───────────────────────────────┐
│  Git repo        │ ───────────────▶ │  GitOpsScheduler             │
│  (branch main)   │                  │  pull → build change set     │
└──────────────────┘                  │  → apply in one transaction  │
                                      └───────────────┬───────────────┘
                                                      │
                                                      ▼
                                      ┌───────────────────────────────┐
                                      │  ConfigService layer          │
                                      │  (apply/delete DTOs)          │
                                      └───────────────┬───────────────┘
                                                      ▼
                                      ┌───────────────────────────────┐
                                      │  PostgreSQL                   │
                                      │  + cvs_sync_log (SUCCESS/FAILED)│
                                      │  + cvs_object_log (per object) │
                                      └───────────────────────────────┘
```

### Repository layout

A configuration repository is a flat set of YAML files, one configuration construct per file. Each file starts with a Kubernetes-style `kind` field that selects the target DTO; the rest of the file is bound to that DTO (unknown properties are an error, enum values are case-insensitive).

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

SQL scripts are stored the same way with `kind: Script` and two fields - the global script `key` (dots replace the directory path) and the script body as a literal `value`:

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

### Sync semantics

- Each cycle pulls the configured branch and diffs its head against the last **successfully** applied commit (`cvs_sync_log` with status `SUCCESS`); on an empty database the whole head is treated as a set of created files.
- The first run of a commit is applied inside a **single transaction**: created and updated constructs are applied in the `kind` order above (datasets additionally in their `sources` dependency order), deleted constructs in the reverse order, and only then the `SUCCESS` marker is written. Any failure rolls the whole commit back.
- Every construct touched by a commit is recorded in `cvs_object_log` (`date_time_rec`, `object_name` from `keyName`, `kind`, `file_path` relative to the repository root, `commit_id`), for both applied and un-managed files.
- A commit that fails YAML parsing, validation or a database constraint is recorded as `FAILED` together with the error message and is **not retried**; a later fixing commit simply rolls the failed content in as part of a new diff.
- Infrastructure failures (unreachable repository, missing local clone) are only logged and retried on the next cycle.
- Commits whose id already has a `cvs_sync_log` row are skipped. Renames are treated as delete + create. Only `*.yaml`, `*.yml` and `*.json` files are configuration files; everything else (e.g. `load.sh`) is ignored.

### CVS management flag

Every construct loaded from the repository gets `isCvsManaged=true`; it stays false for constructs created through the REST API.

- Deleting a YAML file from the repository **does not delete the construct** - the service only clears `isCvsManaged` on the corresponding entity. The actual deletion has to be done by the user through the REST API afterwards.
- Any REST `POST`/`PUT`/`DELETE` on a CVS-managed construct is rejected with `409 Conflict` (`CvsManagedException`): to change or delete a managed construct via the REST API, first remove it from the repository.

### Configuration

All settings live under the `lakehouse.config.cvs.*` prefix (see also [appconf/service_configuration.md](appconf/service_configuration.md)):

| Property | Environment variable | Default | Description |
|---|---|---|---|
| `lakehouse.config.cvs.git.repository-url` | `LAKEHOUSE_CONFIG_GIT_REPOSITORY_URL` | - | URL of the configuration repository (supports `git://`, `ssh://` and `http(s)://`) |
| `lakehouse.config.cvs.git.branch` | `LAKEHOUSE_CONFIG_GIT_BRANCH` | `main` | Branch to synchronize |
| `lakehouse.config.cvs.git.local-clone-path` | `LAKEHOUSE_CONFIG_GIT_LOCAL_CLONE_PATH` | - | Local path where the service keeps its clone |
| `lakehouse.config.cvs.git.private-key-path` | `LAKEHOUSE_CONFIG_GIT_PRIVATE_KEY_PATH` | - | Path to an SSH private key (only for `ssh://` URLs) |
| `lakehouse.config.cvs.git.sync.enabled` | `LAKEHOUSE_CONFIG_GIT_SYNC_ENABLED` | `false` | Enables the CVS scheduler bean |
| `lakehouse.config.cvs.git.sync.interval-ms` | `LAKEHOUSE_CONFIG_GIT_SYNC_INTERVAL_MS` | `30000` | Cycle period |
| `lakehouse.config.cvs.git.sync.initial-delay-ms` | `LAKEHOUSE_CONFIG_GIT_SYNC_INITIAL_DELAY_MS` | `10000` | Delay of the first cycle after startup |

### Demo

The `demo/compose` stack runs a lightweight git server (`git-server`, image `alpine/git` with the `git-daemon` package) that hosts a bare repository under a persistent volume, imports `demo/compose/conf_git` (a YAML mirror of `demo/compose/conf`) into the `main` branch — a root commit on first start, only the differences on later starts — and exposes it over `git://`. `lakehouse-config-svc` is configured with `git://git-server:9418/config-repo.git`, branch `main` and `sync.enabled=true`, so on startup it applies the whole demo configuration from git instead of the REST `load.sh` bootstrap.

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