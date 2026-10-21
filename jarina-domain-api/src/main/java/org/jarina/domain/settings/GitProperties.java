/*
 * "Jarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/jarina
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

package org.jarina.domain.settings;

import org.springframework.util.StringUtils;

/**
 * Git settings of a single domain ({@code jarina.domains.<name>.git}): the repository
 * URL, the default branch and the transport credentials of the domain. The VCS layer is
 * designed to be extensible, but only the Git implementation is currently provided.
 */
public class GitProperties {

    private String repositoryUrl;
    private String branch = "main";
    private String localClonePath;
    private GitCredentialsSettings credentials = new GitCredentialsSettings();

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getLocalClonePath() {
        return localClonePath;
    }

    public void setLocalClonePath(String localClonePath) {
        this.localClonePath = localClonePath;
    }

    public GitCredentialsSettings getCredentials() {
        return credentials;
    }

    public void setCredentials(GitCredentialsSettings credentials) {
        this.credentials = credentials;
    }

    /** Resolved branch, defaulting to {@code main}. */
    public String effectiveBranch() {
        return StringUtils.hasText(branch) ? branch : "main";
    }

    public boolean hasRepository() {
        return StringUtils.hasText(repositoryUrl);
    }
}
