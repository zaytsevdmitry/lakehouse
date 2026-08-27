package org.lakehouse.security.catalog;

import org.apache.spark.sql.execution.datasources.v2.jdbc.JDBCTableCatalog;
import org.apache.spark.sql.util.CaseInsensitiveStringMap;
import org.lakehouse.security.spi.SecretProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom secure JDBC catalog implementation for the Lakehouse platform.
 * Intercepts catalog initialization on both Driver and Executors to dynamically
 * resolve credentials via a configured {@link SecretProvider}.
 *
 * Properties
 * # Configure the custom secure catalog execution manager
 * spark.sql.catalog.processingdb               org.lakehouse.security.catalog.LakehouseSecureJDBCTableCatalog
 * spark.sql.catalog.processingdb.url           jdbc:postgresql://localhost:5432/lakehouse_db
 * spark.sql.catalog.processingdb.user          lakehouse_user
 *
 * # Wire the Bao provider and point to the path/key coordinates using the semicolon divider
 * spark.sql.catalog.processingdb.secretProvider org.lakehouse.security.jdbc.BaoJdbcSecretProvider
 * spark.sql.catalog.processingdb.vault-url     http://lakehouse-openbao:8200
 * spark.sql.catalog.processingdb.secret-key    secret/data/lakehouse/database:password
 */
public class LakehouseSecureJDBCTableCatalog extends JDBCTableCatalog {

    private static final Logger LOG = LoggerFactory.getLogger(LakehouseSecureJDBCTableCatalog.class);

    private static final String OPT_SECRET_PROVIDER = "secretProvider";
    private static final String OPT_SECRET_KEY = "secret-key";
    private static final String SPARK_PASSWORD_OPTION = "password";

    /** Required public no-arg constructor for Spark Catalog Plugin SPI. */
    public LakehouseSecureJDBCTableCatalog() {
        super();
    }

    @Override
    public void initialize(String name, CaseInsensitiveStringMap options) {
        // Create a mutable copy of the configuration options
        Map<String, String> modifiedOptions = new HashMap<>(options.asCaseSensitiveMap());

        String providerClassName = modifiedOptions.get(OPT_SECRET_PROVIDER);
        String secretKey = modifiedOptions.get(OPT_SECRET_KEY);

        if (providerClassName != null && !providerClassName.strip().isEmpty()) {
            LOG.info("Initializing secure catalog '{}' using SecretProvider: {}", name, providerClassName);
            
            if (secretKey == null || secretKey.strip().isEmpty()) {
                throw new IllegalArgumentException(
                        "Catalog '" + name + "' configuration missing required option: " + OPT_SECRET_KEY);
            }

            try {
                // 1. Load the secret provider class via the current context class loader
                Class<?> providerClass = Thread.currentThread().getContextClassLoader().loadClass(providerClassName.strip());
                
                if (!SecretProvider.class.isAssignableFrom(providerClass)) {
                    throw new IllegalArgumentException(
                            "Class " + providerClassName + " must implement org.lakehouse.security.spi.SecretProvider");
                }

                // 2. Instantiate the provider using the required public no-arg constructor
                SecretProvider provider = (SecretProvider) providerClass.getDeclaredConstructor().newInstance();

                // 3. Initialize the provider with the catalog configuration map
                // This allows the provider to extract infrastructure coordinates (e.g., vault-url, auth-role)
                provider.initialize(modifiedOptions);

                // 4. Resolve the plain-text password using the provided secret key coordinates
                // (e.g., 'secretPath:secretKey' for Vault/Bao)
                String decryptedPassword = provider.getPassword(secretKey.strip());

                if (decryptedPassword == null || decryptedPassword.isEmpty()) {
                    throw new IllegalStateException(
                            "SecretProvider returned an empty password for key '" + secretKey + "' in catalog: " + name);
                }

                // 5. Inject the plain-text password into the options expected by the base Spark JDBCTableCatalog
                modifiedOptions.put(SPARK_PASSWORD_OPTION, decryptedPassword);

                // For security reasons, purge provider settings before passing configuration down to Spark core
                modifiedOptions.remove(OPT_SECRET_PROVIDER);
                modifiedOptions.remove(OPT_SECRET_KEY);
                
                // Ensure the provider resource is gracefully closed if applicable
                provider.close();

            } catch (ClassNotFoundException e) {
                LOG.error("SecretProvider class '{}' not found in Spark classpath. Ensure the JAR is placed in /spark/jars", providerClassName);
                throw new RuntimeException("Security configuration error for catalog: " + name, e);
            } catch (IllegalArgumentException | IllegalStateException e) {
                LOG.error("Validation failed during secure catalog initialization for '{}': {}", name, e.getMessage());
                throw e;
            } catch (Exception e) {
                LOG.error("Critical error while retrieving secrets for catalog '{}'", name);
                // Mask the original exception to prevent accidental payload/credential leaks in Spark system logs
                throw new RuntimeException("Catalog access blocked by Lakehouse Security Subsystem. Authorization failed.", e);
            }
        } else {
            LOG.warn("Catalog '{}' is initializing without a secure SecretProvider. Standard password fallback will be used if present.", name);
        }

        // Pass the modified options map (now containing the valid "password" key) to the base Spark JDBCTableCatalog
        super.initialize(name, new CaseInsensitiveStringMap(modifiedOptions));
    }
}
