# Security

How security is organized in the lakehouse ecosystem. All services use [Keycloak](https://www.keycloak.org/) as a single identity provider (OAuth 2.0 / OpenID Connect, JWT tokens).

## Overview

Three security models are used:

| Model | Components | Mechanism |
|:------|:-----------|:----------|
| Service-to-service | lakehouse-config-svc, lakehouse-state-svc, lakehouse-task-executor-svc, lakehouse-task-proxy-for-spark, lakehouse-scheduler-svc | OAuth2 Resource Server (JWT validation) + `client_credentials` / token relay for outgoing calls |
| User-facing | lakehouse-ui-svc (BFF) | OAuth2 Login (authorization code flow) + HTTP session + CSRF |
| Spark applications | lakehouse-task-executor-spark-dq-app, lakehouse-task-executor-spark-dataset-app | OAuth2 Client (`client_credentials`) for callbacks to backend services |

Keycloak realm: **`lakehouse`** (realm import file: `demo/compose/conf_infra/security/realms/lakehouse-realm.json`).

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

## Configuration reference

Environment variables (with defaults from `demo/compose`, override in real deployments):

| Variable | Used by | Meaning |
|:---------|:--------|:--------|
| `KEYCLOAK_ISSUER_URI` | all modules | Issuer URI of the realm, e.g. `http://keycloak.lakehouse:8085/realms/lakehouse` |
| `KEYCLOAK_UI_CLIENT_SECRET` | lakehouse-ui-svc | Secret of `lakehouse-ui-client` |
| `KEYCLOAK_INTERNAL_CLIENT_SECRET` | all backend services, Spark apps | Secret of `lakehouse-internal-client` |
| `LAKEHOUSE_UI_REDIRECT_URI` | lakehouse-ui-svc | Redirect URI template for the authorization code callback |

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
