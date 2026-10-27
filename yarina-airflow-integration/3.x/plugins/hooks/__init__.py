"""Hooks for the Yarina services."""

try:  # package import (``plugins.*``)
    from plugins.hooks.yarina_base_hook import YarinaBaseHook
    from plugins.hooks.yarina_config_hook import YarinaConfigHook
    from plugins.hooks.yarina_spark_proxy_hook import YarinaSparkProxyHook
    from plugins.hooks.yarina_state_hook import YarinaStateHook
except ImportError:  # pragma: no cover - Airflow loads plugin files as top-level modules
    from hooks.yarina_base_hook import YarinaBaseHook  # type: ignore[no-redef]
    from hooks.yarina_config_hook import YarinaConfigHook  # type: ignore[no-redef]
    from hooks.yarina_spark_proxy_hook import YarinaSparkProxyHook  # type: ignore[no-redef]
    from hooks.yarina_state_hook import YarinaStateHook  # type: ignore[no-redef]

__all__ = [
    "YarinaBaseHook",
    "YarinaStateHook",
    "YarinaConfigHook",
    "YarinaSparkProxyHook",
]
