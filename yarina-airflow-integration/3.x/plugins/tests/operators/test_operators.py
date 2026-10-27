# "Yarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
# Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at https://www.apache.org/licenses/LICENSE-2.0.txt

"""Unit tests for lock/unlock/check operators."""

from __future__ import annotations

import pytest
from airflow.exceptions import AirflowException

from plugins.constants import DEFAULT_LOCK_SOURCE
from plugins.operators.yarina_state_check import YarinaStateCheckOperator
from plugins.operators.yarina_state_lock import YarinaStateLockOperator
from plugins.operators.yarina_state_unlock import YarinaStateUnlockOperator

START = "2026-01-01T00:00:00Z"
END = "2026-01-02T00:00:00Z"


def _lock(**kwargs):
    params = {
        "task_id": "lock",
        "dataset_key_name": "target",
        "interval_start": START,
        "interval_end": END,
        "lock_source": "lock-1",
    }
    params.update(kwargs)
    return YarinaStateLockOperator(**params)


def _unlock(**kwargs):
    params = {
        "task_id": "unlock",
        "dataset_key_name": "target",
        "interval_start": START,
        "interval_end": END,
        "lock_source": "lock-1",
    }
    params.update(kwargs)
    return YarinaStateUnlockOperator(**params)


def _check(**kwargs):
    params = {
        "task_id": "check",
        "dataset_key_name": "target",
        "interval_start": START,
        "interval_end": END,
        "lock_source": "lock-1",
        "source_datasets": [],
    }
    params.update(kwargs)
    return YarinaStateCheckOperator(**params)


# ---------------------------------------------------------------------------
# Lock operator
# ---------------------------------------------------------------------------


def test_lock_empty_lock_source_raises(mock_hook, context):
    with pytest.raises(AirflowException, match="lock_source"):
        _lock(lock_source="").execute(context)
    mock_hook.put_state.assert_not_called()


def test_lock_payload_and_xcom(mock_hook, ti, context):
    result = _lock(lock_source="run-1").execute(context)

    assert result == "run-1"
    payload = mock_hook.put_state.call_args.args[0]
    assert payload == {
        "dataSetKeyName": "target",
        "intervalStartDateTime": START,
        "intervalEndDateTime": END,
        "status": "LOCKED",
        "lockSource": "run-1",
    }
    ti.xcom_push.assert_called_once_with(key="active_lock_source", value="run-1")


def test_lock_foreign_lock_propagates(mock_hook, ti, context):
    mock_hook.put_state.side_effect = AirflowException("locked by another run")

    with pytest.raises(AirflowException, match="locked by another run"):
        _lock().execute(context)
    ti.xcom_push.assert_not_called()


# ---------------------------------------------------------------------------
# Unlock operator
# ---------------------------------------------------------------------------


def test_unlock_requires_lock_source_without_force(mock_hook, context):
    with pytest.raises(AirflowException, match="lock_source is required"):
        _unlock(lock_source=None, force_unlock=False).execute(context)
    mock_hook.put_state.assert_not_called()


def test_unlock_sequential(mock_hook, context):
    result = _unlock(lock_source="run-1").execute(context)

    assert result == "run-1"
    payload = mock_hook.put_state.call_args.args[0]
    assert payload["status"] == "SUCCESS"
    assert payload["lockSource"] == "run-1"
    mock_hook.resolve_lock_source.assert_not_called()


def test_unlock_force_uses_explicit(mock_hook, context):
    result = _unlock(
        lock_source="explicit-run", force_unlock=True
    ).execute(context)

    assert result == "explicit-run"
    assert mock_hook.put_state.call_args.args[0]["lockSource"] == "explicit-run"
    mock_hook.resolve_lock_source.assert_not_called()


