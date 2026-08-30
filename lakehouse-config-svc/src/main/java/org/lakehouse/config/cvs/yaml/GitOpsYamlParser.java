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
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLWriteFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;

import java.util.Map;

/**
 * Parses YAML declarative configuration files into DTOs.
 * <p>
 * A file is expected to start with a {@code kind} field (Kubernetes style) that selects
 * the target configuration construct. The kind field is stripped before the rest of the
 * map is bound to the corresponding DTO. Enum fields are deserialized case-insensitively
 * so that projects using {@code postgresql} instead of {@code POSTGRESQL} are not rejected;
 * unknown properties are a hard error to keep the declarative description strict.
 */
@Component
public class GitOpsYamlParser {

    static final String KIND_FIELD = "kind";

    private final YAMLMapper yamlMapper;

    public GitOpsYamlParser() {
        this.yamlMapper = YAMLMapper.builder(
                        YAMLFactory.builder()
                                .disable(YAMLWriteFeature.WRITE_DOC_START_MARKER)
                                .build())
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    /**
     * Parses the given YAML content into a configuration construct.
     *
     * @throws CvsConfigParseException when the content is not a valid declarative configuration
     */
    public ParsedConfig parse(String content) {
        Map<String, Object> root;
        try {
            root = yamlMapper.readValue(requireContent(content), new TypeReference<Map<String, Object>>() {
            });
        } catch (CvsConfigParseException e) {
            throw e;
        } catch (Exception e) {
            throw new CvsConfigParseException("YAML document cannot be parsed as a configuration map", e);
        }
        if (root == null)
            throw new CvsConfigParseException("YAML document is empty");

        Object kindValue = root.remove(KIND_FIELD);
        if (kindValue == null)
            throw new CvsConfigParseException("Missing required field '" + KIND_FIELD + "'");
        ConfigKind kind;
        try {
            kind = ConfigKind.fromYamlValue(String.valueOf(kindValue));
        } catch (IllegalArgumentException e) {
            throw new CvsConfigParseException(e.getMessage(), e);
        }
        try {
            Object dto = yamlMapper.convertValue(root, kind.dtoClass());
            return new ParsedConfig(kind, dto);
        } catch (Exception e) {
            throw new CvsConfigParseException("Cannot bind YAML document to " + kind, e);
        }
    }

    private String requireContent(String content) {
        if (content == null || content.isBlank())
            throw new CvsConfigParseException("YAML document is empty");
        return content;
    }

    /**
     * @return the primary key of the given configuration construct
     */
    public String resolveKey(ParsedConfig parsed) {
        return switch (parsed.kind()) {
            case NAME_SPACE -> ((NameSpaceDTO) parsed.dto()).getKeyName();
            case DRIVER -> ((DriverDTO) parsed.dto()).getKeyName();
            case DATA_SOURCE -> ((DataSourceDTO) parsed.dto()).getKeyName();
            case SCRIPT -> ((ScriptContent) parsed.dto()).key();
            case TASK_EXECUTION_SERVICE_GROUP -> ((TaskExecutionServiceGroupDTO) parsed.dto()).getName();
            case TASK -> ((TaskDTO) parsed.dto()).getName();
            case DATA_SET -> ((DataSetDTO) parsed.dto()).getKeyName();
            case SCENARIO_ACT_TEMPLATE -> ((ScenarioActTemplateDTO) parsed.dto()).getKeyName();
            case QUALITY_METRICS_CONF -> ((QualityMetricsConfDTO) parsed.dto()).getKeyName();
            case SCHEDULE -> ((ScheduleDTO) parsed.dto()).getKeyName();
        };
    }
}