# Security (EN)

## Model

The integration uses the same service-to-service security model as the rest of the
yarina ecosystem:

| Element | Value |
|:--------|:------|
| Identity provider | Keycloak, realm **`yarina`** |
| Client | **`yarina-internal-client`** (confidential, service accounts enabled) |
| Grant | OAuth2 **`client_credentials`** |
| Token | JWT access token sent as `Authorization: Bearer <token>` |
| Transport | HTTP(S) from the Airflow worker to `yarina-state-svc` / `yarina-config-svc` / `yarina-task-proxy-for-spark` |

The hooks obtain a token from `extra.auth_url`, cache it for the lifetime of the
hook instance (one operator `execute` call) and send it to the target service.
Between tasks a new hook is created, so a new token is requested - the token is
never reused across task executions or written to XCom.

Reference: [Security in the yarina repository](../../doc/security/security.md).

## Roles are not required

**No realm role is required.** `yarina-state-svc`, `yarina-config-svc` and
`yarina-task-proxy-for-spark` only validate that the JWT is signed by the `yarina`
realm and issued to `yarina-internal-client`; a valid JWT is enough for the
endpoints used by this module.

## Audit

The backend `AuditLoggingFilter` sees all Airflow calls as the internal technical
client. In the audit log they are attributed to the service account name
**`system`** - exactly like calls from `yarina-task-executor-svc` and other
backend services. Human users are not represented in these audit records.

## Secret storage and rotation

* The client secret is stored in the Airflow Connection (`password`) or in an
  external secrets backend referenced by the Connection.
* In deployments that supply the secret through the environment, use
  `KEYCLOAK_INTERNAL_CLIENT_SECRET`; rotating the value in Keycloak and in the
  secret store requires **no DAG change**.
* The token and password are never logged. Hooks log only the client id, the
  dataset, the interval, the status and the lock source.
* Restrict access to the Airflow metadata database and use Fernet encryption
  (or a secrets backend) for Connection passwords.

## Local development

`extra.security_enabled=false` disables OAuth2 and sends requests without an
`Authorization` header. This is intended **only for local development** against
services started with `yarina.security.enabled=false`. Production deployments
must use Keycloak.

## Spark proxy: no kill endpoints

`YarinaSparkProxyHook` exposes only `create_submission` and `get_status`. It does
not implement the proxy's `/kill`, `/killall` or `/clear` endpoints, so an Airflow
task cannot terminate cluster workloads through this integration.

If `timeout_seconds` expires, the Airflow task fails but the Spark application may
continue running. Use the `submissionId` from the exception to inspect or stop it
manually outside Airflow. Keep `timeout_seconds` conservative and do not rely on
Airflow retries to cancel the previous attempt: a retry creates a new proxy
submission, and the proxy does not deduplicate.

## Operational recommendations

1. Use HTTPS between Airflow and Keycloak/yarina services (or a trusted service
   mesh) in production.
2. Give `yarina-internal-client` only the permissions it needs; do not reuse the
   UI client secret.
3. Store the connection secret in Vault/OpenBao, Kubernetes Secrets or a cloud
   secret manager - not in `airflow.cfg` or DAG files.
4. Monitor 401/403 responses: they indicate an expired/invalid secret or a
   Keycloak client misconfiguration.
5. Do not print request payloads containing tokens; the module deliberately logs
   only non-sensitive metadata.
