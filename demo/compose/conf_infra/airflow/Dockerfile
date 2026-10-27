# Airflow 2.7.3 with the yarina-airflow-integration 2.x plugins.
#
# The build context is the integration root (see docker-compose.yaml), so the
# COPY paths start at 2.x/. The integration is a plugins-only package: it is
# installed into $AIRFLOW_HOME/plugins exactly as described in the module docs.
FROM apache/airflow:2.7.3-python3.11

USER airflow

# The official image already ships the HTTP provider and psycopg2. Pin the
# provider to the version bundled with Airflow 2.7.3 so a rebuild does not
# upgrade Airflow's shared dependencies (aiohttp, yarl) behind its back.
RUN pip install --no-cache-dir "apache-airflow-providers-http==4.6.0"

# Hooks, operators and shared constants. Airflow adds $AIRFLOW_HOME/plugins to
# sys.path, so the integration works with its top-level fallback imports.
COPY --chown=airflow:root 2.x/plugins/ /opt/airflow/plugins/
