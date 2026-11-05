# Examples (EN)

A complete DAG template is available for each major version:

* [examples/2.x/yarina_split_lock_lifecycle_dag.py](../examples/2.x/yarina_split_lock_lifecycle_dag.py)
* [examples/3.x/yarina_split_lock_lifecycle_dag.py](../examples/3.x/yarina_split_lock_lifecycle_dag.py)
* [examples/2.x/yarina_spark_submit_dag.py](../examples/2.x/yarina_spark_submit_dag.py)
* [examples/3.x/yarina_spark_submit_dag.py](../examples/3.x/yarina_spark_submit_dag.py)

## Lifecycle

```text
check_dependencies -> lock_interval -> etl -> unlock_interval
```

1. **check_dependencies** (`YarinaStateCheckOperator`)
   * resolves source datasets (from `source_datasets` or from
     `yarina-config-svc`);
   * calls `POST /v1_0/state/dataset/wrong` for every source and for the target;
   * fails on foreign `LOCKED` states (and optionally on holes).
2. **lock_interval** (`YarinaStateLockOperator`)
   * sends `PUT /v1_0/state/dataset` with `status=LOCKED`;
   * uses `lock_source="airflow-run-{{ run_id }}"`;
   * pushes the lock source to XCom as `active_lock_source`.
3. **etl** - your Bash/Python/Spark task. If it fails, Airflow stops the run and
   the interval stays `LOCKED` (useful for diagnostics and safe retries).
4. **unlock_interval** (`YarinaStateUnlockOperator`)
   * sends `PUT /v1_0/state/dataset` with the same `lock_source` and
     `status=SUCCESS`.

The lock source is unique per DAG run, so two concurrent runs cannot silently
overwrite each other. The state service rejects a `SUCCESS` write with a different
lock source while the interval is still locked.

## `force_unlock` and when to use it

Keep `force_unlock=False` in normal DAGs.

Use `force_unlock=True` **only for an emergency recovery**, for example when:

* the DAG was restarted with a new `run_id`, but the interval is still `LOCKED`
  by the previous run;
* the lock source was lost and the interval cannot be released normally.

With `force_unlock=True` the operator resolves the lock source in this order:

1. explicit `lock_source` (if it differs from `airflow-executor`);
2. XCom `active_lock_source` of `lock_task_id`;
3. the single `LOCKED` lock source returned by `yarina-state-svc`;
4. `airflow-executor` as a last-resort fallback (if no `LOCKED` state exists, the
   `SUCCESS` write still succeeds).

The operator logs a **WARNING** whenever `force_unlock` is used or a foreign lock
is detected. Prefer a separate maintenance DAG or a manual run with
`force_unlock=True` over enabling it permanently.

## Spark submission

`YarinaSparkSubmitOperator` submits a Spark application through
`yarina-task-proxy-for-spark` and waits for a terminal status:

```text
POST /v1/submissions/create -> poll GET /v1/submissions/status/{submissionId} -> FINISHED
```

Minimal task:

```python
YarinaSparkSubmitOperator(
    task_id="submit_spark_job",
    app_resource="s3a://bucket/app.jar",
    main_class="com.example.Main",
    app_args=["--date", "{{ ds }}"],
    spark_properties={
        "spark.app.name": "airflow-{{ run_id }}",
        "spark.executor.instances": "4",
    },
    environment_variables={"AWS_REGION": "eu-west-1"},
    http_conn_id="yarina_spark_proxy_default",
    poll_interval_seconds=15,
    timeout_seconds=3600,
)
```

The task pushes `spark_submission_id` (the proxy id, for example `"42"`) to XCom.
It succeeds on `FINISHED`, fails on `FAILED`/`KILLED`, and fails immediately on
HTTP 404 for the status request. See [spark-operator.md](spark-operator.md) for the
full status table.

> **No kill:** the operator never calls `/kill`, `/killall` or `/clear`. On timeout
> the Spark application may still be running; use the `submissionId` from the
> exception to check `GET /v1/submissions/status/{id}` manually.

The Spark task can be used as the `etl` step inside the interval-state lifecycle,
for example after `lock_interval` and before `unlock_interval`.

## Automatic source resolution

By default (`auto_resolve_sources=True`, `source_datasets=None`) the check operator
calls:

```text
GET /v1_0/configs/datasets/{dataset_key_name}
```

and takes `DataSetDTO.sources.keys()` minus the dataset itself. This is equivalent
to `SourceConfDTO.getTargetDataSet().getSources().keySet()` in
`DependencyCheckStateTaskProcessor`.

Use an explicit list when:

* config-svc is not available from the Airflow worker;
* the DAG must pin dependencies independent of metadata changes;
* you want to check only a subset of sources:

```python
YarinaStateCheckOperator(
    task_id="check_dependencies",
    dataset_key_name="transaction_dds",
    interval_start="{{ data_interval_start.isoformat() }}",
    interval_end="{{ data_interval_end.isoformat() }}",
    lock_source="airflow-run-{{ run_id }}",
    source_datasets=["client_processing", "transaction_processing"],
    auto_resolve_sources=False,
)
```

## Tuning failure behaviour

| Parameter | Default | Meaning |
|:----------|:--------|:--------|
| `fail_on_foreign_lock` | `True` | Fail if the target interval is locked by another run |
| `fail_on_holes` | `False` | Fail if a source interval is not covered by `SUCCESS` |

A common pattern is to keep `fail_on_holes=False` when upstream datasets are
loaded by other schedules, and enable it for strictly dependent daily chains.

## Interval templates

The templates use:

```jinja
interval_start = "{{ data_interval_start.isoformat() }}"
interval_end   = "{{ data_interval_end.isoformat() }}"
lock_source    = "airflow-run-{{ run_id }}"
```

All three fields are in `template_fields`, so they are rendered before execution.
The resulting timestamps include a timezone offset (for example
`2026-01-01T00:00:00+00:00`), which is accepted by the services.

## Diagnosing errors

| Error | Likely cause |
|:------|:-------------|
| `Connection extra 'auth_url' is required` | Connection `extra` is empty; set the token endpoint |
| Keycloak 404 with the `protocol/openid-connect/token` hint | `auth_url` points to the issuer |
| HTTP 500 with the NPE hint | Known state-svc bug: null `lockSource` or null interval dates |
| `lock source mismatch` / foreign lock | Another DAG run holds the interval; wait or use `force_unlock` in an emergency |
| `Dataset '...' not found` | Wrong `dataset_key_name` or config-svc not reachable |
| Spark `submission '...' не найден (404)` | The proxy id is unknown; the operator does not retry |
| Spark timeout | The job may still be running; check the status URL from the exception manually |
| Spark create rejected | Read the proxy `message` and the request payload in the exception |
