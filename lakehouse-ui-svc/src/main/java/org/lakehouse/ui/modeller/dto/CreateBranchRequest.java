package org.lakehouse.ui.modeller.dto;

/**
 * Body for creating a branch in the repository of one domain.
 */
public record CreateBranchRequest(
        String domain,
        String branch,
        String baseBranch) {
}