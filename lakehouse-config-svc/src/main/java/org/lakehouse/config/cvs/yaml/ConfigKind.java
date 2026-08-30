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

package org.lakehouse.config.cvs.yaml;

import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.NameSpaceDTO;
import org.lakehouse.client.api.dto.configs.dq.QualityMetricsConfDTO;
import org.lakehouse.client.api.dto.configs.schedule.DriverDTO;
import org.lakehouse.client.api.dto.configs.schedule.ScenarioActTemplateDTO;
import org.lakehouse.client.api.dto.configs.schedule.ScheduleDTO;
import org.lakehouse.client.api.dto.configs.schedule.TaskDTO;
import org.lakehouse.client.api.dto.configs.schedule.TaskExecutionServiceGroupDTO;

import java.util.Locale;

/**
 * The declarative configuration constructs recognized in a configuration repository.
 * <p>
 * The value of the {@code kind} field of a YAML file selects both the target DTO class and
 * the position of the construct in the dependency aware apply/delete order.
 */
public enum ConfigKind {

    NAME_SPACE("NameSpace", NameSpaceDTO.class, 1),
    DRIVER("Driver", DriverDTO.class, 2),
    DATA_SOURCE("DataSource", DataSourceDTO.class, 3),
    SCRIPT("Script", ScriptContent.class, 4),
    TASK_EXECUTION_SERVICE_GROUP("TaskExecutionServiceGroup", TaskExecutionServiceGroupDTO.class, 5),
    TASK("Task", TaskDTO.class, 6),
    DATA_SET("DataSet", DataSetDTO.class, 7),
    SCENARIO_ACT_TEMPLATE("ScenarioActTemplate", ScenarioActTemplateDTO.class, 8),
    QUALITY_METRICS_CONF("QualityMetricsConf", QualityMetricsConfDTO.class, 9),
    SCHEDULE("Schedule", ScheduleDTO.class, 10);

    private final String yamlValue;
    private final Class<?> dtoClass;
    private final int order;

    ConfigKind(String yamlValue, Class<?> dtoClass, int order) {
        this.yamlValue = yamlValue;
        this.dtoClass = dtoClass;
        this.order = order;
    }

    public String yamlValue() {
        return yamlValue;
    }

    public Class<?> dtoClass() {
        return dtoClass;
    }

    public int order() {
        return order;
    }

    /**
     * Resolves a {@code kind} field value to a {@link ConfigKind}.
     * Matching is case-insensitive and tolerant to dashes, underscores and spaces,
     * so {@code DataSet}, {@code dataset} and {@code data-set} are all accepted.
     *
     * @throws IllegalArgumentException when no construct matches the value
     */
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

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}