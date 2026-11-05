# "Yarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
# Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at https://www.apache.org/licenses/LICENSE-2.0.txt

"""Unit tests for hooks (OAuth2, HTTP errors, state/config semantics)."""

from __future__ import annotations

import json

import pytest
from airflow.exceptions import AirflowException
from airflow.providers.http.hooks.http import HttpHook

import plugins.hooks.yarina_base_hook as base_module
from plugins.hooks.yarina_config_hook import YarinaConfigHook
from plugins.hooks.yarina_state_hook import YarinaStateHook


@pytest.fixture
def state_hook(patch_connection):
    patch_connection.extra_dejson = {"security_enabled": False}
    return YarinaStateHook()


@pytest.fixture
def config_hook(patch_connection):
    patch_connection.extra_dejson = {"security_enabled": False}
    return YarinaConfigHook()


# ---------------------------------------------------------------------------
# Authentication
# ---------------------------------------------------------------------------


def test_auth_success(mocker, patch_connection, fake_response):
    post = mocker.patch.object(
        base_module.requests,
        "post",
        return_value=fake_response(200, {"access_token": "token-123"}),
    )
    hook = YarinaStateHook()

    assert hook._get_oauth_token() == "token-123"
    assert hook._get_oauth_token() == "token-123"
    assert post.call_count == 1

    kwargs = post.call_args.kwargs
    assert kwargs["data"]["grant_type"] == "client_credentials"
    assert kwargs["data"]["client_id"] == "yarina-internal-client"
    assert kwargs["data"]["client_secret"] == "client-secret"
    assert "token-123" not in json.dumps(kwargs)


def test_auth_failure(mocker, patch_connection, fake_response):
    mocker.patch.object(
        base_module.requests,
        "post",
        return_value=fake_response(401, text="invalid client"),
    )
    hook = YarinaStateHook()

    with pytest.raises(AirflowException, match="HTTP 401"):
        hook._get_oauth_token()


def test_missing_auth_url(patch_connection):
    patch_connection.extra_dejson = {}
    hook = YarinaStateHook()

    with pytest.raises(AirflowException, match="auth_url"):
        hook._get_oauth_token()


def test_auth_url_404_hint(mocker, patch_connection, fake_response):
    mocker.patch.object(
        base_module.requests,
        "post",
        return_value=fake_response(404, text="not found"),
    )
    hook = YarinaStateHook()

    with pytest.raises(AirflowException) as exc:
        hook._get_oauth_token()
    assert "protocol/openid-connect/token" in str(exc.value)


def test_security_disabled_has_no_bearer(patch_connection):
    patch_connection.extra_dejson = {"security_enabled": False}
    hook = YarinaStateHook()

    headers = hook._headers()
    assert headers == {
        "Content-Type": "application/json",
        "Accept": "application/json",
    }
    assert "Authorization" not in headers


def test_security_enabled_has_bearer(mocker, patch_connection):
    mocker.patch.object(YarinaStateHook, "_get_oauth_token", return_value="token")
    hook = YarinaStateHook()

    headers = hook._headers()
    assert headers["Authorization"] == "Bearer token"
    assert headers["Content-Type"] == "application/json"
    assert headers["Accept"] == "application/json"


# ---------------------------------------------------------------------------
# State hook
# ---------------------------------------------------------------------------


def test_put_state_raises_on_4xx(mocker, state_hook, fake_response):
    mocker.patch.object(
        HttpHook,
        "run",
        return_value=fake_response(400, text="bad request body"),
    )

    with pytest.raises(AirflowException) as exc:
        state_hook.put_state(
            {
                "dataSetKeyName": "dataset",
                "intervalStartDateTime": "2026-01-01T00:00:00Z",
                "intervalEndDateTime": "2026-01-02T00:00:00Z",
                "status": "LOCKED",
                "lockSource": "airflow",
            }
        )
    assert "HTTP 400" in str(exc.value)
    assert "bad request body" in str(exc.value)


def test_put_state_500_has_npe_hint(mocker, state_hook, fake_response):
    mocker.patch.object(
        HttpHook,
        "run",
        return_value=fake_response(500, text="Internal Server Error"),
    )

    with pytest.raises(AirflowException) as exc:
        state_hook.put_state({"dataSetKeyName": "dataset"})
    message = str(exc.value)
    assert "HTTP 500" in message
    assert "lockSource is null" in message
    assert "NPE" in message


def test_get_states_uses_query_not_json(mocker, state_hook, fake_response):
    params = {
        "dataSetKeyName": "dataset",
        "intervalStartDateTime": "2026-01-01T00:00:00Z",
        "intervalEndDateTime": "2026-01-02T00:00:00Z",
    }
    run = mocker.patch.object(
        HttpHook,
        "run",
        return_value=fake_response(200, [{"dataSetKeyName": "dataset"}]),
    )

    assert state_hook.get_states(params) == [{"dataSetKeyName": "dataset"}]

    kwargs = run.call_args.kwargs
    assert kwargs["endpoint"] == "v1_0/state/dataset"
    assert kwargs["data"] == params
    assert "json" not in kwargs


