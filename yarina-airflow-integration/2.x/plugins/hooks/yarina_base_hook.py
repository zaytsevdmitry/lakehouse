# "Yarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
# Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at https://www.apache.org/licenses/LICENSE-2.0.txt

"""Base hook for the Yarina services.

The hook implements the OAuth2 ``client_credentials`` flow against the
Keycloak realm ``yarina`` and centralises JSON/HTTP error handling for all
derived hooks (state, config and Spark proxy).
"""

from __future__ import annotations

import json
import logging
from typing import Any, Dict, Optional

import requests
from airflow.exceptions import AirflowException
from airflow.providers.http.hooks.http import HttpHook

try:  # package import (``plugins.*``)
    from plugins.constants import DEFAULT_HTTP_CONN_ID, SUCCESS_STATUS_CODES
except ImportError:  # pragma: no cover - Airflow loads plugin files as top-level modules
    from constants import DEFAULT_HTTP_CONN_ID, SUCCESS_STATUS_CODES
logger = logging.getLogger(__name__)

#: Hint shown for 5xx responses. yarina-state-svc has known NPE bugs that
#: surface as HTTP 500 when ``lockSource`` or interval dates are null.
NPE_HINT = (
    " Hint: yarina-state-svc is known to return HTTP 5xx on NPE (NullPointerException) "
    "when lockSource is null or interval dates are null. Check the payload and "
    "the state-svc logs (lockSource=null / null dates are known backend bugs)."
)

TOKEN_404_HINT = (
    "Token endpoint returned HTTP 404. Check that Connection extra 'auth_url' is the "
    "token endpoint, not the issuer: auth_url = {issuer}/protocol/openid-connect/token"
)

#: Timeout used for the Keycloak token request, in seconds.
TOKEN_TIMEOUT_SECONDS = 30