def test_unlock_force_resolves_from_xcom(mock_hook, ti, context):
    ti.xcom_pull.return_value = "xcom-run"

    result = _unlock(
        lock_source=None,
        force_unlock=True,
        lock_task_id="lock-task",
    ).execute(context)

    assert result == "xcom-run"
    ti.xcom_pull.assert_called_once_with(
        key="active_lock_source", task_ids="lock-task"
    )
    assert mock_hook.put_state.call_args.args[0]["lockSource"] == "xcom-run"
    mock_hook.resolve_lock_source.assert_not_called()


def test_unlock_force_resolves_from_get(mock_hook, ti, context):
    ti.xcom_pull.return_value = None
    mock_hook.resolve_lock_source.return_value = "get-run"

    result = _unlock(
        lock_source=None,
        force_unlock=True,
        lock_task_id="lock-task",
    ).execute(context)

    assert result == "get-run"
    assert mock_hook.put_state.call_args.args[0]["lockSource"] == "get-run"


def test_unlock_force_no_locked_falls_back(mock_hook, ti, context):
    ti.xcom_pull.return_value = None
    mock_hook.resolve_lock_source.return_value = None

    result = _unlock(
        lock_source=None,
        force_unlock=True,
        lock_task_id="lock-task",
    ).execute(context)

    assert result == DEFAULT_LOCK_SOURCE
    assert mock_hook.put_state.call_args.args[0]["lockSource"] == DEFAULT_LOCK_SOURCE


def test_unlock_force_default_lock_source_is_not_used_as_explicit(mock_hook, context):
    mock_hook.resolve_lock_source.return_value = "resolved-run"

    result = _unlock(
        lock_source=DEFAULT_LOCK_SOURCE,
        force_unlock=True,
    ).execute(context)

    assert result == "resolved-run"


def test_unlock_foreign_lock_raises(mock_hook, context):
    mock_hook.put_state.side_effect = AirflowException("lock source mismatch")

    with pytest.raises(AirflowException, match="lock source mismatch"):
        _unlock(lock_source="run-1").execute(context)


# ---------------------------------------------------------------------------
# Check operator
# ---------------------------------------------------------------------------


def _set_wrong_states(mock_hook, states_by_dataset):
    def _post_wrong(payload):
        return {"wrongStates": list(states_by_dataset.get(payload["dataSetKeyName"], []))}

    mock_hook.post_wrong.side_effect = _post_wrong


def test_check_all_success(mock_hook, context):
    _set_wrong_states(mock_hook, {"target": [], "source": []})

    assert _check(source_datasets=["source"]).execute(context) is True


def test_check_hole_only_ok(mock_hook, context):
    _set_wrong_states(
        mock_hook,
        {
            "target": [],
            "source": [
                {
                    "dataSetKeyName": "source",
                    "intervalStartDateTime": START,
                    "intervalEndDateTime": END,
                    "status": None,
                    "lockSource": "",
                }
            ],
        },
    )

    assert (
        _check(source_datasets=["source"], fail_on_holes=False).execute(context) is True
    )


def test_check_hole_fail_when_requested(mock_hook, context):
    _set_wrong_states(
        mock_hook,
        {
            "target": [],
            "source": [
                {
                    "dataSetKeyName": "source",
                    "intervalStartDateTime": START,
                    "intervalEndDateTime": END,
                    "status": None,
                    "lockSource": "",
                }
            ],
        },
    )

    with pytest.raises(AirflowException, match="hole"):
        _check(source_datasets=["source"], fail_on_holes=True).execute(context)


def test_check_foreign_lock_fails(mock_hook, context):
    _set_wrong_states(
        mock_hook,
        {
            "target": [
                {
                    "dataSetKeyName": "target",
                    "intervalStartDateTime": START,
                    "intervalEndDateTime": END,
                    "status": "LOCKED",
                    "lockSource": "another-run",
                }
            ],
            "source": [],
        },
    )

    with pytest.raises(AirflowException, match="foreign LOCKED"):
        _check(source_datasets=["source"]).execute(context)


