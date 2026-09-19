# Web UI service (lakehouse-ui-svc)

The lakehouse management web UI: a single point for visualizing and administering the services, the data catalog, schedules, dataset states, Spark submissions and the metadata-driven configuration ("Modelling").

## Overview

`lakehouse-ui-svc` is the service that aggregates data from all the other lakehouse services and provides a single web interface. For the monitoring sections it is a thin aggregation layer: it calls the other services through their REST clients and returns the result to the frontend. Additionally it hosts the **Modelling** surface — an interactive editor for the configuration documents stored in a central Git repository (workspaces, branches, review submission), which is served directly by this service.

The service consists of two parts:

- **backend** — a Spring Boot application that proxies frontend requests to the lakehouse services, serves the static frontend and hosts the modeller backend (workspace storage, Git integration, YAML editing, RBAC);
- **frontend** — a single-page React application (Vite) built into `src/main/resources/static` and served by the same service.

UI sections:

- **Services** — the lakehouse services graph and their status (`UP`/`DOWN`) via health-check.
- **Catalog** — the data catalog tree: sources → schemas → datasets; dataset view (model/DDL, lineage, constraints) and data source view.
- **Schedules** — schedule instance runs for an interval, the schedule instance DAG.
- **SparkJobs** — Spark submissions through `lakehouse-task-proxy-for-spark`: create, status, kill, kill all, clear.
- **VCS** — the configuration GitOps synchronization log (commits) and object log of `lakehouse-config-svc`.
- **Modelling** — the metadata-modelling workbench: create a workspace from a Git branch, create configuration documents of any supported kind (form view driven by a per-kind schema, raw YAML, or the visual editors — **ER Diagram**, **Data Lineage Diagram**, generic **DAG**), create branches, submit changes for review. Workspaces open in a new browser tab via deep links (`?section=modeller&workspace=<id>`).

## Architecture

For the read-only/monitoring sections the service is a thin aggregation layer: each UI section is served by its own controller that delegates to the REST client of the corresponding lakehouse service. The service makes no direct database calls for those sections.

The **Modelling** surface is implemented by the UI service itself: it reads and writes configuration documents in workspaces (local FS or S3 object storage), interacts with the central Git repository for that purpose (via jgit or the GitLab/GitHub API), and exposes schema-driven editing metadata. Workspace contents are user-specific working copies that are opened, edited and finally submitted for review; the authoritative store is the Git repository consumed by `lakehouse-config-svc` (GitOps).

External interactions:

- **lakehouse-config-svc** — data catalog, lineage, constraints, models, schedule headers, GitOps sync logs.
- **lakehouse-scheduler-svc** — schedule instance runs for an interval, the run DAG.
- **lakehouse-state-svc** — dataset interval states.
- **lakehouse-task-proxy-for-spark** — Spark submissions (create, status, kill, clear).
- **Git repository (GitOps)** — central config repository (via `LAKEHOUSE_GIT_URL`); the modeller clones it per workspace, creates branches and submits changes for review.

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

Controllers of the modeller (`org.lakehouse.ui.modeller.controller`):

```
VcsController    /api/vcs          — workspaces lifecycle, branches, review, restore
EditorController /api/workspaces/{workspaceId} — file-level CRUD inside a workspace
SchemaController /api/schema       — per-kind form schemas (KindSchema) for the editors
AdminController  /api/admin        — admin-only: all workspaces, cleanup TTL, sync logs
```

Service statuses are computed by `HealthChecker`: either an HTTP request to `healthCheckUrl` (type `http`) or a TCP port probe (type `tcp`). The set of services, graph edges and vertices are defined by the `lakehouse.ui.services/edges/vertices` configuration.

The frontend is built with Vite (the `frontend` directory), and the build output goes to `src/main/resources/static`. In dev mode Vite proxies `/api` to the service (`vite.config.js`).

## Modules

### lakehouse-ui-svc

The service itself. Contains:

- the entry point `LakehouseUiApplication`;
- monitoring controllers (`controller`): Catalog, Schedule, Services, SparkProxy, State, VcsLog, User;
- monitoring services (`service`): `CatalogService`, `ScheduleService`, `ServicesService`, `SparkProxyService`, `StateService`, `VcsLogService`, `HealthChecker`;
- the `UiServiceProperties` configuration (service list, graph);
- DTOs (`dto`) — frontend representations (`CatalogTreeNodeDTO`, `ConstraintDTO`, `ServiceNodeDTO`, `ScheduleRequestDTO`, `DataSetStateRequestDTO`);
- the modeller **package `org.lakehouse.ui.modeller`**:
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

