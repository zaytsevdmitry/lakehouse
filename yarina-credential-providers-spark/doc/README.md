# yarina-credential-providers-spark

Spark-specific credential providers for Apache Spark 3.5.x that dynamically retrieve secrets from **OpenBao / HashiCorp Vault** and **Yandex Cloud Lockbox** at task execution time on both Driver and Executors — without Spring Framework.

The project is split into two modules:

| Module | Contents |
|--------|----------|
| `yarina-credential-providers-jdbc` | Shared, Spark-independent code: the `SecretProvider` SPI, `VaultHttp` / `LockboxHttp` HTTP clients, `SecretCache`, `SecurityMetrics`, the JDBC providers (`BaoJdbcSecretProvider`, `YcLockboxJdbcSecretProvider`), `YarinaSecurityContext` and the `SecretResolver` helper. Reused by yarina services (e.g. `yarina-task-executor-api`). |
| `yarina-credential-providers-spark` | Spark-specific code: the S3 providers (`BaoS3CredentialsProvider`, `YcLockboxS3CredentialsProvider`) and `YarinaSecureJDBCTableCatalog`. Depends on the shared module. |

---

## 1. Overview & Prerequisites

### What this module does

| Problem | Solution |
|---------|----------|
| JDBC passwords and S3 access keys are hardcoded in `spark-defaults.conf` | Providers fetch secrets at runtime from Vault or Lockbox |
| Secrets appear in Spark UI, logs, config dumps | Values are never logged; only HTTP status codes are emitted on errors |
| Executors process thousands of partitions per second → DDoS risk on secret APIs | JVM-wide in-memory TTL cache (5 minutes) with single-flight loading: one Vault request per secret per TTL window, regardless of the number of provider instances or partitions |

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
mvn clean package -pl yarina-credential-providers-jdbc,yarina-credential-providers-spark -am -DskipTests
```

### Deploying to Spark

Copy **both** JARs to every node's `spark/jars/` directory, **or** pass them via `--jars` at submission:

```bash
spark-submit \
  --jars yarina-credential-providers-jdbc-1.1.1.jar,yarina-credential-providers-spark-1.1.1.jar \
  ...
```

Both JARs have **zero transitive runtime dependencies** — all runtime dependencies (Spark, Hadoop, AWS SDK, Jackson) are already on the Spark classpath. In the demo Docker stack the two JARs are copied into the image by `docker/yarina-spark-aws/build.bash`.

The current version is **1.1.1**. Release notes and the history of breaking changes are maintained in [`CHANGELOG.md`](../../CHANGELOG.md) at the repository root.

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
spark.sql.catalog.processingdb.secretProvider=org.yarina.security.jdbc.YcLockboxJdbcSecretProvider
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
spark.sql.catalog.processingdb.secretProvider=org.yarina.security.jdbc.BaoJdbcSecretProvider
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
| `vault-role` | no | Kubernetes auth role (default: `yarina` — see [Authentication](#6-authentication)) |
| `vault-k8s-auth-path` | no | K8s auth mount path (default: `kubernetes`) |

### 3.3 S3A / MinIO — Yandex Cloud Lockbox

```properties
# ── S3A filesystem ──────────────────────────────────────────────
spark.hadoop.fs.s3a.impl=org.apache.hadoop.fs.s3a.S3AFileSystem
spark.hadoop.fs.s3a.endpoint=https://storage.yandexcloud.net
spark.hadoop.fs.s3a.path.style.access=true

