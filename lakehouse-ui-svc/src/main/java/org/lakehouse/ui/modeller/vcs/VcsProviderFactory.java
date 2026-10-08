/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.apache.org/licenses/LICENSE-2.0.txt
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lakehouse.ui.modeller.vcs;

import org.eclipse.jgit.transport.CredentialsProvider;
import org.lakehouse.ui.modeller.config.ModellerProperties;

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