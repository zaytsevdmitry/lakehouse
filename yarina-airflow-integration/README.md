# yarina-airflow-integration

Apache Airflow integration for the **yarina** ecosystem. The module provides hooks,
operators and DAG templates for two workflows:

1. **Interval-state lifecycle** - dependency check, `LOCKED`/`SUCCESS` state
   management:

   ```text
   check dependencies -> LOCK -> ETL -> SUCCESS (unlock)
   ```

2. **Spark submission through `yarina-task-proxy-for-spark`** - asynchronous
   `spark-submit` with status polling:

   ```text
   POST /v1/submissions/create -> poll GET /v1/submissions/status/{submissionId} -> FINISHED
   ```

It talks to:

| Service | Purpose | Default Connection |
|:--------|:--------|:-------------------|
| `yarina-state-svc` | `LOCKED` / `SUCCESS` interval states, `/wrong` dependency check | `yarina_state_default` |
| `yarina-config-svc` | `DataSetDTO.sources` for automatic source resolution | `yarina_config_default` |
| `yarina-task-proxy-for-spark` | queue and monitor Spark applications (port `8090`) | `yarina_spark_proxy_default` |
| Keycloak realm `yarina` | OAuth2 `client_credentials` via `yarina-internal-client` | connection login/password + `extra.auth_url` |

## Features

* `YarinaBaseHook` - shared OAuth2 `client_credentials` flow, JSON headers, request
  timeouts, sanitised Connection extras (no `auth_url`/`security_enabled` sent as
  HTTP headers), and unified HTTP error handling (including a diagnostic hint for
  known `yarina-state-svc` NPE/5xx bugs).
* `YarinaStateHook` - `put_state`, `get_states`, `post_wrong`,
  `resolve_lock_source`, `find_foreign_locks`.
* `YarinaConfigHook` - `get_dataset`, `get_sources` (sources minus the dataset itself).
* `YarinaSparkProxyHook` - `create_submission` and `get_status` only. HTTP 404 on a
  status request fails immediately; HTTP 5xx is reported as `UNKNOWN` so the
  operator can apply its tolerance. It has no kill API.
* `YarinaStateCheckOperator` - 1:1 semantics with `DependencyCheckStateTaskProcessor`
  from `yarina-task-executor-svc`.
* `YarinaStateLockOperator` / `YarinaStateUnlockOperator` - interval locking and
  success reporting, with an emergency `force_unlock` mode.
* `YarinaSparkSubmitOperator` - builds `CreateSubmissionRequest`, pushes
  `spark_submission_id` to XCom, polls the proxy `submissionId` until `FINISHED`,
  and fails on `FAILED`/`KILLED`/timeout.
* Unit and lifecycle tests (in-memory state-service emulator), no real network.
* DAG templates for Airflow 2.x and 3.x, for both integrations.

> **Spark limitation:** `YarinaSparkSubmitOperator` never calls `/kill`, `/killall`
> or `/clear`. If `timeout_seconds` expires, the Spark application may continue
> running in the cluster. The exception contains the proxy `submissionId` for a
> manual status check: `GET /v1/submissions/status/{id}`.

## Repository layout

```text
yarina-airflow-integration/
├── README.md
├── LICENSE
├── 2.x/
│   └── plugins/
│       ├── __init__.py
│       ├── constants.py
│       ├── hooks/
│       │   ├── __init__.py
│       │   ├── yarina_base_hook.py
│       │   ├── yarina_state_hook.py
│       │   ├── yarina_config_hook.py
│       │   └── yarina_spark_proxy_hook.py
│       ├── operators/
│       │   ├── __init__.py
│       │   ├── yarina_state_lock.py
│       │   ├── yarina_state_unlock.py
│       │   ├── yarina_state_check.py
│       │   └── yarina_spark_submit.py
│       └── tests/
│           ├── conftest.py
│           ├── hooks/
│           └── operators/
├── 3.x/
│   └── plugins/            # same structure; BaseOperator from airflow.sdk
├── doc/                    # EN documentation
├── doc-ru/                 # RU documentation
└── examples/
    ├── 2.x/
    │   ├── yarina_split_lock_lifecycle_dag.py
    │   └── yarina_spark_submit_dag.py
    └── 3.x/
        ├── yarina_split_lock_lifecycle_dag.py
        └── yarina_spark_submit_dag.py
```

The code in `2.x/plugins` and `3.x/plugins` is intentionally duplicated so a user
can copy exactly one folder into `$AIRFLOW_HOME/plugins`. Logic is identical;
only the `BaseOperator` import and the TaskInstance context key differ.

## Compatibility

| Airflow version | Python | Status | Limitations |
|:----------------|:-------|:-------|:------------|
| 2.2 - 2.3 | 3.8 - 3.10 | not tested | DAG templates use `schedule=`; replace with `schedule_interval=` |
| 2.4 - 2.6 | 3.8 - 3.11 | should work | not in CI matrix |
| **2.7.x** | 3.8 - 3.11 | **tested / target** | CI uses 2.7.3 |
| 3.0.x | 3.9 - 3.11 | supported with restrictions | `xcom_push` availability depends on the exact Task SDK build; **use 3.1+** |
| **3.1+** | 3.9 - 3.12 | **tested / target** | CI uses 3.1.x |

`apache-airflow-providers-http` is required for both major versions. No other
third-party dependency besides Airflow and `requests` is used.

## Quick start

1. Install the HTTP provider:

   ```bash
   pip install apache-airflow-providers-http
   ```

2. Copy the folder that matches your Airflow version:

   ```bash
   cp -r yarina-airflow-integration/2.x/plugins/. "$AIRFLOW_HOME/plugins/"   # Airflow 2.x
   # or
   cp -r yarina-airflow-integration/3.x/plugins/. "$AIRFLOW_HOME/plugins/"   # Airflow 3.x
   ```

   Alternatively add the version folder to `PYTHONPATH`.

3. Create the Connections `yarina_state_default`, `yarina_config_default` and
   `yarina_spark_proxy_default` - see [doc/configuration.md](doc/configuration.md).

4. Copy a DAG template from `examples/<version>/` into `$AIRFLOW_HOME/dags/`.

5. Verify the imports:

   ```bash
   python -c "from plugins.operators.yarina_state_lock import YarinaStateLockOperator; \
              from plugins.operators.yarina_spark_submit import YarinaSparkSubmitOperator; \
              print('ok')"
   ```

Detailed guides:

* [Installation](doc/installation.md) ([RU](doc-ru/installation.md))
* [Configuration](doc/configuration.md) ([RU](doc-ru/configuration.md))
* [Security](doc/security.md) ([RU](doc-ru/security.md))
* [Examples](doc/examples.md) ([RU](doc-ru/examples.md))
* [Spark submit operator](doc/spark-operator.md) ([RU](doc-ru/spark-operator.md))

## Tests

```bash
cd yarina-airflow-integration/2.x
PYTHONPATH=. python -m pytest plugins/tests -q

cd ../3.x
PYTHONPATH=. python -m pytest plugins/tests -q
```

The tests use `pytest` + `pytest-mock`, mock all HTTP calls and include an
in-memory state-service emulator for the DAG lifecycle (`check -> lock -> etl -> unlock`).
The Spark tests additionally assert that only `create_submission` and `get_status`
are called and that the hook has no kill/clear API.

## License

Apache License 2.0 - see [LICENSE](LICENSE).
