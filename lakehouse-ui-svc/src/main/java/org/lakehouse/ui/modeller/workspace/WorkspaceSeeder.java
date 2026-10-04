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

package org.lakehouse.ui.modeller.workspace;

import java.util.Map;

/**
 * Seeds a freshly created workspace with the metadata files of one selected branch
 * (of one domain repository). Implemented by the active {@code VcsProvider} in production
 * wiring. The returned paths are the paths within the domain repository (scoped to the
 * domain subtree when the repository hosts several domains).
 */
@FunctionalInterface
public interface WorkspaceSeeder {

    /**
     * @param domain configuration domain whose repository is read
     * @param branch branch to read
     * @return branch content as path-to-YAML map (may be empty for empty branches)
     */
    Map<String, String> snapshot(String domain, String branch);
}