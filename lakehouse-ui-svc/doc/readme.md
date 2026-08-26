# Web UI service (lakehouse-ui-svc)

The lakehouse management web UI: a single point for visualizing and administering the services, the data catalog, schedules, dataset states and Spark submissions.

## Overview

`lakehouse-ui-svc` is the service that aggregates data from all the other lakehouse services and provides a single web interface. It holds no state of its own and does not run lakehouse business logic — it only calls the other services through their REST clients and returns the result to the frontend.

The service consists of two parts:

- **backend** — a Spring Boot application that proxies frontend requests to the lakehouse services and serves the static frontend;
- **frontend** — a single-page React application (Vite) built into `src/main/resources/static` and served by the same service.

UI sections:

- **Services** — the lakehouse services graph and their status (`UP`/`DOWN`) via health-check.
- **Catalog** — the data catalog tree: sources → schemas → datasets; dataset view (model/DDL, lineage, constraints) and data source view.
- **Schedules** — schedule instance runs for an interval, the schedule instance DAG.
- **SparkJobs** — Spark submissions through `lakehouse-task-proxy-for-spark`: create, status, kill, kill all, clear.

## Architecture

The service is a thin aggregation layer: each UI section is served by its own controller that delegates to the REST client of the corresponding lakehouse service. The service makes no direct database calls.

External interactions:

- **lakehouse-config-svc** — data catalog, lineage, constraints, models, schedule headers.
- **lakehouse-scheduler-svc** — schedule instance runs for an interval, the run DAG.
- **lakehouse-state-svc** — dataset interval states.
- **lakehouse-task-proxy-for-spark** — Spark submissions (create, status, kill, clear).

Controllers:

```
CatalogController   /api/catalog      — catalog tree, datasets, lineage, constraints, scripts
ScheduleController  /api/schedules    — schedule runs, headers, DAG
ServicesController  /api/services     — services graph and status
SparkProxyController /api/spark-proxy — Spark submissions
StateController     /api/states       — dataset interval states
```

Service statuses are computed by `HealthChecker`: either an HTTP request to `healthCheckUrl` (type `http`) or a TCP port probe (type `tcp`). The set of services, graph edges and vertices are defined by the `lakehouse.ui.services/edges/vertices` configuration.

The frontend is built with Vite (the `frontend` directory), and the build output goes to `src/main/resources/static`. In dev mode Vite proxies `/api` to the service (`vite.config.js`).

## Modules

### lakehouse-ui-svc

The service itself. Contains:

- the entry point `LakehouseUiApplication`;
- controllers (`controller`): Catalog, Schedule, Services, SparkProxy, State;
- services (`service`): `CatalogService`, `ScheduleService`, `ServicesService`, `SparkProxyService`, `StateService`, `HealthChecker`;
- the `UiServiceProperties` configuration (service list, graph);
- DTOs (`dto`) — frontend representations (`CatalogTreeNodeDTO`, `ConstraintDTO`, `ServiceNodeDTO`, `ScheduleRequestDTO`, `DataSetStateRequestDTO`);
- `GlobalExceptionHandler` — unified error handling;
- the frontend (`src/main/resources/frontend`): React + Vite.

Depends on the REST clients: `lakehouse-config-rest-client`, `lakehouse-scheduler-rest-client`, `lakehouse-state-rest-client`, `lakehouse-task-proxy-for-spark-rest-client`.

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

## Configuration

Main parameters (`src/main/resources/application.yml`):

```yaml
server:
  port: 8084
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
    services:
      - name: lakehouse-config-svc
        url: http://localhost:8080
        health-check-url: http://localhost:8080/healthz
      - name: postgres-db
        url: http://localhost:5432
        health-check-url: localhost:5432
        check-type: tcp
    vertices:
      config-svc: lakehouse-config-svc
      ...
    edges:
      config-svc:
        - state-svc
        - scheduler-svc
```

