# "Yarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
# Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at https://www.apache.org/licenses/LICENSE-2.0.txt

"""Unit tests for YarinaSparkSubmitOperator."""

from __future__ import annotations

from typing import Any, Dict

import pytest
from airflow.exceptions import AirflowException

import plugins.operators.yarina_spark_submit as operator_module
from plugins.hooks.yarina_spark_proxy_hook import YarinaSparkProxyHook
from plugins.operators.yarina_spark_submit import YarinaSparkSubmitOperator


def make_operator(**overrides: Any) -> YarinaSparkSubmitOperator:
    params: Dict[str, Any] = {
        "task_id": "submit_spark_job",
        "app_resource": "s3a://bucket/app.jar",
    }
    params.update(overrides)
    return YarinaSparkSubmitOperator(**params)


def successful_create(submission_id: str = "42") -> Dict[str, Any]:
    return {
        "action": "CreateSubmissionResponse",
        "message": "WAITING",
        "submissionId": submission_id,
        "success": True,
    }


# ---------------------------------------------------------------------------
# Validation
# ---------------------------------------------------------------------------


@pytest.mark.parametrize("app_resource", ["", "   "])
def test_empty_app_resource_raises(app_resource):
    with pytest.raises(ValueError, match="app_resource"):
        make_operator(app_resource=app_resource)


@pytest.mark.parametrize(
    "kwargs",
    [
        {"poll_interval_seconds": 0},
        {"poll_interval_seconds": -1},
        {"timeout_seconds": 0},
        {"timeout_seconds": -1},
    ],
)
def test_invalid_intervals_raise(kwargs):
    with pytest.raises(ValueError):
        make_operator(**kwargs)


# ---------------------------------------------------------------------------
# Submission creation
# ---------------------------------------------------------------------------


def test_create_payload_serialization(mocker, mock_spark_hook, ti, context, fast_sleep):
    mock_spark_hook.create_submission.return_value = successful_create()
    mock_spark_hook.get_status.return_value = {"message": "FINISHED"}

    operator = make_operator(
        main_class="com.example.Main",
        app_args=["--date", "2026-01-01"],
        spark_properties={"spark.app.name": "airflow-test"},
        environment_variables={"AWS_REGION": "eu-west-1"},
        client_spark_version="3.5.0",
    )

    result = operator.execute(context)

    assert result == {"submissionId": "42", "message": "FINISHED"}
    mock_spark_hook.create_submission.assert_called_once_with(
        {
            "action": "CreateSubmissionRequest",
            "appResource": "s3a://bucket/app.jar",
            "clientSparkVersion": "3.5.0",
            "mainClass": "com.example.Main",
            "environmentVariables": {"AWS_REGION": "eu-west-1"},
            "sparkProperties": {"spark.app.name": "airflow-test"},
            "appArgs": ["--date", "2026-01-01"],
        }
    )
    assert [call[0] for call in mock_spark_hook.method_calls] == [
        "create_submission",
        "get_status",
    ]


def test_create_failure_raises(mock_spark_hook, ti, context, fast_sleep):
    mock_spark_hook.create_submission.return_value = {
        "action": "CreateSubmissionResponse",
        "message": "CreateErrorException: boom",
        "submissionId": None,
        "success": False,
    }

    operator = make_operator()

    with pytest.raises(AirflowException) as exc:
        operator.execute(context)
    message = str(exc.value)
    assert "CreateErrorException: boom" in message
    assert "s3a://bucket/app.jar" in message
    mock_spark_hook.get_status.assert_not_called()
    ti.xcom_push.assert_not_called()


@pytest.mark.parametrize("submission_id", [None, "", "   "])
def test_create_missing_submission_id_raises(
    mock_spark_hook, ti, context, fast_sleep, submission_id
):
    mock_spark_hook.create_submission.return_value = {
        "action": "CreateSubmissionResponse",
        "message": "WAITING",
        "submissionId": submission_id,
        "success": True,
    }

    with pytest.raises(AirflowException, match="submissionId"):
        make_operator().execute(context)
    mock_spark_hook.get_status.assert_not_called()


def test_xcom_push_submission_id(mock_spark_hook, ti, context, fast_sleep):
    mock_spark_hook.create_submission.return_value = successful_create()
    mock_spark_hook.get_status.return_value = {"message": "FINISHED"}

    make_operator().execute(context)

    ti.xcom_push.assert_called_once_with(
        key="spark_submission_id", value="42"
    )


# ---------------------------------------------------------------------------
# Polling
# ---------------------------------------------------------------------------


