# Web UI service (jarina-ui-svc)

The jarina management web UI: a single point for visualizing and administering the services, the data catalog, schedules, dataset states, Spark submissions and the metadata-driven configuration ("Modelling").

## Overview

`jarina-ui-svc` is the service that aggregates data from all the other jarina services and provides a single web interface. For the monitoring sections it is a thin aggregation layer: it calls the other services through their REST clients and returns the result to the frontend. Additionally it hosts the **Modelling** surface — an interactive editor for the configuration documents stored in the Git repositories of the configuration domains (workspaces, branches, review submission), which is served directly by this service. A domain owns its own repository, and a workspace is a set of `(domain, branch)` selections — see [Domains](#domains).

The service consists of two parts:

- **backend** — a Spring Boot application that proxies frontend requests to the jarina services, serves the static frontend and hosts the modeller backend (workspace storage, Git integration, YAML editing, RBAC);
- **frontend** — a single-page React application (Vite) built into `src/main/resources/static` and served by the same service.

UI sections:

- **Services** — the jarina services graph and their status (`UP`/`DOWN`) via health-check.
- **Catalog** — the data catalog tree: sources → schemas → datasets; dataset view (model/DDL, lineage, constraints) and data source view.
- **Schedules** — schedule instance runs for an interval, the schedule instance DAG.
- **SparkJobs** — Spark submissions through `jarina-task-proxy-for-spark`: create, status, kill, kill all, clear.
- **VCS** — the configuration GitOps synchronization log (commits) and object log of `jarina-config-svc`.
- **Modelling** — the metadata-modelling workbench: create a workspace from one or more domain branches, create configuration documents of any supported kind (form view driven by a per-kind schema, raw YAML, or the visual editors — **ER Diagram**, **Data Lineage Diagram**, generic **DAG**), create branches, submit changes for review. Workspaces open in a new browser tab via deep links (`?section=modeller&workspace=<id>`).

## Architecture

For the read-only/monitoring sections the service is a thin aggregation layer: each UI section is served by its own controller that delegates to the REST client of the corresponding jarina service. The service makes no direct database calls for those sections.

The **Modelling** surface is implemented by the UI service itself: it reads and writes configuration documents in workspaces (local FS or S3 object storage), interacts with the configuration domain repositories for that purpose (via jgit or the GitLab/GitHub API), and exposes schema-driven editing metadata. Workspace contents are user-specific working copies that are opened, edited and finally submitted for review; the authoritative store is the Git repository consumed by `jarina-config-svc` (GitOps). A domain repository may be dedicated to a single domain (flat files at the repository root) or host several domains under `domains/<name>/` — both layouts are supported.

External interactions:

- **jarina-config-svc** — data catalog, lineage, constraints, models, schedule headers, GitOps sync logs.
- **jarina-scheduler-svc** — schedule instance runs for an interval, the run DAG.
- **jarina-state-svc** — dataset interval states.
- **jarina-task-proxy-for-spark** — Spark submissions (create, status, kill, clear).
- **Git repositories (GitOps)** — one repository per configuration domain (`jarina.modeller.domains.<name>.repository-url`, with `jarina.modeller.git.remote-url` as the legacy single-repository fallback); the modeller clones the selected `(domain, branch)` pair per workspace, creates branches and submits changes for review.

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

Controllers of the modeller (`org.jarina.ui.modeller.controller`):

```
VcsController    /api/vcs          — workspaces lifecycle, branches, review, restore
EditorController /api/workspaces/{workspaceId} — file-level CRUD inside a workspace
SchemaController /api/schema       — per-kind form schemas (KindSchema) for the editors
AdminController  /api/admin        — admin-only: all workspaces, cleanup TTL, sync logs
```

Service statuses are computed by `HealthChecker`: either an HTTP request to `healthCheckUrl` (type `http`) or a TCP port probe (type `tcp`). The set of services, graph edges and vertices are defined by the `jarina.ui.services/edges/vertices` configuration.

The frontend is built with Vite (the `frontend` directory), and the build output goes to `src/main/resources/static`. In dev mode Vite proxies `/api` to the service (`vite.config.js`).

## Domains

A **domain** is the unit of isolation of the configuration in `jarina-config-svc`: every
domain owns its own Git repository, and every construct loaded from it is stamped with the
domain name as `domainKeyName`. That is an ownership label, not part of the identity -
`keyName` stays global, so the modeller never has to disambiguate a name by domain. The
modeller
does not invent domains — it presents them. A configured domain repository is a root of the
branch panel, and a workspace is a set of `(domain, branch)` selections across those
repositories. See
[config-svc: Domains](../../jarina-config-svc/doc/content_configuration/domains.md).

### One repository (and one branch) per domain

`ModellerProperties` (prefix `jarina.modeller`) declares a `domains` map:

```yaml
jarina:
  modeller:
    domains:                        # <domainKeyName>:
      platform:                     #   repository-url: ...
        repository-url: git://git-server:9418/platform.git
        branch-main: main
      analytics:
        repository-url: git://git-server:9418/analytics.git
        branch-main: main
    git:                            # legacy single-repository fallback
      remote-url: ${JARINA_GIT_URL:}
      branch-main: main
```

Resolution rules:

| Rule | Behaviour |
|---|---|
| Domain list | `domainNames()` is the `domains` keys **sorted alphabetically**; the modeller has no domain tree and no `priority` |
| No `domains` | falls back to the single domain `default` if `git.remote-url` is set; an empty list otherwise |
| Repository URL | per-domain `repository-url`, else legacy `git.remote-url`, else the domain is unusable |
| Main branch | per-domain `branch-main`, else legacy `git.branch-main`, else `main` |
| Unavailable repository | the domain is listed with an **empty** branch list (the branch panel still shows the root), it is not an error |
| Missing repository URL | a hard error: `No repository URL configured for domain <name> (jarina.modeller.domains.<name>.repository-url)` |

`branch-main` is also the **target branch of a review**: a submission is merged back into
`domainBranchMain(domain)` of its own domain.

### The branch panel is a domain tree

`GET /api/vcs/branches` returns one `DomainBranchesResponse` per configured domain -
`{domain, branches[], branchMain}` - and the frontend renders it as a tree with the domains at
the root and their branches hanging off them. `POST /api/vcs/branch` takes
`{domain, branch, baseBranch}`, so a branch is always created in a named domain repository.

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
  technical service account as committer, pushed to the domain branch and — for
  GitLab/GitHub — turned into an MR/PR against `branch-main` of that domain;
- a domain whose diff is empty is skipped (`NO_CHANGES`);
- the aggregate answer is `OK` (with the MR/PR URLs, comma-joined when there are several) or
  `NO_CHANGES` when nothing changed at all;
- **the workspace is deleted only on success** — a failed submission leaves it open;
- a user must own the workspace (or be `ADMIN`), and the whole submission runs under a
  per-workspace lock.

### Domain as a filter of the VCS logs

The VCS sections read the synchronization history of `jarina-config-svc`, which is
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
| `jarina.modeller.domains.<name>.repository-url` | Repository of the domain; required for the domain to appear in the branch panel |
| `jarina.modeller.domains.<name>.branch-main` | Main branch of the domain (default `main`), also the review target |
| `jarina.modeller.git.remote-url` | Legacy fallback, used when `domains` is empty; the domain is named `default` |
| `jarina.modeller.git.branch-main` | Legacy fallback main branch (default `main`) |

Per-domain environment variables are not declared in the shipped `application.yml`; a
deployment supplies them, e.g. as system properties
(`-Djarina.modeller.domains.platform.repository-url=...`, as `demo/compose` does) or via
relaxed-binding environment variables.

## Modules

### jarina-ui-svc

The service itself. Contains:

- the entry point `JarinaUiApplication`;
- monitoring controllers (`controller`): Catalog, Schedule, Services, SparkProxy, State, VcsLog, User;
- monitoring services (`service`): `CatalogService`, `ScheduleService`, `ServicesService`, `SparkProxyService`, `StateService`, `VcsLogService`, `HealthChecker`;
- the `UiServiceProperties` configuration (service list, graph);
- DTOs (`dto`) — frontend representations (`CatalogTreeNodeDTO`, `ConstraintDTO`, `ServiceNodeDTO`, `ScheduleRequestDTO`, `DataSetStateRequestDTO`);
- the modeller **package `org.jarina.ui.modeller`**:
  - controllers (`controller`): `VcsController`, `EditorController`, `SchemaController`, `AdminController`;
  - services (`service`): `VcsService`, `ReviewService`, `SchemaService`, `EditorService`, `YamlEditorService`, `EnumOptionsService`, `SyncLogService`, `AdminWorkspaceService`;
  - VCS integration (`vcs`): `VcsProvider` SPI with `LocalGitVcsProvider`, `GitLabApiVcsProvider`, `GitHubAppVcsProvider`, `DisabledVcsProvider` and a provider factory;
  - workspace storage (`storage`): `WorkspaceStorage` SPI with `LocalFsWorkspaceStorage` and S3 (`S3WorkspaceStorage` + a minimal AWS SigV4 signer), plus `WorkspaceManager`,
    `WorkspaceSeeder` and the cleanup task (`WorkspaceCleanupTask`);
  - auth (`auth`): `ModellerRole` (`VIEWER < EDITOR < ADMIN`), `UserContext`, `ForbiddenException`/`NotFoundException`;
  - DTOs (`dto`): `KindSchema`, `FieldSchema`, `TreeResponse`, `FileContentResponse`, `WorkspaceResponse`, review/restore DTOs, etc.;
- `SecurityConfig` — BFF OAuth2 login + modeller RBAC (see Security);
- `GlobalExceptionHandler` — unified error handling;
- the frontend (`src/main/resources/frontend`): React + Vite, including the modeller's visual editors (`ErDiagramEditor`, `DataLineageDiagramEditor`, `DagEditor` — React Flow) and a Vitest unit-test suite.

Depends on: `jarina-common` (shared constants and config DTOs used by the editors — e.g. the `YamlMetadataKind` enum covering the `ERDiagram` and `DataLineageDiagram` kinds, plus the corresponding `ERDiagramDTO` / `DataLineageDiagramDTO`), `jarina-config-rest-client`, `jarina-scheduler-rest-client`, `jarina-state-rest-client`, `jarina-task-proxy-for-spark-rest-client`, `jackson-dataformat-yaml`, `org.eclipse.jgit` (+ SSH), Spring Boot OAuth2 client and resource server.

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
| GET | `/api/user` | Current user profile (`username`, `roles`, `effectiveRole`) |
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
| GET | `/api/admin/workspaces` | All workspaces (admin) |
| DELETE | `/api/admin/workspaces/{workspaceId}` | Force-delete a workspace (admin) |
| GET | `/api/admin/settings/cleanup-ttl-hours` | Inactive-workspace lifetime TTL (admin) |
| PUT | `/api/admin/settings/cleanup-ttl-hours` | Set the cleanup TTL (admin) |
| GET | `/api/admin/sync-logs` | VCS sync log tail (admin) |

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

jarina:
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
      root-directory: /tmp/jarina-workspaces   # when type == local
      s3:                # when type == s3
        endpoint: ...
        bucket: jarina-metadata-workspaces
        access-key: ...
        secret-key: ...
        region: us-east-1
      cleanup-ttl-hours: 4
    vcs-provider: local-git      # [local-git, gitlab-api, github-app, gerrit-ssh]
    # One repository per configuration domain. When `domains` is empty the legacy
    # `git.*` block below is used as the single domain `default`.
    domains:
      platform:
        repository-url: git://git-server:9418/platform.git
        branch-main: main
      analytics:
        repository-url: git://git-server:9418/analytics.git
        branch-main: main
    git:                        # legacy single-repository fallback
      remote-url: ${JARINA_GIT_URL:}
      branch-main: main
    auth-strategy: jwt-rbac      # [jwt-rbac, token-exchange]
    session:
      inactivity-minutes: 30
    vcs-system-account:          # credentials for git operations
      auth-type: token           # [ssh, token, basic]
      ssh-private-key-path: ...
      username: ...
      token: ...
      password: ...
    github:                      # used when vcs-provider == github-app
      app-id: ...
      app-private-key-path: ...
      installation-id: ...
    logging:
      sync-log-capacity: 500
```

| Parameter | Description |
|---|---|
| `server.port` | Service port (8080 in the demo compose; empty here = default) |
| `jarina.client.rest.config.server.url` | URL of `jarina-config-svc` |
| `jarina.client.rest.state.server.url` | URL of `jarina-state-svc` |
| `jarina.client.rest.scheduler.server.url` | URL of `jarina-scheduler-svc` |
| `jarina.client.rest.task-proxy-for-spark.server.url` | URL of `jarina-task-proxy-for-spark` |
| `jarina.ui.health-check-timeout-ms` | Service availability check timeout |
| `jarina.ui.services[]` | Service list (name, url, health-check-url, check-type: `http`/`tcp`) |
| `jarina.ui.vertices` / `edges` | Service graph vertices / edges |
| `jarina.modeller.storage.type` | Workspace storage: `local` (default) or `s3` |
| `jarina.modeller.storage.root-directory` | Local workspace root (default `/tmp/jarina-workspaces`) |
| `jarina.modeller.storage.s3.*` | S3 endpoint/bucket/credentials (S3 type) |
| `jarina.modeller.storage.cleanup-ttl-hours` | Lifetime of an inactive workspace (default 4 h) |
| `jarina.modeller.vcs-provider` | Git integration: `local-git`, `gitlab-api`, `github-app`, `gerrit-ssh` or `disabled` |
| `jarina.modeller.domains.<name>.repository-url` | Repository URL of the domain (required for a domain to be listed in the branch panel) |
| `jarina.modeller.domains.<name>.branch-main` | Main branch of the domain (default `main`); also the review target branch |
| `jarina.modeller.git.remote-url` | Legacy fallback repository URL, used when `domains` is empty; the domain is then named `default` |
| `jarina.modeller.git.branch-main` | Legacy fallback main branch (default `main`) |
| `jarina.modeller.auth-strategy` | `jwt-rbac` (default) or `token-exchange` |
| `jarina.modeller.session.inactivity-minutes` | Workspace session inactivity timeout |
| `jarina.modeller.vcs-system-account.*` | Credentials used for git operations (`ssh`/`token`/`basic`) |
| `jarina.modeller.github.*` | GitHub App settings (`github-app` provider) |
| `jarina.modeller.logging.sync-log-capacity` | In-memory sync log capacity |

## Security

The UI BFF authenticates users through Keycloak (realm `jarina`) using the OAuth 2.0 **authorization code flow** (`oauth2Login()`). After a successful login Spring Security issues the frontend a secure `JSESSIONID` session cookie (`HttpOnly`; under the `prod` profile also `Secure`). State-changing requests are protected from CSRF: the token is exposed to the frontend JS via the `XSRF-TOKEN` cookie and must be sent back in the `X-XSRF-TOKEN` header.

The same realm is also configured as a **resource server** (bearer JWT): service-to-service calls authenticate with the `jarina-internal-client` credentials and validate the JWT against the realm's `certs` endpoint.

Whitelisted paths (no login required): `/healthz`, `/readyz`, `/actuator/**`, `/favicon.ico`. Every other request requires an authenticated session; unauthenticated browser requests are redirected to the Keycloak login page, after login the user returns to `/` (`defaultSuccessUrl`).

### Modeller roles (RBAC)

Access to the modelling endpoints is granted by the Keycloak realm roles below. A **role hierarchy** is configured so `ADMIN` implies `EDITOR` implies `VIEWER` out of the box:

```
ADMIN > EDITOR > VIEWER
```

| Realm role | Access |
|---|---|
| `VIEWER` | Read workspaces, schema editor (read-only), list branches |
| `EDITOR` | Everything the viewer can do + create branches, open/edit/save/delete workspace files, submit reviews, restore |
| `ADMIN` | Everything the editor can do + Admin surface: all workspaces, force delete, cleanup TTL, sync logs |

Rules are applied in `SecurityConfig` (`/api/admin/**` → ADMIN; `/api/workspaces/**` read → VIEWER, write → EDITOR; `/api/vcs/*` per operation). Inside the modeller, `UserContext`/`ModellerRole` additionally enforce workspace ownership (only the owner can edit their workspace) and the frontend derives `readOnly` from `effectiveRole` (`GET /api/user`).

### Required settings

| Property / env | Default | Description |
|---|---|---|
| `KEYCLOAK_ISSUER_URI` | `http://keycloak.jarina:8085/realms/jarina` | Realm URL; auth/token/userinfo/certs endpoints are built from it |
| `KEYCLOAK_UI_CLIENT_SECRET` | `super-secret-bff-key-1234567890` | Secret of the `jarina-ui-client` client |
| `JARINA_UI_REDIRECT_URI` | `{baseUrl}/login/oauth2/code/{registrationId}` | OAuth2 redirect URI of the BFF |
| `KEYCLOAK_INTERNAL_CLIENT_SECRET` | `super-secret-internal-key-987654321` | Secret of the `jarina-internal-client` (service-to-service calls) |
| `JARINA_VCS_PROVIDER` | `local-git` | Git provider for the modeller |
| `JARINA_GIT_URL` | — | Legacy fallback config repository URL (used when no domain is configured) |
| `JARINA_GIT_BRANCH` | `main` | Legacy fallback main branch |
| `JARINA_VCS_AUTH_TYPE` / `JARINA_VCS_USER` / `JARINA_VCS_TOKEN` / `JARINA_VCS_PASSWORD` / `JARINA_VCS_SSH_KEY_PATH` | — | Git system-account credentials |
| `JARINA_WORKSPACE_STORAGE` / `JARINA_WORKSPACE_ROOT` | `local` / `/tmp/jarina-workspaces` | Workspace storage backend |
| `JARINA_WORKSPACE_TTL_HOURS` | `4` | Inactive-workspace cleanup TTL |
| `server.servlet.session.cookie.name` / `.http-only` | `JSESSIONID` / `true` | Session cookie name and HttpOnly flag |
| `server.servlet.session.cookie.secure` | `false` (`true` in the `prod` profile) | Set `true` when the UI is served over HTTPS |

### Configuring accounts and roles in Keycloak

1. **Deploy Keycloak.** The demo compose runs Keycloak 26.0 with the admin console at `http://localhost:8085` (credentials from `KEYCLOAK_ADMIN`/`KEYCLOAK_ADMIN_PASSWORD`, by default `admin`/`admin_local_password`) and imports the reference realm from `demo/compose/conf_infra/security/realms/jarina-realm.json`. In production use a persistent database and change all default passwords/secrets.
2. **Realm roles.** The `jarina` realm defines general roles `USER` and `ADMIN`, plus the modeller roles:
   - `VIEWER` — read-only modelling;
   - `EDITOR` — edit configuration documents;
   - `ADMIN` — admin surface of the modeller.

   Roles are delivered to services in the JWT `realm_access.roles` claim and mapped to `ROLE_…` authorities (`SecurityConfig` reads `realm_access.roles`, `roles`, `resource_access.*.roles`).
3. **Client `jarina-ui-client`.** Confidential client (*Standard Flow Enabled*, *Direct Access Grants* off) used by this BFF. Check that:
   - *Valid redirect URIs* contain the externally visible UI address: by default `http://localhost:8080/*` and `http://localhost:8080/login/oauth2/code/keycloak`;
   - *Web Origins* contains the UI origin (`http://localhost:8080`);
   - When deploying on another host/port, add the corresponding redirect URI and web origin and set `JARINA_UI_REDIRECT_URI` accordingly.
4. **Create users.** Admin Console → realm `jarina` → *Users* → *Add user*: fill in username/email/names, then *Credentials* → set password (turn off *Temporary* for a permanent password).
5. **Assign roles.** *Users* → select the user → *Role mapping* → filter *Filter by realm roles* → assign `USER` and/or `ADMIN` and the needed `*` role by clicking *Assign*. The role hierarchy makes `ADMIN` cover `EDITOR`/`VIEWER` automatically.
6. **Service account.** The confidential client `jarina-internal-client` (*Service Accounts Enabled*) is used by backend services for service-to-service calls; its secret must match `KEYCLOAK_INTERNAL_CLIENT_SECRET` on every service.

After configuration open the UI - the first request redirects to the Keycloak login page; only users with an account in the `jarina` realm can log in.