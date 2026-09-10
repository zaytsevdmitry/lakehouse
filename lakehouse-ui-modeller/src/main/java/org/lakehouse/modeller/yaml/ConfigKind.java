package org.lakehouse.modeller.yaml;

import java.util.Locale;

/**
 * The supported YAML metadata kinds and their canonical {@code kind:} values,
 * mapped to the matching {@code lakehouse-common} DTO classes by the form generator.
 */
public enum ConfigKind {

    NAME_SPACE("NameSpace", "org.lakehouse.client.api.dto.configs.NameSpaceDTO"),
    DRIVER("Driver", "org.lakehouse.client.api.dto.configs.schedule.DriverDTO"),
    DATA_SET("DataSet", "org.lakehouse.client.api.dto.configs.dataset.DataSetDTO"),
    DATA_SOURCE("DataSource", "org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO"),
    QUALITY_METRICS_CONF("QualityMetricsConf", "org.lakehouse.client.api.dto.configs.dq.QualityMetricsConfDTO"),
    SCENARIO_ACT_TEMPLATE("ScenarioActTemplate", "org.lakehouse.client.api.dto.configs.schedule.ScenarioActTemplateDTO"),
    SCHEDULE("Schedule", "org.lakehouse.client.api.dto.configs.schedule.ScheduleDTO"),
    TASK_EXECUTION_SERVICE_GROUP("TaskExecutionServiceGroup",
            "org.lakehouse.client.api.dto.configs.schedule.TaskExecutionServiceGroupDTO"),
    TASK("Task", "org.lakehouse.client.api.dto.configs.schedule.TaskDTO"),
    METRIC_DQ("MetricDQ", "org.lakehouse.client.api.dto.dq.MetricDQStatusDTO"),
    SCRIPT("Script", "org.lakehouse.client.api.dto.configs.script.ScriptDTO");

    private final String yamlValue;
    private final String dtoClassName;

    ConfigKind(String yamlValue, String dtoClassName) {
        this.yamlValue = yamlValue;
        this.dtoClassName = dtoClassName;
    }

    public String yamlValue() {
        return yamlValue;
    }

    public String dtoClassName() {
        return dtoClassName;
    }

    /**
     * Directory of the repository where files of this kind are stored.
     */
    public String directory() {
        return switch (this) {
            case NAME_SPACE -> "nameSpaces";
            case DRIVER -> "drivers";
            case DATA_SET -> "datasets";
            case DATA_SOURCE -> "datasources";
            case QUALITY_METRICS_CONF -> "quality";
            case SCENARIO_ACT_TEMPLATE -> "scenarios";
            case SCHEDULE -> "schedules";
            case TASK_EXECUTION_SERVICE_GROUP -> "taskexecutionservicegroups";
            case TASK -> "tasks";
            case METRIC_DQ -> "config/dq";
            case SCRIPT -> "scripts";
        };
    }

    /**
     * Name of the identifying field in the repository documents of this kind.
     * Most kinds use {@code keyName}; {@code Task} and {@code TaskExecutionServiceGroup}
     * are identified by {@code name} in the repository.
     */
    public String identifierField() {
        return switch (this) {
            case TASK, TASK_EXECUTION_SERVICE_GROUP -> "name";
            case SCRIPT -> "key";
            default -> "keyName";
        };
    }

    public static ConfigKind fromYamlValue(String value) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("Configuration kind must not be blank");
        String normalized = normalize(value);
        for (ConfigKind kind : values()) {
            if (normalize(kind.yamlValue).equals(normalized))
                return kind;
        }
        throw new IllegalArgumentException("Unknown configuration kind: " + value);
    }

    public static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}