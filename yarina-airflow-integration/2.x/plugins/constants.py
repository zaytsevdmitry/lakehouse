# "Yarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
# Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at https://www.apache.org/licenses/LICENSE-2.0.txt

"""Constants shared by the Yarina Airflow integration.

The values mirror the REST contracts of ``yarina-state-svc``,
``yarina-config-svc`` and ``yarina-task-proxy-for-spark`` (see
``Endpoint.java`` in ``yarina-common`` and the proxy documentation).
"""

from enum import Enum


class YarinaStatus(str, Enum):
    """Interval statuses accepted by ``yarina-state-svc``."""

    LOCKED = "LOCKED"
    SUCCESS = "SUCCESS"


class SparkDriverState:
    """Status values returned by ``yarina-task-proxy-for-spark``.

    The proxy returns the state in the ``message`` field of
    ``SubmissionStatusResponse``. ``driverState`` belongs to the direct Spark
    Standalone REST API and is not used by this integration.
    """

    WAITING = "WAITING"
    SUBMITTED = "SUBMITTED"
    RUNNING = "RUNNING"
    FINISHED = "FINISHED"
    FAILED = "FAILED"
    KILLED = "KILLED"
    UNKNOWN = "UNKNOWN"


#: Spark statuses that make the operator fail immediately.
TERMINAL_FAIL = (SparkDriverState.FAILED, SparkDriverState.KILLED)

#: Non-terminal Spark statuses that keep the operator polling.
TRANSIENT = (
    SparkDriverState.WAITING,
    SparkDriverState.SUBMITTED,
    SparkDriverState.RUNNING,
)

#: Default ``lockSource`` used when the DAG does not pass its own value.
DEFAULT_LOCK_SOURCE = "airflow-executor"

#: Default Airflow Connection ids.
DEFAULT_HTTP_CONN_ID = "yarina_state_default"
DEFAULT_CONFIG_HTTP_CONN_ID = "yarina_config_default"
DEFAULT_SPARK_PROXY_HTTP_CONN_ID = "yarina_spark_proxy_default"

#: REST endpoints (relative to the Connection host).
STATE_DATASET_ENDPOINT = "v1_0/state/dataset"
STATE_DATASET_WRONG_ENDPOINT = "v1_0/state/dataset/wrong"
CONFIG_DATASET_ENDPOINT = "v1_0/configs/datasets/{key_name}"
SPARK_PROXY_BASE_PATH = "v1/submissions"
SPARK_PROXY_CREATE_ENDPOINT = SPARK_PROXY_BASE_PATH + "/create"
SPARK_PROXY_STATUS_ENDPOINT = SPARK_PROXY_BASE_PATH + "/status/{submission_id}"

#: Default proxy port (also used in the documentation).
DEFAULT_SPARK_PROXY_PORT = 8090

#: HTTP timeouts for the two proxy calls, in seconds.
CREATE_SUBMISSION_TIMEOUT_SECONDS = 30
STATUS_TIMEOUT_SECONDS = 15

#: Default polling configuration for the Spark submit operator.
DEFAULT_POLL_INTERVAL_SECONDS = 10
DEFAULT_TIMEOUT_SECONDS = 3600
DEFAULT_MAX_CONSECUTIVE_UNKNOWN = 5

#: Status codes treated as successful by the Yarina services.
SUCCESS_STATUS_CODES = (200, 201)
