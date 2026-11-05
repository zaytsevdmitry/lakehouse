# Spark Submit Operator (EN)

`YarinaSparkSubmitOperator` submits a Spark application through
`yarina-task-proxy-for-spark` and waits for its terminal status. The Connection is
described in [configuration.md](configuration.md); a complete DAG template is
available in [examples/2.x/yarina_spark_submit_dag.py](../examples/2.x/yarina_spark_submit_dag.py)
and [examples/3.x/yarina_spark_submit_dag.py](../examples/3.x/yarina_spark_submit_dag.py).

## Flow

1. The operator builds a `CreateSubmissionRequest` from its parameters.
2. It calls `POST /v1/submissions/create` through `YarinaSparkProxyHook`.
3. It validates `success == true` and a non-empty proxy `submissionId`.
4. It pushes `spark_submission_id` to XCom.
5. It polls `GET /v1/submissions/status/{submissionId}` every
   `poll_interval_seconds`.
6. `FINISHED` returns `{"submissionId": "...", "message": "FINISHED"}`.
7. `FAILED`, `KILLED` or timeout raise `AirflowException`.

The proxy is asynchronous: `/create` returns `WAITING` after the task is saved to the
PostgreSQL queue. A scheduler later dispatches `spark-submit`, so `WAITING` can last a
long time. Size `timeout_seconds` for queue time plus application runtime.

## Statuses and operator reaction

The proxy returns the state in the **`message`** field.

| Status / response | Operator reaction |
|:------------------|:------------------|
| `WAITING` | Transient: reset the unknown counter, sleep, poll again. |
| `SUBMITTED` | Transient: reset the unknown counter, sleep, poll again. |
| `RUNNING` | Transient: reset the unknown counter, sleep, poll again. |
| `FINISHED` | Success: return `{"submissionId": id, "message": "FINISHED"}`. |
| `FAILED` | Fail immediately with an `AirflowException`. |
| `KILLED` | Fail immediately with an `AirflowException`. |
| `UNKNOWN` | Increment the consecutive-unknown counter; fail at `max_consecutive_unknown`. |
| HTTP 404 on status | Fail immediately: an unknown proxy id will not become known. |
| HTTP 5xx on status | Treated as `UNKNOWN`; increments the same counter. |
| Other non-2xx | Fail with the HTTP code and response body. |
| Timeout | Fail with a message containing `submissionId` and a manual status URL. |

A transient status after one or more `UNKNOWN` responses resets the counter. The
counter is per operator execution and is not persisted.

## Proxy submission ID

The operator works only with the **proxy** `submissionId`, for example `"42"`. This id
is returned by `/create` and accepted by `/status/{submissionId}`.

The real Spark driver id (`driver-...`) is resolved inside
`yarina-task-proxy-for-spark` and is never exposed to Airflow. Do not try to pass a
driver id to the operator or to `/status`.

## Response model: `message`, not `driverState`

Use the `message` field of `SubmissionStatusResponse`:

```json
{
  "action": "StatusResponse",
  "message": "RUNNING",
  "submissionId": "42",
  "success": true
}
```

`driverState` belongs to the direct Spark Standalone REST API. The `StatusResponse`
model from `yarina-spark-rest-client` is **not** used for the proxy by this module.
Reading `driverState` would be incorrect for the proxy contract.

## No kill endpoints

The operator never calls `/kill`, `/killall` or `/clear` under any circumstance. The
hook exposes only `create_submission` and `get_status`; there are no kill methods to
call.

If `timeout_seconds` expires, **the Spark application may continue running in the
cluster**. The timeout exception contains the proxy `submissionId`, so an operator can
check or stop it manually outside Airflow:

```bash
curl -H "Authorization: Bearer <token>" \
  "http://yarina-task-proxy-for-spark:8090/v1/submissions/status/42"
```

There are no additional HTTP calls after the timeout is raised.

## Timeout and polling settings

| Parameter | Default | Meaning |
|:----------|:--------|:--------|
| `timeout_seconds` | `3600` | Total wall-clock budget for queueing and execution. |
| `poll_interval_seconds` | `10` | Delay between status requests. |
| `max_consecutive_unknown` | `5` | Consecutive `UNKNOWN`/HTTP 5xx responses before failure. |

Recommendations:

* Set `timeout_seconds` above the longest expected queue wait plus the Spark job
  runtime. A job waiting in the PostgreSQL queue still consumes the timeout budget.
* Do not poll too aggressively. `10-30` seconds is usually enough.
* Increase `max_consecutive_unknown` if the proxy is temporarily overloaded or the
  cluster status endpoint is flaky. Every successful transient status resets the
  counter.
* Airflow task retries create a **new** submission. The proxy does not deduplicate
  retries, so configure `retries` deliberately.

## Errors and diagnostics

* **Create rejected:** the exception includes the proxy `message` and the full request
  payload.
* **Failed/Killed:** the exception includes `submissionId` and the terminal status.
* **Timeout:** the exception includes `submissionId` and
  `GET /v1/submissions/status/{id}`. The task may still be running.
* **HTTP 404 on status:** the exception says the submission was not found (404); no
  retry is attempted.
* **HTTP 5xx on status:** logged as a warning and counted as `UNKNOWN`.
* **Keycloak 404:** the exception states that `auth_url` must be
  `{issuer}/protocol/openid-connect/token`, not the issuer.

## Logging

* `INFO` - submission creation, the received `submissionId`, every status poll, and
  the final `FINISHED`.
* `WARNING` - `UNKNOWN` or unexpected statuses, and the approach of the timeout.
* `ERROR` - raised through exceptions for create failures, `FAILED`/`KILLED` and
  timeouts.

Tokens and passwords are never logged.