# ── Secret provider ─────────────────────────────────────────────
spark.hadoop.fs.s3a.aws.credentials.provider=org.yarina.security.s3.YcLockboxS3CredentialsProvider
spark.hadoop.fs.s3a.lockbox.secret-id=eirvjuabcdef12345678
spark.hadoop.fs.s3a.lockbox.access-key-secret=access_key
spark.hadoop.fs.s3a.lockbox.secret-key-secret=secret_key
```

| Property | Required | Default | Description |
|----------|----------|---------|-------------|
| `lockbox.secret-id` | yes | — | Lockbox secret ID |
| `lockbox.access-key-secret` | no | `access_key` | Key name for AWS access key |
| `lockbox.secret-key-secret` | no | `secret_key` | Key name for AWS secret key |

> `spark.hadoop.fs.s3a.impl.disable.cache=true` is **optional** — see the note in [3.4](#34-s3a--minio--openbao--vault).

### 3.4 S3A / MinIO — OpenBao / Vault

```properties
# ── S3A filesystem ──────────────────────────────────────────────
spark.hadoop.fs.s3a.impl=org.apache.hadoop.fs.s3a.S3AFileSystem
spark.hadoop.fs.s3a.endpoint=http://minio:9000
spark.hadoop.fs.s3a.path.style.access=true

