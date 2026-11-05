# "Yarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
# Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at https://www.apache.org/licenses/LICENSE-2.0.txt

"""Hook for reading dataset configuration from ``yarina-config-svc``."""

from __future__ import annotations

import logging
from typing import Any, Dict, List
from urllib.parse import quote

from airflow.exceptions import AirflowException

try:  # package import (``plugins.*``)
    from plugins.constants import CONFIG_DATASET_ENDPOINT, DEFAULT_CONFIG_HTTP_CONN_ID
    from plugins.hooks.yarina_base_hook import YarinaBaseHook
except ImportError:  # pragma: no cover - Airflow loads plugin files as top-level modules
    from constants import (  # type: ignore[no-redef]
        CONFIG_DATASET_ENDPOINT,
        DEFAULT_CONFIG_HTTP_CONN_ID,
    )
    from hooks.yarina_base_hook import YarinaBaseHook  # type: ignore[no-redef]

logger = logging.getLogger(__name__)


class YarinaConfigHook(YarinaBaseHook):
    """Read ``DataSetDTO`` objects and their ``sources``."""

    def __init__(
        self,
        method: str = "GET",
        http_conn_id: str = DEFAULT_CONFIG_HTTP_CONN_ID,
        *args: Any,
        **kwargs: Any,
    ) -> None:
        super().__init__(method=method, http_conn_id=http_conn_id, *args, **kwargs)

    def get_dataset(self, key_name: str) -> Dict[str, Any]:
        """Return ``DataSetDTO`` by key name."""
        endpoint = CONFIG_DATASET_ENDPOINT.format(key_name=quote(key_name, safe=""))
        response = self._request("GET", endpoint)
        if response.status_code == 404:
            raise AirflowException(
                "Dataset '{}' not found in yarina-config-svc (dataset not found / "
                "датасет не найден). GET {} failed with HTTP 404. Check the dataset "
                "key name and the yarina-config-svc deployment.".format(
                    key_name, endpoint
                )
            )
        self._check(response, endpoint)
        try:
            body = response.json()
        except ValueError as exc:
            raise AirflowException(
                "Unexpected non-JSON response for dataset '{}': {}".format(
                    key_name, response.text
                )
            ) from exc
        if not isinstance(body, dict):
            raise AirflowException(
                "Unexpected response for dataset '{}': expected a JSON object".format(
                    key_name
                )
            )
        return body

    def get_sources(self, key_name: str) -> List[str]:
        """Return source dataset keys, excluding the dataset itself.

        This mirrors ``DataSetDTO.sources.keySet()`` minus the target dataset
        in ``DependencyCheckStateTaskProcessor``.
        """
        dataset = self.get_dataset(key_name)
        sources = dataset.get("sources") or {}

        if isinstance(sources, dict):
            source_keys = list(sources.keys())
        elif isinstance(sources, list):
            source_keys = [
                item.get("keyName")
                for item in sources
                if isinstance(item, dict) and item.get("keyName")
            ]
        else:
            source_keys = []

        own_key = dataset.get("keyName") or key_name
        result = [key for key in source_keys if key and key not in (key_name, own_key)]
        logger.info("Dataset '%s' sources resolved from config: %s", key_name, result)
        return result
