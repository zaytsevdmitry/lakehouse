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

import java.time.Instant;
import java.util.List;

/**
 * Persisted per-workspace bookkeeping stored in {@code _workspace.json} inside the
 * workspace directory. Not exposed through the file tree; excluded from review commits.
 */
public record WorkspaceMetadata(
        String workspace,
        List<BranchSelection> selections,
        String owner,
        Instant createdAt,
        Instant lastAccessedAt) {

    public WorkspaceMetadata withLastAccessedAt(Instant newLastAccessedAt) {
        return new WorkspaceMetadata(workspace, selections, owner, createdAt, newLastAccessedAt);
    }
}