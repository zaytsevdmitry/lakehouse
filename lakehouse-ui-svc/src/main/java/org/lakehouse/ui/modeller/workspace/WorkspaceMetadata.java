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