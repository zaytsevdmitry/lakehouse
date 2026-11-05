# Installation (EN)

## Requirements

| Component | Requirement |
|:----------|:------------|
| Apache Airflow | **2.2+** for `2.x/`; **3.1+** recommended for `3.x/` (3.0.x has `xcom_push` restrictions) |
| Python | 3.8 - 3.11 for Airflow 2.x; 3.9 - 3.12 for Airflow 3.x |
| HTTP provider | `apache-airflow-providers-http` |
| Network access | `yarina-state-svc`, `yarina-config-svc`, `yarina-task-proxy-for-spark` (port `8090`), Keycloak realm `yarina` |

Install the provider into the same environment as Airflow:

```bash
pip install apache-airflow-providers-http
```

## Installation options

### Option A - copy into `$AIRFLOW_HOME/plugins` (recommended for DAG-only setups)

```bash
# Airflow 2.x
cp -r yarina-airflow-integration/2.x/plugins/. "$AIRFLOW_HOME/plugins/"

# Airflow 3.x
cp -r yarina-airflow-integration/3.x/plugins/. "$AIRFLOW_HOME/plugins/"
```

The trailing `/.` copies hidden files such as `.airflowignore` too. The resulting
layout must contain `$AIRFLOW_HOME/plugins/operators/...`,
`$AIRFLOW_HOME/plugins/hooks/...` and `$AIRFLOW_HOME/plugins/constants.py`.
Restart the scheduler, DAG processor and webserver after copying the files.

### Option B - keep the module as a separate package

Add the version directory (the folder that contains `plugins/`) to `PYTHONPATH`:

```bash
export PYTHONPATH="/opt/yarina-airflow-integration/2.x:${PYTHONPATH}"
```

or install it with your deployment tooling / Docker image build.

## Verification

Check the import (run from `$AIRFLOW_HOME` or with the version folder on `PYTHONPATH`):

```bash
# Airflow 2.x
python -c "from plugins.operators.yarina_state_lock import YarinaStateLockOperator; \
           from plugins.operators.yarina_state_unlock import YarinaStateUnlockOperator; \
           from plugins.operators.yarina_state_check import YarinaStateCheckOperator; \
           from plugins.operators.yarina_spark_submit import YarinaSparkSubmitOperator; \
           print('yarina operators imported')"

# Airflow 3.x
python -c "from plugins.operators.yarina_state_check import YarinaStateCheckOperator; \
           from plugins.operators.yarina_spark_submit import YarinaSparkSubmitOperator; \
           print('ok')"
```

`airflow plugins` should finish without import errors. Note that the module does not
register an `AirflowPlugin` entry point, so the operators are not listed there - the
import check above is the authoritative verification.

## Troubleshooting

| Symptom | Cause / fix |
|:--------|:------------|
| `ModuleNotFoundError: No module named 'airflow.sdk'` | You copied `3.x/plugins` into Airflow 2.x. Use `2.x/plugins`. |
| `ModuleNotFoundError: No module named 'airflow.models'` (3.x) | Use `3.x/plugins`; `BaseOperator` is imported from `airflow.sdk`. |
| `ModuleNotFoundError: No module named 'plugins'` | Start Airflow from `$AIRFLOW_HOME` or add the **parent of `plugins/`** to `PYTHONPATH`. Do not add `plugins/` itself. |
| `ModuleNotFoundError: No module named 'airflow.providers.http'` | `pip install apache-airflow-providers-http`. |
| `airflow.sdk` import fails on Airflow 3.0.x | Upgrade to 3.1+; early 3.0 Task SDK builds are not fully supported. |
| DAG import warns about `schedule` | Airflow < 2.4: replace `schedule=` with `schedule_interval=` in the DAG template. |
| `AirflowException: Connection extra 'auth_url' is required` | Fill `extra.auth_url` in the Connection or set `extra.security_enabled=false` for local development. |
| HTTP 404 from Keycloak | `auth_url` points to the issuer. Use `{issuer}/protocol/openid-connect/token`. |
| HTTP 500 from state-svc | Known backend NPE bugs (null `lockSource`, null dates). Check the payload first; the hook message contains a hint. |
| HTTP 500 from the Spark proxy on create | The proxy or its scheduler failed. The exception contains the response body; check `yarina-task-proxy-for-spark` logs. |
| `submission '...' не найден (404)` | The proxy does not know the id. The operator fails immediately without retrying; verify the `submissionId` and the proxy database. |
| Spark timeout message | The task may still be running. Check `GET /v1/submissions/status/{id}` manually; the operator does not kill the job. |

The Spark operator deliberately has no `/kill`, `/killall` or `/clear` calls. A
timeout leaves the application running in the cluster; stop it outside Airflow if
needed.

## Tests (optional)

```bash
cd yarina-airflow-integration/2.x
PYTHONPATH=. python -m pytest plugins/tests -q

cd ../3.x
PYTHONPATH=. python -m pytest plugins/tests -q
```

Expected result: `82 passed` for each supported major version (Airflow 2.7.x and
3.1.x in CI). No running proxy, Keycloak or Airflow database is required.