Depends on: `lakehouse-common` (shared constants and config DTOs used by the editors — e.g. the `YamlMetadataKind` enum covering the `ERDiagram` and `DataLineageDiagram` kinds, plus the corresponding `ERDiagramDTO` / `DataLineageDiagramDTO`), `lakehouse-config-rest-client`, `lakehouse-scheduler-rest-client`, `lakehouse-state-rest-client`, `lakehouse-task-proxy-for-spark-rest-client`, `jackson-dataformat-yaml`, `org.eclipse.jgit` (+ SSH), Spring Boot OAuth2 client and resource server.

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
| GET | `/api/vcs/logs` | GitOps sync log (commits) with filters |
| GET | `/api/vcs/objects` | GitOps object log (changed configuration objects) |
| GET | `/api/user` | Current user profile (`username`, `roles`, `effectiveRole`) |
| GET | `/api/vcs/workspaces` | Workspaces of the current user |
| POST | `/api/vcs/workspace` | Open a workspace for a branch (creates a working copy) |
| DELETE | `/api/vcs/workspace/{workspaceId}` | Delete the user's workspace |
| GET | `/api/vcs/branches` | Available branches of the config repository |
| POST | `/api/vcs/branch` | Create a branch (`branch`, `baseBranch`) |
| POST | `/api/vcs/review/{workspaceId}` | Submit the workspace for review (commit + comment) |
| POST | `/api/vcs/workspace/{workspaceId}/restore` | Restore a file/folder from the VCS state |
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

lakehouse:
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
      root-directory: /tmp/lakehouse-workspaces   # when type == local
      s3:                # when type == s3
        endpoint: ...
        bucket: lakehouse-metadata-workspaces
        access-key: ...
        secret-key: ...
        region: us-east-1
      cleanup-ttl-hours: 4
    vcs-provider: local-git      # [local-git, gitlab-api, github-app, gerrit-ssh, disabled]
    git:
      remote-url: ${LAKEHOUSE_GIT_URL}
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
| `lakehouse.client.rest.config.server.url` | URL of `lakehouse-config-svc` |
| `lakehouse.client.rest.state.server.url` | URL of `lakehouse-state-svc` |
| `lakehouse.client.rest.scheduler.server.url` | URL of `lakehouse-scheduler-svc` |
| `lakehouse.client.rest.task-proxy-for-spark.server.url` | URL of `lakehouse-task-proxy-for-spark` |
| `lakehouse.ui.health-check-timeout-ms` | Service availability check timeout |
| `lakehouse.ui.services[]` | Service list (name, url, health-check-url, check-type: `http`/`tcp`) |
| `lakehouse.ui.vertices` / `edges` | Service graph vertices / edges |
| `lakehouse.modeller.storage.type` | Workspace storage: `local` (default) or `s3` |
| `lakehouse.modeller.storage.root-directory` | Local workspace root (default `/tmp/lakehouse-workspaces`) |
| `lakehouse.modeller.storage.s3.*` | S3 endpoint/bucket/credentials (S3 type) |
| `lakehouse.modeller.storage.cleanup-ttl-hours` | Lifetime of an inactive workspace (default 4 h) |
| `lakehouse.modeller.vcs-provider` | Git integration: `local-git`, `gitlab-api`, `github-app`, `gerrit-ssh` or `disabled` |
| `lakehouse.modeller.git.remote-url` | Central config repository URL |
| `lakehouse.modeller.git.branch-main` | Main branch name |
| `lakehouse.modeller.auth-strategy` | `jwt-rbac` (default) or `token-exchange` |
| `lakehouse.modeller.session.inactivity-minutes` | Workspace session inactivity timeout |
| `lakehouse.modeller.vcs-system-account.*` | Credentials used for git operations (`ssh`/`token`/`basic`) |
| `lakehouse.modeller.github.*` | GitHub App settings (`github-app` provider) |
| `lakehouse.modeller.logging.sync-log-capacity` | In-memory sync log capacity |

## Security

The UI BFF authenticates users through Keycloak (realm `lakehouse`) using the OAuth 2.0 **authorization code flow** (`oauth2Login()`). After a successful login Spring Security issues the frontend a secure `JSESSIONID` session cookie (`HttpOnly`; under the `prod` profile also `Secure`). State-changing requests are protected from CSRF: the token is exposed to the frontend JS via the `XSRF-TOKEN` cookie and must be sent back in the `X-XSRF-TOKEN` header.

The same realm is also configured as a **resource server** (bearer JWT): service-to-service calls authenticate with the `lakehouse-internal-client` credentials and validate the JWT against the realm's `certs` endpoint.

Whitelisted paths (no login required): `/healthz`, `/readyz`, `/actuator/**`, `/favicon.ico`. Every other request requires an authenticated session; unauthenticated browser requests are redirected to the Keycloak login page, after login the user returns to `/` (`defaultSuccessUrl`).

### Modeller roles (RBAC)

Access to the modelling endpoints is granted by the Keycloak realm roles below. A **role hierarchy** is configured so `ADMIN` implies `EDITOR` implies `VIEWER` out of the box:

```
LAKEHOUSE_MODELLER_ADMIN > LAKEHOUSE_MODELLER_EDITOR > LAKEHOUSE_MODELLER_VIEWER
```

| Realm role | Access |
|---|---|
| `LAKEHOUSE_MODELLER_VIEWER` | Read workspaces, schema editor (read-only), list branches |
| `LAKEHOUSE_MODELLER_EDITOR` | Everything the viewer can do + create branches, open/edit/save/delete workspace files, submit reviews, restore |
| `LAKEHOUSE_MODELLER_ADMIN` | Everything the editor can do + Admin surface: all workspaces, force delete, cleanup TTL, sync logs |

