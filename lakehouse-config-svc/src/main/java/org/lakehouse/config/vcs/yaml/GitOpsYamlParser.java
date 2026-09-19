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

package org.lakehouse.config.vcs.yaml;

import org.lakehouse.client.api.constant.YamlMetadataKind;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.NameSpaceDTO;
import org.lakehouse.client.api.dto.configs.dq.QualityMetricsConfDTO;
import org.lakehouse.client.api.dto.configs.schedule.DriverDTO;
import org.lakehouse.client.api.dto.configs.schedule.ScenarioActTemplateDTO;
import org.lakehouse.client.api.dto.configs.schedule.ScheduleDTO;
import org.lakehouse.client.api.dto.configs.schedule.TaskDTO;
import org.lakehouse.client.api.dto.configs.schedule.TaskExecutionServiceGroupDTO;
import org.lakehouse.client.api.dto.configs.script.ScriptDTO;
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
     * Preliminary parsing stage: reads the document and resolves its {@code kind} without
     * binding the body to the target DTO. Callers that only process configuration objects
     * must skip documents whose {@link PreliminaryConfig#kind()} reports
     * {@code isConfig() == false} before calling {@link #parseFull(PreliminaryConfig)}.
     *
     * @throws VcsConfigParseException when the content is not a valid declarative configuration
     */
    public PreliminaryConfig parsePreliminary(String content) {
        Map<String, Object> root;
        try {
            root = yamlMapper.readValue(requireContent(content), new TypeReference<Map<String, Object>>() {
            });
        } catch (VcsConfigParseException e) {
            throw e;
        } catch (Exception e) {
            throw new VcsConfigParseException("YAML document cannot be parsed as a configuration map", e);
        }
        if (root == null)
            throw new VcsConfigParseException("YAML document is empty");

        Object kindValue = root.remove(KIND_FIELD);
        if (kindValue == null)
            throw new VcsConfigParseException("Missing required field '" + KIND_FIELD + "'");
        try {
            return new PreliminaryConfig(YamlMetadataKind.fromYamlValue(String.valueOf(kindValue)), root);
        } catch (IllegalArgumentException e) {
            throw new VcsConfigParseException(e.getMessage(), e);
        }
    }

    /**
     * Full parsing stage: binds the already detected document body to the DTO of its kind.
     * Callers are expected to invoke it only for constructs with {@code kind.isConfig() == true}.
     *
     * @throws VcsConfigParseException when the body does not match the target DTO
     */
    public ParsedConfig parseFull(PreliminaryConfig preliminary) {
        YamlMetadataKind kind = preliminary.kind();
        try {
            Object dto = yamlMapper.convertValue(preliminary.body(), kind.dtoClass());
            return new ParsedConfig(kind, dto);
        } catch (Exception e) {
            throw new VcsConfigParseException("Cannot bind YAML document to " + kind, e);
        }
    }

    /**
     * Convenience wrapper combining {@link #parsePreliminary(String)} and
     * {@link #parseFull(PreliminaryConfig)}.
     *
     * @throws VcsConfigParseException when the content is not a valid declarative configuration
     */
    public ParsedConfig parse(String content) {
        return parseFull(parsePreliminary(content));
    }

    private String requireContent(String content) {
        if (content == null || content.isBlank())
            throw new VcsConfigParseException("YAML document is empty");
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
            case SCRIPT -> ((ScriptDTO) parsed.dto()).getKey();
            case TASK_EXECUTION_SERVICE_GROUP -> ((TaskExecutionServiceGroupDTO) parsed.dto()).getName();
            case TASK -> ((TaskDTO) parsed.dto()).getName();
            case DATA_SET -> ((DataSetDTO) parsed.dto()).getKeyName();
            case SCENARIO_ACT_TEMPLATE -> ((ScenarioActTemplateDTO) parsed.dto()).getKeyName();
            case QUALITY_METRICS_CONF -> ((QualityMetricsConfDTO) parsed.dto()).getKeyName();
            case SCHEDULE -> ((ScheduleDTO) parsed.dto()).getKeyName();
            default -> throw new IllegalStateException(
                    "Configuration kind is not managed by the config service: " + parsed.kind().yamlValue());
        };
    }
}