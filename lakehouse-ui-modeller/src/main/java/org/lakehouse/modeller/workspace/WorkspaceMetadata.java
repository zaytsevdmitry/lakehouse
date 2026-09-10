package org.lakehouse.modeller.workspace;

import java.time.Instant;

/**
 * Persisted per-workspace bookkeeping stored in {@code _workspace.json} inside the
 * workspace directory. Not exposed through the file tree; excluded from review commits.
 */
public record WorkspaceMetadata(
        String workspace,
        String branch,
        String owner,
        Instant createdAt,
        Instant lastAccessedAt) {

    public WorkspaceMetadata withLastAccessedAt(Instant newLastAccessedAt) {
        return new WorkspaceMetadata(workspace, branch, owner, createdAt, newLastAccessedAt);
    }
}