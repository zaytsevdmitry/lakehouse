# "Yarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
# Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at https://www.apache.org/licenses/LICENSE-2.0.txt

"""Dependency/concurrency check operator.

The logic mirrors ``DependencyCheckStateTaskProcessor`` from
``yarina-task-executor-svc``:

* every source dataset is checked through ``POST v1_0/state/dataset/wrong``;
* a source state is acceptable only when it is the current run's ``LOCKED``
  state covering exactly the whole interval;
* the target dataset itself fails only when a foreign ``LOCKED`` state exists;
* holes (intervals without ``SUCCESS``) are reported only when
  ``fail_on_holes=True``.
"""

from __future__ import annotations

import logging
from datetime import datetime
from typing import Any, Dict, List, Optional, Tuple

from airflow.exceptions import AirflowException
from airflow.sdk import BaseOperator

try:  # package import (``plugins.*``)
    from plugins.constants import (
        DEFAULT_CONFIG_HTTP_CONN_ID,
        DEFAULT_HTTP_CONN_ID,
        DEFAULT_LOCK_SOURCE,
        YarinaStatus,
    )
    from plugins.hooks.yarina_config_hook import YarinaConfigHook
    from plugins.hooks.yarina_state_hook import YarinaStateHook
except ImportError:  # pragma: no cover - Airflow loads plugin files as top-level modules
    from constants import (  # type: ignore[no-redef]
        DEFAULT_CONFIG_HTTP_CONN_ID,
        DEFAULT_HTTP_CONN_ID,
        DEFAULT_LOCK_SOURCE,
        YarinaStatus,
    )
    from hooks.yarina_config_hook import YarinaConfigHook  # type: ignore[no-redef]
    from hooks.yarina_state_hook import YarinaStateHook  # type: ignore[no-redef]

logger = logging.getLogger(__name__)


def _parse_datetime(value: Any) -> Any:
    """Parse an ISO datetime for comparison, returning the raw value on error."""
    if not isinstance(value, str):
        return value
    try:
        return datetime.fromisoformat(value.replace("Z", "+00:00"))
    except ValueError:
        return value