| Parameter | Description |
|---|---|
| `server.port` | Service port |
| `lakehouse.client.rest.config.server.url` | URL of `lakehouse-config-svc` |
| `lakehouse.client.rest.state.server.url` | URL of `lakehouse-state-svc` |
| `lakehouse.client.rest.scheduler.server.url` | URL of `lakehouse-scheduler-svc` |
| `lakehouse.client.rest.task-proxy-for-spark.server.url` | URL of `lakehouse-task-proxy-for-spark` |
| `lakehouse.ui.health-check-timeout-ms` | Service availability check timeout |
| `lakehouse.ui.services[].name` | Service name shown in the UI |
| `lakehouse.ui.services[].url` | Service URL |
| `lakehouse.ui.services[].health-check-url` | Health-check URL (defaults to `url`) |
| `lakehouse.ui.services[].check-type` | Check type: `http` (default) or `tcp` |
| `lakehouse.ui.vertices` | Graph vertices: key → service name |
| `lakehouse.ui.edges` | Graph edges: vertex key → list of target vertices |

## Security

The UI BFF authenticates users through Keycloak (realm `lakehouse`) using the OAuth 2.0 **authorization code flow** (`oauth2Login()`). After a successful login Spring Security issues the frontend a secure `JSESSIONID` session cookie (`HttpOnly`; under the `prod` profile also `Secure`). State-changing requests are protected from CSRF: the token is exposed to the frontend JS via the `XSRF-TOKEN` cookie and must be sent back in the `X-XSRF-TOKEN` header.

Whitelisted paths (no login required): `/healthz`, `/readyz`, `/actuator/**`, `/favicon.ico`. Every other request requires an authenticated session; unauthenticated browser requests are redirected to the Keycloak login page, after login the user returns to `/` (`defaultSuccessUrl`).

### Required settings

| Property / env | Default | Description |
|---|---|---|
| `KEYCLOAK_ISSUER_URI` | `http://lakehouse-auth-svc:8080/realms/lakehouse` | Realm URL; auth/token/userinfo/certs endpoints are built from it |
| `KEYCLOAK_UI_CLIENT_SECRET` | `super-secret-bff-key-1234567890` | Secret of the `lakehouse-ui-client` client |
| `LAKEHOUSE_UI_REDIRECT_URI` | `{baseUrl}/login/oauth2/code/{registrationId}` | OAuth2 redirect URI of the BFF |
| `server.servlet.session.cookie.name` / `.http-only` | `JSESSIONID` / `true` | Session cookie name and HttpOnly flag |
| `server.servlet.session.cookie.secure` | `false` (`true` in the `prod` profile) | Set `true` when the UI is served over HTTPS |

### Configuring accounts and roles in Keycloak

1. **Deploy Keycloak.** The demo compose runs Keycloak 26.0 with the admin console at `http://localhost:8085` (credentials from `KEYCLOAK_ADMIN`/`KEYCLOAK_ADMIN_PASSWORD`, by default `admin`/`admin_local_password`) and imports the reference realm from `demo/compose/conf_infra/security/realms/lakehouse-realm.json`. In production use a persistent database and change all default passwords/secrets.
2. **Realm roles.** The `lakehouse` realm defines two realm roles:
   - `USER` - regular ecosystem user;
   - `ADMIN` - administrator with full access.

   Roles are delivered to services in the JWT `realm_access.roles` claim and mapped there to `ROLE_USER`/`ROLE_ADMIN` authorities (`KeycloakRoleConverter`).
3. **Client `lakehouse-ui-client`.** Confidential client (*Standard Flow Enabled*, *Direct Access Grants* off) used by this BFF. Check that:
   - *Valid redirect URIs* contain the externally visible UI address: by default `http://localhost:8080/*` and `http://localhost:8080/login/oauth2/code/keycloak`;
   - *Web Origins* contains the UI origin (`http://localhost:8080`);
   - When deploying on another host/port, add the corresponding redirect URI and web origin and set `LAKEHOUSE_UI_REDIRECT_URI` accordingly.
4. **Create users.** Admin Console → realm `lakehouse` → *Users* → *Add user*: fill in username/email/names, then *Credentials* → set password (turn off *Temporary* for a permanent password).
5. **Assign roles.** *Users* → select the user → *Role mapping* → filter *Filter by realm roles* → assign `USER` and/or `ADMIN` by clicking *Assign*.
6. **Service account.** The confidential client `lakehouse-internal-client` (*Service Accounts Enabled*) is used by backend services for service-to-service calls; its secret must match `KEYCLOAK_INTERNAL_CLIENT_SECRET` on every service.

After configuration open the UI - the first request redirects to the Keycloak login page; only users with an account in the `lakehouse` realm can log in.