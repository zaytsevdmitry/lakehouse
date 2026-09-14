package org.lakehouse.modeller.dto;

/**
 * Admin TTL override body.
 */
public record CleanupTtlRequest(
        int hours) {
}