Rules are applied in `SecurityConfig` (`/api/admin/**` → ADMIN; `/api/workspaces/**` read → VIEWER, write → EDITOR; `/api/vcs/*` per operation). Inside the modeller, `UserContext`/`ModellerRole` additionally enforce workspace ownership (only the owner can edit their workspace) and the frontend derives `readOnly` from `effectiveRole` (`GET /api/user`).

### Required settings

| Property / env | Default | Description |
|---|---|---|
| `KEYCLOAK_ISSUER_URI` | `http://keycloak.lakehouse:8085/realms/lakehouse` | Realm URL; auth/token/userinfo/certs endpoints are built from it |
| `KEYCLOAK_UI_CLIENT_SECRET` | `super-secret-bff-key-1234567890` | Secret of the `lakehouse-ui-client` client |
| `LAKEHOUSE_UI_REDIRECT_URI` | `{baseUrl}/login/oauth2/code/{registrationId}` | OAuth2 redirect URI of the BFF |
| `KEYCLOAK_INTERNAL_CLIENT_SECRET` | `super-secret-internal-key-987654321` | Secret of the `lakehouse-internal-client` (service-to-service calls) |
| `LAKEHOUSE_VCS_PROVIDER` | `local-git` | Git provider for the modeller |
| `LAKEHOUSE_GIT_URL` | — | Central config repository URL |
| `LAKEHOUSE_GIT_BRANCH` | `main` | Main branch |
| `LAKEHOUSE_VCS_AUTH_TYPE` / `LAKEHOUSE_VCS_USER` / `LAKEHOUSE_VCS_TOKEN` / `LAKEHOUSE_VCS_PASSWORD` / `LAKEHOUSE_VCS_SSH_KEY_PATH` | — | Git system-account credentials |
| `LAKEHOUSE_WORKSPACE_STORAGE` / `LAKEHOUSE_WORKSPACE_ROOT` | `local` / `/tmp/lakehouse-workspaces` | Workspace storage backend |
| `LAKEHOUSE_WORKSPACE_TTL_HOURS` | `4` | Inactive-workspace cleanup TTL |
| `server.servlet.session.cookie.name` / `.http-only` | `JSESSIONID` / `true` | Session cookie name and HttpOnly flag |
| `server.servlet.session.cookie.secure` | `false` (`true` in the `prod` profile) | Set `true` when the UI is served over HTTPS |

### Configuring accounts and roles in Keycloak

1. **Deploy Keycloak.** The demo compose runs Keycloak 26.0 with the admin console at `http://localhost:8085` (credentials from `KEYCLOAK_ADMIN`/`KEYCLOAK_ADMIN_PASSWORD`, by default `admin`/`admin_local_password`) and imports the reference realm from `demo/compose/conf_infra/security/realms/lakehouse-realm.json`. In production use a persistent database and change all default passwords/secrets.
2. **Realm roles.** The `lakehouse` realm defines general roles `USER` and `ADMIN`, plus the modeller roles:
   - `LAKEHOUSE_MODELLER_VIEWER` — read-only modelling;
   - `LAKEHOUSE_MODELLER_EDITOR` — edit configuration documents;
   - `LAKEHOUSE_MODELLER_ADMIN` — admin surface of the modeller.

   Roles are delivered to services in the JWT `realm_access.roles` claim and mapped to `ROLE_…` authorities (`SecurityConfig` reads `realm_access.roles`, `roles`, `resource_access.*.roles`).
3. **Client `lakehouse-ui-client`.** Confidential client (*Standard Flow Enabled*, *Direct Access Grants* off) used by this BFF. Check that:
   - *Valid redirect URIs* contain the externally visible UI address: by default `http://localhost:8080/*` and `http://localhost:8080/login/oauth2/code/keycloak`;
   - *Web Origins* contains the UI origin (`http://localhost:8080`);
   - When deploying on another host/port, add the corresponding redirect URI and web origin and set `LAKEHOUSE_UI_REDIRECT_URI` accordingly.
4. **Create users.** Admin Console → realm `lakehouse` → *Users* → *Add user*: fill in username/email/names, then *Credentials* → set password (turn off *Temporary* for a permanent password).
5. **Assign roles.** *Users* → select the user → *Role mapping* → filter *Filter by realm roles* → assign `USER` and/or `ADMIN` and the needed `LAKEHOUSE_MODELLER_*` role by clicking *Assign*. The role hierarchy makes `ADMIN` cover `EDITOR`/`VIEWER` automatically.
6. **Service account.** The confidential client `lakehouse-internal-client` (*Service Accounts Enabled*) is used by backend services for service-to-service calls; its secret must match `KEYCLOAK_INTERNAL_CLIENT_SECRET` on every service.

After configuration open the UI - the first request redirects to the Keycloak login page; only users with an account in the `lakehouse` realm can log in.