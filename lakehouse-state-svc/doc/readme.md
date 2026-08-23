# lakehouse-state-svc

The lakehouse service for storing and managing dataset interval states. For each dataset it maintains a coverage of the time series with intervals and their states (LOCKED/SUCCESS), protects intervals from conflicting changes, and makes it possible to find "gaps" - intervals that have not been processed yet or have been processed unsuccessfully.

## Overview

`lakehouse-state-svc` is responsible for:

- **Storing interval states** - for each dataset (`dataSetKeyName`) it stores records about time intervals with the state `LOCKED` or `SUCCESS`.
- **Writing a state** - when a new interval is written, existing intersecting intervals are rebuilt (merge); duplicates are prevented by the unique constraint `(dataSetKeyName, intervalStartDateTime, intervalEndDateTime)`.
- **Conflict protection** - if a new `lockSource` does not match the one already recorded for unclosed (non-SUCCESS) intervals, the write is rejected with a `LockedStateRuntimeException`.
- **Finding "gaps"** - retrieving the list of intervals without the `SUCCESS` state (not processed, or with the `LOCKED` state) within a given time window. It serves as a signal that tasks need to be launched (used by the scheduler/executors).
- **State output** - retrieving all dataset states within a given interval.

## Architecture

```
┌──────────────────────────┐        ┌──────────────────────────────────────┐
│ lakehouse-scheduler-svc  │  REST  │        lakehouse-state-svc           │
│ task-executor-svc        │ ─────▶ │                                      │
│ (via state-rest-client)  │        │  ┌────────────────────────────────┐  │
└──────────────────────────┘        │  │ StateController               │  │
                                    │  │  POST /state/dataset/wrong    │  │
                                    │  │  PUT  /state/dataset          │  │
                                    │  │  GET  /state/dataset          │  │
                                    │  └───────────────┬────────────────┘  │
                                    │                  ▼                   │
                                    │  ┌────────────────────────────────┐  │
                                    │  │ StateService                  │  │
                                    │  │  checkForPossibleChanges      │  │
                                    │  │  save (merge)                 │  │
                                    │  │  getStatesByDataSetAndInterval│  │
                                    │  │  getWrongStateByInterval      │  │
                                    │  └───────────────┬────────────────┘  │
                                    │                  ▼                   │
                                    │  ┌────────────────────────────────┐  │
                                    │  │ StateFactory  (merge,          │  │
                                    │  │  sortStates, leftRightPad,     │  │
                                    │  │  feelGaps) / StateMapper       │  │
                                    │  └───────────────┬────────────────┘  │
                                    │                  ▼                   │
                                    │  ┌────────────────────────────────┐  │
                                    │  │ PostgreSQL                    │  │
                                    │  │ (schema lakehouse_state)      │  │
                                    │  └────────────────────────────────┘  │
                                    └──────────────────────────────────────┘
```

- **StateController** - REST API (see [restapi.md](restapi.md)): writing a state, retrieving states, finding "gaps".
- **StateService** - business logic: checking for possible changes (conflict protection), saving with interval rebuilding, retrieving states and "gaps".
- **StateFactory** - interval algorithms: `merge` (rebuilding intersecting intervals), `sortStates`, `leftRightPad` and `feelGaps` (filling gaps at the borders and inside the window), `getForRemove`.
- **StateMapper** - converting the `DataSetState` entity to DTO (`DataSetStateDTO`) and back.
- **DataSetStateRepository (JPA)** - persistence, finding interval intersections (`findIntersection`).

The dataset interval state model is described in [state_model/state-models.MD](state_model/state-models.MD).

## Modules

### lakehouse-state-svc

Spring Boot application implementing the state service. Entry point: `org.lakehouse.state.LakehouseStateApplication`. Runs on port **8082**.

### lakehouse-state-rest-client

Java client (`StateRestClientApi`/`StateRestClientApiImpl`) for accessing `lakehouse-state-svc` from other services (task-executor-svc, scheduler-svc and others). Performs typed requests to the `/v1_0/state/...` endpoints via `RestClientHelper`. The base URL is set by the `lakehouse.client.rest.state.server.url` property.

## API Endpoints

The service runs on port **8082**; all endpoints start with `/v1_0`:

| Method | Endpoint                        | Purpose                                       |
|:-------|:--------------------------------|:----------------------------------------------|
| POST   | `/v1_0/state/dataset/wrong`     | Retrieving "gaps" - intervals without the SUCCESS status in a given window |
| PUT    | `/v1_0/state/dataset`           | Writing an interval state (rebuilding intersections) |
| GET    | `/v1_0/state/dataset`           | Retrieving dataset states within a given interval |

Request bodies - `DataSetIntervalDTO` (`dataSetKeyName`, `intervalStartDateTime`, `intervalEndDateTime`); writing a state - `DataSetStateDTO` (additionally `status` [LOCKED/SUCCESS], `lockSource`); the "gaps" response - `DataSetWrongStateResponseDTO` with the `wrongStates` list.

## Configuration

Application parameters (port, datasource, JPA, health endpoints) are described in [appconf/service_configuration.md](appconf/service_configuration.md).

## Security

`lakehouse-state-svc` is protected with OAuth 2.0 / OIDC and uses Keycloak as the identity provider (realm `lakehouse`). Spring Security is configured as an **OAuth2 resource server**: every request must carry a valid JWT issued by the realm, otherwise the service returns `401`.

### Authentication

- **User requests** (UI BFF, CLI, direct API calls) - the JWT is validated via `spring.security.oauth2.resourceserver.jwt.issuer-uri`. Roles from the `realm_access.roles` claim are converted to `ROLE_<NAME>` authorities by `KeycloakRoleConverter` and can be used with `@PreAuthorize`.
- **Service-to-service calls** (`BearerTokenClientHttpRequestInterceptor`) - when a request originates from a background task (no user JWT in the `SecurityContext`), the outgoing `RestClient` obtains a `client_credentials` token through the `OAuth2AuthorizedClientManager` using the `keycloak-internal` registration (client `lakehouse-internal-client`) and attaches it as `Authorization: Bearer`. When a user JWT is present, it is propagated unchanged.
- Security can be switched off entirely with `lakehouse.security.enabled=false` (all requests become anonymous).

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

Whitelisted paths (no token required): `/healthz`, `/readyz`, `/actuator/**`, `/v3/api-docs/**`, `/swagger-ui/**`.

### Keycloak realm

The `lakehouse` realm must contain:

- **`lakehouse-internal-client`** - confidential client with *Service Accounts Enabled* (service-to-service calls);
- Realm roles `USER` / `ADMIN` (optional, used with `@PreAuthorize`).

The reference realm import is in `demo/compose/conf_infra/security/realms/lakehouse-realm.json`.