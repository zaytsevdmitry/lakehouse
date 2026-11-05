# "Yarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
# Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at https://www.apache.org/licenses/LICENSE-2.0.txt

"""Airflow 2.x DAG template: check -> lock -> etl -> unlock.

Connections required:

* ``yarina_state_default``  - yarina-state-svc
* ``yarina_config_default`` - yarina-config-svc

The lock source is unique per DAG run. The unlock task passes the same value,
so ``force_unlock`` stays ``False`` in the normal lifecycle.
"""

from datetime import datetime

from airflow import DAG
from airflow.operators.bash import BashOperator

try:  # when the parent of ``plugins`` is on PYTHONPATH
    from plugins.operators.yarina_state_check import YarinaStateCheckOperator
    from plugins.operators.yarina_state_lock import YarinaStateLockOperator
    from plugins.operators.yarina_state_unlock import YarinaStateUnlockOperator
except ImportError:  # when Airflow adds ``$AIRFLOW_HOME/plugins`` to sys.path
    from operators.yarina_state_check import YarinaStateCheckOperator  # type: ignore[no-redef]
    from operators.yarina_state_lock import YarinaStateLockOperator  # type: ignore[no-redef]
    from operators.yarina_state_unlock import YarinaStateUnlockOperator  # type: ignore[no-redef]

DAG_ID = "yarina_split_lock_lifecycle"
DATASET = "transaction_dds"
LOCK_SOURCE = "airflow-run-{{ run_id }}"

with DAG(
    dag_id=DAG_ID,
    description="Yarina interval lifecycle: check, lock, ETL, unlock",
    start_date=datetime(2026, 1, 1),
    schedule="@daily",
    catchup=False,
    max_active_runs=1,
    default_args={"retries": 0},
    tags=["yarina", "state", "lifecycle"],
) as dag:
    interval_start = "{{ data_interval_start.isoformat() }}"
    interval_end = "{{ data_interval_end.isoformat() }}"

    check_dependencies = YarinaStateCheckOperator(
        task_id="check_dependencies",
        dataset_key_name=DATASET,
        interval_start=interval_start,
        interval_end=interval_end,
        lock_source=LOCK_SOURCE,
        # source_datasets=["client_processing", "transaction_processing"],
        # ^ optional explicit list; by default sources are read from
        #   yarina-config-svc (DataSetDTO.sources).
        auto_resolve_sources=True,
        config_http_conn_id="yarina_config_default",
        state_http_conn_id="yarina_state_default",
        fail_on_foreign_lock=True,
        fail_on_holes=False,
    )

    lock_interval = YarinaStateLockOperator(
        task_id="lock_interval",
        dataset_key_name=DATASET,
        interval_start=interval_start,
        interval_end=interval_end,
        lock_source=LOCK_SOURCE,
        http_conn_id="yarina_state_default",
    )

    etl = BashOperator(
        task_id="etl",
        bash_command=(
            "echo 'Run your ETL here. The interval is locked for this DAG run.'"
        ),
    )

    unlock_interval = YarinaStateUnlockOperator(
        task_id="unlock_interval",
        dataset_key_name=DATASET,
        interval_start=interval_start,
        interval_end=interval_end,
        lock_source=LOCK_SOURCE,
        http_conn_id="yarina_state_default",
        # Keep force_unlock=False: the lock belongs to this run and must be
        # released with exactly the same lock_source. Enable force_unlock=True
        # only for an emergency recovery, e.g. after the DAG was restarted
        # with a new run_id while the interval is still LOCKED by the old run.
        force_unlock=False,
        # lock_task_id="lock_interval",  # useful with force_unlock=True
    )

    check_dependencies >> lock_interval >> etl >> unlock_interval
