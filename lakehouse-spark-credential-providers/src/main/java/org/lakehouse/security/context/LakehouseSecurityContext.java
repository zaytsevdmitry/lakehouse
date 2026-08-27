package org.lakehouse.security.context;

import org.lakehouse.security.LockboxHttp;
import org.lakehouse.security.SecretCache;
import org.lakehouse.security.VaultHttp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Static, thread-safe context for resolving secrets directly inside distributed
 * Spark code (map, foreachPartition, UDF) on both Driver and Executors.
 *
 * <p>HTTP clients are kept in a JVM-static map, so they are <b>never serialised</b>
 * and shipped to Executors. Each client is created lazily on first use inside a
 * partition lambda and then reused for all subsequent partitions in the same JVM.
 *
 * <p>Supported provider types:
 * <ul>
 *   <li>{@code "bao"} — OpenBao / HashiCorp Vault (KV v2), via {@link VaultHttp}</li>
 *   <li>{@code "lockbox"} — Yandex Cloud Lockbox, via {@link LockboxHttp}</li>
 * </ul>
 *
 * <p>Vault base URL resolution order (Bao provider):
 * <ol>
 *   <li>system property {@code lakehouse.vault.url}</li>
 *   <li>environment variable {@code VAULT_URL}</li>
 *   <li>default {@code http://vault:8200}</li>
 * </ol>
 *
 * <p>Usage:
 * <pre>
 * df.foreachPartition(partition -> {
 *     String dbPassword = LakehouseSecurityContext.getSecret("bao", "secret/data/db", "password");
 *     ...
 * });
 * </pre>
 */
public final class LakehouseSecurityContext {

    private static final Logger LOG = LoggerFactory.getLogger(LakehouseSecurityContext.class);

    public static final String PROVIDER_BAO = "bao";
    public static final String PROVIDER_LOCKBOX = "lockbox";

    private static final String DEFAULT_VAULT_URL = "http://vault:8200";
    private static final String SYS_PROP_VAULT_URL = "lakehouse.vault.url";
    private static final String ENV_VAULT_URL = "VAULT_URL";

    /** Per-JVM registry of lazy-initialised HTTP clients (never serialised). */
    private static final Map<String, Object> CLIENTS = new ConcurrentHashMap<>();
    /** Per-JVM TTL cache preventing DDoS of the secret-server API. */
    private static final SecretCache CACHE = new SecretCache();

    private LakehouseSecurityContext() {
    }

    /**
     * Resolve a secret using a three-argument coordinate (recommended API).
     *
     * @param providerType {@code "bao"} or {@code "lockbox"}
     * @param path         Vault KV path (e.g. {@code secret/data/db}) or Lockbox secret ID
     * @param key          key inside the secret (e.g. {@code password}, {@code access_key})
     * @return the secret value
     */
    public static String getSecret(String providerType, String path, String key) {
        if (providerType == null || providerType.isBlank()) {
            throw new IllegalArgumentException("providerType must not be blank");
        }
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("path must not be blank");
        }
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }

        String cacheKey = providerType + ":" + path + ":" + key;
        String cached = CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        String value = switch (providerType.toLowerCase()) {
            case PROVIDER_BAO -> baoClient().getSecret(path, key);
            case PROVIDER_LOCKBOX -> lockboxClient().getSecret(path, key, "latest");
            default -> throw new IllegalArgumentException(
                    "Unknown secret provider type: '" + providerType + "'. Supported: "
                            + PROVIDER_BAO + ", " + PROVIDER_LOCKBOX + ".");
        };
        CACHE.put(cacheKey, value);
        return value;
    }

    /**
     * Resolve a secret using the combined {@code path:key} coordinate.
     *
     * @param providerType {@code "bao"} or {@code "lockbox"}
     * @param pathWithKey  combined coordinate (e.g. {@code secret/data/db:password})
     * @return the secret value
     */
    public static String getSecret(String providerType, String pathWithKey) {
        if (pathWithKey == null || providerType == null) {
            throw new IllegalArgumentException("providerType and pathWithKey must not be null");
        }
        String[] parts = pathWithKey.split(":", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException(
                    "Provider '" + providerType + "' pathWithKey must be in format 'path:key', got: '" + pathWithKey + "'");
        }
        return getSecret(providerType, parts[0], parts[1]);
    }

    /**
     * Drop all cached clients and cached values. The next {@link #getSecret} call
     * creates a fresh client.
     */
    public static void reset() {
        CLIENTS.clear();
        CACHE.clear();
        LOG.debug("LakehouseSecurityContext reset: clients and cache cleared");
    }

    /* ------------------------------------------------------------------ */

    private static VaultHttp baoClient() {
        return (VaultHttp) CLIENTS.computeIfAbsent(PROVIDER_BAO, k -> {
            LOG.info("Created Vault client");
            return new VaultHttp(resolveVaultUrl(), Map.of());
        });
    }

    private static LockboxHttp lockboxClient() {
        return (LockboxHttp) CLIENTS.computeIfAbsent(PROVIDER_LOCKBOX, k -> {
            LOG.info("Created Lockbox client");
            return new LockboxHttp();
        });
    }

    private static String resolveVaultUrl() {
        String sysProp = System.getProperty(SYS_PROP_VAULT_URL);
        if (sysProp != null && !sysProp.isBlank()) {
            return sysProp;
        }
        String env = System.getenv(ENV_VAULT_URL);
        if (env != null && !env.isBlank()) {
            return env;
        }
        return DEFAULT_VAULT_URL;
    }
}