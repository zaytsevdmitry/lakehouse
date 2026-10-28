# "Yarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
# Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at https://www.apache.org/licenses/LICENSE-2.0.txt

"""Operator that submits a Spark application through ``yarina-task-proxy-for-spark``.

The operator submits the application, waits for the proxy status to become
terminal, and returns when the application finishes successfully. It never
calls the proxy ``/kill``, ``/killall`` or ``/clear`` endpoints.
"""

from __future__ import annotations

import json
import logging
import time
from typing import Any, Dict, Iterable, Optional, Tuple

from airflow.exceptions import AirflowException
from airflow.models import BaseOperator

try:  # package import (``plugins.*``)
    from plugins.constants import (
        DEFAULT_MAX_CONSECUTIVE_UNKNOWN,
        DEFAULT_POLL_INTERVAL_SECONDS,
        DEFAULT_SPARK_PROXY_HTTP_CONN_ID,
        DEFAULT_TIMEOUT_SECONDS,
        TERMINAL_FAIL,
        TRANSIENT,
        SparkDriverState,
    )
    from plugins.hooks.yarina_spark_proxy_hook import YarinaSparkProxyHook
except ImportError:  # pragma: no cover - Airflow loads plugin files as top-level modules
    from constants import (  # type: ignore[no-redef]
        DEFAULT_MAX_CONSECUTIVE_UNKNOWN,
        DEFAULT_POLL_INTERVAL_SECONDS,
        DEFAULT_SPARK_PROXY_HTTP_CONN_ID,
        DEFAULT_TIMEOUT_SECONDS,
        TERMINAL_FAIL,
        TRANSIENT,
        SparkDriverState,
    )
    from hooks.yarina_spark_proxy_hook import YarinaSparkProxyHook  # type: ignore[no-redef]

logger = logging.getLogger(__name__)


