# Security

How security is organized in the lakehouse ecosystem. All services use [Keycloak](https://www.keycloak.org/) as a single identity provider (OAuth 2.0 / OpenID Connect, JWT tokens).

## Overview

The security model is divided into four areas:

| Model | Components | Mechanism |
|:------|:-----------|:----------|
| Service-to-service | lakehouse-config-svc, lakehouse-state-svc, lakehouse-task-executor-svc, lakehouse-task-proxy-for-spark, lakehouse-scheduler-svc | OAuth2 Resource Server (JWT validation) + `client_credentials` / token relay for outgoing calls |
| User-facing | lakehouse-ui-svc (BFF) | OAuth2 Login (authorization code flow) + HTTP session + CSRF |
| Spark applications | lakehouse-task-executor-spark-dq-app, lakehouse-task-executor-spark-dataset-app | OAuth2 Client (`client_credentials`) for callbacks to backend services |
| Data source credentials | lakehouse-task-executor-svc (JDBC path), Spark drivers/executors | Runtime secret resolution from OpenBao/Vault or Yandex Cloud Lockbox (credential providers) - no plaintext passwords in configuration |

Keycloak realm: **`lakehouse`** (realm import file: `demo/compose/conf_infra/security/realms/lakehouse-realm.json`).

Secret material (database passwords, S3 keys) is not stored in configuration files at all - it is resolved at runtime
from secret stores (OpenBao/Vault or Yandex Cloud Lockbox), see [section 4](#4-secrets-for-data-connections-credential-providers).

---

## 1. Service-to-service security

The five backend services are configured identically as **OAuth2 Resource Servers**:

- lakehouse-config-svc
- lakehouse-state-svc
- lakehouse-task-executor-svc
- lakehouse-task-proxy-for-spark
- lakehouse-scheduler-svc

Each service has its own `SecurityConfig` (`@EnableWebSecurity`, `@EnableMethodSecurity`) with the same filter chain logic.

### Inbound requests

1. Every request must carry a valid JWT issued by the Keycloak realm: `Authorization: Bearer <token>`.
2. The token signature is validated against the Keycloak JWKS endpoint; the issuer is taken from
   `spring.security.oauth2.resourceserver.jwt.issuer-uri`.
3. Roles are extracted from the JWT claim `realm_access.roles` by `KeycloakRoleConverter`
   (module `lakehouse-common-health`, package `org.lakehouse.security`): each role becomes a Spring authority
   `ROLE_<NAME>` (e.g. `ADMIN` -> `ROLE_ADMIN`). Standard scopes are mapped to `SCOPE_*` authorities.
4. CSRF is disabled - the services are stateless resource servers.
5. Security can be switched off entirely with the property `lakehouse.security.enabled=false`
   (all requests become anonymous). Default: `true`.

Whitelisted paths (no token required):

| Path | Purpose |
|:-----|:--------|
| `/healthz`, `/readyz` | Kubernetes liveness/readiness probes |
| `/actuator/**` | Monitoring endpoints |
| `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`, `/swagger-resources/**`, `/webjars/**` | OpenAPI documentation |

All other paths require authentication.

### Outbound calls

Every backend service also acts as an OAuth2 **client** when it calls other services.
The shared configuration `RestClientSecurityConfiguration` from `lakehouse-common-health` registers
the `BearerTokenClientHttpRequestInterceptor` on the auto-configured Spring `RestClient.Builder`,
so all REST clients of the project (config/state/scheduler/spark-proxy rest-client modules) are secured transparently.

Token resolution on each outgoing request (`BearerTokenClientHttpRequestInterceptor.resolveToken()`):

| Situation | Token used |
|:----------|:-----------|
| The current thread processes a user request authenticated with a JWT (`JwtAuthenticationToken` in `SecurityContextHolder`) | The user's JWT is relayed as-is (**token relay**) |
| Background processing without a user context (Kafka consumers, schedulers, task processors) | A fresh **`client_credentials`** token is obtained from Keycloak for the service account client `lakehouse-internal-client`; tokens are cached and refreshed automatically by the `OAuth2AuthorizedClientManager` |

Registration id of the internal client is configurable via `lakehouse.security.oauth2.client-registration-id`
(default `keycloak-internal`).

### Role of Keycloak

Keycloak issues and validates all identities in one realm `lakehouse`:

| Object | Value | Notes |
|:-------|:------|:------|
| Realm | `lakehouse` | Imported at startup in the demo compose |
| Client | `lakehouse-ui-client` | Confidential client for the UI BFF, standard flow only |
| Client | `lakehouse-internal-client` | Confidential client with enabled **service accounts**, used by all services and Spark drivers for `client_credentials` |
| Realm roles | `USER`, `ADMIN` | Mapped to `ROLE_USER`, `ROLE_ADMIN` |

Demo users (for local environment only - change passwords/secrets via environment variables in real deployments):

| User | Password | Roles |
|:-----|:---------|:------|
| `de_view` | `de_view` | USER |
| `de_editor` | `de_editor` | ADMIN |

> Warning: the default client secrets shown in `application.yml` files (`super-secret-bff-key-*`,
> `super-secret-internal-key-*`) are for development only. Always override them through
> `KEYCLOAK_UI_CLIENT_SECRET` / `KEYCLOAK_INTERNAL_CLIENT_SECRET` environment variables in production.

### Audit

Audit is implemented by the shared servlet filter `AuditLoggingFilter`
(`lakehouse-common-health`, `org.lakehouse.security`). It is registered in every backend service
after the authorization filter (`http.addFilterAfter(auditLoggingFilter, AuthorizationFilter.class)`),
so it sees the final response status.

For every inbound request exactly one structured line is written to the logger **`AUDIT_LOG`**:

```text
User ID: <jwt.sub>, Username: <jwt.preferred_username>, Method: <HTTP method>, URI: <path>, HTTP status: <status>
```

Details:

- `User ID` - JWT claim `sub`; `Username` - claim `preferred_username`.
- If the token is a **service account** token (claim `preferred_username` starts with `service-account-`
  or claim `azp` equals the configured internal client id), the username is replaced with the configured
  system account name (`lakehouse.security.audit.service-account-name`, default `system`).
- Anonymous/whitelisted requests are logged with `-` placeholders.
- The logback configuration of every service routes the `AUDIT_LOG` logger to a JSON console appender
  (logstash encoder) with custom fields `log_type=audit` and `service=<service name>`:

```xml
<appender name="AUDIT_CONSOLE_JSON" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <customFields>{"log_type":"audit", "service":"lakehouse-config-svc"}</customFields>
    </encoder>
</appender>
<logger name="AUDIT_LOG" level="INFO" additivity="false">
    <appender-ref ref="AUDIT_CONSOLE_JSON"/>
</logger>
```

Audit is log-based: events are not persisted to a database or sent to external systems -
log collection (ELK/Loki/etc.) is expected to capture them.

### Diagram

![interservice_security.png](interservice_security.png) (source: [interservice-security.puml](interservice-security.puml))

---

## 2. User-facing security (lakehouse-ui-svc)

The UI service is a **Backend-for-Frontend (BFF)**. It does not validate incoming JWTs itself -
instead it authenticates users interactively through Keycloak.

### Login flow

1. An unauthenticated user opens any page - the BFF redirects them to Keycloak
   (Spring Security `oauth2Login()`, **OAuth2 authorization code flow**).
2. The user authenticates in Keycloak; consent/credentials are handled entirely on the Keycloak side.
3. Keycloak redirects back to `{baseUrl}/login/oauth2/code/keycloak`; the BFF exchanges the code for tokens
   server-side (client secret never leaves the backend).
4. Spring creates an `OAuth2AuthenticationToken` with the authorities mapped from Keycloak claims;
   the browser receives an HTTP session cookie **`JSESSIONID`** (`HttpOnly`; `Secure` in the `prod` profile).
5. After login the user lands on the default success URL `/`.

Client registration: `lakehouse-ui-client` with scopes `openid, profile, email`;
redirect URI template `{baseUrl}/login/oauth2/code/{registrationId}`
(override with `LAKEHOUSE_UI_REDIRECT_URI` behind a proxy/load balancer).

### Session and CSRF

- Sessions are created `IF_REQUIRED`; the session cookie name is `JSESSIONID` (see `server.servlet.session.cookie.*`).
- Unlike the backend services, **CSRF protection is enabled**: the token is stored in a cookie
  `XSRF-TOKEN` readable by frontend JavaScript (`CookieCsrfTokenRepository.withHttpOnlyFalse()`).
  A dedicated filter eagerly loads the token so the cookie is always written to responses.
  State-changing requests from the SPA must send the header `X-XSRF-TOKEN`.

Whitelisted paths: `/actuator/**`, `/healthz`, `/readyz`, `/favicon.ico`. Everything else requires an authenticated session.

### Calls to backend services

The BFF calls config/state/scheduler/task-proxy REST APIs through the same secured `RestClient.Builder`.
Because the user session holds `OAuth2AuthenticationToken` (not a `JwtAuthenticationToken`),
`BearerTokenClientHttpRequestInterceptor` always falls back to the **service account**
`client_credentials` token of `lakehouse-internal-client`. Backend services therefore authorize these
calls as the internal technical client, not as the human user.

### Role of Keycloak

- Single entry point for interactive login (standard flow of the confidential client `lakehouse-ui-client`);
- Issues ID/access/refresh tokens during the authorization code flow;
- Stores users, roles (`USER`/`ADMIN`) and their attributes;
- Also issues service account tokens used by the BFF for backend calls.

### Audit

The BFF itself does not write audit records. All user operations that reach backend services are audited
there by `AuditLoggingFilter` (section 1); since the BFF uses the service account token,
such records show the configured system username (`system`) rather than the human login.
Requests served entirely by the BFF (static resources, login pages) are covered by ordinary application logs.

### Diagram

![ui_security.png](ui_security.png) (source: [ui-security.puml](ui-security.puml))

---

## 3. Authorization of Spark applications

Applies to the driver applications:

- lakehouse-task-executor-spark-dq-app
- lakehouse-task-executor-spark-dataset-app

### Job submission

1. `lakehouse-task-executor-svc` consumes tasks from Kafka and submits Spark jobs over the Spark REST API
   (`POST /v1/submissions`) - either directly to the cluster or through **task-proxy-for-spark**.
2. When submission goes through task-proxy-for-spark, the call carries a bearer token obtained by the
   standard interceptor (`client_credentials` of `lakehouse-internal-client`), and the proxy validates it
   as any other backend service.
3. The Spark driver starts as a regular Spring Boot application inside the cluster.

> Limitation: a vanilla Apache Spark Standalone master REST endpoint has no OAuth support. When jobs are
> submitted directly to the cluster (bypassing the proxy), transport-level protection is the operator's
> responsibility (network isolation, Spark native auth options).

### Driver callbacks

Both driver applications include `org.lakehouse.security` in their component scan, so they get the same
`RestClientSecurityConfiguration` + `BearerTokenClientHttpRequestInterceptor`. Inside the driver JVM there
is no user security context, therefore all callbacks acquire their own **`client_credentials`** tokens:

- fetching the scheduled task description from scheduler-svc (`SchedulerRestClientApi`);
- fetching source/task configurations from config-svc (`ConfigRestClientApi`);
- reporting results/statuses back to the backend services.

Backend services validate these tokens exactly like any other request (issuer/JWKS validation +
`AuditLoggingFilter`, which logs them under the service account name).

No secrets or tokens are passed as Spark job arguments - the driver authenticates itself directly against
Keycloak using its own client credentials (`KEYCLOAK_ISSUER_URI`, `KEYCLOAK_INTERNAL_CLIENT_SECRET`).

### Secret redaction

`SparkSessionConfiguration` (module `lakehouse-task-executor-spark-api`) sets Spark log redaction regexes
`spark.redaction.regex` and `spark.sql.redaction.string.regex` (default:
`(?i)secret|password|token|access[.]key|credentials|private`) so credentials do not leak into driver/executor logs
and the Spark UI.

### Diagram

![spark_apps_security.png](spark_apps_security.png) (source: [spark-apps-security.puml](spark-apps-security.puml))

---

## 4. Secrets for data connections (credential providers)

Database passwords and S3 access keys are **not stored** in configuration files. They are resolved at runtime
by two optional modules:

| Module | Contents |
|:-------|:---------|
| `lakehouse-credential-providers-jdbc` | Spark-independent library: `SecretProvider` SPI, `SecretResolver` helper, HTTP clients `VaultHttp` / `LockboxHttp`, in-memory `SecretCache` (5-minute TTL per JVM), JDBC providers `BaoJdbcSecretProvider` (OpenBao/Vault KV v2) and `YcLockboxJdbcSecretProvider` (Yandex Cloud Lockbox) |
| `lakehouse-credential-providers-spark` | Spark-specific: `LakehouseSecureJDBCTableCatalog` - a secure replacement for Spark's `JDBCTableCatalog` (password resolution on Driver and Executors) and S3 credential providers for Spark S3A |

Both entry points that open a JDBC connection now go through `SecretResolver`:

- lakehouse services - `JdbcConnectionFactory.getConnection(...)` (`lakehouse-task-executor-api`);
- Spark drivers - `LakehouseSecureJDBCTableCatalog.initialize(...)`.

### Provider option contract

| Option | Provider | Required | Description |
|:-------|:---------|:---------|:------------|
| `secretProvider` | both | yes | Fully qualified class name of the `SecretProvider` implementation |
| `secret-key` | both | yes | Combined `path:key` coordinate: OpenBao - `secretPath:key` (e.g. `kv/data/lakehouse/database:password`); Lockbox - `secretId:key` (e.g. `e4ta...:password`) |
| `vault-url` | OpenBao/Vault | yes | Vault HTTP API base URL, e.g. `http://openbao:8200` |
| `vault-role` | OpenBao/Vault | no | Kubernetes auth role (default `lakehouse`) |
| `vault-k8s-auth-path` | OpenBao/Vault | no | Kubernetes auth mount path (default `kubernetes`) |
| `secret-id` | Lockbox | yes | Yandex Cloud Lockbox secret id |
| `secret-version` | Lockbox | no | Specific secret version (default `latest`) |

The resolved value is injected into the connection options as the `password` key, and all listed security
options are **removed** before the map reaches a JDBC driver or Spark catalog. If `secretProvider` is absent,
the options are passed through unchanged (backward compatibility).

### Where to configure

- **Spark (global defaults, `spark-defaults.conf`)** - prefixed catalog options; the password is fetched on the driver:

  ```
  spark.sql.catalog.processingdb                org.lakehouse.security.catalog.LakehouseSecureJDBCTableCatalog
  spark.sql.catalog.processingdb.url            jdbc:postgresql://db-host:5432/db
  spark.sql.catalog.processingdb.user           app_user
  spark.sql.catalog.processingdb.secretProvider org.lakehouse.security.jdbc.BaoJdbcSecretProvider
  spark.sql.catalog.processingdb.vault-url      http://openbao:8200
  spark.sql.catalog.processingdb.secret-key     kv/data/lakehouse/database:password
  ```

  Real example: `demo/compose/conf_infra/spark-defaults.conf`.

- **Spark (per-datasource)** - the same keys with the full `spark.sql.catalog.<name>.` prefix placed in the
  datasource `service.properties`. `SparkStandAloneClusterTaskProcessor` forwards all `spark.*` keys to the
  driver; if `spark.sql.catalog.<name>.url` is missing, it is built from the datasource `host`/`port`/`urn`.

- **lakehouse services (JDBC tasks)** - the datasource `service.properties` (`ServiceDTO.properties`); the URL
  is built from `host`/`port`/`urn` unless an explicit `url` is given:

  ```json
  "properties": {
    "user": "app_user",
    "secretProvider": "org.lakehouse.security.jdbc.BaoJdbcSecretProvider",
    "secret-key": "kv/data/lakehouse/database:password",
    "vault-url": "http://openbao:8200"
  }
  ```

  Real example: `demo/compose/conf/datasources/processingdb.json`.

### OpenBao / Vault setup

1. Enable a KV v2 secrets engine and write the secret, e.g. `bao kv put kv/lakehouse/database password=<value>`.
2. Create a read-only policy for the paths the services read (`kv/data/lakehouse/database`, `kv/data/infrastructure/minio`)
   and issue a scoped token for the consuming components. Reference script: `demo/compose/conf_infra/openbao/init.sh`.
3. The token is supplied to the process via the `VAULT_TOKEN` environment variable or - inside Kubernetes - via
   the Service Account (options `vault-role` / `vault-k8s-auth-path`). Demo docker-compose variables:
   `BAO_DEV_ROOT_TOKEN_ID`, `BAO_TOKEN`, `LAKEHOUSE_DB_PASSWORD` (seeding) and `VAULT_TOKEN` (consumers).
4. In Kubernetes the OpenBao service is reachable as `http://<release>-openbao:8200` (umbrella chart `lakehouse-management`).

### Yandex Cloud Lockbox setup

1. A Lockbox secret containing the required keys (e.g. `password`, `access_key`, `secret_key`).
2. Every VM/worker that opens connections must have a Service Account with the `lockbox.payloadViewer` role.
   The IAM token is fetched from the Instance Metadata Service, or from an authorized key file configured via
   `YC_AUTH_KEY_PATH`.

### Security guarantees

- Plaintext passwords are never present in YAML/JSON configuration, task arguments or Spark job arguments.
- Secrets are never logged: on errors only HTTP status codes are logged, error messages are masked
  (`SecurityException`, `RuntimeException("Catalog access blocked...")`).
- Spark log redaction (`spark.redaction.regex`, section 3) additionally protects driver/executor logs.
- Resolved secrets are cached in memory per JVM with a 5-minute TTL.

---

## Configuration reference

Environment variables (with defaults from `demo/compose`, override in real deployments):

| Variable | Used by | Meaning |
|:---------|:--------|:--------|
| `KEYCLOAK_ISSUER_URI` | all modules | Issuer URI of the realm, e.g. `http://keycloak.lakehouse:8085/realms/lakehouse` |
| `KEYCLOAK_UI_CLIENT_SECRET` | lakehouse-ui-svc | Secret of `lakehouse-ui-client` |
| `KEYCLOAK_INTERNAL_CLIENT_SECRET` | all backend services, Spark apps | Secret of `lakehouse-internal-client` |
| `LAKEHOUSE_UI_REDIRECT_URI` | lakehouse-ui-svc | Redirect URI template for the authorization code callback |
| `VAULT_TOKEN` | lakehouse-task-executor-svc, Spark drivers/executors | OpenBao/Vault token used by `BaoJdbcSecretProvider` and the S3 providers (section 4) |
| `YC_AUTH_KEY_PATH` | lakehouse-task-executor-svc, Spark drivers/executors | Path to the authorized key file for the Lockbox IAM token (alternative to Instance Metadata, section 4) |

Application properties:

| Property | Default | Meaning |
|:---------|:--------|:--------|
| `lakehouse.security.enabled` | `true` | Master switch of the backend filter chains (`false` = permit all) |
| `lakehouse.security.audit.service-account-name` | `system` | Username written to audit lines for service-account tokens |
| `lakehouse.security.oauth2.internal-client-id` | `lakehouse-internal-client` | `azp` value treated as "service account" by the audit filter |
| `lakehouse.security.oauth2.client-registration-id` | `keycloak-internal` | Client registration used by the outgoing-call interceptor |

Related module documentation: [config-svc](../../lakehouse-config-svc/doc/readme.md),
[state-svc](../../lakehouse-state-svc/doc/readme.md),
[scheduler-svc](../../lakehouse-scheduler-svc/doc/readme.md),
[task-executor-svc](../../lakehouse-task-executor-svc/doc/readme.md),
[task-proxy-for-spark](../../lakehouse-task-proxy-for-spark/doc/readme.md),
[ui-svc](../../lakehouse-ui-svc/doc/readme.md).
