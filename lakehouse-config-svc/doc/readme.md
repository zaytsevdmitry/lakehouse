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