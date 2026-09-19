package org.lakehouse.ui.modeller.vcs;

/**
 * Commit author resolved from the authenticated Keycloak user (spec 3.2). The committer
 * is always the fixed technical account.
 */
public record VcsReviewAuthor(String name, String email, String username) {
}