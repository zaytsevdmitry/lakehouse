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

package org.lakehouse.config.vcs.configuration;

import org.lakehouse.config.vcs.LakehouseVCSProperties;
import org.lakehouse.config.vcs.VcsClient;
import org.lakehouse.config.vcs.client.GitVcsClient;
import org.springframework.stereotype.Component;

/**
 * Builds a {@link VcsClient} for a single configuration domain from the Git settings
 * declared for that domain in {@link LakehouseVCSProperties}.
 */
@Component
public class GitVcsClientFactory {

    /**
     * @return a Git client bound to the repository declared for the given domain
     */
    public VcsClient create(String domainName, LakehouseVCSProperties.DomainProperties domain) {
        LakehouseVCSProperties.GitProperties git = domain.getGit();
        return new GitVcsClient(
                git.getRepositoryUrl(),
                git.getBranch(),
                git.getLocalClonePath(),
                git.getPrivateKeyPath());
    }
}