# "Yarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
# Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at https://www.apache.org/licenses/LICENSE-2.0.txt

"""Unit tests for YarinaBaseHook and YarinaSparkProxyHook."""

from __future__ import annotations

import json
from types import SimpleNamespace

import pytest
from airflow.exceptions import AirflowException
from airflow.providers.http.hooks.http import HttpHook

import plugins.hooks.yarina_base_hook as base_module
from plugins.hooks.yarina_spark_proxy_hook import YarinaSparkProxyHook


# ---------------------------------------------------------------------------
# Authentication and headers
# ---------------------------------------------------------------------------


def test_auth_success_caches_token(mocker, patch_connection, fake_response):
    post = mocker.patch.object(
        base_module.requests,
        "post",
        return_value=fake_response(200, {"access_token": "token-123"}),
    )
    hook = YarinaSparkProxyHook()

    assert hook._get_oauth_token() == "token-123"
    assert hook._get_oauth_token() == "token-123"
    assert post.call_count == 1

    kwargs = post.call_args.kwargs
    assert kwargs["data"]["grant_type"] == "client_credentials"
    assert kwargs["data"]["client_id"] == "yarina-internal-client"
    assert kwargs["data"]["client_secret"] == "client-secret"
    assert kwargs["timeout"] == 30


def test_auth_failure_includes_body(mocker, patch_connection, fake_response):
    mocker.patch.object(
        base_module.requests,
        "post",
        return_value=fake_response(401, text="invalid client"),
    )
    hook = YarinaSparkProxyHook()

    with pytest.raises(AirflowException, match="HTTP 401"):
        hook._get_oauth_token()


def test_missing_auth_url_raises(patch_connection):
    patch_connection.extra_dejson = {}
    hook = YarinaSparkProxyHook()

    with pytest.raises(AirflowException, match="auth_url"):
        hook._get_oauth_token()


def test_auth_url_404_has_token_endpoint_hint(
    mocker, patch_connection, fake_response
):
    mocker.patch.object(
        base_module.requests,
        "post",
        return_value=fake_response(404, text="not found"),
    )
    hook = YarinaSparkProxyHook()

    with pytest.raises(AirflowException) as exc:
        hook._get_oauth_token()
    message = str(exc.value)
    assert "protocol/openid-connect/token" in message
    assert "issuer" in message


def test_security_disabled_headers_have_no_bearer(spark_hook):
    headers = spark_hook._headers()

    assert headers["Content-Type"] == "application/json"
    assert headers["Accept"] == "application/json"
    assert "Authorization" not in headers


def test_security_enabled_headers_have_bearer(mocker, patch_connection):
    mocker.patch.object(
        YarinaSparkProxyHook, "_get_oauth_token", return_value="token-123"
    )
    hook = YarinaSparkProxyHook()

    headers = hook._headers()
    assert headers["Authorization"] == "Bearer token-123"
    assert headers["Content-Type"] == "application/json"
    assert headers["Accept"] == "application/json"


def test_get_conn_removes_connection_extras_from_headers(mocker, spark_hook):
    session = SimpleNamespace(
        headers={
            "auth_url": "http://keycloak/token",
            "security_enabled": False,
            "X-Test": "kept",
        }
    )
    mocker.patch.object(base_module.HttpHook, "get_conn", return_value=session)

    result = spark_hook.get_conn()

    assert result is session
    assert result.auth is None
    assert "auth_url" not in result.headers
    assert "security_enabled" not in result.headers
    assert result.headers["X-Test"] == "kept"


def test_request_uses_bearer_instead_of_basic_auth(
    mocker, patch_connection, fake_response
):
    patch_connection.extra_dejson = {"security_enabled": True}
    patch_connection.port = None
    mocker.patch.object(
        YarinaSparkProxyHook, "_get_oauth_token", return_value="token-123"
    )
    seen = {}

    def run_and_check(_self, session, prepared, extra_options):
        seen["authorization"] = prepared.headers.get("Authorization")
        return fake_response(200, {"success": True, "submissionId": "42"})

    mocker.patch.object(base_module.HttpHook, "run_and_check", run_and_check)
    hook = YarinaSparkProxyHook()

    hook.create_submission({"appResource": "s3a://bucket/app.jar"})

    # The Connection login/password are Keycloak credentials, not HTTP Basic
    # auth. HttpHook must not overwrite the bearer token they produce.
    assert seen["authorization"] == "Bearer token-123"


