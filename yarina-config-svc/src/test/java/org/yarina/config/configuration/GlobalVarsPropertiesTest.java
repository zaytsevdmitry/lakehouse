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

package org.yarina.config.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GlobalVarsPropertiesTest {

    @Test
    void bindsGvarsFromConfigurationProperties() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("yarina.config.gvars.defaultTimezone", "UTC")
                .withProperty("yarina.config.gvars.defaultLocale", "ru_RU");

        GlobalVarsProperties properties = new GlobalVarsProperties(environment);

        assertThat(properties.getGvars())
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of("defaultTimezone", "UTC", "defaultLocale", "ru_RU"));
        assertThat(properties.get("defaultTimezone")).isEqualTo("UTC");
        assertThat(properties.get("unknownKey")).isNull();
    }

    @Test
    void bindsGvarsFromEnvironmentVariables() {
        MockEnvironment environment = new MockEnvironment();
        environment.getPropertySources().addFirst(
                new SystemEnvironmentPropertySource("systemEnvironment",
                        Map.of("YARINA_CONFIG_GVARS_TIMEZONE", "Europe/Moscow")));

        GlobalVarsProperties properties = new GlobalVarsProperties(environment);

        assertThat(properties.getGvars()).containsExactlyInAnyOrderEntriesOf(
                Map.of("timezone", "Europe/Moscow"));
    }

    @Test
    void bindsEmptyMapWhenNamespaceIsAbsent() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("yarina.security.enabled", "true");

        GlobalVarsProperties properties = new GlobalVarsProperties(environment);

        assertThat(properties.getGvars()).isEmpty();
        assertThat(properties.get("anyKey")).isNull();
    }

    @Test
    void exposesReadOnlySnapshot() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("yarina.config.gvars.key", "value");

        GlobalVarsProperties properties = new GlobalVarsProperties(environment);

        assertThatThrownBy(() -> properties.getGvars().put("other", "other"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
