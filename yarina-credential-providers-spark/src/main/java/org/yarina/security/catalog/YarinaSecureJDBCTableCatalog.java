/*
 * "Yarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/yarina
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
package org.yarina.security.catalog;

import org.apache.spark.sql.execution.datasources.v2.jdbc.JDBCTableCatalog;
import org.apache.spark.sql.util.CaseInsensitiveStringMap;
import org.yarina.security.SecretResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom secure JDBC catalog implementation for the Yarina platform.
 * Intercepts catalog initialization on both Driver and Executors to dynamically
 * resolve credentials via a configured {@link SecretProvider}.
 *
 * Properties
 * # Configure the custom secure catalog execution manager
 * spark.sql.catalog.processingdb               org.yarina.security.catalog.YarinaSecureJDBCTableCatalog
 * spark.sql.catalog.processingdb.url           jdbc:postgresql://localhost:5432/yarina_db
 * spark.sql.catalog.processingdb.user          yarina_user
 *
 * # Wire the Bao provider and point to the path/key coordinates using the semicolon divider
 * spark.sql.catalog.processingdb.secretProvider org.yarina.security.jdbc.BaoJdbcSecretProvider
 * spark.sql.catalog.processingdb.vault-url     http://yarina-openbao:8200
 * spark.sql.catalog.processingdb.secret-key    secret/data/yarina/database:password
 */
public class YarinaSecureJDBCTableCatalog extends JDBCTableCatalog {

    private static final Logger LOG = LoggerFactory.getLogger(YarinaSecureJDBCTableCatalog.class);

    private static final String SPARK_PASSWORD_OPTION = SecretResolver.PASSWORD_KEY;

    /** Required public no-arg constructor for Spark Catalog Plugin SPI. */
    public YarinaSecureJDBCTableCatalog() {
        super();
    }

    @Override
    public void initialize(String name, CaseInsensitiveStringMap options) {
        // Create a mutable copy of the configuration options
        Map<String, String> modifiedOptions = new HashMap<>(options.asCaseSensitiveMap());

        if (SecretResolver.hasProvider(modifiedOptions)) {
            LOG.info("Initializing secure catalog '{}' using SecretProvider: {}",
                    name, modifiedOptions.get(SecretResolver.OPT_SECRET_PROVIDER));

            try {
                // Resolve the plain-text password via the configured secret provider and inject it
                // into the options expected by the base Spark JDBCTableCatalog
                modifiedOptions = SecretResolver.resolveAndSanitize(modifiedOptions);
            } catch (IllegalArgumentException | IllegalStateException e) {
                LOG.error("Validation failed during secure catalog initialization for '{}': {}", name, e.getMessage());
                throw e;
            } catch (Exception e) {
                LOG.error("Critical error while retrieving secrets for catalog '{}'", name);
                // Mask the original exception to prevent accidental payload/credential leaks in Spark system logs
                throw new RuntimeException("Catalog access blocked by Yarina Security Subsystem. Authorization failed.", e);
            }
        } else {
            LOG.warn("Catalog '{}' is initializing without a secure SecretProvider. Standard password fallback will be used if present.", name);
        }

        // Pass the modified options map (now containing the valid "password" key) to the base Spark JDBCTableCatalog
        super.initialize(name, new CaseInsensitiveStringMap(modifiedOptions));
    }
}