def test_check_own_lock_not_foreign(mock_hook, context):
    _set_wrong_states(
        mock_hook,
        {
            "target": [
                {
                    "dataSetKeyName": "target",
                    "intervalStartDateTime": START,
                    "intervalEndDateTime": END,
                    "status": "LOCKED",
                    "lockSource": "lock-1",
                }
            ],
            "source": [],
        },
    )

    assert _check(source_datasets=["source"]).execute(context) is True


def test_check_foreign_lock_can_be_ignored(mock_hook, context):
    _set_wrong_states(
        mock_hook,
        {
            "target": [
                {
                    "dataSetKeyName": "target",
                    "intervalStartDateTime": START,
                    "intervalEndDateTime": END,
                    "status": "LOCKED",
                    "lockSource": "another-run",
                }
            ],
            "source": [],
        },
    )

    assert (
        _check(
            source_datasets=["source"], fail_on_foreign_lock=False
        ).execute(context)
        is True
    )


def test_check_empty_lock_source_rejected(mock_hook, context):
    with pytest.raises(AirflowException, match="lock_source"):
        _check(lock_source="").execute(context)
    mock_hook.post_wrong.assert_not_called()


def test_check_resolves_sources_from_config(
    mock_hook, mock_config_hook, context
):
    mock_config_hook.get_sources.return_value = ["source"]
    _set_wrong_states(mock_hook, {"target": [], "source": []})

    operator = _check(source_datasets=None, auto_resolve_sources=True)
    assert operator.execute(context) is True

    mock_config_hook.get_sources.assert_called_once_with("target")
    datasets = [
        call.args[0]["dataSetKeyName"] for call in mock_hook.post_wrong.call_args_list
    ]
    assert datasets == ["source", "target"]


def test_check_explicit_sources_override(mock_hook, mock_config_hook, context):
    _set_wrong_states(mock_hook, {"target": [], "explicit": []})

    assert (
        _check(
            source_datasets=["explicit"], auto_resolve_sources=True
        ).execute(context)
        is True
    )
    mock_config_hook.get_sources.assert_not_called()


def test_check_auto_resolve_disabled(mock_hook, mock_config_hook, context):
    _set_wrong_states(mock_hook, {"target": []})

    assert (
        _check(
            source_datasets=None, auto_resolve_sources=False
        ).execute(context)
        is True
    )
    mock_config_hook.get_sources.assert_not_called()
    assert mock_hook.post_wrong.call_count == 1
    assert mock_hook.post_wrong.call_args.args[0]["dataSetKeyName"] == "target"


def test_check_source_pass_same_lock_and_interval(mock_hook, context):
    _set_wrong_states(
        mock_hook,
        {
            "target": [],
            "source": [
                {
                    "dataSetKeyName": "source",
                    "intervalStartDateTime": START,
                    "intervalEndDateTime": END,
                    "status": "LOCKED",
                    "lockSource": "lock-1",
                }
            ],
        },
    )

    assert (
        _check(source_datasets=["source"], fail_on_holes=True).execute(context) is True
    )


def test_check_source_fail_on_partial_interval(mock_hook, context):
    _set_wrong_states(
        mock_hook,
        {
            "target": [],
            "source": [
                {
                    "dataSetKeyName": "source",
                    "intervalStartDateTime": "2026-01-01T06:00:00Z",
                    "intervalEndDateTime": END,
                    "status": "LOCKED",
                    "lockSource": "lock-1",
                }
            ],
        },
    )

    with pytest.raises(AirflowException, match="does not cover the whole interval"):
        _check(source_datasets=["source"], fail_on_holes=True).execute(context)


def test_check_source_null_lock_source_does_not_crash(mock_hook, context):
    _set_wrong_states(
        mock_hook,
        {
            "target": [],
            "source": [
                {
                    "dataSetKeyName": "source",
                    "intervalStartDateTime": START,
                    "intervalEndDateTime": END,
                    "status": "LOCKED",
                    "lockSource": None,
                }
            ],
        },
    )

    with pytest.raises(AirflowException, match="empty lockSource"):
        _check(source_datasets=["source"]).execute(context)
