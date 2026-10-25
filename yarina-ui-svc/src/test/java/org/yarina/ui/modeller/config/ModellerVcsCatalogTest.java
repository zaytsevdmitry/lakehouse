/*
 * "Yarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/yarina
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

package org.yarina.ui.modeller.config;

import org.yarina.domain.settings.DomainSettings;
import org.yarina.domain.settings.GitCredentialsSettings;
import org.yarina.domain.settings.GitProperties;
import org.yarina.domain.vcs.VcsClient;
import org.yarina.domain.vcs.client.GitVcsClient;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModellerVcsCatalogTest {

    private static DomainSettings.DomainProperties domain(String repositoryUrl) {
        DomainSettings.DomainProperties domain = new DomainSettings.DomainProperties();
        GitProperties git = new GitProperties();
        git.setRepositoryUrl(repositoryUrl);
        GitCredentialsSettings credentials = new GitCredentialsSettings();
        credentials.setAuthType("token");
        credentials.setToken("dummy");
        git.setCredentials(credentials);
        domain.setGit(git);
        return domain;
    }

    private static DomainSettings settings(DomainSettings.DomainProperties... domains) {
        DomainSettings settings = new DomainSettings();
        Map<String, DomainSettings.DomainProperties> map = new LinkedHashMap<>();
        String[] names = {"platform", "analytics", "pocessing"};
        for (int i = 0; i < domains.length; i++)
            map.put(names[i], domains[i]);
        settings.setDomains(map);
        settings.canonicalize();
        return settings;
    }

    @Test
    void servesEveryDomainWithAGitClient() {
        ModellerVcsCatalog catalog = new ModellerVcsCatalog(settings(
                domain("https://git.example/platform.git"),
                domain("ssh://git@gerrit.example/analytics.git")));

        VcsClient platform = catalog.clientFor("platform");
        VcsClient analytics = catalog.clientFor("analytics");

        assertThat(platform).isInstanceOf(GitVcsClient.class);
        assertThat(analytics).isInstanceOf(GitVcsClient.class);
        assertThat(catalog.clientFor("platform")).isSameAs(platform);
    }

    @Test
    void missingRepositoryOfConfiguredDomainFailsAtFirstUse() {
        DomainSettings settings = settings(domain(null));

        ModellerVcsCatalog catalog = new ModellerVcsCatalog(settings);

        assertThatThrownBy(() -> catalog.clientFor("platform"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("repository-url");
    }

    @Test
    void rejectsMissingDomain() {
        ModellerVcsCatalog catalog = new ModellerVcsCatalog(new DomainSettings());

        assertThatThrownBy(() -> catalog.clientFor(" "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Domain is required");
    }
}
