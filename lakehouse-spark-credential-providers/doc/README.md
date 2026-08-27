# lakehouse-spark-credential-providers

Lightweight Java module for Apache Spark 3.5.x that dynamically retrieves secrets from **OpenBao / HashiCorp Vault** and **Yandex Cloud Lockbox** at task execution time on both Driver and Executors — without Spring Framework.

---

## 1. Overview & Prerequisites

### What this module does

| Problem | Solution |
|---------|----------|
| JDBC passwords and S3 access keys are hardcoded in `spark-defaults.conf` | Providers fetch secrets at runtime from Vault or Lockbox |
| Secrets appear in Spark UI, logs, config dumps | Values are never logged; only HTTP status codes are emitted on errors |
| Executors process thousands of partitions per second → DDoS risk on secret APIs | 5-minute in-memory TTL cache per JVM |

### Supported backends

| Backend | JDBC Provider | S3 Provider | Auth method |
|---------|---------------|-------------|-------------|
| OpenBao / Vault KV v2 | `BaoJdbcSecretProvider` | `BaoS3CredentialsProvider` | Token env (`VAULT_TOKEN`) or Kubernetes ServiceAccount JWT |
| Yandex Cloud Lockbox | `YcLockboxJdbcSecretProvider` | `YcLockboxS3CredentialsProvider` | Instance Metadata (IAM token) or Authorized Key file |

### Prerequisites for OpenBao / Vault

1. A running OpenBao or HashiCorp Vault instance with KV v2 secrets engine enabled.
2. Network connectivity from every Spark Executor to the Vault HTTP API port (default `8200`).
3. **Authentication — choose one:**
   - **Token Auth** — set the `VAULT_TOKEN` environment variable on every node.
   - **Kubernetes Auth** — Spark Pods must have a ServiceAccount with a Vault role; the token is read automatically from `/var/run/secrets/kubernetes.io/serviceaccount/token`.

### Prerequisites for Yandex Cloud Lockbox

1. A Yandex Cloud Lockbox secret containing the required keys (e.g. `access_key`, `secret_key`, `password`).
2. Every Spark Worker VM or Pod must have a **Service Account** attached with the `lockbox.payloadViewer` role.
3. No explicit credentials are needed — the IAM token is obtained automatically from the Instance Metadata Service at `http://169.254.169.254/computeMetadata/v1/...`.
4. Alternatively, set the `YC_AUTH_KEY_PATH` environment variable pointing to an authorized key file with an OAuth token.

---

## 2. Installation

### Building

```bash
mvn clean package -pl lakehouse-spark-credential-providers -am -DskipTests
```

### Deploying to Spark

Copy the JAR to every node's `spark/jars/` directory, **or** pass it via `--jars` at submission:

```bash
spark-submit \
  --jars lakehouse-spark-credential-providers-0.8.0.jar \
  ...
```

The JAR has **zero transitive dependencies** — all runtime dependencies (Spark, Hadoop, AWS SDK, Jackson) are already on the Spark classpath.

---

## 3. Configuration via `spark-defaults.conf`

### 3.1 JDBC Catalog — Yandex Cloud Lockbox

```properties
# ── Catalog wiring ──────────────────────────────────────────────
spark.sql.catalog.processingdb=org.apache.spark.sql.execution.datasources.v2.jdbc.JDBCTableCatalog
spark.sql.catalog.processingdb.url=jdbc:postgresql://db-host:5432/mydb
spark.sql.catalog.processingdb.driver=org.postgresql.Driver
spark.sql.catalog.processingdb.user=app_user

# ── Secret provider ─────────────────────────────────────────────
spark.sql.catalog.processingdb.secretProvider=org.lakehouse.security.jdbc.YcLockboxJdbcSecretProvider
spark.sql.catalog.processingdb.secret-id=eirvjuabcdef12345678
spark.sql.catalog.processingdb.secret-key=password
```

| Property | Required | Description |
|----------|----------|-------------|
| `secretProvider` | yes | FQCN of the provider class |
| `secret-id` | yes | Lockbox secret ID |
| `secret-key` | yes | Key inside the Lockbox payload |
| `secret-version` | no | Specific version; defaults to `latest` |

