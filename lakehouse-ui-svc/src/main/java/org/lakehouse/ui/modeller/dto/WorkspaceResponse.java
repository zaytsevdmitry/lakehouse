package org.lakehouse.ui.modeller.dto;

import org.lakehouse.ui.modeller.workspace.BranchSelection;

import java.time.Instant;
import java.util.List;

/**
 * External view of a server-side workspace.
 */
public record WorkspaceResponse(
        String id,
        List<BranchSelection> branches,
        String owner,
        Instant createdAt,
        Instant lastAccessedAt,
        boolean own) {
}