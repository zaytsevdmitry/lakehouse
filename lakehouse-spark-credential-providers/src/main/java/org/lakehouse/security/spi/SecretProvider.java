package org.lakehouse.security.spi;

import java.util.Map;

/**
 * Minimal secret provider SPI for Spark catalogs (JDBC, etc.).
 * <p>
 * Implementations must have a public no-arg constructor (Spark SPI requirement).
 * Spark calls {@link #initialize(Map)} with catalog options, then {@link #getSecret(String)}
 * and/or {@link #getPassword(String)} as needed.
 */
public interface SecretProvider {

    /**
     * Called once after construction with the catalog's configuration map.
     */
    void initialize(Map<String, String> conf);

    /**
     * Retrieve a secret by key.
     */
    String getSecret(String key);

    /**
     * Retrieve a password by key. Default delegates to {@link #getSecret(String)}.
     */
    default String getPassword(String key) {
        return getSecret(key);
    }

    /**
     * Release resources.
     */
    default void close() {
    }
}
