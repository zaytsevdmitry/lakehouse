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

package org.lakehouse.config.exception;

/**
 * Raised when an incoming configuration object belongs to a domain different from the
 * domain of the construct already stored in the database. Such an overwrite is forbidden:
 * every construct belongs to exactly one domain as soon as it is present in the database.
 */
public class DomainConflictException extends RuntimeException {

    public DomainConflictException(String keyName, String storedDomain, String incomingDomain) {
        super(String.format(
                "Configuration object %s is managed by domain '%s' and cannot be overwritten by domain '%s'",
                keyName, storedDomain, incomingDomain));
    }
}