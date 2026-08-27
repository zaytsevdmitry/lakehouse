package org.lakehouse.security.jdbc;

import org.lakehouse.security.LockboxHttp;
import org.lakehouse.security.SecretCache;
import org.lakehouse.security.spi.SecretProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Secret provider that retrieves JDBC credentials from Yandex Cloud Lockbox.
 *
 * <p>Required catalog options:
 * <ul>
 *   <li>{@code secret-id} — Lockbox secret ID</li>
 *   <li>{@code secret-key} — key inside the secret payload</li>
 *   <li>{@code secret-version} — (optional) specific version; defaults to {@code latest}</li>
 * </ul>
 *
 * <p>Authentication: Yandex Cloud Instance Metadata Service (IAM token) or Authorized Key file.
 *
 * <p>Usage in spark-defaults.conf:
 * <pre>
 * spark.sql.catalog.processingdb.secretProvider=org.lakehouse.security.jdbc.YcLockboxJdbcSecretProvider
 * spark.sql.catalog.processingdb.secret-id=eirvju...
 * spark.sql.catalog.processingdb.secret-key=password
 * </pre>
 */
public class YcLockboxJdbcSecretProvider implements SecretProvider {

    private static final Logger LOG = LoggerFactory.getLogger(YcLockboxJdbcSecretProvider.class);

    private final SecretCache cache = new SecretCache();
    private LockboxHttp lockbox;

    /** Required public no-arg constructor for Spark SPI. */
    public YcLockboxJdbcSecretProvider() {
    }

    @Override
    public void initialize(Map<String, String> conf) {
        this.lockbox = new LockboxHttp();
        LOG.info("YcLockboxJdbcSecretProvider initialised");
    }

    @Override
    public String getSecret(String key) {
        String cacheKey = "yc:jdbc:" + key;
        String cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        String secretId = key;
        String secretKey = null;
        // If key contains ':', treat as secretId:secretKey
        if (key.contains(":")) {
            String[] parts = key.split(":", 2);
            secretId = parts[0];
            secretKey = parts[1];
        }
        if (secretKey == null || secretKey.isEmpty()) {
            throw new IllegalArgumentException(
                    "YcLockboxJdbcSecretProvider key must be 'secretId:secretKey' or 'secretId' with secret-key option");
        }
        String value = lockbox.getSecret(secretId, secretKey, null);
        cache.put(cacheKey, value);
        return value;
    }

    @Override
    public String getPassword(String key) {
        return getSecret(key);
    }

    @Override
    public void close() {
        cache.clear();
    }
}
