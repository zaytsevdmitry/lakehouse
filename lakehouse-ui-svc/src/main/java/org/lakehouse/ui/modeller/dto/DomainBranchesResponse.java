package org.lakehouse.ui.modeller.dto;

import java.util.List;

/**
 * Branches of a single domain repository, the root node of the branch panel tree.
 */
public record DomainBranchesResponse(
        String domain,
        List<String> branches,
        String branchMain) {
}