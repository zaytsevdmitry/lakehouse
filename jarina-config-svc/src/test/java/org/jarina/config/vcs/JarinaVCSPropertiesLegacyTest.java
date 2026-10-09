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

package org.jarina.config.vcs;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The legacy single-repository configuration ({@code jarina.config.vcs.git.*}) must keep
 * working: it is exposed as the {@code default} root domain.
 */
class JarinaVCSPropertiesLegacyTest {

    @Test
    void legacyGitSettingsBecomeTheDefaultRootDomain() {
        JarinaVCSProperties properties = new JarinaVCSProperties();
        properties.getGit().setRepositoryUrl("https://git.example/conf.git");
        properties.getGit().setBranch("release");

        List<JarinaVCSProperties.DomainRef> roots = properties.rootDomains();

        assertThat(roots).hasSize(1);
        assertThat(roots.get(0).name()).isEqualTo("default");
        assertThat(roots.get(0).properties().getGit().getRepositoryUrl())
                .isEqualTo("https://git.example/conf.git");
        assertThat(roots.get(0).properties().getGit().getBranch()).isEqualTo("release");
        assertThat(properties.orderedDomains())
                .extracting(JarinaVCSProperties.DomainRef::name)
                .containsExactly("default");
    }

    @Test
    void noRepositoryConfiguredYieldsNoDomains() {
        JarinaVCSProperties properties = new JarinaVCSProperties();

        assertThat(properties.rootDomains()).isEmpty();
        assertThat(properties.orderedDomains()).isEmpty();
    }

    @Test
    void blankLegacyRepositoryUrlYieldsNoDomains() {
        JarinaVCSProperties properties = new JarinaVCSProperties();
        properties.getGit().setRepositoryUrl("   ");

        assertThat(properties.rootDomains()).isEmpty();
    }

    @Test
    void configuredDomainsWinOverTheLegacyGitSettings() {
        JarinaVCSProperties properties = new JarinaVCSProperties();
        properties.getGit().setRepositoryUrl("https://git.example/legacy.git");
        properties.setDomains(linked("platform", 0));

        assertThat(properties.rootDomains())
                .extracting(JarinaVCSProperties.DomainRef::name)
                .containsExactly("platform");
    }

    @Test
    void domainsWithoutPriorityAreSortedAfterConfiguredOnes() {
        JarinaVCSProperties properties = new JarinaVCSProperties();
        LinkedHashMap<String, JarinaVCSProperties.DomainProperties> domains = new LinkedHashMap<>();
        domains.put("zeta", domain(null));
        domains.put("alpha", domain(null));
        domains.put("first", domain(0));
        properties.setDomains(domains);

        assertThat(properties.rootDomains())
                .extracting(JarinaVCSProperties.DomainRef::name)
                .containsExactly("first", "alpha", "zeta");
    }

    private static LinkedHashMap<String, JarinaVCSProperties.DomainProperties> linked(String name, int priority) {
        LinkedHashMap<String, JarinaVCSProperties.DomainProperties> map = new LinkedHashMap<>();
        map.put(name, domain(priority));
        return map;
    }

    private static JarinaVCSProperties.DomainProperties domain(Integer priority) {
        JarinaVCSProperties.DomainProperties properties = new JarinaVCSProperties.DomainProperties();
        properties.setPriority(priority);
        JarinaVCSProperties.GitProperties git = new JarinaVCSProperties.GitProperties();
        git.setRepositoryUrl("https://git.example/" + priority + ".git");
        properties.setGit(git);
        return properties;
    }
}