class YarinaSparkSubmitOperator(BaseOperator):
    """Submit a Spark application and poll its proxy status until terminal.

    The proxy is asynchronous: ``/create`` returns ``WAITING`` immediately and
    the application is queued in PostgreSQL. ``WAITING`` may therefore last a
    long time; set ``timeout_seconds`` with enough headroom.

    The operator works only with the proxy ``submissionId`` (for example
    ``"42"``) and reads the status from the response ``message`` field.
    """

    template_fields: Tuple[str, ...] = (
        "app_resource",
        "main_class",
        "app_args",
        "spark_properties",
        "environment_variables",
        "client_spark_version",
    )

    def __init__(
        self,
        app_resource: str,
        main_class: str = "",
        app_args: Optional[Iterable[str]] = None,
        spark_properties: Optional[Dict[str, str]] = None,
        environment_variables: Optional[Dict[str, str]] = None,
        client_spark_version: str = "",
        http_conn_id: str = DEFAULT_SPARK_PROXY_HTTP_CONN_ID,
        poll_interval_seconds: int = DEFAULT_POLL_INTERVAL_SECONDS,
        timeout_seconds: int = DEFAULT_TIMEOUT_SECONDS,
        terminal_fail_states: Iterable[str] = TERMINAL_FAIL,
        transient_states: Iterable[str] = TRANSIENT,
        max_consecutive_unknown: int = DEFAULT_MAX_CONSECUTIVE_UNKNOWN,
        **kwargs: Any,
    ) -> None:
        super().__init__(**kwargs)

        if not app_resource or not str(app_resource).strip():
            raise ValueError("app_resource must not be empty")
        if poll_interval_seconds <= 0:
            raise ValueError("poll_interval_seconds must be greater than 0")
        if timeout_seconds <= 0:
            raise ValueError("timeout_seconds must be greater than 0")

        self.app_resource = app_resource
        self.main_class = main_class
        self.app_args = list(app_args or [])
        self.spark_properties = dict(spark_properties or {})
        self.environment_variables = dict(environment_variables or {})
        self.client_spark_version = client_spark_version
        self.http_conn_id = http_conn_id
        self.poll_interval_seconds = poll_interval_seconds
        self.timeout_seconds = timeout_seconds
        self.terminal_fail_states = tuple(
            str(state).strip().upper() for state in terminal_fail_states
        )
        self.transient_states = tuple(
            str(state).strip().upper() for state in transient_states
        )
        self.max_consecutive_unknown = max_consecutive_unknown

    def _build_payload(self) -> Dict[str, Any]:
        """Build ``CreateSubmissionRequest`` with the exact proxy field names."""
        return {
            "action": "CreateSubmissionRequest",
            "appResource": self.app_resource,
            "clientSparkVersion": self.client_spark_version,
            "mainClass": self.main_class,
            "environmentVariables": dict(self.environment_variables),
            "sparkProperties": dict(self.spark_properties),
            "appArgs": list(self.app_args),
        }

    @staticmethod
    def _get_ti(context: Dict[str, Any]) -> Any:
        return context.get("task_instance") or context.get("ti")

    def execute(self, context: Dict[str, Any]) -> Dict[str, str]:
        payload = self._build_payload()
        logger.info(
            "Submitting Spark application: app_resource=%s main_class=%s",
            self.app_resource,
            self.main_class or "<default>",
        )

        hook = YarinaSparkProxyHook(http_conn_id=self.http_conn_id)
        response = hook.create_submission(payload)

        submission_id = response.get("submissionId")
        if not response.get("success") or submission_id is None or not str(
            submission_id
        ).strip():
            raise AirflowException(
                "yarina-task-proxy-for-spark rejected the Spark submission: "
                "message={!r}, submissionId={!r}. Request payload: {}".format(
                    response.get("message"),
                    submission_id,
                    json.dumps(payload, ensure_ascii=False),
                )
            )

        submission_id = str(submission_id)
        logger.info(
            "Spark submission created: submissionId=%s status=%s",
            submission_id,
            response.get("message"),
        )

        ti = self._get_ti(context)
        if ti is not None:
            ti.xcom_push(key="spark_submission_id", value=submission_id)

        return self._poll_until_terminal(hook, submission_id)

    def _poll_until_terminal(self, hook, submission_id: str) -> Dict[str, str]:
        deadline = time.monotonic() + self.timeout_seconds
        unknown_streak = 0
        approaching_logged = False

        while True:
            response = hook.get_status(submission_id)
            raw_state = response.get("driverState")
            status = str(raw_state if raw_state is not None else "").strip().upper()
            message = response.get("message")
            logger.info(
                "Poll Spark submission %s: driverState=%s message=%s",
                submission_id, raw_state, message,
            )

            if status == SparkDriverState.FINISHED:
                return {"submissionId": submission_id,
                        "driverState": SparkDriverState.FINISHED}

            if status in self.terminal_fail_states:
                raise AirflowException(
                    "Spark submission {} finished with driverState={} "
                    "(message={!r}). Inspect manually: "
                    "GET /v1/submissions/status/{}".format(
                        submission_id, status, message, submission_id
                    )
                )

            if status in self.transient_states:
                unknown_streak = 0
            else:
                unknown_streak += 1
                logger.warning(
                    "Spark submission %s returned unexpected driverState %r "
                    "(%d consecutive); message=%r",
                    submission_id, raw_state, unknown_streak, message,
                )
                if unknown_streak >= self.max_consecutive_unknown:
                    raise AirflowException(
                        "Spark submission {} returned unexpected driverState "
                        "{!r} {} times in a row (last message={!r}); giving up.".format(
                            submission_id, raw_state, unknown_streak, message
                        )
                    )

            now = time.monotonic()
            if now >= deadline:
                raise AirflowException(self._timeout_message(submission_id))

            remaining = deadline - now
            if not approaching_logged and remaining <= max(self.poll_interval_seconds, 60):
                logger.warning(
                    "Approaching timeout for Spark submission %s: %d seconds left",
                    submission_id, remaining,
                )
                approaching_logged = True

                time.sleep(self.poll_interval_seconds)

    def _timeout_message(self, submission_id: str) -> str:
        return (
            "Timeout of {}s reached while waiting for submission {}. "
            "The task may still be running in the cluster; please check the status manually: "
            "GET /v1/submissions/status/{}"
        ).format(self.timeout_seconds, submission_id, submission_id)
