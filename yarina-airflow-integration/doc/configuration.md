# Configuration (EN)

The integration uses standard Airflow **Connections**. Secrets are stored only in
the Connection (or in an external secret backend), never in DAG code.

## Connections

| Connection id | Host | Login | Password | Extra |
|:--------------|:-----|:------|:---------|:------|
| `yarina_state_default` | `http://yarina-state-svc:8082` | `yarina-internal-client` | `KEYCLOAK_INTERNAL_CLIENT_SECRET` | `{"auth_url": "http://keycloak:8085/realms/yarina/protocol/openid-connect/token"}` |
| `yarina_config_default` | `http://yarina-config-svc:8080` | `yarina-internal-client` | `KEYCLOAK_INTERNAL_CLIENT_SECRET` | same `auth_url` |
| `yarina_spark_proxy_default` | `http://yarina-task-proxy-for-spark:8090` | `yarina-internal-client` | `KEYCLOAK_INTERNAL_CLIENT_SECRET` | same `auth_url` |

> **Important:** `auth_url` is the **token endpoint**, not the issuer. It must end
> with `/protocol/openid-connect/token`. A 404 from Keycloak almost always means
> that the issuer URL was used instead of the token endpoint.

### Create with the Airflow CLI

```bash
export KEYCLOAK_ISSUER_URI="http://keycloak:8085/realms/yarina"
export KEYCLOAK_INTERNAL_CLIENT_SECRET="<client-secret>"

airflow connections add yarina_state_default \
  --conn-type http \
  --conn-host "http://yarina-state-svc:8082" \
  --conn-login "yarina-internal-client" \
  --conn-password "${KEYCLOAK_INTERNAL_CLIENT_SECRET}" \
  --conn-extra "{\"auth_url\": \"${KEYCLOAK_ISSUER_URI}/protocol/openid-connect/token\"}"

airflow connections add yarina_config_default \
  --conn-type http \
  --conn-host "http://yarina-config-svc:8080" \
  --conn-login "yarina-internal-client" \
  --conn-password "${KEYCLOAK_INTERNAL_CLIENT_SECRET}" \
  --conn-extra "{\"auth_url\": \"${KEYCLOAK_ISSUER_URI}/protocol/openid-connect/token\"}"

airflow connections add yarina_spark_proxy_default \
  --conn-type http \
  --conn-host "http://yarina-task-proxy-for-spark:8090" \
  --conn-login "yarina-internal-client" \
  --conn-password "${KEYCLOAK_INTERNAL_CLIENT_SECRET}" \
  --conn-extra "{\"auth_url\": \"${KEYCLOAK_ISSUER_URI}/protocol/openid-connect/token\"}"
```

### Connection fields understood by the hooks

| Field | Required | Description |
|:------|:---------|:------------|
| `host` | yes | Base URL of the service, including scheme and port. The Spark proxy uses port `8090`. |
| `login` | yes (when security is on) | Keycloak client id, normally `yarina-internal-client` |
| `password` | yes (when security is on) | Keycloak client secret |
| `extra.auth_url` | yes (when security is on) | Absolute token endpoint |
| `extra.security_enabled` | no, default `true` | `false` disables OAuth2 (local development only) |

## Environment variables

The values below are deployment conventions; Airflow itself does not read them
unless your Connection/secret backend is configured to do so.

| Variable | Meaning |
|:---------|:--------|
| `KEYCLOAK_ISSUER_URI` | Realm issuer URI, e.g. `http://keycloak:8085/realms/yarina` |
| `KEYCLOAK_INTERNAL_CLIENT_SECRET` | Secret of the `yarina-internal-client` service account |

In production prefer an Airflow secrets backend (Vault/OpenBao, Kubernetes Secrets,
cloud secret manager) and reference it from the Connection.

## Local development without security

`yarina-state-svc` and `yarina-config-svc` can run with security disabled
(`yarina.security.enabled=false`). In this mode set:

