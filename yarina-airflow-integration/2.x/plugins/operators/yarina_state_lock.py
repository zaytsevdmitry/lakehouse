# "Yarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
# Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at https://www.apache.org/licenses/LICENSE-2.0.txt

"""Operator that acquires a ``LOCKED`` state for a dataset interval."""

from __future__ import annotations

import logging
from typing import Any, Dict, Tuple

from airflow.exceptions import AirflowException
from airflow.models import BaseOperator

try:  # package import (``plugins.*``)
    from plugins.constants import (
        DEFAULT_HTTP_CONN_ID,
        DEFAULT_LOCK_SOURCE,
        YarinaStatus,
    )
    from plugins.hooks.yarina_state_hook import YarinaStateHook
except ImportError:  # pragma: no cover - Airflow loads plugin files as top-level modules
    from constants import (  # type: ignore[no-redef]
        DEFAULT_HTTP_CONN_ID,
        DEFAULT_LOCK_SOURCE,
        YarinaStatus,
    )
    from hooks.yarina_state_hook import YarinaStateHook  # type: ignore[no-redef]

logger = logging.getLogger(__name__)


class YarinaStateLockOperator(BaseOperator):
    """Lock a dataset interval for the current DAG run.

    Sends ``PUT v1_0/state/dataset`` with ``status=LOCKED`` and pushes the
    effective lock source to XCom under ``active_lock_source``.

    The operation is idempotent: re-sending the same ``lockSource`` for the
    same interval succeeds because the service overwrites its own lock.
    """

    template_fields: Tuple[str, ...] = (
        "dataset_key_name",
        "interval_start",
        "interval_end",
        "lock_source",
    )

    def __init__(
        self,
        dataset_key_name: str,
        interval_start: str,
        interval_end: str,
        lock_source: str = DEFAULT_LOCK_SOURCE,
        http_conn_id: str = DEFAULT_HTTP_CONN_ID,
        **kwargs: Any,
    ) -> None:
        super().__init__(**kwargs)
        self.dataset_key_name = dataset_key_name
        self.interval_start = interval_start
        self.interval_end = interval_end
        self.lock_source = lock_source
        self.http_conn_id = http_conn_id

    def _get_ti(self, context: Dict[str, Any]) -> Any:
        return context.get("task_instance") or context.get("ti")

    def execute(self, context: Dict[str, Any]) -> str:
        if not self.dataset_key_name or not str(self.dataset_key_name).strip():
            raise AirflowException("dataset_key_name must not be empty")
        if not self.interval_start or not self.interval_end:
            raise AirflowException("interval_start and interval_end must not be empty")
        if not self.lock_source or not str(self.lock_source).strip():
            raise AirflowException(
                "lock_source must not be empty; the state service rejects a null lockSource"
            )

        payload = {
            "dataSetKeyName": self.dataset_key_name,
            "intervalStartDateTime": self.interval_start,
            "intervalEndDateTime": self.interval_end,
            "status": YarinaStatus.LOCKED.value,
            "lockSource": self.lock_source,
        }

        logger.info(
            "LOCK dataset=%s interval=[%s, %s] lockSource=%s",
            self.dataset_key_name,
            self.interval_start,
            self.interval_end,
            self.lock_source,
        )
        hook = YarinaStateHook(http_conn_id=self.http_conn_id)
        hook.put_state(payload)

        ti = self._get_ti(context)
        if ti is not None:
            ti.xcom_push(key="active_lock_source", value=self.lock_source)

        logger.info("LOCKED dataset=%s lockSource=%s", self.dataset_key_name, self.lock_source)
        return self.lock_source