### 3.2 JDBC Catalog — OpenBao / Vault

```properties
# ── Catalog wiring ──────────────────────────────────────────────
spark.sql.catalog.processingdb=org.apache.spark.sql.execution.datasources.v2.jdbc.JDBCTableCatalog
spark.sql.catalog.processingdb.url=jdbc:postgresql://db-host:5432/mydb
spark.sql.catalog.processingdb.driver=org.postgresql.Driver
spark.sql.catalog.processingdb.user=app_user

# ── Secret provider ─────────────────────────────────────────────
spark.sql.catalog.processingdb.secretProvider=org.lakehouse.security.jdbc.BaoJdbcSecretProvider
spark.sql.catalog.processingdb.vault-url=http://vault:8200
spark.sql.catalog.processingdb.secret-path=secret/data/processingdb
spark.sql.catalog.processingdb.secret-key=password
```

| Property | Required | Description |
|----------|----------|-------------|
| `secretProvider` | yes | FQCN of the provider class |
| `vault-url` | yes | Vault HTTP API base URL |
| `secret-path` | yes | KV v2 path (e.g. `secret/data/myapp/db`) |
| `secret-key` | yes | Key inside the secret |
| `vault-role` | no | Kubernetes auth role (default: `lakehouse`) |
| `vault-k8s-auth-path` | no | K8s auth mount path (default: `kubernetes`) |

### 3.3 S3A / MinIO — Yandex Cloud Lockbox

```properties
# ── S3A filesystem ──────────────────────────────────────────────
spark.hadoop.fs.s3a.impl=org.apache.hadoop.fs.s3a.S3AFileSystem
spark.hadoop.fs.s3a.endpoint=https://storage.yandexcloud.net
spark.hadoop.fs.s3a.path.style.access=true
spark.hadoop.fs.s3a.impl.disable.cache=true

# ── Secret provider ─────────────────────────────────────────────
spark.hadoop.fs.s3a.aws.credentials.provider=org.lakehouse.security.s3.YcLockboxS3CredentialsProvider
spark.hadoop.fs.s3a.lockbox.secret-id=eirvjuabcdef12345678
spark.hadoop.fs.s3a.lockbox.access-key-secret=access_key
spark.hadoop.fs.s3a.lockbox.secret-key-secret=secret_key
```

| Property | Required | Default | Description |
|----------|----------|---------|-------------|
| `lockbox.secret-id` | yes | — | Lockbox secret ID |
| `lockbox.access-key-secret` | no | `access_key` | Key name for AWS access key |
| `lockbox.secret-key-secret` | no | `secret_key` | Key name for AWS secret key |

### 3.4 S3A / MinIO — OpenBao / Vault

```properties
# ── S3A filesystem ──────────────────────────────────────────────
spark.hadoop.fs.s3a.impl=org.apache.hadoop.fs.s3a.S3AFileSystem
spark.hadoop.fs.s3a.endpoint=http://minio:9000
spark.hadoop.fs.s3a.path.style.access=true
spark.hadoop.fs.s3a.impl.disable.cache=true

# ── Secret provider ─────────────────────────────────────────────
spark.hadoop.fs.s3a.aws.credentials.provider=org.lakehouse.security.s3.BaoS3CredentialsProvider
spark.hadoop.fs.s3a.bao.vault-url=http://vault:8200
spark.hadoop.fs.s3a.bao.secret-path=secret/data/s3
spark.hadoop.fs.s3a.bao.access-key-secret=access_key
spark.hadoop.fs.s3a.bao.secret-key-secret=secret_key
```

| Property | Required | Default | Description |
|----------|----------|---------|-------------|
| `bao.vault-url` | yes | — | Vault HTTP API base URL |
| `bao.secret-path` | yes | — | KV path for the S3 credentials |
| `bao.access-key-secret` | no | `access_key` | Key name for AWS access key |
| `bao.secret-key-secret` | no | `secret_key` | Key name for AWS secret key |

---

## 4. Using `LakehouseSecurityContext` in Application Code

