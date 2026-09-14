package org.lakehouse.modeller.dto;

/**
 * Result of a restore-from-VCS operation: how many files were overwritten.
 */
public record RestoreResponse(
        int restored) {
}