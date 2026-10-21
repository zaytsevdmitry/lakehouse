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

import org.jarina.domain.settings.DomainSettings;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Binding of the VCS settings declared in {@code application.yml}
 * ({@code jarina.config.vcs.sync}) and of the shared domain settings
 * ({@code jarina.domains.*}) onto {@link JarinaVCSProperties} / {@link DomainSettings}.
 */
class JarinaVCSPropertiesCredentialsTest {

    @Test
    void bindsSyncSettings() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("jarina.config.vcs.sync.enabled", "true");
        values.put("jarina.config.vcs.sync.interval-ms", "5000");
        values.put("jarina.config.vcs.sync.initial-delay-ms", "1000");

        JarinaVCSProperties properties = bindSync(values);

        assertThat(properties.getSync().isEnabled()).isTrue();
        assertThat(properties.getSync().getIntervalMs()).isEqualTo(5000);
        assertThat(properties.getSync().getInitialDelayMs()).isEqualTo(1000);
    }

    @Test
    void syncDefaultsWhenUnconfigured() {
        JarinaVCSProperties properties = bindSync(Map.of());

        assertThat(properties.getSync().isEnabled()).isFalse();
        assertThat(properties.getSync().getIntervalMs()).isEqualTo(30000);
        assertThat(properties.getSync().getInitialDelayMs()).isEqualTo(10000);
    }

    @Test
    void bindsPerDomainSettingsAndCredentials() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("jarina.domains.analytics.priority", "5");
        values.put("jarina.domains.analytics.git.repository-url", "https://git.example/analytics.git");
        values.put("jarina.domains.analytics.git.credentials.auth-type", "basic");
        values.put("jarina.domains.analytics.git.credentials.username", "alice");
        values.put("jarina.domains.analytics.git.credentials.password", "secret");

        DomainSettings settings = bindDomains(values);

        DomainSettings.DomainProperties analytics = settings.getDomains().get("analytics");
        assertThat(analytics.getPriority()).isEqualTo(5);
        assertThat(analytics.getGit().getCredentials().isBasic()).isTrue();
        assertThat(analytics.getGit().getCredentials().getUsername()).isEqualTo("alice");
        assertThat(analytics.getGit().getCredentials().getPassword()).isEqualTo("secret");
        assertThat(analytics.hasRepository()).isTrue();
    }

    @Test
    void sshAuthTypeIsInferredFromPrivateKey() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("jarina.domains.platform.git.repository-url", "ssh://git@example/platform.git");
        values.put("jarina.domains.platform.git.credentials.ssh-private-key-path", "/keys/id_ed25519");

        DomainSettings settings = bindDomains(values);

        DomainSettings.DomainProperties platform = settings.getDomains().get("platform");
        assertThat(platform.getGit().getCredentials().isSsh()).isTrue();
        assertThat(platform.getGit().getCredentials().hasCredentials()).isTrue();
    }

    @Test
    void orderedDomainsHonourPriorityAndNesting() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("jarina.domains.platform.priority", "0");
        values.put("jarina.domains.platform.domains.analytics.priority", "1");
        values.put("jarina.domains.raw.priority", "2");

        DomainSettings settings = bindDomains(values);

        assertThat(settings.orderedDomains())
                .extracting(DomainSettings.DomainRef::name)
                .containsExactly("platform", "analytics", "raw");
    }

    private static JarinaVCSProperties bindSync(Map<String, Object> values) {
        JarinaVCSProperties properties = new JarinaVCSProperties();
        new Binder(new MapConfigurationPropertySource(values))
                .bind("jarina.config.vcs", Bindable.ofInstance(properties));
        return properties;
    }

    private static DomainSettings bindDomains(Map<String, Object> values) {
        DomainSettings settings = new DomainSettings();
        new Binder(new MapConfigurationPropertySource(values))
                .bind("jarina", Bindable.ofInstance(settings));
        settings.canonicalize();
        return settings;
    }
}
