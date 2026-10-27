# "Yarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
# Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at https://www.apache.org/licenses/LICENSE-2.0.txt

"""End-to-end lifecycle tests against an in-memory yarina-state-svc emulator.

The DAG shape is ``check -> lock -> etl -> unlock``; tasks are executed
manually so the tests do not need an Airflow metadata database.
"""

from __future__ import annotations

from typing import Any, Dict, List, Optional

import pytest
from airflow.exceptions import AirflowException

from plugins.constants import DEFAULT_LOCK_SOURCE
from plugins.operators.yarina_state_check import YarinaStateCheckOperator
from plugins.operators.yarina_state_lock import YarinaStateLockOperator
from plugins.operators.yarina_state_unlock import YarinaStateUnlockOperator

START = "2026-01-01T00:00:00Z"
END = "2026-01-02T00:00:00Z"
LOCK_SOURCE = "airflow-run-test"


class InMemoryStateService:
    """Tiny emulator of the yarina-state-svc interval state store."""

    def __init__(self) -> None:
        self.states: List[Dict[str, Any]] = []

    @staticmethod
    def _matches(state: Dict[str, Any], dataset: str, start: str, end: str) -> bool:
        return (
            state.get("dataSetKeyName") == dataset
            and state.get("intervalStartDateTime") == start
            and state.get("intervalEndDateTime") == end
        )

    def put_state(self, payload: Dict[str, Any]) -> None:
        for state in self.states:
            if self._matches(
                state,
                payload["dataSetKeyName"],
                payload["intervalStartDateTime"],
                payload["intervalEndDateTime"],
            ):
                state.update(payload)
                return
        self.states.append(dict(payload))

    def get_states(self, params: Dict[str, Any]) -> List[Dict[str, Any]]:
        return [
            dict(state)
            for state in self.states
            if self._matches(
                state,
                params["dataSetKeyName"],
                params["intervalStartDateTime"],
                params["intervalEndDateTime"],
            )
        ]

    def post_wrong(self, payload: Dict[str, Any]) -> Dict[str, Any]:
        states = self.get_states(payload)
        if not states:
            return {
                "wrongStates": [
                    {
                        "dataSetKeyName": payload["dataSetKeyName"],
                        "intervalStartDateTime": payload["intervalStartDateTime"],
                        "intervalEndDateTime": payload["intervalEndDateTime"],
                        "status": None,
                        "lockSource": "",
                    }
                ]
            }
        return {
            "wrongStates": [
                state for state in states if state.get("status") != "SUCCESS"
            ]
        }

    def find(self, dataset: str, start: str, end: str) -> Optional[Dict[str, Any]]:
        for state in self.states:
            if self._matches(state, dataset, start, end):
                return dict(state)
        return None


class EmulatorStateHook:
    """Drop-in replacement for YarinaStateHook backed by the emulator."""

    def __init__(self, service: InMemoryStateService) -> None:
        self.service = service

    def put_state(self, payload: Dict[str, Any]) -> None:
        self.service.put_state(payload)

    def get_states(self, params: Dict[str, Any]) -> List[Dict[str, Any]]:
        return self.service.get_states(params)

    def post_wrong(self, payload: Dict[str, Any]) -> Dict[str, Any]:
        return self.service.post_wrong(payload)

    def resolve_lock_source(
        self, dataset: str, start: str, end: str
    ) -> Optional[str]:
        states = self.service.get_states(
            {
                "dataSetKeyName": dataset,
                "intervalStartDateTime": start,
                "intervalEndDateTime": end,
            }
        )
        lock_sources = {
            state.get("lockSource")
            for state in states
            if state.get("status") == "LOCKED" and state.get("lockSource")
        }
        if len(lock_sources) > 1:
            raise AirflowException("Several LOCKED lockSources found")
        return next(iter(lock_sources), None)


class EmulatorConfigHook:
    """Drop-in replacement for YarinaConfigHook with a fixed source list."""

    def __init__(self, sources: List[str]) -> None:
        self.sources = sources

    def get_sources(self, key_name: str) -> List[str]:
        return list(self.sources)


