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
package org.lakehouse.security.spi;

import java.util.Map;

/**
 * Minimal secret provider SPI for catalogs, JDBC connection factories and distributed code (JDBC, S3, etc.).
 * <p>
 * Implementations must have a public no-arg constructor (SPI requirement).
 * The caller invokes {@link #initialize(Map)} with the configuration map, then {@link #getSecret(String)}
 * and/or {@link #getPassword(String)} as needed, and finally {@link #close()}.
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
