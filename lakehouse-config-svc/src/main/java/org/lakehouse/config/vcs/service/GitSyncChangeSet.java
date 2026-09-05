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

package org.lakehouse.config.vcs.service;

import java.util.List;

/**
 * The set of declarative configuration changes extracted from a single repository commit.
 *
 * @param toApply  created and updated constructs; applied in dependency order
 * @param toDelete deleted constructs; applied in reverse dependency order
 */
public record GitSyncChangeSet(List<GitSyncItem> toApply, List<GitSyncItem> toDelete) {

    public boolean isEmpty() {
        return toApply.isEmpty() && toDelete.isEmpty();
    }
}