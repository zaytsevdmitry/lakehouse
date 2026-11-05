# "Yarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
# Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at https://www.apache.org/licenses/LICENSE-2.0.txt

"""Operator that releases a dataset interval (``SUCCESS``)."""

from __future__ import annotations

import logging
from typing import Any, Dict, Optional, Tuple

from airflow.exceptions import AirflowException
from airflow.sdk import BaseOperator

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


class YarinaStateUnlockOperator(BaseOperator):
    """Mark a dataset interval as ``SUCCESS``.

    In the normal lifecycle ``lock_source`` is the same value that was passed
    to :class:`YarinaStateLockOperator`, therefore no lookup is required.

    .. warning::
       ``force_unlock=True`` is an **emergency-only** switch. Use it when the
       lock source is lost, for example after restarting a DAG with a new
       ``run_id`` while an interval is still ``LOCKED`` by the previous run.
       In the regular scenario keep ``force_unlock=False``: the operator must
       only be able to release a lock that belongs to the current run.

    With ``force_unlock=True`` the effective lock source is resolved in this
    order:

    1. explicit ``lock_source`` if it differs from ``DEFAULT_LOCK_SOURCE``;
    2. XCom ``active_lock_source`` of the task ``lock_task_id``;
    3. the single ``LOCKED`` lock source returned by the state service;
    4. ``DEFAULT_LOCK_SOURCE`` as a last-resort fallback.
    """

    template_fields: Tuple[str, ...] = (
        "dataset_key_name",
        "interval_start",
        "interval_end",
        "lock_source",
        "lock_task_id",
    )

    def __init__(
        self,
        dataset_key_name: str,
        interval_start: str,
        interval_end: str,
        lock_source: Optional[str] = None,
        force_unlock: bool = False,
        lock_task_id: Optional[str] = None,
        http_conn_id: str = DEFAULT_HTTP_CONN_ID,
        **kwargs: Any,
    ) -> None:
        super().__init__(**kwargs)
        self.dataset_key_name = dataset_key_name
        self.interval_start = interval_start
        self.interval_end = interval_end
        self.lock_source = lock_source
        self.force_unlock = force_unlock
        self.lock_task_id = lock_task_id
        self.http_conn_id = http_conn_id

    def _get_ti(self, context: Dict[str, Any]) -> Any:
        return context.get("ti") or context.get("task_instance")

    def _resolve_force_lock_source(
        self, hook: YarinaStateHook, context: Dict[str, Any]
    ) -> str:
        logger.warning(
            "force_unlock=True for dataset=%s interval=[%s, %s]: emergency override, "
            "using a lock source that may belong to another run",
            self.dataset_key_name,
            self.interval_start,
            self.interval_end,
        )

        if self.lock_source and self.lock_source != DEFAULT_LOCK_SOURCE:
            logger.warning("force_unlock: using explicit lock_source=%s", self.lock_source)
            return self.lock_source

        if self.lock_task_id:
            ti = self._get_ti(context)
            if ti is not None:
                lock_source = ti.xcom_pull(
                    key="active_lock_source", task_ids=self.lock_task_id
                )
                if lock_source:
                    logger.warning(
                        "force_unlock: resolved lock_source=%s from XCom of task '%s'",
                        lock_source,
                        self.lock_task_id,
                    )
                    return lock_source

        resolved = hook.resolve_lock_source(
            self.dataset_key_name, self.interval_start, self.interval_end
        )
        if resolved:
            logger.warning("force_unlock: resolved lock_source=%s from state service", resolved)
            return resolved

        logger.warning(
            "force_unlock: no LOCKED state found, falling back to DEFAULT_LOCK_SOURCE=%s",
            DEFAULT_LOCK_SOURCE,
        )
        return DEFAULT_LOCK_SOURCE

    def execute(self, context: Dict[str, Any]) -> str:
        if not self.dataset_key_name or not str(self.dataset_key_name).strip():
            raise AirflowException("dataset_key_name must not be empty")
        if not self.interval_start or not self.interval_end:
            raise AirflowException("interval_start and interval_end must not be empty")

        hook = YarinaStateHook(http_conn_id=self.http_conn_id)
        if self.force_unlock:
            lock_source = self._resolve_force_lock_source(hook, context)
        else:
            if not self.lock_source or not str(self.lock_source).strip():
                raise AirflowException(
                    "lock_source is required when force_unlock=False. "
                    "Pass the same lock_source that was used by the lock task."
                )
            lock_source = self.lock_source

        payload = {
            "dataSetKeyName": self.dataset_key_name,
            "intervalStartDateTime": self.interval_start,
            "intervalEndDateTime": self.interval_end,
            "status": YarinaStatus.SUCCESS.value,
            "lockSource": lock_source,
        }

        logger.info(
            "UNLOCK dataset=%s interval=[%s, %s] lockSource=%s",
            self.dataset_key_name,
            self.interval_start,
            self.interval_end,
            lock_source,
        )
        hook.put_state(payload)
        logger.info("SUCCESS dataset=%s lockSource=%s", self.dataset_key_name, lock_source)
        return lock_source
