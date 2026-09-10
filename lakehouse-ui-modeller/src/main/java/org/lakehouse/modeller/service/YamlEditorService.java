package org.lakehouse.modeller.service;

import org.lakehouse.modeller.yaml.ConfigKind;
import org.lakehouse.modeller.yaml.VcsConfigParseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLWriteFeature;

/**
 * Thin wrapper over the Jackson 3 YAML mapper used for all metadata documents.
 * Reads write the plain-text block style expected by the repository.
 */
public class YamlEditorService {

    private final YAMLMapper mapper;

    public YamlEditorService() {
        // Re-serialization must reproduce the plain block style of the repository
        // (no "---" document-start marker, plain scalars, 2-space indented list
        // items). Otherwise an unchanged "Save" would rewrite every file.
        this.mapper = YAMLMapper.builder()
                .disable(YAMLWriteFeature.WRITE_DOC_START_MARKER)
                .enable(YAMLWriteFeature.MINIMIZE_QUOTES)
                .enable(YAMLWriteFeature.INDENT_ARRAYS_WITH_INDICATOR)
                .build();
    }

    /**
     * Creates the initial document of a kind with its key name (spec 6.1).
     */
    public String defaultYaml(ConfigKind kind, String keyName) {
        return serialize(createEmptyNode(kind, keyName));
    }

    public ObjectNode createEmptyNode(ConfigKind kind, String identifier) {
        ObjectNode node = mapper.createObjectNode();
        node.put("kind", kind.yamlValue());
        if (identifier != null && !identifier.isBlank())
            node.put(kind.identifierField(), identifier);
        return node;
    }

    public ObjectNode parse(String yaml) {
        if (yaml == null || yaml.isBlank())
            throw new VcsConfigParseException("Configuration content must not be blank");
        try {
            JsonNode node = mapper.readTree(yaml.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            if (node == null || !node.isObject())
                throw new VcsConfigParseException("Configuration content must be a YAML mapping");
            return (ObjectNode) node;
        } catch (tools.jackson.core.JacksonException e) {
            throw new VcsConfigParseException("Cannot parse configuration YAML: " + e.getMessage(), e);
        }
    }

    public String serialize(JsonNode node) {
        try {
            return mapper.writeValueAsString(node);
        } catch (tools.jackson.core.JacksonException e) {
            throw new VcsConfigParseException("Cannot serialize configuration YAML: " + e.getMessage(), e);
        }
    }

    public ConfigKind kindOf(ObjectNode node) {
        JsonNode kind = node.get("kind");
        if (kind == null || kind.isNull() || kind.asText(null) == null)
            throw new VcsConfigParseException("Configuration document is missing the 'kind' field");
        try {
            return ConfigKind.fromYamlValue(kind.asText());
        } catch (IllegalArgumentException e) {
            throw new VcsConfigParseException(e.getMessage());
        }
    }

    public java.util.Optional<ConfigKind> knownKindOf(ObjectNode node) {
        JsonNode kind = node.get("kind");
        if (kind == null || kind.isNull() || kind.asText(null) == null)
            return java.util.Optional.empty();
        for (ConfigKind candidate : ConfigKind.values()) {
            if (ConfigKind.normalize(candidate.yamlValue()).equals(ConfigKind.normalize(kind.asText())))
                return java.util.Optional.of(candidate);
        }
        return java.util.Optional.empty();
    }

    public String keyNameOf(ObjectNode node) {
        return identifierOf(node, "keyName");
    }

    public String identifierOf(ObjectNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText(null);
    }
}