package org.lakehouse.modeller.vcs;

import org.eclipse.jgit.transport.CredentialsProvider;
import org.lakehouse.modeller.config.ModellerProperties;

/**
 * Resolves the configured VCS provider strategy name into an implementation.
 * Only the selected provider is constructed, so providers that require extra
 * configuration (e.g. a GitLab remote URL) are never instantiated for a
 * differently-configured deployment.
 */
public final class VcsProviderFactory {

    private VcsProviderFactory() {
    }

    public static VcsProvider create(String name,
                                     ModellerProperties properties,
                                     CredentialsProvider credentials) {
        return switch (normalize(name)) {
            case "local-git", "gerrit" -> new LocalGitVcsProvider(properties, credentials);
            case "gitlab-api" -> new GitLabApiVcsProvider(properties, credentials);
            case "github-app" -> new GitHubAppVcsProvider(properties);
            case "none", "" -> new DisabledVcsProvider();
            default -> throw new IllegalArgumentException(
                    "Unsupported lakehouse.modeller.vcs-provider: " + name);
        };
    }

    public static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}