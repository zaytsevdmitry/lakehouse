package org.lakehouse.ui.modeller.dto;

import java.time.Instant;

/**
 * External view of a server-side workspace.
 */
public record WorkspaceResponse(
        String id,
        String branch,
        String owner,
        Instant createdAt,
        Instant lastAccessedAt,
        boolean own) {
}