class YarinaBaseHook(HttpHook):
    """Common HTTP/OAuth2 logic for Yarina hooks.

    Connection fields:

    * ``host`` - base URL of the service (for example
      ``http://yarina-state-svc:8082`` or
      ``http://yarina-task-proxy-for-spark:8090``);
    * ``login`` - Keycloak client id (``yarina-internal-client``);
    * ``password`` - Keycloak client secret;
    * ``extra.auth_url`` - absolute token endpoint;
    * ``extra.security_enabled`` - ``false`` disables OAuth2 (local development).
    """

    def __init__(
        self,
        method: str = "GET",
        http_conn_id: str = DEFAULT_HTTP_CONN_ID,
        *args: Any,
        **kwargs: Any,
    ) -> None:
        super().__init__(method=method, http_conn_id=http_conn_id, *args, **kwargs)
        self._token: Optional[str] = None

        conn = self.get_connection(http_conn_id)
        self._login = conn.login
        self._password = conn.password
        extra = self._read_extra(conn)
        self._auth_url: Optional[str] = extra.get("auth_url")
        self._security_enabled = self._as_bool(extra.get("security_enabled", True))

        if self._security_enabled and not self._auth_url:
            logger.warning(
                "Connection '%s' has security enabled but no extra.auth_url", http_conn_id
            )

    @staticmethod
    def _read_extra(conn: Any) -> Dict[str, Any]:
        """Return Connection extra as a dict, tolerating invalid/empty JSON."""
        try:
            extra = conn.extra_dejson
        except Exception:  # pragma: no cover - defensive against malformed Connection
            logger.warning("Could not parse Connection extra as JSON")
            return {}
        return extra if isinstance(extra, dict) else {}

    @staticmethod
    def _as_bool(value: Any) -> bool:
        if isinstance(value, str):
            return value.strip().lower() not in ("false", "0", "no", "off")
        return bool(value)

    def _get_oauth_token(self) -> str:
        """Obtain an access token with the ``client_credentials`` grant.

        The token is cached for the lifetime of the hook instance (one task
        ``execute`` call). It is never logged.
        """
        if self._token:
            return self._token
        if not self._auth_url:
            raise AirflowException(
                "Connection extra 'auth_url' is required when security is enabled. "
                "Set auth_url to {issuer}/protocol/openid-connect/token."
            )
        if not self._login or not self._password:
            raise AirflowException(
                "Connection login/password (Keycloak client id/secret) must be set."
            )

        timeout = TOKEN_TIMEOUT_SECONDS
        try:
            response = requests.post(
                self._auth_url,
                data={
                    "grant_type": "client_credentials",
                    "client_id": self._login,
                    "client_secret": self._password,
                },
                headers={"Content-Type": "application/x-www-form-urlencoded"},
                timeout=timeout,
            )
        except requests.RequestException as exc:
            raise AirflowException(
                "Could not reach Keycloak token endpoint {}: {}".format(
                    self._auth_url, exc
                )
            ) from exc

        if response.status_code == 404:
            raise AirflowException(
                "Keycloak authentication failed: {} (auth_url={})".format(
                    TOKEN_404_HINT, self._auth_url
                )
            )
        if response.status_code not in SUCCESS_STATUS_CODES:
            raise AirflowException(
                "Keycloak authentication failed with HTTP {}: {}".format(
                    response.status_code, response.text
                )
            )

        try:
            payload = response.json()
            token = payload["access_token"]
        except (ValueError, KeyError, TypeError) as exc:
            raise AirflowException(
                "Keycloak response does not contain access_token: {}".format(
                    response.text
                )
            ) from exc

        self._token = token
        logger.info("Obtained Keycloak access token for client '%s'", self._login)
        return token

    def _headers(self) -> Dict[str, str]:
        """Build request headers.

        JSON transport headers are always present. When security is enabled,
        the cached bearer token is added as ``Authorization``.
        """
        headers = {
            "Content-Type": "application/json",
            "Accept": "application/json",
        }
        if self._security_enabled:
            headers["Authorization"] = "Bearer {}".format(self._get_oauth_token())
        return headers

    def get_conn(self, *args: Any, **kwargs: Any) -> Any:
        """Create a session and keep Yarina settings out of HTTP headers.

        ``HttpHook`` copies every unrecognised Connection extra into the
        session headers and turns ``login``/``password`` into HTTP Basic auth.
        Here those fields are Keycloak client credentials, and the request is
        authenticated with the bearer token from :meth:`_headers`; the Basic
        auth header would overwrite the bearer token, so it is disabled.
        ``auth_url`` and ``security_enabled`` are application-level settings,
        not HTTP headers, so they are removed before the first request.
        """
        session = super().get_conn(*args, **kwargs)
        session.auth = None
        for key in ("auth_url", "security_enabled"):
            session.headers.pop(key, None)
        return session

    @staticmethod
    def _check(response: Any, endpoint: str, npe_hint: bool = False) -> Any:
        """Validate an HTTP response and return it, or raise AirflowException.

        ``npe_hint`` is enabled only by :class:`YarinaStateHook`: its 5xx
        diagnostics point at known ``yarina-state-svc`` NPE bugs. Other
        services keep the generic error message.
        """
        if response.status_code in SUCCESS_STATUS_CODES:
            return response

        message = "Yarina API call failed: {} returned HTTP {}: {}".format(
            endpoint, response.status_code, response.text
        )
        if npe_hint and 500 <= response.status_code < 600:
            message += NPE_HINT
        raise AirflowException(message)

    def _request(
        self,
        method: str,
        endpoint: str,
        data: Any = None,
        timeout: Optional[int] = None,
        extra_options: Optional[Dict[str, Any]] = None,
    ) -> Any:
        """Execute an HTTP request through :class:`HttpHook`.

        ``HttpHook.run`` uses ``self.method``. The attribute is switched per
        call and restored afterwards, so one hook instance can safely call
        both GET and PUT/POST endpoints.

        For GET requests the parameters are passed in ``data=`` (HttpHook
        converts them to query parameters); JSON bodies are passed as a
        pre-serialised ``data`` string as well. ``json=`` is intentionally
        not used.
        """
        method = method.upper()
        previous_method = getattr(self, "method", None)
        self.method = method
        headers = self._headers()
        options: Dict[str, Any] = {"check_response": False}
        if timeout is not None:
            options["timeout"] = timeout
        if extra_options:
            options.update(extra_options)
        try:
            return super().run(
                endpoint=endpoint,
                data=data,
                headers=headers,
                extra_options=options,
            )
        except requests.RequestException as exc:
            raise AirflowException(
                "HTTP request to {} failed: {}".format(endpoint, exc)
            ) from exc
        finally:
            if previous_method is not None:
                self.method = previous_method

    def _request_json(
        self,
        method: str,
        endpoint: str,
        payload: Dict[str, Any],
        timeout: Optional[int] = None,
        npe_hint: bool = False,
    ) -> Any:
        """Send a JSON body, validate the status code and return the response."""
        response = self._request(
            method,
            endpoint,
            data=json.dumps(payload, ensure_ascii=False),
            timeout=timeout,
        )
        return self._check(response, endpoint, npe_hint=npe_hint)
