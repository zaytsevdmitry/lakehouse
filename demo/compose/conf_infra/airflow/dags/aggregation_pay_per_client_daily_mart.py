# "Yarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
# Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at https://www.apache.org/licenses/LICENSE-2.0.txt

"""Airflow 2.x DAG template: check -> lock -> submit -> unlock.

Connections required:

* ``yarina_state_default``  - yarina-state-svc
* ``yarina_config_default`` - yarina-config-svc
* ``yarina_spark_proxy_default`` - yarina-task-proxy-for-spark (port 8090)
"""

from datetime import datetime
from airflow import DAG
from operators.yarina_state_check import YarinaStateCheckOperator
from operators.yarina_state_lock import YarinaStateLockOperator
from operators.yarina_state_unlock import YarinaStateUnlockOperator
from operators.yarina_spark_submit import YarinaSparkSubmitOperator

DAG_ID = "aggregation_pay_per_client_daily_mart"
DATASET = "aggregation_pay_per_client_daily_mart"
LOCK_SOURCE = "airflow-run-{{ run_id }}"

# Spark application
SPARK_APP_RESOURCE = "file:///opt/yarina-task-spark-apps/yarina-task-executor-spark-dataset-app-1.1.0-jar-with-dependencies.jar"
SPARK_MAIN_CLASS = "org.yarina.taskexecutor.spark.dataset.SparkProcessorApplication"

# spark proxy conf
SHARED_SPARK_CONFIG = {
    "app_resource": SPARK_APP_RESOURCE,
    "main_class": SPARK_MAIN_CLASS,
    "http_conn_id": "yarina_spark_proxy_default",
    "poll_interval_seconds": 15,
    "timeout_seconds": 3600,
}

DEFAULT_SPARK_PROPERTIES = {
    "spark.ui.enabled": "true",
    "spark.driver.cores": "1",
    "spark.driver.memory": "1g",
    "spark.executor.cores": "1",
    "spark.executor.memory": "1g",
    "spark.sql.catalog.processingdb.url": "jdbc:postgresql://db-dev:5432/postgresDB",
}

with DAG(
        dag_id=DAG_ID,
        description="Submit a Spark application aggregation_pay_per_client_daily_mart through yarina-task-proxy-for-spark",
        start_date=datetime(2025, 1, 1),
        schedule="@daily",
        catchup=True,
        max_active_runs=1,
        default_args={"retries": 10},
        tags=["lifecycle", "spark", "proxy"],
) as dag:

    interval_start = "{{ data_interval_start.isoformat() }}"
    interval_end = "{{ data_interval_end.isoformat() }}"


    def get_spark_args(body_name: str) -> list:
        return [
            "--yarina.client.rest.config.server.url=http://yarina-config-svc:8082",
            "--yarina.client.rest.scheduler.server.url=http://yarina-scheduler-svc:8083",
            "--scheduledTaskDriverKeyName=spark_iceberg",
            f"--scheduledTaskTaskProcessorBody={body_name}",
            f"--scheduledTaskIntervalStartDateTime={interval_start}",
            f"--scheduledTaskIntervalEndDateTime={interval_end}",
            f"--scheduledTaskDataSetKeyName={DATASET}",
            f"--scheduledTaskTargetDateTime={{{{ ds }}}}",
            f"--scheduledTaskTryNum={{{{ ti.try_number }}}}",
        ]

    check_dependencies = YarinaStateCheckOperator(
        task_id="check_dependencies",
        dataset_key_name=DATASET,
        interval_start=interval_start,
        interval_end=interval_end,
        lock_source=LOCK_SOURCE,
        auto_resolve_sources=True,
        config_http_conn_id="yarina_config_default",
        state_http_conn_id="yarina_state_default",
        fail_on_foreign_lock=True,
        fail_on_holes=True,
    )

    lock_interval = YarinaStateLockOperator(
        task_id="lock_interval",
        dataset_key_name=DATASET,
        interval_start=interval_start,
        interval_end=interval_end,
        lock_source=LOCK_SOURCE,
        http_conn_id="yarina_state_default",
    )

    create_table = YarinaSparkSubmitOperator(
        task_id="submit_spark_job_create_table",
        app_args=get_spark_args("createTableSQLProcessorBody"),
        spark_properties={**DEFAULT_SPARK_PROPERTIES, "spark.app.name": "airflow-{{ run_id }}-createTable"},
        **SHARED_SPARK_CONFIG,
    )

    load = YarinaSparkSubmitOperator(
        task_id="submit_spark_job_load",
        app_args=get_spark_args("mergeSQLProcessorBody"),
        spark_properties={**DEFAULT_SPARK_PROPERTIES, "spark.app.name": "airflow-{{ run_id }}-load"},
        **SHARED_SPARK_CONFIG,
    )

    compact = YarinaSparkSubmitOperator(
        task_id="submit_spark_job_compact",
        app_args=get_spark_args("compactTableSQLProcessorBody"),
        spark_properties={**DEFAULT_SPARK_PROPERTIES, "spark.app.name": "airflow-{{ run_id }}-compact"},
        **SHARED_SPARK_CONFIG,
    )

    unlock_interval = YarinaStateUnlockOperator(
        task_id="unlock_interval",
        dataset_key_name=DATASET,
        interval_start=interval_start,
        interval_end=interval_end,
        lock_source=LOCK_SOURCE,
        http_conn_id="yarina_state_default",
        force_unlock=False,
    )

    check_dependencies >> lock_interval >> create_table >> load >> compact >> unlock_interval
