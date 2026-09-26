package org.lakehouse.ui.modeller.workspace;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Server-side working copy of the metadata repositories for a set of (domain, branch)
 * selections of one user. Every selection is checked out into a parallel folder named
 * {@code <domain> (<branch>)} inside the workspace directory.
 */
public record Workspace(
        String id,
        List<BranchSelection> selections,
        String owner,
        Instant createdAt,
        Instant lastAccessedAt) {

    public boolean isOwner(String username) {
        return Objects.equals(owner, username);
    }

    /** The branch selected for the given domain, or {@code null} when the domain is absent. */
    public String branchOf(String domain) {
        return selections.stream()
                .filter(s -> Objects.equals(s.domain(), domain))
                .map(BranchSelection::branch)
                .findFirst()
                .orElse(null);
    }

    /** Compact human-readable representation used in the UI and logs: {@code platform (main), analytics (dev)}. */
    public String display() {
        return selections.stream().map(BranchSelection::folder).reduce((a, b) -> a + ", " + b).orElse("");
    }
}