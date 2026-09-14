package org.lakehouse.modeller.dto;

/**
 * Branch creation request (spec section 7).
 */
public record CreateBranchRequest(
        String branch,
        String baseBranch) {
}