# ── Secret provider ─────────────────────────────────────────────
spark.hadoop.fs.s3a.aws.credentials.provider=org.yarina.security.s3.BaoS3CredentialsProvider
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
| `bao.vault-role` | no | `yarina` | Kubernetes auth role (see [Authentication](#6-authentication)) |
| `bao.vault-k8s-auth-path` | no | `kubernetes` | Kubernetes auth mount path in Vault |
| `bao.vault-k8s-token-path` | no | `/var/run/secrets/kubernetes.io/serviceaccount/token` | Path to the ServiceAccount JWT |

Notes:

- Both keys of the credential pair (`access_key` + `secret_key`) are fetched with a **single** HTTP request to Vault (a batch read of the whole secret payload).
- The provider fails fast (in the constructor) when `fs.s3a.bao.vault-url` or `fs.s3a.bao.secret-path` is missing.
- **About `spark.hadoop.fs.s3a.impl.disable.cache=true`:** this option is *not required* for the providers to work. When set, every `FileSystem.get` creates a fresh `S3AFileSystem` **and a fresh provider instance** — since 1.1.1 all instances in one JVM share the credential cache and the Vault client, so the option no longer multiplies OpenBao requests (see [5.1 Cache scope](#51-cache-scope)); it still multiplies S3A client objects, so keep it only if the platform relies on per-task filesystem isolation.

### 3.5 Multiple buckets and multiple clouds

Every bucket can override the provider options with the standard S3A bucket-scoped prefix `fs.s3a.bucket.<name>.*` — S3A copies those keys over the global `fs.s3a.*` values for that bucket's filesystem:

```properties
# ── Common settings ─────────────────────────────────────────────
spark.hadoop.fs.s3a.impl=org.apache.hadoop.fs.s3a.S3AFileSystem
spark.hadoop.fs.s3a.path.style.access=true
spark.hadoop.fs.s3a.aws.credentials.provider=org.yarina.security.s3.BaoS3CredentialsProvider
spark.hadoop.fs.s3a.bao.vault-url=http://vault:8200

# ── Bucket "data": its own secret path ──────────────────────────
spark.hadoop.fs.s3a.bucket.data.endpoint=http://minio-prod:9000
spark.hadoop.fs.s3a.bucket.data.bao.secret-path=secret/data/s3-prod

# ── Bucket "archive": another secret, another role, another Vault ─
spark.hadoop.fs.s3a.bucket.archive.endpoint=http://minio-archive:9000
spark.hadoop.fs.s3a.bucket.archive.bao.vault-url=http://vault-archive:8200
spark.hadoop.fs.s3a.bucket.archive.bao.secret-path=secret/data/s3-archive
spark.hadoop.fs.s3a.bucket.archive.bao.vault-role=archive-reader
```

| Scenario | Supported since 1.1.1 |
|----------|----------------------|
| One OpenBao, several buckets with different `secret-path` | Yes — each `(vault-url, secret-path)` pair is an independent cache entry; `refresh()` of one bucket does not touch the others |
| Several OpenBao instances (`bao.vault-url` differs per bucket) | Yes — a separate Vault client per `(vault-url, vault-role, auth-path)` |
| Kubernetes auth with different roles per bucket (`bao.vault-role`) | Yes — the role is part of the client key |
| Different static `VAULT_TOKEN` per OpenBao instance | **No** — the environment variable is JVM-global and static tokens are never refreshed. Per-provider explicit tokens exist only as a `VaultHttp` constructor overload for programmatic use and are not exposed as S3A/JDBC options; use Kubernetes auth with distinct roles instead |

---

## 4. Using `YarinaSecurityContext` in Application Code

When writing custom Spark transformations (`map`, `foreachPartition`, UDFs) that need a secret directly inside the distributed closure on the Executor, use the static context:

```java
import org.yarina.security.context.YarinaSecurityContext;

df.foreachPartition(partition -> {
    // Lazy init: the Vault/Lockbox client is created once per Executor JVM
    String dbPassword = YarinaSecurityContext.getSecret(
        "lockbox", "eirvjuabcdef12345678", "password"
    );

    try (Connection conn = DriverManager.getConnection(jdbcUrl, "app_user", dbPassword)) {
        // process partition...
    }
});
```

### Why not create the client on the Driver and pass it in the closure?

Objects captured by a Spark closure are **serialised** and shipped to Executors via Java serialization. HTTP clients (`HttpClient`, Vault/Lockbox clients) hold open sockets, thread pools, and non-serialisable state — sending them across the wire causes `java.io.NotSerializableException` or undefined behaviour.

`YarinaSecurityContext` solves this: the `static` field `CLIENTS` lives in the Executor's JVM and is **never serialised**. The client is created lazily on the first call inside the Executor's `foreachPartition` lambda, then reused for subsequent partitions in the same JVM.

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

1. System property `yarina.vault.url`
2. Environment variable `VAULT_URL`
3. Default: `http://vault:8200`

### Using the providers in yarina services (JDBC path)

The shared module is also used by non-Spark yarina services. The JDBC connection factory
of `yarina-task-executor-api` (`JdbcConnectionFactory`) applies the same option contract:
when a `ServiceDTO.properties` map contains `secretProvider`, the password is resolved before
`DriverManager.getConnection(...)` and the security keys are stripped from the JDBC properties.

```properties
# Service.properties of a JDBC DataSource (storage of ServiceDTO.properties in yarina-config-svc)
url            jdbc:postgresql://postgres:5432/yarina_db
user           yarina_user
secretProvider org.yarina.security.jdbc.BaoJdbcSecretProvider
secret-key     kv/data/yarina/database:password
vault-url      http://openbao:8200
```

Supported keys: `secretProvider`, `secret-key` (combined `path:key`), and provider-specific
`vault-url`, `vault-role`, `vault-k8s-auth-path`, `secret-id`, `secret-version`. All of them are
removed before the options are passed to the JDBC driver. When `secretProvider` is absent the map
is passed through unchanged, so existing configurations keep working as-is.

---

## 5. Caching and OpenBao load

### 5.1 Cache scope

Secrets are cached **per JVM** (Spark Driver or Executor process), not per provider instance:

- The credential cache and the Vault clients of `BaoS3CredentialsProvider` live in `static`
  fields shared by **all** provider instances of the JVM. Hadoop S3A may create many instances
  (one per `FileSystem`; with `fs.s3a.impl.disable.cache=true` — one per `FileSystem.get`),
  but they all reuse the same cache, so the OpenBao load stays proportional to the TTL and
  not to the number of instances.
- **TTL: 5 minutes.** An expired entry is removed from the cache on the next access; the
  following call fetches fresh credentials. Errors are **never** cached — a failed fetch does
  not stick, the next call goes to Vault again.
- **Cache key:** `bao:s3:<vault-url>:<secret-path>` — different buckets (different secret
  paths) and different Vault servers never share an entry.
- **Single-flight:** when many threads/misses happen simultaneously (e.g. 50 concurrent
  partitions), exactly **one** HTTP request is issued; the other threads wait for its result.
  A JVM-wide `HttpClient` is shared for all Vault traffic — connections are reused instead of
  multiplied.
- **Batch fetch:** the access/secret key pair is read with **one** HTTP request
  (`GET /v1/<secret-path>`) instead of one request per key.
- `refresh()` invalidates only the caller's own `(vault-url, secret-path)` entry — credentials
  of neighbouring buckets in the same JVM stay cached.

### 5.2 Secret rotation

- **S3 path:** Hadoop S3A calls `refresh()` on the provider when it detects expired
  credentials; this drops that provider's cache entry and the next request re-reads the
  secret from Vault. Independently of `refresh()`, any rotated secret is picked up within
  the 5-minute TTL at the latest.
- **JDBC path:** the password is resolved when a connection is created; a rotated secret is
  picked up on the next cache miss (at most 5 minutes later). Existing connections keep the
  password they were opened with — restart the application or rely on the connection pool
  rotation to re-open them.
- **Vault token rotation** is handled separately — see below.

### 5.3 Vault token refresh on HTTP 403

- A token obtained via **Kubernetes auth** is renewable: when Vault rejects a read with
  HTTP 403, the client re-authenticates (a single Kubernetes login, even under concurrency)
  and repeats the request **exactly once** — there is no retry storm.
- A token from `VAULT_TOKEN` (or passed programmatically) is **static**: on 403 the request
  fails fast without re-auth. See [Authentication](#6-authentication) for why Kubernetes auth
  is recommended in production.

---

## 6. Authentication

Token resolution priority in `VaultHttp`:

1. Explicit token passed programmatically to the `VaultHttp` constructor (not exposed as a
   Spark/Hadoop option);
2. `VAULT_TOKEN` environment variable;
3. Kubernetes auth (ServiceAccount JWT from
   `/var/run/secrets/kubernetes.io/serviceaccount/token` by default, configurable via
   `fs.s3a.bao.vault-k8s-token-path`).

Recommendations:

- **Use Kubernetes auth in production.** The ServiceAccount token is mounted into the Pod by
  kubelet, is short-lived, is bound to a Vault role, and never appears in configuration
  files. A rejected token self-heals via re-authentication.
- **`VAULT_TOKEN` is visible** to every process and user on the node through
  `/proc/<pid>/environ`, is shared by all tasks scheduled on that node, and is never
  refreshed — an expired token fails every task with `HTTP 403` until the variable is
  updated manually.
- **`vault-role` defaults to `yarina`.** If the role bound in OpenBao for the `kubernetes`
  auth method has a different name, it **must** be set explicitly (`vault-role` for JDBC,
  `fs.s3a.bao.vault-role` for S3A) — a mismatch produces
  `Vault k8s auth failed, HTTP 403` on every login.

---

## 7. TLS / transport security

- Both HTTP clients (`VaultHttp`, `LockboxHttp`) use the JDK `HttpClient` and fully support
  `https://` Vault/Lockbox URLs: TLS is negotiated by the standard JSSE stack with hostname
  verification always on.
- **Custom CA:** import the certificate into the JVM truststore used by the Spark JVMs
  (`keytool -importcert -cacerts`, or point
  `-Djavax.net.ssl.trustStore=/path/to/truststore.jks` at a custom one). This is the standard
  mechanism for self-signed or corporate PKI certificates.
- **mTLS is not supported** for the Vault connection in this version: the client does not
  present a client certificate. Vault's API authentication is token-based, so this only
  matters when a corporate TLS-terminating proxy demands client certificates — in that case
  terminate TLS in a sidecar (e.g. `stunnel`/`envoy`) and point the provider at the local
  plaintext endpoint.

---

## 8. Metrics and diagnostics

All counters are JVM-wide, dependency-free (`SecurityMetrics` in the shared module). Two export paths are always available:

1. **JMX** — the MBean `org.yarina.security:type=SecurityMetrics` exposes the attribute
   `PrometheusText` (the whole registry in the Prometheus text format) and the `Counters`
   table (one row per counter). Inspect it with `jconsole`/`jmxterm`; section 9 shows
   how to deliver it to Prometheus via the JMX exporter javaagent.
2. **Programmatic** — `org.yarina.security.SecurityMetrics.scrapePrometheus()` renders the
   same text; a platform service that already runs Spring Actuator/Micrometer (e.g.
   `yarina-task-proxy-for-spark`) can publish it periodically under its own
   `/actuator/prometheus` endpoint.

| Metric | Source | Meaning |
|--------|--------|---------|
| `cache_hits_total` | `SecretCache` | Lookups served from the cache |
| `cache_misses_total` | `SecretCache` | Lookups that had to be (re)loaded |
| `cache_evictions_total` | `SecretCache` | Entries removed (TTL expiry, `refresh()`) |
| `vault_requests_total{op="read"}` | providers | Secret reads initiated |
| `vault_requests_total{op="auth"}` | providers | Vault client creations |
| `provider_instances_created_total` | providers | Provider instances created in the JVM |
| `vault_http_requests_total{status="…"}` | `VaultHttp` | Raw Vault HTTP reads by status code |
| `vault_auth_total` | `VaultHttp` | Kubernetes logins performed |
| `vault_token_refresh_total` | `VaultHttp` | 403-triggered token refreshes |

How to sanity-check the OpenBao load:

- **`cache_hits_total` must dominate `cache_misses_total`** — a healthy JVM shows one burst
  of misses per secret per 5-minute window and thousands of hits in between.
- `vault_auth_total` ≈ number of distinct `(vault-url, vault-role, auth-path)` combinations in
  the JVM — **not** one login per provider instance. A login storm in the OpenBao access log
  (`POST /v1/auth/kubernetes/login`) means pre-1.1.1 clients or a broken deployment.
- `provider_instances_created_total` growing quickly (e.g. one per `FileSystem.get` with
  `fs.s3a.impl.disable.cache=true`) is expected and harmless — as long as
  `vault_auth_total` and `vault_requests_total{op="read"}` stay proportional to the TTL.

---

## 9. Setting up metrics export to Prometheus

A Spark deployment with these providers produces two independent metric streams, and
each of them is wired to Prometheus separately:

| Stream | Counters | Where they live | Export |
|--------|----------|-----------------|--------|
| Credential-provider health (Section 8) | `cache_hits_total`, `vault_auth_total`, … | JVM of every Spark driver/executor | JMX exporter javaagent → HTTP scrape (this section) |
| Proxy service health | `yarina_task_proxy4spark_*` | JVM of `yarina-task-proxy-for-spark` | Spring Boot Actuator `/actuator/prometheus` — see the proxy README, section *Prometheus setup* |

The providers are a library inside the Spark JVMs and ship with zero runtime
dependencies, so they never push metrics anywhere by themselves — the export is pure
infrastructure configuration. `SecurityMetrics` publishes the JMX MBean
`org.yarina.security:type=SecurityMetrics` whose `Counters` attribute holds one row per
counter; the [Prometheus JMX exporter](https://github.com/prometheus/jmx_exporter)
javaagent turns those rows into an HTTP scrape endpoint. No application code changes
are required.

### 9.1 Attach the JMX exporter javaagent to the Spark JVMs

1. Download the javaagent (`jmx_prometheus_javaagent-<version>.jar`, e.g. 1.7.0) from
   the [JMX exporter releases](https://github.com/prometheus/jmx_exporter/releases) and
   place it on every Spark node — for `docker/yarina-spark-aws`, bake it into the image,
   e.g. under `/opt/spark/jmx/`.

2. Create the exporter config `/opt/spark/jmx/security-metrics.yaml`. Every row of the
   `Counters` attribute arrives at the rule engine as
   `org.yarina.security<type=SecurityMetrics, key=<metric-id>><>Counters: <value>`:

   ```yaml
   rules:
     - pattern: 'org.yarina.security<type=SecurityMetrics, key=([^>]+)><>Counters: (\d+)'
       name: yarina_security_counters
       value: '$2'
       labels:
         metric: '$1'
       type: COUNTER
       help: 'SecurityMetrics counters of the yarina credential providers (see the README section "Metrics and diagnostics")'
   ```

3. Enable the agent on the driver and the executors in `spark-defaults.conf`
   (distinct ports — every JVM serves its own endpoint):

   ```properties
   # ── Metrics export to Prometheus (JMX exporter javaagent) ─────────
   spark.driver.extraJavaOptions   -javaagent:/opt/spark/jmx/jmx_prometheus_javaagent-1.7.0.jar=9091:/opt/spark/jmx/security-metrics.yaml
   spark.executor.extraJavaOptions -javaagent:/opt/spark/jmx/jmx_prometheus_javaagent-1.7.0.jar=9092:/opt/spark/jmx/security-metrics.yaml
   ```

The driver port must be free on the driver node and the executor port on every worker
node. Several JVMs of the same kind on one node (e.g. several executors per worker) need
a distinct port each — otherwise keep one executor per node. In `cluster` deploy mode
(the way `yarina-task-proxy-for-spark` submits applications) the driver runs on a worker
node too.

Quick check while a job is running:

```bash
curl -s http://<driver-host>:9091/metrics | grep yarina_security
```

Every counter from Section 8 is exported as one sample of a single metric family
(the exporter appends `_total` to counter names automatically):

```
yarina_security_counters_total{metric="cache_hits_total"} 18234
yarina_security_counters_total{metric="vault_auth_total"} 3
yarina_security_counters_total{metric="vault_requests_total{op=\"read\"}"} 29
```

### 9.2 Scrape configuration in Prometheus

`prometheus.yml` (host names follow the `demo/compose` stand — replace them with the
real ones; the reference configuration lives in
`demo/compose/conf_infra/prometheus/prometheus.yml`):

```yaml
scrape_configs:
  - job_name: spark-security-driver
    static_configs:
      - targets: ['spark-worker-1:9091']
        labels:
          role: driver
  - job_name: spark-security-executors
    static_configs:
      - targets: ['spark-worker-1:9092']
        labels:
          role: executor
```

### 9.3 Reading the metrics in PromQL / Grafana

The metric id from Section 8 travels in the `metric` label:

| What to watch (Section 8) | PromQL |
|---------------------------|--------|
| Cache hit ratio | `rate(yarina_security_counters_total{metric="cache_hits_total"}[5m]) / (rate(yarina_security_counters_total{metric="cache_hits_total"}[5m]) + rate(yarina_security_counters_total{metric="cache_misses_total"}[5m]))` |
| Vault reads | `sum by (instance) (rate(yarina_security_counters_total{metric=~"vault_requests_total.*"}[5m]))` |
| Kubernetes logins | `rate(yarina_security_counters_total{metric="vault_auth_total"}[5m])` |
| 403-driven token refreshes | `rate(yarina_security_counters_total{metric="vault_token_refresh_total"}[5m])` |
| Vault HTTP by status | `rate(yarina_security_counters_total{metric=~"vault_http_requests_total.*"}[5m])` |

A jump of `vault_auth_total` / `vault_requests_total{op="read"}` per `instance` is the
"login storm" signature from Section 8; `vault_http_requests_total{status="5.."}` points
at OpenBao-side trouble. On the demo stand Grafana is available with the Prometheus
datasource pre-provisioned — see `demo/compose/README-INSTALL.md`.

### 9.4 Verifying without Prometheus

- `curl http://<driver-host>:9091/metrics` — the javaagent output shown above.
- `jconsole` / `jmxterm` — the `org.yarina.security:type=SecurityMetrics` MBean: the
  `PrometheusText` attribute (the whole registry as text) and the `Counters` table. No
  agent needed, works in any JVM with the providers on the classpath.
- `SecurityMetrics.scrapePrometheus()` — the same text programmatically; a platform
  service embedding the providers (e.g. `yarina-task-executor-api`) can publish it
  under its own actuator endpoint.

---

## 10. Security & Troubleshooting

### 10.1 Protect secrets in Spark UI

Enable Spark's built-in redaction to prevent secrets from appearing in the Web UI and event logs:

```properties
spark.ui.redaction.regex=url|password|secret|token|key
```

### 10.2 Timeouts

| Parameter | Value |
|-----------|-------|
| Connection timeout | 3 seconds |
| Read timeout | 5 seconds |

If the secret server is unreachable, the provider throws `SecurityException` — **no secrets or tokens are included in the log output**. Non-200 responses (other than a 403 with a renewable token, which triggers exactly one re-auth + retry) are **not retried**.

### 10.3 What to check when a task fails

| Symptom | Where to look |
|---------|---------------|
| `SecurityException: Vault access denied, HTTP 403` | Vault token is expired or lacks read permission on the path. With `VAULT_TOKEN` this fails permanently (static token is not refreshed) — use Kubernetes auth, which re-authenticates automatically |
| `SecurityException: Vault k8s auth failed, HTTP 403` | `vault-role` does not match the role bound in OpenBao for the `kubernetes` auth method; the default is `yarina` — set it explicitly if yours differs |
| `SecurityException: No Vault token available` | Set `VAULT_TOKEN` env or use Kubernetes Auth in a Pod with a ServiceAccount |
| `SecurityException: Secret key not found in Vault response` | Check `secret-path` and `secret-key` in your config |
| `SecurityException: BaoS3CredentialsProvider: secret key 'access_key' not found in Vault payload` | The KV secret at `bao.secret-path` does not contain the configured key names (`bao.access-key-secret` / `bao.secret-key-secret`) |
| Many `POST /v1/auth/kubernetes/login` entries in the OpenBao access log | A sign of Vault clients being re-created (pre-1.1.1 behaviour: one client per provider instance). Since 1.1.1 a JVM performs one login per `(vault-url, vault-role, auth-path)`; upgrade and redeploy the JARs |
| `SecurityException: Lockbox access denied, HTTP 403` | Service account lacks `lockbox.payloadViewer` role |
| `SecurityException: Metadata service unreachable` | Spark workers are not inside Yandex Cloud, or metadata service is blocked |
| JDBC connection fails (password is correct in Vault) | Check `user` and `url` in catalog config — only the **password** comes from the provider |

**Important:** passwords and tokens are **never** written to logs at any log level (INFO, DEBUG, TRACE). On errors, only HTTP status codes and exception class names are logged.

### 10.4 Log messages

| Log level | Example message |
|-----------|-----------------|
| `INFO` | `BaoJdbcSecretProvider initialised` |
| `INFO` | `BaoS3CredentialsProvider initialised` |
| `INFO` | `Creating Vault client for [redacted]` |
| `INFO` | `Vault token was rejected (HTTP 403); re-authenticating` |
| `ERROR` | `Vault returned HTTP 403 for path [redacted]` |
| `ERROR` | `Metadata service request failed: Connection refused` |

The path/secret-id is always `[redacted]` in log output.

---

## 11. Secret payload examples

### Vault KV v2 (OpenBao)

```bash
# write the S3 credential pair (values are examples; never commit real ones)
bao kv put secret/data/s3 \
    access_key='mInIo-ak-9f2e11a8b64c' \
    secret_key='REDACTED-32-chars-secret-value'
```

`GET /v1/secret/data/s3` returns (values masked):

```json
{
  "data": {
    "data": {
      "access_key": "mInIo-ak-9f2e11••••",
      "secret_key": "••••••••••••••••••••••••"
    },
    "metadata": { "created_time": "2026-11-03T10:15:00Z", "version": 3 }
  }
}
```

The provider maps `access_key` → AWS access key and `secret_key` → AWS secret key (key names
configurable via `bao.access-key-secret` / `bao.secret-key-secret`). A JDBC secret
(`secret/data/myapp/db`) uses the same shape with e.g. a `password` key.

### Yandex Cloud Lockbox

The Lockbox secret with ID `eirvjuabcdef12345678` contains `entries` (values masked):

```json
{
  "entries": [
    { "key": "access_key", "value": "YCAJEvh7yZ•••••••••" },
    { "key": "secret_key", "value": "•••••••••••••••••••••••••••" }
  ]
}
```

The provider maps the entries by key name; `lockbox.access-key-secret` /
`lockbox.secret-key-secret` select which keys hold the AWS credential pair.
