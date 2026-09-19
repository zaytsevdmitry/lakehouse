package org.lakehouse.ui.modeller.dto;

/**
 * Review (commit + push + MR/PR) workload body.
 */
public record ReviewRequest(
        String comment,
        String commitMessage) {
}