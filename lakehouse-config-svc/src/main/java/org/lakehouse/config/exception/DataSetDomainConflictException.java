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
 * Raised when a schedule references a data set that belongs to a configuration domain
 * different from the domain of the schedule itself. A domain's declarative repository is
 * only allowed to reference constructs of the same domain: wiring to another domain (or
 * to a manually created data set owned by no domain) would make a commit's applicability
 * depend on a repository it does not control.
 */
public class DataSetDomainConflictException extends RuntimeException {

    public DataSetDomainConflictException(String scheduleKeyName, String dataSetKeyName,
                                          String scheduleDomain, String dataSetDomain) {
        super(String.format(
                "Schedule %s of domain '%s' references data set %s of domain '%s'; "
                        + "a schedule may only reference data sets of its own domain",
                scheduleKeyName, scheduleDomain, dataSetKeyName, dataSetDomain));
    }
}