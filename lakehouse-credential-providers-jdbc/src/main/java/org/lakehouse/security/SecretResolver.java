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
package org.lakehouse.security;

import org.lakehouse.security.spi.SecretProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Shared helper that resolves a plain-text secret through the {@link SecretProvider} SPI
 * from a flat options map (Spark catalog options, ServiceDTO.properties, etc.).
 *
 * <p>Option keys consumed by the security subsystem (all removed before the map is
 * passed downstream to a JDBC driver or a Spark catalog):
 * <ul>
 *   <li>{@code secretProvider} — FQCN of the {@link SecretProvider} implementation</li>
 *   <li>{@code secret-key} — combined {@code path:key} coordinate, e.g. {@code secret/data/db:password}</li>
 *   <li>{@code vault-url}, {@code vault-role}, {@code vault-k8s-auth-path} — Vault coordinates</li>
 *   <li>{@code secret-id}, {@code secret-version} — Yandex Cloud Lockbox coordinates</li>
 * </ul>
 */
public final class SecretResolver {

    private static final Logger LOG = LoggerFactory.getLogger(SecretResolver.class);

    public static final String OPT_SECRET_PROVIDER = "secretProvider";
    public static final String OPT_SECRET_KEY = "secret-key";
    public static final String PASSWORD_KEY = "password";

    /** Keys consumed by the security subsystem; never passed downstream to drivers/catalogs. */
    private static final Set<String> PROVIDER_KEYS = Set.of(
            OPT_SECRET_PROVIDER, OPT_SECRET_KEY,
            "vault-url", "vault-role", "vault-k8s-auth-path",
            "secret-id", "secret-version");

    private SecretResolver() {
    }

    /**
     * @return {@code true} when the options map configures a {@link SecretProvider}.
     */
    public static boolean hasProvider(Map<String, String> options) {
        String providerClassName = options.get(OPT_SECRET_PROVIDER);
        return providerClassName != null && !providerClassName.strip().isEmpty();
    }

    /**
     * Resolve the plain-text password via the configured {@link SecretProvider}.
     *
     * <p>The provider class is loaded through the current context class loader (overridable),
     * instantiated with its required public no-arg constructor, initialised with the full
     * options map and closed before returning.
     *
     * @param options options map containing at least {@code secretProvider} and {@code secret-key}
     * @return the resolved secret value, never {@code null} nor blank
     * @throws IllegalArgumentException for configuration errors (missing key, wrong SPI)
     * @throws SecurityException        when the secret cannot be resolved; the message is masked
     */
    public static String resolvePassword(Map<String, String> options) {
        if (!hasProvider(options)) {
            return null;
        }
        String providerClassName = options.get(OPT_SECRET_PROVIDER).strip();
        String secretKey = options.get(OPT_SECRET_KEY);
        if (secretKey == null || secretKey.strip().isEmpty()) {
            throw new IllegalArgumentException(
                    "Secret provider configured but missing required option: " + OPT_SECRET_KEY);
        }

        try {
            // 1. Load the secret provider via the current context class loader
            Class<?> providerClass = Thread.currentThread().getContextClassLoader().loadClass(providerClassName);
            if (!SecretProvider.class.isAssignableFrom(providerClass)) {
                throw new IllegalArgumentException(
                        "Class " + providerClassName + " must implement " + SecretProvider.class.getName());
            }

            // 2. Instantiate using the required public no-arg constructor and initialise with the options map
            SecretProvider provider = (SecretProvider) providerClass.getDeclaredConstructor().newInstance();
            try {
                provider.initialize(options);

                // 3. Resolve the secret using the combined 'path:key' coordinates
                String value = provider.getPassword(secretKey.strip());
                if (value == null || value.isEmpty()) {
                    throw new IllegalStateException(
                            "SecretProvider returned an empty secret for key '" + secretKey + "'");
                }
                return value;
            } finally {
                provider.close();
            }
        } catch (ClassNotFoundException e) {
            LOG.error("SecretProvider class '{}' not found on the classpath", providerClassName);
            throw new SecurityException("SecretProvider class not found: " + providerClassName, e);
        } catch (SecurityException | IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Critical error while resolving a secret", e);
            throw new SecurityException("Secret resolution blocked by Lakehouse Security Subsystem.", e);
        }
    }

    /**
     * Return a copy of {@code options} without the security subsystem keys. When no provider is
     * configured the original map is returned unchanged.
     */
    public static Map<String, String> sanitize(Map<String, String> options) {
        if (!hasProvider(options)) {
            return options;
        }
        Map<String, String> result = new HashMap<>(options);
        result.keySet().removeAll(PROVIDER_KEYS);
        return result;
    }

    /**
     * Resolve the secret via the configured provider and return {@code options} sanitized of the
     * security subsystem keys with the resolved value injected under the {@code password} key.
     * When no provider is configured the original map is returned unchanged.
     */
    public static Map<String, String> resolveAndSanitize(Map<String, String> options) {
        if (!hasProvider(options)) {
            return options;
        }
        Map<String, String> result = sanitize(options);
        result.put(PASSWORD_KEY, resolvePassword(options));
        return result;
    }
}