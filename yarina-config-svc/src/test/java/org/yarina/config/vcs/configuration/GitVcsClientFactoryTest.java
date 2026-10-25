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

package org.yarina.config.vcs.configuration;

import org.yarina.domain.settings.DomainSettings;
import org.yarina.domain.settings.GitProperties;
import org.yarina.domain.vcs.DomainVcsFactory;
import org.yarina.domain.vcs.DomainVcsSpec;
import org.yarina.domain.vcs.VcsClient;
import org.yarina.domain.vcs.client.GitVcsClient;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GitVcsClientFactoryTest {

    private static DomainSettings settings(String repositoryUrl) {
        DomainSettings settings = new DomainSettings();
        GitProperties git = new GitProperties();
        git.setRepositoryUrl(repositoryUrl);
        git.setLocalClonePath("/tmp/yarina-vcs-test-clone");
        DomainSettings.DomainProperties domain = new DomainSettings.DomainProperties();
        domain.setGit(git);
        Map<String, DomainSettings.DomainProperties> domains = new LinkedHashMap<>();
        domains.put("platform", domain);
        settings.setDomains(domains);
        settings.canonicalize();
        return settings;
    }

    @Test
    void buildsSpecOfTheRequestedDomain() {
        DomainVcsSpec spec = settings("https://git.example/platform.git").buildSpec("platform", true);

        assertThat(spec.name()).isEqualTo("platform");
        assertThat(spec.repositoryUrl()).isEqualTo("https://git.example/platform.git");
        assertThat(spec.branch()).isEqualTo("main");
        assertThat(spec.localClonePath()).isEqualTo("/tmp/yarina-vcs-test-clone");
    }

    @Test
    void repositoryCredentialsAreIndependentOfTheUrlScheme() {
        assertThat(repository("ssh://git@gerrit.example.com:29418/platform.git"))
                .isInstanceOf(GitVcsClient.class);
        assertThat(repository("https://github.com/acme/conf.git"))
                .isInstanceOf(GitVcsClient.class);
    }

    @Test
    void missingRepositoryFailsWithPropertyHint() {
        assertThatThrownBy(() -> new DomainSettings().buildSpec("platform", true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("repository-url");
    }

    private VcsClient repository(String repositoryUrl) {
        return DomainVcsFactory.repositoryFor(settings(repositoryUrl).buildSpec("platform", true));
    }
}