```json
{"security_enabled": false}
```

in the Connection `extra`. No `auth_url`, login or password is then required.
**This mode is for local development only.**

## DAG examples

### Automatic source resolution (recommended)

```python
YarinaStateCheckOperator(
    task_id="check_dependencies",
    dataset_key_name="transaction_dds",
    interval_start="{{ data_interval_start.isoformat() }}",
    interval_end="{{ data_interval_end.isoformat() }}",
    lock_source="airflow-run-{{ run_id }}",
    auto_resolve_sources=True,
    config_http_conn_id="yarina_config_default",
    state_http_conn_id="yarina_state_default",
)
```

The operator calls `GET /v1_0/configs/datasets/{keyName}` and uses
`DataSetDTO.sources` minus the dataset itself.

### Explicit source list

```python
YarinaStateCheckOperator(
    task_id="check_dependencies",
    dataset_key_name="transaction_dds",
    interval_start="{{ data_interval_start.isoformat() }}",
    interval_end="{{ data_interval_end.isoformat() }}",
    lock_source="airflow-run-{{ run_id }}",
    source_datasets=["client_processing", "transaction_processing"],
    auto_resolve_sources=False,
    state_http_conn_id="yarina_state_default",
)
```

Use explicit sources when config-svc is unavailable or when the DAG must pin its
dependencies independently from metadata changes.

## Tuning the check

| Parameter | Default | Effect |
|:----------|:--------|:-------|
| `fail_on_foreign_lock` | `True` | Fail when the target dataset is `LOCKED` by another lock source |
| `fail_on_holes` | `False` | Treat intervals without `SUCCESS` in source datasets as errors |
| `auto_resolve_sources` | `True` | Read sources from config-svc when `source_datasets` is omitted |

## Spark submit operator parameters

| Parameter | Type | Default | Description |
|:----------|:-----|:--------|:------------|
| `app_resource` | `str` | required | `appResource` passed to the proxy (JAR, Python file, etc.) |
| `main_class` | `str` | `""` | `mainClass` for Java/Scala applications |
| `app_args` | `list[str]` | `[]` | `appArgs` list; Jinja templates are rendered |
| `spark_properties` | `dict[str, str]` | `{}` | `sparkProperties`, e.g. `spark.executor.instances` |
| `environment_variables` | `dict[str, str]` | `{}` | `environmentVariables` for the Spark driver |
| `client_spark_version` | `str` | `""` | `clientSparkVersion` |
| `http_conn_id` | `str` | `yarina_spark_proxy_default` | Airflow Connection id |
| `poll_interval_seconds` | `int` | `10` | Delay between status requests; must be greater than 0 |
| `timeout_seconds` | `int` | `3600` | Total wait budget; must be greater than 0 |
| `terminal_fail_states` | `Iterable[str]` | `("FAILED", "KILLED")` | States that fail the task immediately |
| `transient_states` | `Iterable[str]` | `("WAITING", "SUBMITTED", "RUNNING")` | Non-terminal states that reset the unknown counter |
| `max_consecutive_unknown` | `int` | `5` | Consecutive `UNKNOWN`/HTTP 5xx responses tolerated before failure |

`app_resource`, `main_class`, `app_args`, `spark_properties`,
`environment_variables` and `client_spark_version` are template fields, so Jinja
expressions such as `{{ ds }}` and `{{ var.value.name }}` work. See
[spark-operator.md](spark-operator.md) for the status model and timeout behaviour.

## Secrets hygiene

* Never put a client secret or token in a DAG file, a DAG default arg or an XCom.
* Do not log tokens: the hooks only log the client id and high-level steps.
* For `environment_variables` referenced from a DAG, prefer Airflow Variables or a
  secrets backend over hard-coded credentials; the operator logs only the app
  resource and main class, never the rendered variables.
* Rotate the secret by updating the Keycloak client / secret backend; no DAG change
  is required.
