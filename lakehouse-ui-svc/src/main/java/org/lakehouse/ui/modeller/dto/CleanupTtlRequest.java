package org.lakehouse.ui.modeller.dto;

/**
 * Admin TTL override body.
 */
public record CleanupTtlRequest(
        int hours) {
}