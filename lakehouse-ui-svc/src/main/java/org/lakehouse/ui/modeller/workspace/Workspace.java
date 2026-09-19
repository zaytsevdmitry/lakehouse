package org.lakehouse.ui.modeller.workspace;

import java.time.Instant;
import java.util.Objects;

/**
 * Server-side working copy of the metadata repository for a single (user, branch) pair.
 */
public record Workspace(
        String id,
        String branch,
        String owner,
        Instant createdAt,
        Instant lastAccessedAt) {

    public boolean isOwner(String username) {
        return Objects.equals(owner, username);
    }
}