When writing custom Spark transformations (`map`, `foreachPartition`, UDFs) that need a secret directly inside the distributed closure on the Executor, use the static context:

```java
import org.lakehouse.security.context.LakehouseSecurityContext;

df.foreachPartition(partition -> {
    // Lazy init: the Vault/Lockbox client is created once per Executor JVM
    String dbPassword = LakehouseSecurityContext.getSecret(
        "lockbox", "eirvjuabcdef12345678", "password"
    );

    try (Connection conn = DriverManager.getConnection(jdbcUrl, "app_user", dbPassword)) {
        // process partition...
    }
});
```

### Why not create the client on the Driver and pass it in the closure?

Objects captured by a Spark closure are **serialised** and shipped to Executors via Java serialization. HTTP clients (`HttpClient`, Vault/Lockbox clients) hold open sockets, thread pools, and non-serialisable state — sending them across the wire causes `java.io.NotSerializableException` or undefined behaviour.

`LakehouseSecurityContext` solves this: the `static` field `CLIENTS` lives in the Executor's JVM and is **never serialised**. The client is created lazily on the first call inside the Executor's `foreachPartition` lambda, then reused for subsequent partitions in the same JVM.

### API

```java
// Three-argument form (recommended)
public static String getSecret(String providerType, String path, String key)

// Two-argument convenience (path must contain ':' as separator)
public static String getSecret(String providerType, String pathWithKey)
```

| Parameter | Values |
|-----------|--------|
| `providerType` | `"bao"` — OpenBao/Vault, `"lockbox"` — Yandex Lockbox |
| `path` | Vault: KV path (e.g. `secret/data/db`); Lockbox: secret ID |
| `key` | Key inside the secret (e.g. `password`, `access_key`) |

### Configuration via system properties / env vars

The Bao client reads the Vault URL from (in priority order):

1. System property `lakehouse.vault.url`
2. Environment variable `VAULT_URL`
3. Default: `http://vault:8200`

---

## 5. Security & Troubleshooting

### 5.1 Protect secrets in Spark UI

Enable Spark's built-in redaction to prevent secrets from appearing in the Web UI and event logs:

```properties
spark.ui.redaction.regex=url|password|secret|token|key
```

### 5.2 Caching behaviour

- Secrets are cached in-memory per JVM with a **5-minute TTL**.
- If the secret server returns an error, the provider throws `SecurityException` (failing the Spark task) — it does **not** cache error responses.
- `refresh()` is called by Hadoop S3A on credential expiry, which clears the cache and forces a fresh fetch.

### 5.3 Timeouts

| Parameter | Value |
|-----------|-------|
| Connection timeout | 3 seconds |
| Read timeout | 5 seconds |

If the secret server is unreachable, the provider throws `SecurityException` with the message `"<service> unreachable: <cause>"` — **no secrets or tokens are included in the log output**.

### 5.4 What to check when a task fails

| Symptom | Where to look |
|---------|---------------|
| `SecurityException: Vault access denied, HTTP 403` | Vault token is expired or lacks read permission on the path |
| `SecurityException: Lockbox access denied, HTTP 403` | Service account lacks `lockbox.payloadViewer` role |
| `SecurityException: Metadata service unreachable` | Spark workers are not inside Yandex Cloud, or metadata service is blocked |
| `SecurityException: No Vault token available` | Set `VAULT_TOKEN` env or use Kubernetes Auth in a Pod with a ServiceAccount |
| `IllegalArgumentExceptiSecret key not found in Vault response` | Check `secret-path` and `secret-key` in your config |
| JDBC connection fails (password is correct in Vault) | Check `user` and `url` in catalog config — only the **password** comes from the provider |

**Important:** passwords and tokens are **never** written to logs at any log level (INFO, DEBUG, TRACE). On errors, only HTTP status codes and exception class names are logged.

### 5.5 Log messages

| Log level | Example message |
|-----------|-----------------|
| `INFO` | `BaoJdbcSecretProvider initialised` |
| `ERROR` | `Vault returned HTTP 403 for path [redacted]` |
| `ERROR` | `Metadata service request failed: Connection refused` |

The path/secret-id is always `[redacted]` in log output.
