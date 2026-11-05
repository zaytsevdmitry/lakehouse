# "Yarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
# Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at https://www.apache.org/licenses/LICENSE-2.0.txt

"""Hook for ``yarina-state-svc`` interval states."""

from __future__ import annotations

import logging
from typing import Any, Dict, List, Optional

from airflow.exceptions import AirflowException

try:  # package import (``plugins.*``)
    from plugins.constants import (
        DEFAULT_HTTP_CONN_ID,
        STATE_DATASET_ENDPOINT,
        STATE_DATASET_WRONG_ENDPOINT,
        YarinaStatus,
    )
    from plugins.hooks.yarina_base_hook import YarinaBaseHook
except ImportError:  # pragma: no cover - Airflow loads plugin files as top-level modules
    from constants import (  # type: ignore[no-redef]
        DEFAULT_HTTP_CONN_ID,
        STATE_DATASET_ENDPOINT,
        STATE_DATASET_WRONG_ENDPOINT,
        YarinaStatus,
    )
    from hooks.yarina_base_hook import YarinaBaseHook  # type: ignore[no-redef]

logger = logging.getLogger(__name__)


class YarinaStateHook(YarinaBaseHook):
    """Read and write ``LOCKED``/``SUCCESS`` states of dataset intervals."""

    def __init__(
        self,
        method: str = "GET",
        http_conn_id: str = DEFAULT_HTTP_CONN_ID,
        *args: Any,
        **kwargs: Any,
    ) -> None:
        super().__init__(method=method, http_conn_id=http_conn_id, *args, **kwargs)

    def put_state(self, payload: Dict[str, Any]) -> Any:
        """Write a state (``Data_SetStateDTO``) via ``PUT v1_0/state/dataset``."""
        logger.info(
            "PUT state dataset=%s interval=[%s, %s] status=%s lockSource=%s",
            payload.get("dataSetKeyName"),
            payload.get("intervalStartDateTime"),
            payload.get("intervalEndDateTime"),
            payload.get("status"),
            payload.get("lockSource"),
        )
        return self._request_json(
            "PUT", STATE_DATASET_ENDPOINT, payload, npe_hint=True
        )

    def get_states(self, params: Dict[str, Any]) -> List[Dict[str, Any]]:
        """Return states overlapping an interval.

        The service contract uses a GET request with the interval passed as
        query parameters. Parameters are passed through ``data=`` (HttpHook
        turns them into query parameters for GET); ``json=`` is not used.
        """
        response = self._request("GET", STATE_DATASET_ENDPOINT, data=params)
        self._check(response, STATE_DATASET_ENDPOINT, npe_hint=True)
        try:
            body = response.json()
        except ValueError:
            body = []
        return body if isinstance(body, list) else []

    def post_wrong(self, payload: Dict[str, Any]) -> Dict[str, Any]:
        """Return intervals not covered by ``SUCCESS`` (``POST .../wrong``)."""
        response = self._request_json(
            "POST", STATE_DATASET_WRONG_ENDPOINT, payload, npe_hint=True
        )
        try:
            body = response.json() or {}
        except ValueError:
            body = {}
        if not isinstance(body, dict):
            body = {}
        wrong_states = body.get("wrongStates")
        body["wrongStates"] = wrong_states if isinstance(wrong_states, list) else []
        return body

    def resolve_lock_source(
        self, dataset: str, start: str, end: str
    ) -> Optional[str]:
        """Return the single ``LOCKED`` lockSource for the interval, if any.

        Raises :class:`AirflowException` when several different lock sources
        are present - the caller must then use an explicit ``lock_source``.
        """
        states = self.get_states(
            {
                "dataSetKeyName": dataset,
                "intervalStartDateTime": start,
                "intervalEndDateTime": end,
            }
        )
        lock_sources: List[str] = []
        for state in states:
            if not isinstance(state, dict):
                continue
            lock_source = state.get("lockSource")
            if (
                state.get("status") == YarinaStatus.LOCKED.value
                and lock_source
                and lock_source not in lock_sources
            ):
                lock_sources.append(lock_source)

        if len(lock_sources) > 1:
            raise AirflowException(
                "Several LOCKED lockSources found for dataset '{}' interval [{}, {}]: {}. "
                "Pass an explicit lock_source or resolve the conflict manually.".format(
                    dataset, start, end, ", ".join(lock_sources)
                )
            )
        return lock_sources[0] if lock_sources else None

    def find_foreign_locks(
        self,
        dataset: str,
        start: str,
        end: str,
        own_lock_source: Optional[str],
    ) -> List[Dict[str, Any]]:
        """Return ``LOCKED`` states whose lockSource differs from ``own_lock_source``."""
        wrong = self.post_wrong(
            {
                "dataSetKeyName": dataset,
                "intervalStartDateTime": start,
                "intervalEndDateTime": end,
            }
        )
        result: List[Dict[str, Any]] = []
        for state in wrong.get("wrongStates", []):
            if not isinstance(state, dict):
                continue
            lock_source = state.get("lockSource")
            if (
                state.get("status") == YarinaStatus.LOCKED.value
                and lock_source
                and lock_source != own_lock_source
            ):
                result.append(state)
        return result
