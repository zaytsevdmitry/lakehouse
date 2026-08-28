/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lakehouse.security.jdbc;

import org.lakehouse.security.SecretCache;
import org.lakehouse.security.VaultHttp;
import org.lakehouse.security.spi.SecretProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Secret provider that retrieves JDBC credentials from OpenBao / Vault.
 *
 * <p>Required catalog options:
 * <ul>
 *   <li>{@code vault-url} — Vault HTTP API base URL, e.g. {@code http://vault:8200}</li>
 *   <li>{@code secret-path} — KV path, e.g. {@code secret/data/processingdb}</li>
 *   <li>{@code secret-key} — key inside the secret, e.g. {@code password}</li>
 * </ul>
 *
 * <p>Authentication: Kubernetes Auth (service-account JWT) or Token Auth (VAULT_TOKEN env).
 *
 * <p>Usage in spark-defaults.conf:
 * <pre>
 * spark.sql.catalog.processingdb.secretProvider=org.lakehouse.security.jdbc.BaoJdbcSecretProvider
 * spark.sql.catalog.processingdb.vault-url=http://vault:8200
 * spark.sql.catalog.processingdb.secret-path=secret/data/processingdb
 * spark.sql.catalog.processingdb.secret-key=password
 * </pre>
 */
public class BaoJdbcSecretProvider implements SecretProvider {

    private static final Logger LOG = LoggerFactory.getLogger(BaoJdbcSecretProvider.class);

    private static final String OPT_VAULT_URL = "vault-url";

    private final SecretCache cache = new SecretCache();
    private VaultHttp vault;

    /** Required public no-arg constructor for Spark SPI. */
    public BaoJdbcSecretProvider() {
    }

    @Override
    public void initialize(Map<String, String> conf) {
        String vaultUrl = require(conf, OPT_VAULT_URL);
        this.vault = new VaultHttp(vaultUrl, conf);
        LOG.info("BaoJdbcSecretProvider initialised");
    }

    @Override
    public String getSecret(String key) {
        String cacheKey = "bao:jdbc:" + key;
        String cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        // key encodes "secret-path:secret-key"
        String[] parts = key.split(":", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException(
                    "BaoJdbcSecretProvider key must be in format 'secretPath:secretKey'");
        }
        String secretPath = parts[0];
        String secretKey = parts[1];
        String value = vault.getSecret(secretPath, secretKey);
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

    private static String require(Map<String, String> map, String key) {
        String val = map.get(key);
        if (val == null || val.isEmpty()) {
            throw new IllegalArgumentException("Required option missing: " + key);
        }
        return val;
    }
}
