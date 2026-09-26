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