def _patch_hooks(mocker, service: InMemoryStateService, sources: List[str]) -> None:
    factory = lambda **kwargs: EmulatorStateHook(service)  # noqa: E731
    mocker.patch(
        "plugins.operators.yarina_state_lock.YarinaStateHook", side_effect=factory
    )
    mocker.patch(
        "plugins.operators.yarina_state_unlock.YarinaStateHook", side_effect=factory
    )
    mocker.patch(
        "plugins.operators.yarina_state_check.YarinaStateHook", side_effect=factory
    )
    mocker.patch(
        "plugins.operators.yarina_state_check.YarinaConfigHook",
        return_value=EmulatorConfigHook(sources),
    )


def _build_dag(dag, etl_operator_cls, sources: List[str]):
    check = YarinaStateCheckOperator(
        task_id="check",
        dataset_key_name="target",
        interval_start=START,
        interval_end=END,
        lock_source=LOCK_SOURCE,
        source_datasets=sources,
        fail_on_holes=False,
        dag=dag,
    )
    lock = YarinaStateLockOperator(
        task_id="lock",
        dataset_key_name="target",
        interval_start=START,
        interval_end=END,
        lock_source=LOCK_SOURCE,
        dag=dag,
    )
    etl = etl_operator_cls(task_id="etl", dag=dag)
    unlock = YarinaStateUnlockOperator(
        task_id="unlock",
        dataset_key_name="target",
        interval_start=START,
        interval_end=END,
        lock_source=LOCK_SOURCE,
        dag=dag,
    )
    check >> lock >> etl >> unlock
    return check, lock, etl, unlock


def test_e2e01_happy_path(mocker, dag, context, etl_operator_cls):
    service = InMemoryStateService()
    _patch_hooks(mocker, service, sources=["source"])

    check, lock, etl, unlock = _build_dag(dag, etl_operator_cls, ["source"])

    check.execute(context)
    lock.execute(context)
    assert service.find("target", START, END)["status"] == "LOCKED"

    etl.execute(context)
    unlock.execute(context)

    state = service.find("target", START, END)
    assert state is not None
    assert state["status"] == "SUCCESS"
    assert state["lockSource"] == LOCK_SOURCE


def test_e2e02_lock_remains_on_failure(
    mocker, dag, context, etl_operator_cls, failing_etl_operator_cls
):
    service = InMemoryStateService()
    _patch_hooks(mocker, service, sources=["source"])

    check = YarinaStateCheckOperator(
        task_id="check",
        dataset_key_name="target",
        interval_start=START,
        interval_end=END,
        lock_source=LOCK_SOURCE,
        source_datasets=["source"],
        fail_on_holes=False,
        dag=dag,
    )
    lock = YarinaStateLockOperator(
        task_id="lock",
        dataset_key_name="target",
        interval_start=START,
        interval_end=END,
        lock_source=LOCK_SOURCE,
        dag=dag,
    )
    failing_etl = failing_etl_operator_cls(task_id="etl", dag=dag)
    check >> lock >> failing_etl

    check.execute(context)
    lock.execute(context)
    with pytest.raises(RuntimeError, match="ETL failed"):
        failing_etl.execute(context)

    state = service.find("target", START, END)
    assert state is not None
    assert state["status"] == "LOCKED"
    assert state["lockSource"] == LOCK_SOURCE


def test_e2e03_force_unlock(mocker, dag, context):
    service = InMemoryStateService()
    _patch_hooks(mocker, service, sources=[])

    service.put_state(
        {
            "dataSetKeyName": "target",
            "intervalStartDateTime": START,
            "intervalEndDateTime": END,
            "status": "LOCKED",
            "lockSource": "legacy-run",
        }
    )

    unlock = YarinaStateUnlockOperator(
        task_id="unlock",
        dataset_key_name="target",
        interval_start=START,
        interval_end=END,
        lock_source=None,
        force_unlock=True,
        lock_task_id=None,
        dag=dag,
    )

    result = unlock.execute(context)

    assert result == "legacy-run"
    state = service.find("target", START, END)
    assert state is not None
    assert state["status"] == "SUCCESS"
    assert state["lockSource"] == "legacy-run"


def test_e2e03b_force_unlock_falls_back_to_default(mocker, dag, context):
    service = InMemoryStateService()
    _patch_hooks(mocker, service, sources=[])

    unlock = YarinaStateUnlockOperator(
        task_id="unlock",
        dataset_key_name="target",
        interval_start=START,
        interval_end=END,
        lock_source=None,
        force_unlock=True,
        lock_task_id=None,
        dag=dag,
    )

    assert unlock.execute(context) == DEFAULT_LOCK_SOURCE
    assert service.find("target", START, END)["status"] == "SUCCESS"
