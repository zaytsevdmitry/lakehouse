package org.lakehouse.ui.modeller.service;

import org.lakehouse.client.api.constant.YamlMetadataKind;
import org.lakehouse.ui.modeller.yaml.VcsConfigParseException;
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
                .enable(YAMLWriteFeature.LITERAL_BLOCK_STYLE)
                .enable(YAMLWriteFeature.INDENT_ARRAYS_WITH_INDICATOR)
                .build();
    }

    /**
     * Creates the initial document of a kind with its key name (spec 6.1).
     */
    public String defaultYaml(YamlMetadataKind kind, String keyName) {
        return serialize(createEmptyNode(kind, keyName));
    }

    public ObjectNode createEmptyNode(YamlMetadataKind kind, String identifier) {
        ObjectNode node = mapper.createObjectNode();
        node.put("kind", kind.yamlValue());
        if (identifier != null && !identifier.isBlank())
            node.put(kind.identifierField(), identifier);
        if (kind == YamlMetadataKind.DATA_LINEAGE_DIAGRAM) {
            // lineage diagrams start empty; the editor fills spec.datasets
            ObjectNode spec = mapper.createObjectNode();
            spec.putArray("datasets");
            spec.putObject("layout").putObject("positions");
            node.set("spec", spec);
        }
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
            String yaml = mapper.writeValueAsString(node);
            try {
                String rewritten = rewriteMultilineQuotedScalars(yaml);
                if (rewritten == null)
                    return yaml;
                // Safety net: literals must round-trip to the identical tree, otherwise
                // keep the emitter's (quoted) output rather than risk corruption.
                JsonNode check = mapper.readTree(rewritten.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                return node.equals(check) ? rewritten : yaml;
            } catch (RuntimeException e) {
                return yaml;
            }
        } catch (tools.jackson.core.JacksonException e) {
            throw new VcsConfigParseException("Cannot serialize configuration YAML: " + e.getMessage(), e);
        }
    }

    /**
     * snakeyaml-engine only emits literal block scalars when the value has no
     * trailing whitespace on a line (a space before a line break disables the
     * block style), so the Jackson mapper falls back to a double-quoted scalar
     * with escaped newlines and fold continuations for such values. This is valid
     * YAML but mumbo-jumbo for code fields like Script {@code value}, so any
     * double-quoted scalar that decodes to a multiline string is re-emitted as a
     * literal block that preserves the content byte for byte.
     *
     * @return the rewritten YAML, or {@code null} when nothing needed rewriting
     */
    private static String rewriteMultilineQuotedScalars(String yaml) {
        int n = yaml.length();
        int cursor = 0;
        int pos = 0;
        StringBuilder out = new StringBuilder(yaml.length() + 64);
        boolean changed = false;
        while (pos < n) {
            if (yaml.charAt(pos) != '"') {
                pos++;
                continue;
            }
            int lineStart = yaml.lastIndexOf('\n', pos - 1) + 1;
            int end = pos + 1;
            StringBuilder raw = new StringBuilder();
            boolean closed = false;
            while (end < n) {
                char ch = yaml.charAt(end);
                if (ch == '\\') {
                    raw.append(ch);
                    if (end + 1 < n) {
                        end++;
                        raw.append(yaml.charAt(end));
                    }
                    end++;
                    continue;
                }
                if (ch == '"') {
                    closed = true;
                    end++;
                    break;
                }
                raw.append(ch);
                end++;
            }
            if (!closed) {
                pos++;
                continue;
            }
            String decoded;
            try {
                decoded = decodeDoubleQuoted(raw.toString());
            } catch (IllegalArgumentException e) {
                pos = end;
                continue;
            }
            if (decoded.indexOf('\n') < 0) {
                pos = end;
                continue;
            }
            int after = end;
            if (after < n && yaml.charAt(after) == '\n')
                after++;
            out.append(yaml, cursor, lineStart);
            out.append(literalBlock(yaml.substring(lineStart, pos), decoded));
            cursor = after;
            pos = after;
            changed = true;
        }
        if (!changed)
            return null;
        out.append(yaml, cursor, n);
        return out.toString();
    }

    /** Decodes the content of a YAML double-quoted scalar (without the quotes). */
    private static String decodeDoubleQuoted(String raw) {
        StringBuilder sb = new StringBuilder(raw.length());
        int i = 0;
        int n = raw.length();
        while (i < n) {
            char c = raw.charAt(i);
            if (c == '\\') {
                if (i + 1 >= n)
                    throw new IllegalArgumentException("dangling escape in double-quoted scalar");
                char e = raw.charAt(i + 1);
                switch (e) {
                    case '0' -> sb.append('\0');
                    case 'a' -> sb.append('\u0007');
                    case 'b' -> sb.append('\b');
                    case 't' -> sb.append('\t');
                    case 'n' -> sb.append('\n');
                    case 'v' -> sb.append('\u000B');
                    case 'f' -> sb.append('\f');
                    case 'r' -> sb.append('\r');
                    case 'e' -> sb.append('\u001B');
                    case '"' -> sb.append('"');
                    case '/' -> sb.append('/');
                    case '\\' -> sb.append('\\');
                    case 'N' -> sb.append('\u0085');
                    case '_' -> sb.append('\u00A0');
                    case 'L' -> sb.append('\u2028');
                    case 'P' -> sb.append('\u2029');
                    case ' ' -> sb.append(' ');
                    case '\n' -> {
                        // escaped line break: drops the break and the following indentation
                        i += 2;
                        while (i < n && (raw.charAt(i) == ' ' || raw.charAt(i) == '\t'))
                            i++;
                        continue;
                    }
                    case 'x' -> {
                        i += 2;
                        sb.append((char) hex(raw, i, 2));
                        i += 2;
                        continue;
                    }
                    case 'u' -> {
                        i += 2;
                        sb.append((char) hex(raw, i, 4));
                        i += 4;
                        continue;
                    }
                    case 'U' -> {
                        i += 2;
                        sb.appendCodePoint(hex(raw, i, 8));
                        i += 8;
                        continue;
                    }
                    default -> throw new IllegalArgumentException("unknown escape \\" + e + " in double-quoted scalar");
                }
                i += 2;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    private static int hex(String s, int from, int len) {
        if (from + len > s.length())
            throw new IllegalArgumentException("truncated hex escape");
        int value = 0;
        for (int i = 0; i < len; i++) {
            int digit = Character.digit(s.charAt(from + i), 16);
            if (digit < 0)
                throw new IllegalArgumentException("invalid hex escape");
            value = (value << 4) | digit;
        }
        return value;
    }

    /**
     * Builds a literal block replacement for a {@code key: "…multiline…"} scalar.
     * {@code keyPart} is everything on the key line before the opening quote.
     */
    private static String literalBlock(String keyPart, String value) {
        int indent = 0;
        while (indent < keyPart.length() && keyPart.charAt(indent) == ' ')
            indent++;
        String blockIndent = " ".repeat(indent + 2);
        int trailing = 0;
        for (int i = value.length() - 1; i >= 0 && value.charAt(i) == '\n'; i--)
            trailing++;
        String core = trailing == 0 ? value : value.substring(0, value.length() - trailing);
        String chomp = trailing == 0 ? "-" : trailing == 1 ? "" : "+";
        StringBuilder out = new StringBuilder(value.length() + 16);
        out.append(keyPart).append('|').append(chomp).append('\n');
        for (String line : core.split("\n", -1)) {
            if (line.isEmpty())
                out.append('\n');
            else
                out.append(blockIndent).append(line).append('\n');
        }
        for (int i = 1; i < trailing; i++)
            out.append('\n');
        return out.toString();
    }

    public YamlMetadataKind kindOf(ObjectNode node) {
        JsonNode kind = node.get("kind");
        if (kind == null || kind.isNull() || kind.asText(null) == null)
            throw new VcsConfigParseException("Configuration document is missing the 'kind' field");
        try {
            return YamlMetadataKind.fromYamlValue(kind.asText());
        } catch (IllegalArgumentException e) {
            throw new VcsConfigParseException(e.getMessage());
        }
    }

    public java.util.Optional<YamlMetadataKind> knownKindOf(ObjectNode node) {
        JsonNode kind = node.get("kind");
        if (kind == null || kind.isNull() || kind.asText(null) == null)
            return java.util.Optional.empty();
        for (YamlMetadataKind candidate : YamlMetadataKind.values()) {
            if (YamlMetadataKind.normalize(candidate.yamlValue()).equals(YamlMetadataKind.normalize(kind.asText())))
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