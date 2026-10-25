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

package org.yarina.taskexecutor.configuration;

import org.junit.jupiter.api.Test;
import org.yarina.taskexecutor.service.ExecuteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "yarina.task-executor.domains.[platform].[yarinastorage].service-properties.secretProvider=org.yarina.security.jdbc.BaoJdbcSecretProvider",
        "yarina.task-executor.domains.pocessing.processingdb.service-properties.user=postgresUser",
})
public class DomainDataSourceServicePropertiesTest {

    @Autowired
    DomainDataSourceServiceProperties domainDataSourceServiceProperties;
    @Autowired
    ExecuteService executeService;
    @Autowired
    ConfigurableEnvironment environment;

    @Test
    void bindsDomainsDataSourcesAndServiceProperties() {
        assertThat(environment.getProperty(
                "yarina.task-executor.domains.platform.yarinastorage.service-properties.secretProvider"))
                .isEqualTo("org.yarina.security.jdbc.BaoJdbcSecretProvider");
        Map<String, String> platformProperties = domainDataSourceServiceProperties
                .getServiceProperties("platform", "yarinastorage").orElseThrow();
        assertThat(platformProperties)
                .containsEntry("secretProvider", "org.yarina.security.jdbc.BaoJdbcSecretProvider")
                .containsEntry("secret-key", "kv/data/yarina/database:password")
                .containsEntry("vault-url", "http://openbao:8200")
                .containsEntry("fetchSize", "10000");
        Map<String, String> pocessingProperties = domainDataSourceServiceProperties
                .getServiceProperties("pocessing", "processingdb").orElseThrow();
        assertThat(pocessingProperties).containsEntry("user", "postgresUser");
        // the same global bean is exposed by the task executor service entry point
        assertThat(executeService.getDomainDataSourceServiceProperties())
                .isSameAs(domainDataSourceServiceProperties);
    }
}