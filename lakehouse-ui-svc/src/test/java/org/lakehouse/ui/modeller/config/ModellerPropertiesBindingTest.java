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

package org.lakehouse.ui.modeller.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ModellerPropertiesBindingTest {

    @Test
    void baseSettingsBindFromApplicationYml() throws Exception {
        List<PropertySource<?>> loaded =
                new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yml"));

        StandardEnvironment environment = new StandardEnvironment();
        loaded.forEach(environment.getPropertySources()::addLast);

        ModellerProperties properties = Binder.get(environment)
                .bind("lakehouse.modeller", Bindable.of(ModellerProperties.class))
                .orElseThrow(() -> new IllegalStateException("failed to bind lakehouse.modeller"));

        // The custom lakehouse.modeller.security.oauth2 switchboard was removed in
        // favour of the standard spring.security.oauth2.* config; only the platform
        // strategies remain bound here.
        assertThat(properties.getVcsProvider()).isEqualTo("local-git");
        assertThat(properties.getAuthStrategy()).isEqualTo("jwt-rbac");
        assertThat(properties.getStorage().getType()).isEqualTo("local");
        assertThat(properties.getStorage().getCleanupTtlHours()).isEqualTo(4L);
        assertThat(properties.getSession().getInactivityMinutes()).isEqualTo(30);
        assertThat(properties.getLogging().getSyncLogCapacity()).isEqualTo(500);
    }

    @Test
    void domainRepositoriesBindFromCommandLineProperties() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("cli", java.util.Map.of(
                "lakehouse.modeller.domains.platform.repository-url", "git://git-server:9418/config-repo.git",
                "lakehouse.modeller.domains.platform.branch-main", "main",
                "lakehouse.modeller.domains.analytics.repository-url", "git://other.git",
                "lakehouse.modeller.domains.analytics.branch-main", "dev",
                "lakehouse.modeller.git.remote-url", "git://legacy.git",
                "lakehouse.modeller.git.branch-main", "legacy")));

        ModellerProperties properties = Binder.get(environment)
                .bind("lakehouse.modeller", Bindable.of(ModellerProperties.class))
                .orElseThrow(() -> new IllegalStateException("failed to bind lakehouse.modeller"));

        assertThat(properties.domainNames()).containsExactly("analytics", "platform");
        assertThat(properties.domainRemoteUrl("platform")).isEqualTo("git://git-server:9418/config-repo.git");
        assertThat(properties.domainBranchMain("platform")).isEqualTo("main");
        assertThat(properties.domainRemoteUrl("analytics")).isEqualTo("git://other.git");
        assertThat(properties.domainBranchMain("analytics")).isEqualTo("dev");
        // unknown domains fall back to the legacy single-repository settings
        assertThat(properties.domainRemoteUrl("unknown")).isEqualTo("git://legacy.git");
        assertThat(properties.domainBranchMain("unknown")).isEqualTo("legacy");
    }
}