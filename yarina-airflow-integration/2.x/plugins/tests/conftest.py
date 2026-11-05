# "Yarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
# Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at https://www.apache.org/licenses/LICENSE-2.0.txt

"""Shared pytest fixtures for the Airflow 2.x plugin tests."""

from __future__ import annotations

import json
import sys
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, Optional
from unittest.mock import MagicMock

import pytest
from airflow import DAG
from airflow.models import BaseOperator

# Make the ``plugins`` package importable regardless of the current directory.
PLUGINS_ROOT = Path(__file__).resolve().parents[2]
if str(PLUGINS_ROOT) not in sys.path:
    sys.path.insert(0, str(PLUGINS_ROOT))

from plugins.hooks.yarina_base_hook import YarinaBaseHook  # noqa: E402
from plugins.hooks.yarina_spark_proxy_hook import YarinaSparkProxyHook  # noqa: E402
from plugins.operators import yarina_spark_submit as spark_operator_module  # noqa: E402
from plugins.operators import yarina_state_check as check_module  # noqa: E402
from plugins.operators import yarina_state_lock as lock_module  # noqa: E402
from plugins.operators import yarina_state_unlock as unlock_module  # noqa: E402


class FakeResponse:
    """Minimal ``requests.Response`` replacement."""

    def __init__(
        self,
        status_code: int = 200,
        payload: Any = None,
        text: Optional[str] = None,
    ) -> None:
        self.status_code = status_code
        self._payload = payload
        if text is not None:
            self.text = text
        elif payload is not None:
            self.text = json.dumps(payload)
        else:
            self.text = ""

    def json(self) -> Any:
        if self._payload is None:
            raise ValueError("response body is not JSON")
        return self._payload


class StubETLOperator(BaseOperator):
    """No-op ETL task used by the lifecycle tests."""

    def execute(self, context: Dict[str, Any]) -> str:
        return "etl"


class FailingETLOperator(BaseOperator):
    """ETL task that always fails, used by the failure lifecycle test."""

    def execute(self, context: Dict[str, Any]) -> str:
        raise RuntimeError("ETL failed")


@pytest.fixture
def fake_response():
    """Factory for :class:`FakeResponse` instances."""

    def _make(
        status_code: int = 200,
        payload: Any = None,
        text: Optional[str] = None,
    ) -> FakeResponse:
        return FakeResponse(status_code=status_code, payload=payload, text=text)

    return _make


@pytest.fixture
def connection() -> MagicMock:
    """A Connection-like object with state-svc defaults."""
    conn = MagicMock()
    conn.host = "http://yarina-state-svc:8082"
    conn.login = "yarina-internal-client"
    conn.password = "client-secret"
    conn.extra_dejson = {
        "auth_url": "http://keycloak:8085/realms/yarina/protocol/openid-connect/token"
    }
    return conn


@pytest.fixture
def patch_connection(mocker, connection) -> MagicMock:
    """Patch Connection lookup for every hook instantiation."""
    mocker.patch.object(YarinaBaseHook, "get_connection", return_value=connection)
    return connection


@pytest.fixture
def ti() -> MagicMock:
    """Mock TaskInstance with XCom support."""
    task_instance = MagicMock()
    task_instance.xcom_pull.return_value = None
    return task_instance


@pytest.fixture
def context(ti) -> Dict[str, Any]:
    """Task execution context exposing both 2.x and 3.x TaskInstance keys."""
    return {"ti": ti, "task_instance": ti}


@pytest.fixture
def mock_hook(mocker) -> MagicMock:
    """Patch ``YarinaStateHook`` in all operator modules with one mock."""
    hook = MagicMock()
    for module in (lock_module, unlock_module, check_module):
        mocker.patch.object(module, "YarinaStateHook", return_value=hook)
    return hook


@pytest.fixture
def mock_config_hook(mocker) -> MagicMock:
    """Patch ``YarinaConfigHook`` in the check operator module."""
    hook = MagicMock()
    hook.get_sources.return_value = []
    mocker.patch.object(check_module, "YarinaConfigHook", return_value=hook)
    return hook


@pytest.fixture
def spark_hook(patch_connection) -> YarinaSparkProxyHook:
    """A Spark proxy hook with OAuth disabled, for request-shape tests."""
    patch_connection.extra_dejson = {"security_enabled": False}
    return YarinaSparkProxyHook()


@pytest.fixture
def mock_spark_hook(mocker) -> MagicMock:
    """Patch ``YarinaSparkProxyHook`` in the Spark submit operator module."""
    hook = MagicMock()
    mocker.patch.object(
        spark_operator_module, "YarinaSparkProxyHook", return_value=hook
    )
    return hook


@pytest.fixture
def fast_sleep(mocker) -> MagicMock:
    """Replace the Spark operator sleep with a no-op recorder."""
    return mocker.patch.object(spark_operator_module.time, "sleep")


@pytest.fixture
def dag() -> DAG:
    """A tiny DAG for operator/lifecycle tests."""
    return DAG(
        "yarina_airflow_integration_test",
        start_date=datetime(2026, 1, 1),
        schedule="@daily",
        catchup=False,
    )


@pytest.fixture
def etl_operator_cls():
    return StubETLOperator


@pytest.fixture
def failing_etl_operator_cls():
    return FailingETLOperator