class YarinaStateCheckOperator(BaseOperator):
    """Check dependencies and foreign locks before processing an interval.

    ``source_datasets`` may be supplied explicitly. When it is omitted and
    ``auto_resolve_sources=True`` (default), the list is read from
    ``yarina-config-svc`` via ``DataSetDTO.sources``.
    """

    template_fields: Tuple[str, ...] = (
        "dataset_key_name",
        "interval_start",
        "interval_end",
        "lock_source",
        "source_datasets",
    )

    def __init__(
        self,
        dataset_key_name: str,
        interval_start: str,
        interval_end: str,
        lock_source: str = DEFAULT_LOCK_SOURCE,
        source_datasets: Optional[List[str]] = None,
        auto_resolve_sources: bool = True,
        config_http_conn_id: str = DEFAULT_CONFIG_HTTP_CONN_ID,
        state_http_conn_id: str = DEFAULT_HTTP_CONN_ID,
        fail_on_foreign_lock: bool = True,
        fail_on_holes: bool = False,
        **kwargs: Any,
    ) -> None:
        super().__init__(**kwargs)
        self.dataset_key_name = dataset_key_name
        self.interval_start = interval_start
        self.interval_end = interval_end
        self.lock_source = lock_source
        self.source_datasets = source_datasets
        self.auto_resolve_sources = auto_resolve_sources
        self.config_http_conn_id = config_http_conn_id
        self.state_http_conn_id = state_http_conn_id
        self.fail_on_foreign_lock = fail_on_foreign_lock
        self.fail_on_holes = fail_on_holes

    def _validate(self) -> None:
        if not self.dataset_key_name or not str(self.dataset_key_name).strip():
            raise AirflowException("dataset_key_name must not be empty")
        if not self.interval_start or not self.interval_end:
            raise AirflowException("interval_start and interval_end must not be empty")
        if not self.lock_source or not str(self.lock_source).strip():
            raise AirflowException(
                "lock_source must not be empty; the state service rejects a null lockSource"
            )

    def _resolve_sources(self) -> List[str]:
        if self.source_datasets is not None:
            sources = list(self.source_datasets)
            logger.info(
                "CHECK dataset=%s explicit source datasets: %s",
                self.dataset_key_name,
                sources,
            )
            return sources
        if not self.auto_resolve_sources:
            logger.info(
                "CHECK dataset=%s source auto-resolution disabled, no sources",
                self.dataset_key_name,
            )
            return []

        config_hook = YarinaConfigHook(http_conn_id=self.config_http_conn_id)
        sources = config_hook.get_sources(self.dataset_key_name)
        logger.info(
            "CHECK dataset=%s sources resolved from yarina-config-svc: %s",
            self.dataset_key_name,
            sources,
        )
        return sources

    def _interval_payload(self, dataset: str) -> Dict[str, str]:
        return {
            "dataSetKeyName": dataset,
            "intervalStartDateTime": self.interval_start,
            "intervalEndDateTime": self.interval_end,
        }

    def _same_interval(self, state: Dict[str, Any]) -> bool:
        return (
            _parse_datetime(state.get("intervalStartDateTime"))
            == _parse_datetime(self.interval_start)
            and _parse_datetime(state.get("intervalEndDateTime"))
            == _parse_datetime(self.interval_end)
        )

    def _pass_source(self, state: Dict[str, Any], source_dataset: str) -> bool:
        """Return ``True`` when a source state is a **problem**.

        This mirrors the ``pass`` predicate of
        ``DependencyCheckStateTaskProcessor``: the state is acceptable only if
        it belongs to the expected source, is ``LOCKED``, uses our own
        ``lock_source`` and covers exactly the whole requested interval.
        ``lockSource=None`` is treated as a problem instead of raising
        ``AttributeError`` (unlike the Java version).
        """
        return (
            state.get("dataSetKeyName") != source_dataset
            or state.get("status") != YarinaStatus.LOCKED.value
            or not state.get("lockSource")
            or state.get("lockSource") != self.lock_source
            or not self._same_interval(state)
        )

    @staticmethod
    def _format_problem(state: Dict[str, Any], reason: str) -> str:
        return (
            "dataset={} status={} lockSource={} interval=[{}, {}] reason={}".format(
                state.get("dataSetKeyName"),
                state.get("status"),
                state.get("lockSource"),
                state.get("intervalStartDateTime"),
                state.get("intervalEndDateTime"),
                reason,
            )
        )

    def execute(self, context: Dict[str, Any]) -> bool:
        self._validate()
        state_hook = YarinaStateHook(http_conn_id=self.state_http_conn_id)
        sources = self._resolve_sources()
        problems: List[str] = []

        for source_dataset in sources:
            wrong = state_hook.post_wrong(self._interval_payload(source_dataset))
            for state in wrong.get("wrongStates", []):
                if not isinstance(state, dict):
                    continue

                status = state.get("status")
                if status == YarinaStatus.LOCKED.value:
                    if self._pass_source(state, source_dataset):
                        lock_source = state.get("lockSource")
                        if lock_source and lock_source != self.lock_source:
                            reason = "foreign LOCKED by '{}'".format(lock_source)
                        elif not lock_source:
                            reason = "LOCKED with empty lockSource"
                        else:
                            reason = (
                                "LOCKED by '{}' does not cover the whole interval".format(
                                    lock_source
                                )
                            )
                        problems.append(self._format_problem(state, reason))
                elif self.fail_on_holes:
                    problems.append(
                        self._format_problem(
                            state, "hole: interval is not covered by SUCCESS"
                        )
                    )

        own_wrong = state_hook.post_wrong(self._interval_payload(self.dataset_key_name))
        if self.fail_on_foreign_lock:
            for state in own_wrong.get("wrongStates", []):
                if not isinstance(state, dict):
                    continue
                lock_source = state.get("lockSource")
                if (
                    state.get("status") == YarinaStatus.LOCKED.value
                    and lock_source is not None
                    and lock_source != self.lock_source
                ):
                    logger.warning(
                        "CHECK dataset=%s found foreign LOCKED lockSource=%s",
                        self.dataset_key_name,
                        lock_source,
                    )
                    problems.append(
                        self._format_problem(
                            state, "foreign LOCKED by '{}'".format(lock_source)
                        )
                    )

        if problems:
            message = (
                "Dependency check failed for dataset '{}' interval [{}, {}]:\n{}"
            ).format(
                self.dataset_key_name,
                self.interval_start,
                self.interval_end,
                "\n".join("- {}".format(problem) for problem in problems),
            )
            logger.error(message)
            raise AirflowException(message)

        logger.info(
            "CHECK OK dataset=%s interval=[%s, %s] lockSource=%s sources=%s",
            self.dataset_key_name,
            self.interval_start,
            self.interval_end,
            self.lock_source,
            sources,
        )
        return True