def test_get_states_sends_get_method(mocker, state_hook, fake_response):
    seen = {}

    def _run(**kwargs):
        seen["method"] = state_hook.method
        return fake_response(200, [])

    mocker.patch.object(HttpHook, "run", side_effect=_run)

    state_hook.get_states({"dataSetKeyName": "dataset"})
    assert seen["method"] == "GET"


def test_post_wrong_empty_returns_structure(mocker, state_hook, fake_response):
    run = mocker.patch.object(HttpHook, "run", return_value=fake_response(200, {}))

    assert state_hook.post_wrong({"dataSetKeyName": "dataset"}) == {"wrongStates": []}
    payload = json.loads(run.call_args.kwargs["data"])
    assert payload == {"dataSetKeyName": "dataset"}


def test_post_wrong_missing_list_returns_structure(mocker, state_hook, fake_response):
    mocker.patch.object(
        HttpHook,
        "run",
        return_value=fake_response(200, {"wrongStates": None}),
    )

    assert state_hook.post_wrong({"dataSetKeyName": "dataset"}) == {"wrongStates": []}


def test_find_foreign_locks_filters(mocker, state_hook):
    states = [
        {"status": "LOCKED", "lockSource": "own"},
        {"status": "LOCKED", "lockSource": "foreign"},
        {"status": "SUCCESS", "lockSource": "foreign"},
        {"status": "LOCKED", "lockSource": None},
    ]
    mocker.patch.object(state_hook, "post_wrong", return_value={"wrongStates": states})

    result = state_hook.find_foreign_locks(
        "dataset", "2026-01-01T00:00:00Z", "2026-01-02T00:00:00Z", "own"
    )
    assert result == [{"status": "LOCKED", "lockSource": "foreign"}]


def test_resolve_lock_source_multiple_raises(mocker, state_hook):
    mocker.patch.object(
        state_hook,
        "get_states",
        return_value=[
            {"status": "LOCKED", "lockSource": "one"},
            {"status": "LOCKED", "lockSource": "two"},
        ],
    )

    with pytest.raises(AirflowException, match="Several LOCKED lockSources"):
        state_hook.resolve_lock_source(
            "dataset", "2026-01-01T00:00:00Z", "2026-01-02T00:00:00Z"
        )


def test_resolve_lock_source_none(mocker, state_hook):
    mocker.patch.object(
        state_hook,
        "get_states",
        return_value=[
            {"status": "SUCCESS", "lockSource": "done"},
            {"status": "LOCKED", "lockSource": None},
        ],
    )

    assert (
        state_hook.resolve_lock_source(
            "dataset", "2026-01-01T00:00:00Z", "2026-01-02T00:00:00Z"
        )
        is None
    )


def test_resolve_lock_source_single(mocker, state_hook):
    mocker.patch.object(
        state_hook,
        "get_states",
        return_value=[
            {"status": "LOCKED", "lockSource": "only"},
            {"status": "LOCKED", "lockSource": "only"},
        ],
    )

    assert (
        state_hook.resolve_lock_source(
            "dataset", "2026-01-01T00:00:00Z", "2026-01-02T00:00:00Z"
        )
        == "only"
    )


# ---------------------------------------------------------------------------
# Config hook
# ---------------------------------------------------------------------------


def test_config_get_sources_filters_own(mocker, config_hook):
    mocker.patch.object(
        config_hook,
        "get_dataset",
        return_value={
            "keyName": "target",
            "sources": {"source_a": {}, "target": {}, "source_b": {}},
        },
    )

    assert config_hook.get_sources("target") == ["source_a", "source_b"]


def test_config_get_sources_404_raises(mocker, config_hook, fake_response):
    mocker.patch.object(
        HttpHook,
        "run",
        return_value=fake_response(404, text="not found"),
    )

    with pytest.raises(AirflowException, match="not found"):
        config_hook.get_sources("missing")


def test_config_get_sources_no_sources_field(mocker, config_hook):
    mocker.patch.object(
        config_hook, "get_dataset", return_value={"keyName": "target"}
    )

    assert config_hook.get_sources("target") == []


def test_config_get_sources_uses_config_endpoint(mocker, config_hook, fake_response):
    run = mocker.patch.object(
        HttpHook,
        "run",
        return_value=fake_response(
            200, {"keyName": "target", "sources": {"source_a": {}}}
        ),
    )

    assert config_hook.get_sources("target") == ["source_a"]
    assert run.call_args.kwargs["endpoint"] == "v1_0/configs/datasets/target"
