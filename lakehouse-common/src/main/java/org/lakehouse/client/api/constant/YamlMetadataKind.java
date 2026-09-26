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

package org.lakehouse.client.api.constant;

import org.lakehouse.client.api.dto.configs.datalineage.DataLineageDiagramDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.dq.QualityMetricsConfDTO;
import org.lakehouse.client.api.dto.configs.erdiagram.ERDiagramDTO;
import org.lakehouse.client.api.dto.configs.schedule.DriverDTO;
import org.lakehouse.client.api.dto.configs.schedule.ScenarioActTemplateDTO;
import org.lakehouse.client.api.dto.configs.schedule.ScheduleDTO;
import org.lakehouse.client.api.dto.configs.schedule.TaskDTO;
import org.lakehouse.client.api.dto.configs.schedule.TaskExecutionServiceGroupDTO;
import org.lakehouse.client.api.dto.configs.script.ScriptDTO;
import org.lakehouse.client.api.dto.dq.MetricDQStatusDTO;

import java.util.Locale;

/**
 * The declarative metadata constructs recognized in a metadata repository.
 * <p>
 * The value of the {@code kind} field of a YAML file selects the target DTO class,
 * the field that identifies a document and the position of the construct in the
 * dependency aware apply/delete order. {@link #isConfig()} tells whether the construct
 * is a configuration object that the configuration service has to apply; constructs
 * like {@link #ER_DIAGRAM} are stored in the repository but are not applied.
 */
public enum YamlMetadataKind {

    DRIVER("Driver", DriverDTO.class, 2, "keyName", true),
    DATA_SOURCE("DataSource", DataSourceDTO.class, 3, "keyName", true),
    SCRIPT("Script", ScriptDTO.class, 4, "key", true),
    TASK_EXECUTION_SERVICE_GROUP("TaskExecutionServiceGroup", TaskExecutionServiceGroupDTO.class, 5, "name", true),
    TASK("Task", TaskDTO.class, 6, "name", true),
    DATA_SET("DataSet", DataSetDTO.class, 7, "keyName", true),
    SCENARIO_ACT_TEMPLATE("ScenarioActTemplate", ScenarioActTemplateDTO.class, 8, "keyName", true),
    QUALITY_METRICS_CONF("QualityMetricsConf", QualityMetricsConfDTO.class, 9, "keyName", true),
    SCHEDULE("Schedule", ScheduleDTO.class, 10, "keyName", true),
    ER_DIAGRAM("ERDiagram", ERDiagramDTO.class, 11, "keyName", false),
    METRIC_DQ("MetricDQ", MetricDQStatusDTO.class, 12, "keyName", true),
    DATA_LINEAGE_DIAGRAM("DataLineageDiagram", DataLineageDiagramDTO.class, 13, "keyName", false);

    private final String yamlValue;
    private final Class<?> dtoClass;
    private final int order;
    private final String identifierField;
    private final boolean isConfig;

    YamlMetadataKind(String yamlValue, Class<?> dtoClass, int order, String identifierField, boolean isConfig) {
        this.yamlValue = yamlValue;
        this.dtoClass = dtoClass;
        this.order = order;
        this.identifierField = identifierField;
        this.isConfig = isConfig;
    }

    public String yamlValue() {
        return yamlValue;
    }

    public Class<?> dtoClass() {
        return dtoClass;
    }

    /** Fully qualified name of {@link #dtoClass()}. */
    public String dtoClassName() {
        return dtoClass.getName();
    }

    public int order() {
        return order;
    }

    /** Name of the identifying field in the repository documents of this kind. */
    public String identifierField() {
        return identifierField;
    }

    /** Whether the construct is a configuration object that has to be applied by the config service. */
    public boolean isConfig() {
        return isConfig;
    }

    /** Directory of the repository where files of this kind are stored. */
    public String directory() {
        return switch (this) {
            case DRIVER -> "drivers";
            case ER_DIAGRAM -> "erdiagrams";
            case DATA_SET -> "datasets";
            case DATA_SOURCE -> "datasources";
            case QUALITY_METRICS_CONF -> "quality";
            case SCENARIO_ACT_TEMPLATE -> "scenarios";
            case SCHEDULE -> "schedules";
            case TASK_EXECUTION_SERVICE_GROUP -> "taskexecutionservicegroups";
            case TASK -> "tasks";
            case METRIC_DQ -> "config/dq";
            case SCRIPT -> "scripts";
            case DATA_LINEAGE_DIAGRAM -> "datalineagediagrams";
        };
    }

    /**
     * Resolves a {@code kind} field value to a {@link YamlMetadataKind}.
     * Matching is case-insensitive and tolerant to dashes, underscores and spaces,
     * so {@code DataSet}, {@code dataset} and {@code data-set} are all accepted.
     *
     * @throws IllegalArgumentException when no construct matches the value
     */
    public static YamlMetadataKind fromYamlValue(String value) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("Configuration kind must not be blank");
        String normalized = normalize(value);
        for (YamlMetadataKind kind : values()) {
            if (normalize(kind.yamlValue).equals(normalized))
                return kind;
        }
        throw new IllegalArgumentException("Unknown configuration kind: " + value);
    }

    public static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