def test_poll_until_finished(mock_spark_hook, ti, context, fast_sleep):
    mock_spark_hook.create_submission.return_value = successful_create()
    mock_spark_hook.get_status.side_effect = [
        {"message": "WAITING"},
        {"message": "SUBMITTED"},
        {"message": "RUNNING"},
        {"message": "FINISHED"},
    ]

    result = make_operator().execute(context)

    assert result == {"submissionId": "42", "message": "FINISHED"}
    assert mock_spark_hook.get_status.call_count == 4
    assert fast_sleep.call_count == 3


def _assert_terminal_failure(mock_spark_hook, ti, context, fast_sleep, status):
    mock_spark_hook.create_submission.return_value = successful_create()
    mock_spark_hook.get_status.return_value = {"message": status}

    with pytest.raises(AirflowException) as exc:
        make_operator().execute(context)
    message = str(exc.value)
    assert status in message
    assert "42" in message
    assert mock_spark_hook.get_status.call_count == 1
    fast_sleep.assert_not_called()


def test_failed_raises(mock_spark_hook, ti, context, fast_sleep):
    _assert_terminal_failure(mock_spark_hook, ti, context, fast_sleep, "FAILED")


def test_killed_raises(mock_spark_hook, ti, context, fast_sleep):
    _assert_terminal_failure(mock_spark_hook, ti, context, fast_sleep, "KILLED")


def test_unknown_streak_fails(mock_spark_hook, ti, context, fast_sleep):
    mock_spark_hook.create_submission.return_value = successful_create()
    mock_spark_hook.get_status.return_value = {"message": "UNKNOWN"}

    with pytest.raises(AirflowException) as exc:
        make_operator(max_consecutive_unknown=5).execute(context)

    assert "5 times" in str(exc.value)
    assert mock_spark_hook.get_status.call_count == 5
    assert fast_sleep.call_count == 4


def test_unknown_resets_on_transient(mock_spark_hook, ti, context, fast_sleep):
    mock_spark_hook.create_submission.return_value = successful_create()
    mock_spark_hook.get_status.side_effect = [
        {"message": "UNKNOWN"},
        {"message": "WAITING"},
        {"message": "UNKNOWN"},
        {"message": "FINISHED"},
    ]

    result = make_operator(max_consecutive_unknown=2).execute(context)

    assert result == {"submissionId": "42", "message": "FINISHED"}
    assert mock_spark_hook.get_status.call_count == 4
    assert fast_sleep.call_count == 3


def test_timeout_raises_without_extra_calls(
    mocker, mock_spark_hook, ti, context, fast_sleep
):
    mock_spark_hook.create_submission.return_value = successful_create()
    mock_spark_hook.get_status.return_value = {"message": "WAITING"}
    mocker.patch.object(
        operator_module.time, "monotonic", side_effect=[0.0, 0.0, 100.0]
    )

    operator = make_operator(timeout_seconds=10, poll_interval_seconds=5)

    with pytest.raises(AirflowException) as exc:
        operator.execute(context)

    message = str(exc.value)
    assert "Таймаут 10s" in message
    assert "42" in message
    assert "GET /v1/submissions/status/42" in message
    mock_spark_hook.create_submission.assert_called_once()
    assert mock_spark_hook.get_status.call_count == 1
    assert fast_sleep.call_count == 1
    # After the timeout there are no additional HTTP calls of any kind.
    assert [call[0] for call in mock_spark_hook.method_calls] == [
        "create_submission",
        "get_status",
    ]


def test_status_404_propagates(mock_spark_hook, ti, context, fast_sleep):
    mock_spark_hook.create_submission.return_value = successful_create()
    mock_spark_hook.get_status.side_effect = AirflowException(
        "submission '42' не найден (404)"
    )

    with pytest.raises(AirflowException, match="не найден"):
        make_operator().execute(context)

    assert mock_spark_hook.get_status.call_count == 1
    fast_sleep.assert_not_called()


# ---------------------------------------------------------------------------
# API surface: no kill endpoints
# ---------------------------------------------------------------------------


def test_hook_and_operator_have_no_kill_api():
    for name in (
        "kill_submission",
        "kill_all_submissions",
        "clear_submissions",
        "kill",
        "kill_all",
        "clear",
    ):
        assert not hasattr(YarinaSparkProxyHook, name)

    # BaseOperator itself exposes ``clear`` (a DAG/task-instance operation),
    # so only the proxy-specific kill surface is checked on the operator.
    for name in (
        "kill_submission",
        "kill_all_submissions",
        "clear_submissions",
        "kill",
        "kill_all",
    ):
        assert not hasattr(YarinaSparkSubmitOperator, name)
