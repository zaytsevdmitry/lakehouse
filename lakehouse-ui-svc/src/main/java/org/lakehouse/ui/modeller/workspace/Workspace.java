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
import java.util.Objects;

/**
 * Server-side working copy of the metadata repositories for a set of (domain, branch)
 * selections of one user. Every selection is checked out into a parallel folder named
 * {@code <domain> (<branch>)} inside the workspace directory.
 */
public record Workspace(
        String id,
        List<BranchSelection> selections,
        String owner,
        Instant createdAt,
        Instant lastAccessedAt) {

    public boolean isOwner(String username) {
        return Objects.equals(owner, username);
    }

    /** The branch selected for the given domain, or {@code null} when the domain is absent. */
    public String branchOf(String domain) {
        return selections.stream()
                .filter(s -> Objects.equals(s.domain(), domain))
                .map(BranchSelection::branch)
                .findFirst()
                .orElse(null);
    }

    /** Compact human-readable representation used in the UI and logs: {@code platform (main), analytics (dev)}. */
    public String display() {
        return selections.stream().map(BranchSelection::folder).reduce((a, b) -> a + ", " + b).orElse("");
    }
}