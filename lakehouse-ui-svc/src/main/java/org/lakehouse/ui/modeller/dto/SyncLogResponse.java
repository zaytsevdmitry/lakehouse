package org.lakehouse.ui.modeller.dto;

import java.time.Instant;

/**
 * One entry of the server-side review/sync log ring buffer.
 */
public record SyncLogResponse(
        Instant timestamp,
        String level,
        String user,
        String action,
        String message,
        String workspaceId) {
}