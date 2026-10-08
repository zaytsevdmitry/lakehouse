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

package org.lakehouse.taskexecutor.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@ConfigurationProperties(prefix = "lakehouse.task-executor")
public class DomainDataSourceServiceProperties {

    // domainKeyName -> (dataSourceKeyName -> DataSourceConfig)
    // Обычное приватное поле, никакого наследования класса от HashMap!
    private Map<String, Map<String, DataSourceConfig>> domains = new HashMap<>();

    // Магия Spring Boot: когда префикс указывает прямо на динамическую мапу,
    // мы используем этот сеттер. Spring передаст сюда всю карту доменов.
    public void setDomains(Map<String, Map<String, DataSourceConfig>> domains) {
        this.domains = domains;
    }

    // Для совместимости со старым кодом или Spring Binder
    public Map<String, Map<String, DataSourceConfig>> getDomains() {
        return domains;
    }

    /**
     * Безопасный метод получения динамической карты свойств.
     */
    public Optional<Map<String, String>> getServiceProperties(String domainKey, String dataSourceKey) {
        if (domains == null) return Optional.empty();

        Map<String, DataSourceConfig> dataSources = domains.get(domainKey);
        if (dataSources == null) return Optional.empty();

        DataSourceConfig dataSource = dataSources.get(dataSourceKey);
        if (dataSource == null) return Optional.empty();

        return Optional.ofNullable(dataSource.getServiceProperties());
    }

    /**
     * Обычный внутренний класс для мапинга блока "service-properties"
     */
    public static class DataSourceConfig {
        // Сюда запишутся ваши динамические secretProvider, vault-url и т.д.
        private Map<String, String> serviceProperties;

        public Map<String, String> getServiceProperties() {
            return serviceProperties;
        }

        public void setServiceProperties(Map<String, String> serviceProperties) {
            this.serviceProperties = serviceProperties;
        }
    }
}
