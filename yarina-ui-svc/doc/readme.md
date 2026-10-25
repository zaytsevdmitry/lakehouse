# Web UI service (yarina-ui-svc)

The yarina management web UI: a single point for visualizing and administering the services, the data catalog, schedules, dataset states, Spark submissions and the metadata-driven configuration ("Modelling").

## Overview

`yarina-ui-svc` is the service that aggregates data from all the other yarina services and provides a single web interface. For the monitoring sections it is a thin aggregation layer: it calls the other services through their REST clients and returns the result to the frontend. Additionally it hosts the **Modelling** surface — an interactive editor for the configuration documents stored in the Git repositories of the configuration domains (workspaces, branches, review submission), which is served directly by this service. A domain owns its own repository, and a workspace is a set of `(domain, branch)` selections — see [Domains](#domains).

The service consists of two parts:

- **backend** — a Spring Boot application that proxies frontend requests to the yarina services, serves the static frontend and hosts the modeller backend (workspace storage, Git integration, YAML editing, RBAC);
- **frontend** — a single-page React application (Vite) built into `src/main/resources/static` and served by the same service.

UI sections:

- **Services** — the yarina services graph and their status (`UP`/`DOWN`) via health-check.
- **Catalog** — the data catalog tree: sources → schemas → datasets; dataset view (model/DDL, lineage, constraints) and data source view.
- **Schedules** — schedule instance runs for an interval, the schedule instance DAG.
- **SparkJobs** — Spark submissions through `yarina-task-proxy-for-spark`: create, status, kill, kill all, clear.
- **VCS** — the configuration GitOps synchronization log (commits) and object log of `yarina-config-svc`.
- **Modelling** — the metadata-modelling workbench: create a workspace from one or more domain branches, create configuration documents of any supported kind (form view driven by a per-kind schema, raw YAML, or the visual editors — **ER Diagram**, **Data Lineage Diagram**, generic **DAG**), create branches, submit changes for review. Workspaces open in a new browser tab via deep links (`?section=modeller&workspace=<id>`).

## Architecture

For the read-only/monitoring sections the service is a thin aggregation layer: each UI section is served by its own controller that delegates to the REST client of the corresponding yarina service. The service makes no direct database calls for those sections.

The **Modelling** surface is implemented by the UI service itself: it reads and writes configuration documents in workspaces (local FS or S3 object storage), interacts with the configuration domain repositories for that purpose (via JGit), and exposes schema-driven editing metadata. Workspace contents are user-specific working copies that are opened, edited and finally submitted for review; the authoritative store is the Git repository consumed by `yarina-config-svc` (GitOps). A domain repository may be dedicated to a single domain (flat files at the repository root) or host several domains under `domains/<name>/` — both layouts are supported.

External interactions:

- **yarina-config-svc** — data catalog, lineage, constraints, models, schedule headers, GitOps sync logs.
- **yarina-scheduler-svc** — schedule instance runs for an interval, the run DAG.
- **yarina-state-svc** — dataset interval states.
- **yarina-task-proxy-for-spark** — Spark submissions (create, status, kill, clear).
- **Git repositories (GitOps)** — one repository per configuration domain (`yarina.domains.<name>.git.repository-url`, declared in the shared `DomainSettings` of `yarina-domain-api`); the modeller clones the selected `(domain, branch)` pair per workspace, creates branches and submits changes for review.

Controllers (top-level `controller`):

```
CatalogController   /api/catalog      — catalog tree, datasets, lineage, constraints, scripts
ScheduleController  /api/schedules    — schedule runs, headers, DAG
ServicesController  /api/services     — services graph and status
SparkProxyController /api/spark-proxy — Spark submissions
StateController     /api/states       — dataset interval states
VcsLogController    /api/vcs          — GitOps sync log and object log
UserController      /api/user         — current user profile (incl. modeller role)
```

Controllers of the modeller (`org.yarina.ui.modeller.controller`):

```
VcsController    /api/vcs          — workspaces lifecycle, branches, review, restore
EditorController /api/workspaces/{workspaceId} — file-level CRUD inside a workspace
SchemaController /api/schema       — per-kind form schemas (KindSchema) for the editors
AdminController  /api/workspace/admin — WORKSPACE_ADMIN-only: all workspaces, cleanup TTL, sync logs
```

Service statuses are computed by `HealthChecker`: either an HTTP request to `healthCheckUrl` (type `http`) or a TCP port probe (type `tcp`). The set of services, graph edges and vertices are defined by the `yarina.ui.services/edges/vertices` configuration.

The frontend is built with Vite (the `frontend` directory), and the build output goes to `src/main/resources/static`. In dev mode Vite proxies `/api` to the service (`vite.config.js`).

## Domains

A **domain** is the unit of isolation of the configuration in `yarina-config-svc`: every
domain owns its own Git repository, and every construct loaded from it is stamped with the
domain name as `domainKeyName`. That is an ownership label, not part of the identity -
`keyName` stays global, so the modeller never has to disambiguate a name by domain. The
modeller
does not invent domains — it presents them. A configured domain repository is a root of the
branch panel, and a workspace is a set of `(domain, branch)` selections across those
repositories. See
[config-svc: Domains](../../yarina-config-svc/doc/content_configuration/domains.md).

### One repository (and one branch) per domain

Domains are declared under `yarina.domains` and bound by the shared `DomainSettings` of
`yarina-domain-api` (canonical reference:
[yarina-domain-api/doc/readme.md](../../yarina-domain-api/doc/readme.md)); `yarina.modeller`
no longer holds domains — it is workspace storage / auth / session only:

```yaml
yarina:
  domains:                        # <domainKeyName>:
    platform:                     #   git.repository-url: ...
      git:
        repository-url: git://git-server:9418/platform.git
        branch: main
        credentials:              # per-domain, never merged
          auth-type: ssh
          ssh-private-key-path: /keys/platform_ed25519
    analytics:
      git:
        repository-url: git://git-server:9418/analytics.git
```

Resolution rules:

| Rule | Behaviour |
|---|---|
| Domain list | `domainNames()` is the configured domain names **sorted alphabetically**; the modeller presents a flat list of domains and ignores `priority` and the domain tree |
| Repository URL | `yarina.domains.<name>.git.repository-url`; there is **no legacy single-repository fallback** any more |
| Main branch | `yarina.domains.<name>.git.branch`, default `main` |
| Unavailable repository | the domain is listed with an **empty** branch list (`VcsException` is caught per domain; the branch panel still shows the root), it is not an error |
| Missing repository URL | a hard error on use (`IllegalStateException` → HTTP 400): `Domain '<name>': no repository URL configured (property: yarina.domains.<name>.git.repository-url)` |
| Invalid credentials | reported at startup: `ModellerVcsCatalog` warms up the client of every configured domain that has a repository, so a bad `auth-type` or an unreadable key fails fast |

`git.branch` is also the **target branch of a review**: a submission is merged back into
`domainSettings.branch(domain)` of its own domain.

### The branch panel is a flat list of domains

`GET /api/vcs/branches` returns one `DomainBranchesResponse` per configured domain -
`{domain, branches[], branchMain}` - and the frontend renders it as a tree with the domains at
the root and their branches hanging off them; the configured domain *tree* is not reflected
here, because `domainNames()` is a flat, alphabetically sorted list. `POST /api/vcs/branch`
takes `{domain, branch, baseBranch}`, so a branch is always created in a named domain
repository.

### A workspace is a set of `(domain, branch)` pairs

`POST /api/vcs/workspace` takes `branches: [{domain, branch}, ...]`:

- at least one selection is required, and every entry needs a non-blank `domain` **and**
  `branch`;
- a domain not listed in the request is simply not part of the workspace;
- the workspace id is `md5(username + "|" + sorted "domain=branch" pairs)`, so the same user
  reopening the same selection set gets the same workspace;
- each selection is checked out into its **own folder** named `<domain> (<branch>)`, so the
  file tree of a multi-domain workspace looks like `platform (feature-a)/ … analytics (main)/ …`;
- the selection list is persisted in `_workspace.json` and returned in `WorkspaceResponse`.

Inside the workspace, `domainKeyName` is **not editable**. It is excluded from every form
schema (`SchemaService.DERIVED_PROPERTIES`) because the domain is derived from the repository
a construct is submitted to, not from the file content — the same YAML is valid in every
domain. A repository may also be shared: if it hosts several domains under `domains/<name>/`,
the workspace keeps only the subtree of the selected domain and strips the prefix, so a
workspace folder never contains a `domains/` level.

### Review is submitted per domain

`POST /api/vcs/review/{workspaceId}` takes only `{comment, commitMessage}` — the split is
derived server-side:

- the workspace files are grouped by the `<domain> (<branch>)` folder that covers them; a file
  outside every selected domain folder is rejected;
- each group is committed to its own `(domain, branch)` with the user as author and the
  technical service account as committer and pushed to the domain branch;
- a domain whose diff is empty is skipped (`NO_CHANGES`);
- the aggregate answer is `OK` (with the resulting commit URLs, comma-joined when there are
  several) or `NO_CHANGES` when nothing changed at all;
- **the workspace is deleted only on success** — a failed submission leaves it open;
- a user must own the workspace, hold `EDITOR` for every selected domain, and the whole
  submission runs under a per-workspace lock.

### Domain as a filter of the VCS logs

The VCS sections read the synchronization history of `yarina-config-svc`, which is
per domain. Both read-only endpoints accept a `domainKeyName` filter, and both tables show
the `Domain Key Name` column:

| Endpoint | Filters |
|---|---|
| `GET /api/vcs/logs` | `status`, `commitId`, `domainKeyName` |
| `GET /api/vcs/objects` | `commitId`, `kind`, `from`, `to`, `filePath`, `objectName`, `domainKeyName` |

The filter is passed through the REST client to config-svc unchanged, so the UI shows exactly
what the configuration service recorded for a given domain.

### Configuration reference

| Property | Description |
|---|---|
| `yarina.domains.<name>.git.repository-url` | Repository of the domain; required for the domain to be usable in the branch panel |
| `yarina.domains.<name>.git.branch` | Main branch of the domain (default `main`), also the review target |
| `yarina.domains.<name>.git.credentials.auth-type` | `ssh` / `token` / `basic`, inferred from the declared credentials when omitted |
| `yarina.domains.<name>.git.credentials.ssh-private-key-path` / `.username` / `.token` / `.password` | Per-domain git credentials; an empty block means anonymous access |
| `yarina.modeller.storage.*` | Workspace storage (see [Configuration](#configuration)) |

This service does not need `git.local-clone-path`: it clones into transient work directories,
unlike `yarina-config-svc`, which keeps a persistent clone per domain.

Per-domain settings are not declared in the shipped `application.yml`; a deployment
supplies them, e.g. as system properties
(`-Dyarina.domains.platform.git.repository-url=...`, as `demo/compose` does) or via
relaxed-binding environment variables
(`YARINA_DOMAINS_PLATFORM_GIT_REPOSITORY_URL`, `YARINA_DOMAINS_PLATFORM_GIT_BRANCH`).

## Modules

### yarina-ui-svc

The service itself. Contains:

- the entry point `YarinaUiApplication`;
- monitoring controllers (`controller`): Catalog, Schedule, Services, SparkProxy, State, VcsLog, User;
- monitoring services (`service`): `CatalogService`, `ScheduleService`, `ServicesService`, `SparkProxyService`, `StateService`, `VcsLogService`, `HealthChecker`;
- the `UiServiceProperties` configuration (service list, graph);
- DTOs (`dto`) — frontend representations (`CatalogTreeNodeDTO`, `ConstraintDTO`, `ServiceNodeDTO`, `ScheduleRequestDTO`, `DataSetStateRequestDTO`);
- the modeller **package `org.yarina.ui.modeller`**:
  - controllers (`controller`): `VcsController`, `EditorController`, `SchemaController`, `AdminController`;
  - services (`service`): `VcsService`, `ReviewService`, `SchemaService`, `EditorService`, `YamlEditorService`, `EnumOptionsService`, `SyncLogService`, `AdminWorkspaceService`;
  - VCS integration (`config`): `ModellerProperties` (workspace storage, auth strategy, session, logging — domains and git credentials live in the shared `DomainSettings` of `yarina-domain-api`) and `ModellerVcsCatalog`, which builds one `GitVcsClient` per domain through `DomainVcsFactory` from the shared `yarina-domain-api` module;
  - workspace storage (`storage`): `WorkspaceStorage` SPI with `LocalFsWorkspaceStorage` and S3 (`S3WorkspaceStorage` + a minimal AWS SigV4 signer), plus `WorkspaceManager`,
    `WorkspaceSeeder` and the cleanup task (`WorkspaceCleanupTask`);
  - auth (`auth`): `DomainRole` (`VIEWER`/`EDITOR`/`CONTROL`), `ModellerRoleSupport` (domain role parsing), `UserContext`, `DomainAccess`, `ForbiddenException`/`NotFoundException`;
  - DTOs (`dto`): `KindSchema`, `FieldSchema`, `TreeResponse`, `FileContentResponse`, `WorkspaceResponse`, review/restore DTOs, etc.;
- `SecurityConfig` — BFF OAuth2 login + modeller RBAC (see Security);
- `GlobalExceptionHandler` — unified error handling;
- the frontend (`src/main/resources/frontend`): React + Vite, including the modeller's visual editors (`ErDiagramEditor`, `DataLineageDiagramEditor`, `DagEditor` — React Flow) and a Vitest unit-test suite.

Depends on: `yarina-domain-api` (shared VCS library: `VcsClient`, its JGit implementation, credentials and per-domain spec/factory), `yarina-common` (shared constants and config DTOs used by the editors — e.g. the `YamlMetadataKind` enum covering the `ERDiagram` and `DataLineageDiagram` kinds, plus the corresponding `ERDiagramDTO` / `DataLineageDiagramDTO`), `yarina-config-rest-client`, `yarina-scheduler-rest-client`, `yarina-state-rest-client`, `yarina-task-proxy-for-spark-rest-client`, `jackson-dataformat-yaml`, `org.eclipse.jgit` (+ SSH), Spring Boot OAuth2 client and resource server.

## API Endpoints

| Method | Path | Description |
|---|---|---|
| GET | `/api/catalog/tree` | Catalog tree: sources → schemas → datasets |
| GET | `/api/catalog/dataset/{keyName}` | Dataset by key name |
| GET | `/api/catalog/dataset/{keyName}/lineage` | Dataset lineage |
| GET | `/api/catalog/dataset/{keyName}/constraints` | Dataset constraints |
| GET | `/api/catalog/script/{key}` | SQL script by key |
| GET | `/api/catalog/dataset/{keyName}/model-script` | Dataset model (DDL) |
| GET | `/api/catalog/datasource/{keyName}` | Data source by key name |
| POST | `/api/schedules` | Schedule instance runs for an interval (`fromDate`, `toDate`, `names`) |
| GET | `/api/schedules/headers` | Schedule headers |
| GET | `/api/schedules/dag/{id}` | Schedule instance DAG by id |
| GET | `/api/services` | Service list with statuses |
| GET | `/api/services/edges` | Service graph edges |
| GET | `/api/services/vertices` | Service graph vertices |
| GET | `/api/spark-proxy/submissions` | Submission list (`limit`, `lastId`, `id`, `status`, `dateFrom`, `dateTo`) |
| GET | `/api/spark-proxy/submissions/{id}/spark-properties` | Submission spark properties |
| POST | `/api/spark-proxy/submissions` | Create a submission |
| GET | `/api/spark-proxy/submissions/status/{submissionId}` | Submission status |
| POST | `/api/spark-proxy/submissions/kill/{submissionId}` | Kill a submission |
| POST | `/api/spark-proxy/submissions/killall` | Kill all submissions |
| POST | `/api/spark-proxy/submissions/clear` | Clear completed submissions |
| POST | `/api/states` | Dataset interval states (`dataSetKeyName`, `fromDate`, `toDate`) |
| GET | `/api/vcs/logs` | GitOps sync log (commits), filters `status`, `commitId`, `domainKeyName` |
| GET | `/api/vcs/objects` | GitOps object log (changed configuration objects), filters `commitId`, `kind`, `from`, `to`, `filePath`, `objectName`, `domainKeyName` |
| GET | `/api/user` | Current user profile (`username`, `name`, `email`, `roles`, `domains`) |
| GET | `/api/vcs/workspaces` | Workspaces of the current user |
| POST | `/api/vcs/workspace` | Open a workspace for a list of `{domain, branch}` selections (creates a working copy) |
| DELETE | `/api/vcs/workspace/{workspaceId}` | Delete the user's workspace |
| GET | `/api/vcs/branches` | Branches grouped by domain: one entry per configured domain repository |
| POST | `/api/vcs/branch` | Create a branch in a domain repository (`domain`, `branch`, `baseBranch`) |
| POST | `/api/vcs/review/{workspaceId}` | Submit the workspace for review, split per domain (`comment`, `commitMessage`); answer `OK` or `NO_CHANGES` |
| POST | `/api/vcs/workspace/{workspaceId}/restore` | Restore a file/folder from the VCS state; the owning domain is resolved from the path |
| GET | `/api/schema` | Form schemas of all configuration kinds |
| GET | `/api/schema/{kind}` | Form schema of one configuration kind |
| GET | `/api/workspaces/{workspaceId}/tree` | File tree of the workspace |
| GET | `/api/workspaces/{workspaceId}/dirs` | Directory list |
| POST | `/api/workspaces/{workspaceId}/dirs` | Create a directory |
| POST | `/api/workspaces/{workspaceId}/dirs/move` | Move a directory |
| DELETE | `/api/workspaces/{workspaceId}/dirs/{path}` | Delete a directory (recursive) |
| POST | `/api/workspaces/{workspaceId}/files` | Create a metadata file (`kind`, `keyName`, `directory`) |
| GET | `/api/workspaces/{workspaceId}/files/{path}` | Read a file (YAML + kind + editability flag) |
| PUT | `/api/workspaces/{workspaceId}/files/{path}` | Save a file (`yaml`, `keyName`) |
| POST | `/api/workspaces/{workspaceId}/files/rename` | Rename a file |
| POST | `/api/workspaces/{workspaceId}/files/move` | Move a file |
| DELETE | `/api/workspaces/{workspaceId}/files/{path}` | Delete a file |
| GET | `/api/workspace/admin/workspaces` | All workspaces (WORKSPACE_ADMIN) |
| DELETE | `/api/workspace/admin/workspaces/{workspaceId}` | Force-delete a workspace (WORKSPACE_ADMIN) |
| GET | `/api/workspace/admin/settings/cleanup-ttl-hours` | Inactive-workspace lifetime TTL (WORKSPACE_ADMIN) |
| PUT | `/api/workspace/admin/settings/cleanup-ttl-hours` | Set the cleanup TTL (WORKSPACE_ADMIN) |
| GET | `/api/workspace/admin/sync-logs` | VCS sync log tail (WORKSPACE_ADMIN) |

## Configuration

Main parameters (`src/main/resources/application.yml`):

```yaml
spring:
  security:
    oauth2:
      client:            # keycloak (authorization-code) + keycloak-internal (client_credentials)
      resourceserver:    # bearer JWT validated against the same realm
    threads:
      virtual:
        enabled: true

yarina:
  client:
    rest:
      config:
        server:
          url: http://localhost:8080
      state:
        server:
          url: http://localhost:8082
      scheduler:
        server:
          url: http://localhost:8081
      task-proxy-for-spark:
        server:
          url: http://localhost:8099
  ui:
    health-check-timeout-ms: 3000
    services:            # list of services with health-check-url / check-type
    vertices: {}         # graph vertices: key → service name
    edges: {}            # graph edges: vertex key → list of targets
  modeller:
    storage:
      type: local        # [local, s3]
      root-directory: /tmp/yarina-workspaces   # when type == local
      s3:                # when type == s3
        endpoint: ...
        bucket: yarina-metadata-workspaces
        access-key: ...
        secret-key: ...
        region: us-east-1
      cleanup-ttl-hours: 4
    auth-strategy: jwt-rbac      # [jwt-rbac, token-exchange]
    session:
      inactivity-minutes: 30
    logging:
      sync-log-capacity: 500
  # Configuration domains (yarina.domains.<name>.git.*), bound by the shared
  # DomainSettings of yarina-domain-api. A domain is active iff its
  # repository-url is set; credentials are per-domain and never merged.
  domains:
    platform:
      git:
        repository-url: git://git-server:9418/platform.git
        branch: main
        credentials:             # per-domain credentials
          auth-type: ssh
          ssh-private-key-path: /keys/platform_ed25519
    analytics:
      git:
        repository-url: git://git-server:9418/analytics.git
        branch: main
```

| Parameter | Description |
|---|---|
| `server.port` | Service port (8080 in the demo compose; empty here = default) |
| `yarina.client.rest.config.server.url` | URL of `yarina-config-svc` |
| `yarina.client.rest.state.server.url` | URL of `yarina-state-svc` |
| `yarina.client.rest.scheduler.server.url` | URL of `yarina-scheduler-svc` |
| `yarina.client.rest.task-proxy-for-spark.server.url` | URL of `yarina-task-proxy-for-spark` |
| `yarina.ui.health-check-timeout-ms` | Service availability check timeout |
| `yarina.ui.services[]` | Service list (name, url, health-check-url, check-type: `http`/`tcp`) |
| `yarina.ui.vertices` / `edges` | Service graph vertices / edges |
| `yarina.modeller.storage.type` | Workspace storage: `local` (default) or `s3` |
| `yarina.modeller.storage.root-directory` | Local workspace root (default `/tmp/yarina-workspaces`) |
| `yarina.modeller.storage.s3.*` | S3 endpoint/bucket/credentials (S3 type) |
| `yarina.modeller.storage.cleanup-ttl-hours` | Lifetime of an inactive workspace (default 4 h) |
| `yarina.domains.<name>.git.repository-url` | Repository URL of the domain (required for a domain to be usable in the branch panel) |
| `yarina.domains.<name>.git.branch` | Main branch of the domain (default `main`); also the review target branch |
| `yarina.domains.<name>.git.credentials.*` | Per-domain git credentials (`auth-type`, `ssh-private-key-path`, `username`, `token`, `password`); no global account, no merge |
| `yarina.modeller.auth-strategy` | `jwt-rbac` (default) or `token-exchange` |
| `yarina.modeller.session.inactivity-minutes` | Workspace session inactivity timeout |
| `yarina.modeller.logging.sync-log-capacity` | In-memory sync log capacity |

## Security

The UI BFF authenticates users through Keycloak (realm `yarina`) using the OAuth 2.0 **authorization code flow** (`oauth2Login()`). After a successful login Spring Security issues the frontend a secure `JSESSIONID` session cookie (`HttpOnly`; under the `prod` profile also `Secure`). State-changing requests are protected from CSRF: the token is exposed to the frontend JS via the `XSRF-TOKEN` cookie and must be sent back in the `X-XSRF-TOKEN` header.

The same realm is also configured as a **resource server** (bearer JWT): service-to-service calls authenticate with the `yarina-internal-client` credentials and validate the JWT against the realm's `certs` endpoint.

Whitelisted paths (no login required): `/healthz`, `/readyz`, `/actuator/**`, `/favicon.ico`. Every other request requires an authenticated session; unauthenticated browser requests are redirected to the Keycloak login page, after login the user returns to `/` (`defaultSuccessUrl`).

### Modeller roles (RBAC)

Access is **domain-driven**. Realm roles carry a domain prefix, `<domain>:<ROLE>`, with `ROLE` in `VIEWER` / `EDITOR` / `CONTROL` (for example `platform:EDITOR`, `analytics:CONTROL`). `VIEWER` is implied by the other two; `EDITOR` and `CONTROL` are orthogonal — `EDITOR` allows config/repo changes but not stopping tasks, `CONTROL` allows stopping tasks but not editing. The global, non-domain `WORKSPACE_ADMIN` role governs the admin surface.

| Realm role | Access |
|---|---|
| `<domain>:VIEWER` | Read that domain's datasets/schedules/VCS logs (BFF filters by domain), open a workspace for reading, delete own workspace |
| `<domain>:EDITOR` | + create branches, open/edit/save/delete/move workspace files, submit reviews, restore — for that domain |
| `<domain>:CONTROL` | + stop/kill Spark submissions |
| `WORKSPACE_ADMIN` (global, no domain) | Everything under `/api/workspace/admin/**`: all workspaces, force delete, cleanup TTL, sync logs |

`SecurityConfig` gates the admin surface (`/api/workspace/admin/**` → `hasRole('WORKSPACE_ADMIN')`) and keeps everything else `authenticated()`. The domain and ownership checks live in the services (`EditorService`/`VcsService`/`ReviewService`/`SparkProxyController`/`AdminWorkspaceService`) and throw `ForbiddenException` (→ `403`); `CatalogService`/`ScheduleService`/`VcsLogService` filter by the user's allowed domains (`DomainAccess`). The frontend only *reflects* the rights: the modeller `readOnly` hint is derived per-domain from `domains` in `GET /api/user` (editable only with `EDITOR` for the domain of the selected file), and it no longer depends on workspace ownership (`workspace.own`), which the backend enforces separately.

### Required settings

| Property / env | Default | Description |
|---|---|---|
| `KEYCLOAK_ISSUER_URI` | `http://keycloak.yarina:8085/realms/yarina` | Realm URL; auth/token/userinfo/certs endpoints are built from it |
| `KEYCLOAK_UI_CLIENT_SECRET` | `super-secret-bff-key-1234567890` | Secret of the `yarina-ui-client` client |
| `YARINA_UI_REDIRECT_URI` | `{baseUrl}/login/oauth2/code/{registrationId}` | OAuth2 redirect URI of the BFF |
| `KEYCLOAK_INTERNAL_CLIENT_SECRET` | `super-secret-internal-key-987654321` | Secret of the `yarina-internal-client` (service-to-service calls) |
| `YARINA_DOMAINS_<NAME>_GIT_REPOSITORY_URL` | — | Repository URL of a configuration domain (relaxed binding of `yarina.domains.<name>.git.repository-url`) |
| `YARINA_DOMAINS_<NAME>_GIT_BRANCH` | `main` | Branch of a configuration domain |
| `YARINA_DOMAINS_<NAME>_GIT_CREDENTIALS_SSH_PRIVATE_KEY_PATH` | — | SSH key of a configuration domain |
| `YARINA_WORKSPACE_STORAGE` / `YARINA_WORKSPACE_ROOT` | `local` / `/tmp/yarina-workspaces` | Workspace storage backend |
| `YARINA_WORKSPACE_TTL_HOURS` | `4` | Inactive-workspace cleanup TTL |
| `server.servlet.session.cookie.name` / `.http-only` | `JSESSIONID` / `true` | Session cookie name and HttpOnly flag |
| `server.servlet.session.cookie.secure` | `false` (`true` in the `prod` profile) | Set `true` when the UI is served over HTTPS |

### Configuring accounts and roles in Keycloak

1. **Deploy Keycloak.** The demo compose runs Keycloak 26.0 with the admin console at `http://localhost:8085` (credentials from `KEYCLOAK_ADMIN`/`KEYCLOAK_ADMIN_PASSWORD`, by default `admin`/`admin_local_password`) and imports the reference realm from `demo/compose/conf_infra/security/realms/yarina-realm.json`. In production use a persistent database and change all default passwords/secrets.
2. **Realm roles.** The `yarina` realm defines the domain-driven modeller roles and the global admin role:
   - `<domain>:VIEWER` — read-only modelling for that domain;
   - `<domain>:EDITOR` — edit configuration documents of that domain;
   - `<domain>:CONTROL` — stop Spark submissions;
   - `WORKSPACE_ADMIN` — admin surface of the modeller.

   The demo realm ships `platform:*`, `analytics:*` and `processing:*` for the configured domains. Roles are delivered to services in the JWT `realm_access.roles` claim and mapped to authorities (`SecurityConfig` reads `realm_access.roles`, `roles`, `resource_access.*.roles`); a role without a `<domain>:` prefix (other than `WORKSPACE_ADMIN`) is ignored.
3. **Client `yarina-ui-client`.** Confidential client (*Standard Flow Enabled*, *Direct Access Grants* off) used by this BFF. Check that:
   - *Valid redirect URIs* contain the externally visible UI address: by default `http://localhost:8080/*` and `http://localhost:8080/login/oauth2/code/keycloak`;
   - *Web Origins* contains the UI origin (`http://localhost:8080`);
   - When deploying on another host/port, add the corresponding redirect URI and web origin and set `YARINA_UI_REDIRECT_URI` accordingly.
4. **Create users.** Admin Console → realm `yarina` → *Users* → *Add user*: fill in username/email/names, then *Credentials* → set password (turn off *Temporary* for a permanent password).
5. **Assign roles.** *Users* → select the user → *Role mapping* → filter *Filter by realm roles* → assign the appropriate domain roles (`<domain>:VIEWER`/`EDITOR`/`CONTROL`) and, for workspace administrators, the global `WORKSPACE_ADMIN` role.
6. **Service account.** The confidential client `yarina-internal-client` (*Service Accounts Enabled*) is used by backend services for service-to-service calls; its secret must match `KEYCLOAK_INTERNAL_CLIENT_SECRET` on every service.

After configuration open the UI - the first request redirects to the Keycloak login page; only users with an account in the `yarina` realm can log in.