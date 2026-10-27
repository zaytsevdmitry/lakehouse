# "Yarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
# Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at https://www.apache.org/licenses/LICENSE-2.0.txt

"""Hook for the ``yarina-task-proxy-for-spark`` REST API.

Only two operations are exposed:

* ``POST /v1/submissions/create`` - queue a Spark application;
* ``GET  /v1/submissions/status/{submissionId}`` - read its proxy status.

The proxy's ``/kill``, ``/killall`` and ``/clear`` endpoints are deliberately
not implemented. Killing work is outside the scope of this integration.
"""

from __future__ import annotations

import logging
from typing import Any, Dict
from urllib.parse import quote

from airflow.exceptions import AirflowException

try:  # package import (``plugins.*``)
    from plugins.constants import (
        CREATE_SUBMISSION_TIMEOUT_SECONDS,
        DEFAULT_SPARK_PROXY_HTTP_CONN_ID,
        SPARK_PROXY_CREATE_ENDPOINT,
        SPARK_PROXY_STATUS_ENDPOINT,
        STATUS_TIMEOUT_SECONDS,
        SparkDriverState,
    )
    from plugins.hooks.yarina_base_hook import YarinaBaseHook
except ImportError:  # pragma: no cover - Airflow loads plugin files as top-level modules
    from constants import (  # type: ignore[no-redef]
        CREATE_SUBMISSION_TIMEOUT_SECONDS,
        DEFAULT_SPARK_PROXY_HTTP_CONN_ID,
        SPARK_PROXY_CREATE_ENDPOINT,
        SPARK_PROXY_STATUS_ENDPOINT,
        STATUS_TIMEOUT_SECONDS,
        SparkDriverState,
    )
    from hooks.yarina_base_hook import YarinaBaseHook  # type: ignore[no-redef]

logger = logging.getLogger(__name__)


class YarinaSparkProxyHook(YarinaBaseHook):
    """Client for ``yarina-task-proxy-for-spark`` (default port ``8090``).

    The client always works with the proxy ``submissionId`` (for example
    ``"42"``). The real Spark driver id (``driver-...``) is resolved inside the
    proxy and never exposed here.
    """

    def __init__(
        self,
        method: str = "GET",
        http_conn_id: str = DEFAULT_SPARK_PROXY_HTTP_CONN_ID,
        *args: Any,
        **kwargs: Any,
    ) -> None:
        super().__init__(method=method, http_conn_id=http_conn_id, *args, **kwargs)

    def create_submission(self, payload: Dict[str, Any]) -> Dict[str, Any]:
        """Queue a Spark application and return the proxy response.

        The response contains ``action``, ``message``, ``submissionId`` and
        ``success``. A non-2xx response is converted to
        :class:`AirflowException` with the HTTP code and body.
        """
        logger.info("Creating Spark submission via %s", SPARK_PROXY_CREATE_ENDPOINT)
        response = self._request_json(
            "POST",
            SPARK_PROXY_CREATE_ENDPOINT,
            payload,
            timeout=CREATE_SUBMISSION_TIMEOUT_SECONDS,
        )
        return self._parse_json_object(
            response, SPARK_PROXY_CREATE_ENDPOINT, "create response"
        )

    def get_status(self, submission_id: str) -> Dict[str, Any]:
        """Return ``SubmissionStatusResponse`` for a proxy submission id.

        HTTP 404 is raised immediately: an unknown id cannot become known by
        polling again. HTTP 5xx is converted to a synthetic ``UNKNOWN`` status
        so the operator can apply its ``max_consecutive_unknown`` tolerance.
        """
        if submission_id is None or not str(submission_id).strip():
            raise AirflowException("submission_id must not be empty")

        safe_id = quote(str(submission_id), safe="")
        endpoint = SPARK_PROXY_STATUS_ENDPOINT.format(submission_id=safe_id)
        logger.debug("Polling Spark submission %s via %s", submission_id, endpoint)
        response = self._request(
            "GET",
            endpoint,
            timeout=STATUS_TIMEOUT_SECONDS,
        )

        if response.status_code == 404:
            raise AirflowException(
                "submission '{}' не найден (404)".format(submission_id)
            )

        if 500 <= response.status_code < 600:
            logger.warning(
                "Spark status request for submission %s returned HTTP %s: %s; "
                "treating the status as %s",
                submission_id,
                response.status_code,
                response.text,
                SparkDriverState.UNKNOWN,
            )
            return {
                "action": "StatusResponse",
                "message": SparkDriverState.UNKNOWN,
                "submissionId": str(submission_id),
                "success": False,
            }

        self._check(response, endpoint)
        return self._parse_json_object(
            response, endpoint, "status response for '{}'".format(submission_id)
        )

    @staticmethod
    def _parse_json_object(
        response: Any, endpoint: str, description: str
    ) -> Dict[str, Any]:
        try:
            body = response.json()
        except ValueError as exc:
            raise AirflowException(
                "Unexpected non-JSON {} from {}: {}".format(
                    description, endpoint, response.text
                )
            ) from exc
        if not isinstance(body, dict):
            raise AirflowException(
                "Unexpected {} from {}: expected a JSON object, got {}".format(
                    description, endpoint, type(body).__name__
                )
            )
        return body
