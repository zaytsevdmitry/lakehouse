# "Yarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
# Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at https://www.apache.org/licenses/LICENSE-2.0.txt

"""Airflow 3.x DAG template: submit a Spark application through the proxy.

Required Connection:

* ``yarina_spark_proxy_default`` - yarina-task-proxy-for-spark (port 8090)

The operator does not call ``/kill``, ``/killall`` or ``/clear``. If
``timeout_seconds`` expires, the Spark application may continue running; the
exception contains the proxy ``submissionId`` for a manual status check.
"""

from datetime import datetime

from airflow.sdk import DAG

try:  # when the parent of ``plugins`` is on PYTHONPATH
    from plugins.operators.yarina_spark_submit import YarinaSparkSubmitOperator
except ImportError:  # when Airflow adds ``$AIRFLOW_HOME/plugins`` to sys.path
    from operators.yarina_spark_submit import (  # type: ignore[no-redef]
        YarinaSparkSubmitOperator,
    )

DAG_ID = "yarina_spark_submit_example"

with DAG(
    dag_id=DAG_ID,
    description="Submit a Spark application through yarina-task-proxy-for-spark",
    start_date=datetime(2026, 1, 1),
    schedule="@daily",
    catchup=False,
    max_active_runs=1,
    default_args={"retries": 0},
    tags=["yarina", "spark", "proxy"],
) as dag:
    submit = YarinaSparkSubmitOperator(
        task_id="submit_spark_job",
        app_resource="s3a://bucket/app.jar",
        main_class="com.example.Main",
        app_args=["--date", "{{ ds }}"],
        spark_properties={
            "spark.app.name": "airflow-{{ run_id }}",
            "spark.executor.instances": "4",
        },
        environment_variables={
            "AWS_ACCESS_KEY_ID": "{{ var.value.aws_key }}",
        },
        http_conn_id="yarina_spark_proxy_default",
        poll_interval_seconds=15,
        timeout_seconds=3600,
        # The proxy never exposes the real Spark driver-id. The operator polls
        # the proxy submissionId ("42", ...) and reads the state from "message".
        # No /kill, /killall or /clear calls are made. On timeout the task may
        # still be running in the cluster - check
        # GET /v1/submissions/status/{submissionId} manually.
    )
