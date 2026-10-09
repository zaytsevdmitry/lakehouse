#!/bin/sh

# "Jarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
# Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/jarina
# 
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
#     https://www.apache.org/licenses/LICENSE-2.0.txt
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

set -e

echo "=== OpenBao: starting dev-mode server ==="
bao server -dev \
  -dev-listen-address="${BAO_DEV_LISTEN_ADDRESS:-0.0.0.0:8200}" \
  -dev-root-token-id="${BAO_DEV_ROOT_TOKEN_ID:-jarina-root-token}" &
SERVER_PID=$!
trap 'echo "=== OpenBao: shutting down ==="; kill $SERVER_PID; exit 0' TERM INT

export BAO_ADDR="${BAO_ADDR:-http://127.0.0.1:8200}"
export BAO_TOKEN="${BAO_DEV_ROOT_TOKEN_ID:-jarina-root-token}"

echo "=== OpenBao: waiting for API readiness ==="
until bao status >/dev/null 2>&1; do
  sleep 0.5
done

echo "=== OpenBao: enabling KV v2 engine ==="
bao secrets enable -version=2 kv || echo "KV v2 engine already enabled"

echo "=== OpenBao: writing jarina/database secret ==="
bao kv put kv/jarina/database password="${JARINA_DB_PASSWORD:-postgresPW}"

echo "=== OpenBao: writing infrastructure/minio secret ==="
bao kv put kv/infrastructure/minio \
  access_key="${MINIO_ROOT_USER:-spark_user}" \
  secret_key="${MINIO_ROOT_PASSWORD:-spark_pwd}"

echo "=== OpenBao: creating read-only policy for Spark services ==="
bao policy write jarina-spark-readonly - <<'EOF'
path "kv/data/jarina/database" {
  capabilities = ["read"]
}
path "kv/data/infrastructure/minio" {
  capabilities = ["read"]
}
EOF

echo "=== OpenBao: creating scoped token for Spark services ==="
JARINA_SPARK_TOKEN="${JARINA_SPARK_TOKEN:-jarina-spark-token}"
bao token create \
  -policy=jarina-spark-readonly \
  -ttl=8760h \
  -id="${JARINA_SPARK_TOKEN}" \
  -field=token

echo "=== OpenBao: verifying scoped token permissions ==="
bao token capabilities "${JARINA_SPARK_TOKEN}" kv/data/jarina/database
bao token capabilities "${JARINA_SPARK_TOKEN}" kv/data/infrastructure/minio

echo "=== OpenBao: seeding complete, server is ready ==="
wait $SERVER_PID