# ---------------------------------------------------------------------------
# create_submission
# ---------------------------------------------------------------------------


def test_create_submission_success(mocker, spark_hook, fake_response):
    payload = {
        "action": "CreateSubmissionRequest",
        "appResource": "s3a://bucket/app.jar",
    }
    seen = {}

    def _run(**kwargs):
        seen["method"] = spark_hook.method
        seen["kwargs"] = kwargs
        return fake_response(
            200,
            {
                "action": "CreateSubmissionResponse",
                "message": "WAITING",
                "submissionId": "42",
                "success": True,
            },
        )

    mocker.patch.object(HttpHook, "run", side_effect=_run)

    result = spark_hook.create_submission(payload)

    assert result["submissionId"] == "42"
    assert result["success"] is True
    assert seen["method"] == "POST"
    kwargs = seen["kwargs"]
    assert kwargs["endpoint"] == "v1/submissions/create"
    assert json.loads(kwargs["data"]) == payload
    assert kwargs["extra_options"]["timeout"] == 30
    assert kwargs["headers"]["Content-Type"] == "application/json"


def test_create_submission_http_error(mocker, spark_hook, fake_response):
    mocker.patch.object(
        HttpHook,
        "run",
        return_value=fake_response(500, text="proxy exploded"),
    )

    with pytest.raises(AirflowException) as exc:
        spark_hook.create_submission({"appResource": "s3a://bucket/app.jar"})
    message = str(exc.value)
    assert "HTTP 500" in message
    assert "proxy exploded" in message
    # The NPE hint is specific to yarina-state-svc and must not leak here.
    assert "NPE" not in message


# ---------------------------------------------------------------------------
# get_status
# ---------------------------------------------------------------------------


def test_get_status_success(mocker, spark_hook, fake_response):
    seen = {}

    def _run(**kwargs):
        seen["method"] = spark_hook.method
        seen["kwargs"] = kwargs
        return fake_response(
            200,
            {
                "action": "StatusResponse",
                "message": "RUNNING",
                "submissionId": "42",
                "success": True,
            },
        )

    mocker.patch.object(HttpHook, "run", side_effect=_run)

    result = spark_hook.get_status("42")

    assert result["message"] == "RUNNING"
    assert seen["method"] == "GET"
    kwargs = seen["kwargs"]
    assert kwargs["endpoint"] == "v1/submissions/status/42"
    assert kwargs["extra_options"]["timeout"] == 15
    assert "data" in kwargs


def test_get_status_404_raises(mocker, spark_hook, fake_response):
    run = mocker.patch.object(
        HttpHook,
        "run",
        return_value=fake_response(404, text="not found"),
    )

    with pytest.raises(AirflowException) as exc:
        spark_hook.get_status("42")
    assert "submission '42' не найден (404)" in str(exc.value)
    assert run.call_count == 1


def test_get_status_5xx_returns_unknown(mocker, spark_hook, fake_response):
    mocker.patch.object(
        HttpHook,
        "run",
        return_value=fake_response(503, text="temporarily unavailable"),
    )

    result = spark_hook.get_status("42")

    assert result == {
        "action": "StatusResponse",
        "message": "UNKNOWN",
        "submissionId": "42",
        "success": False,
    }


def test_get_status_non_json_raises(mocker, spark_hook, fake_response):
    mocker.patch.object(
        HttpHook,
        "run",
        return_value=fake_response(200, text="<html>not json</html>"),
    )

    with pytest.raises(AirflowException, match="non-JSON"):
        spark_hook.get_status("42")


def test_hook_api_has_no_kill_endpoints():
    for name in (
        "kill_submission",
        "kill_all_submissions",
        "clear_submissions",
        "kill",
        "kill_all",
        "clear",
    ):
        assert not hasattr(YarinaSparkProxyHook, name)
