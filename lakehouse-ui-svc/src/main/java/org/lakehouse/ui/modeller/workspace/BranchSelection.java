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

/**
 * A single branch picked for one configuration domain in a workspace.
 * A workspace is a set of such selections — one branch per domain repository.
 */
public record BranchSelection(String domain, String branch) {

    public String key() {
        return domain + "=" + branch;
    }

    /** Folder name of the branch checkout inside the workspace directory. */
    public String folder() {
        return domain + " (" + branch + ")";
    }

    /** True for a selection whose files live under the given workspace path. */
    public boolean covers(String path) {
        return folder().equals(path) || path.startsWith(folder() + "/");
    }
}