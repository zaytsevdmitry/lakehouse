package org.lakehouse.ui.modeller.service;

import org.junit.jupiter.api.Test;
import org.lakehouse.client.api.constant.YamlMetadataKind;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;

class YamlEditorServiceTest {

    private final YamlEditorService yaml = new YamlEditorService();

    private static final String REPO_STYLE =
            "kind: DataSet\n" +
            "keyName: client_processing\n" +
            "scripts:\n" +
            "  - key: dataset-sql-model.client_processing.sql\n" +
            "columnSchema:\n" +
            "  - name: id\n" +
            "    description: Client id\n" +
            "    dataType: varchar(255)\n" +
            "    nullable: false\n" +
            "constraints:\n" +
            "  client_processing_pk:\n" +
            "    type: primary\n" +
            "    columns: id\n" +
            "    enabled: true\n" +
            "description: remote dataset with clients\n";

    @Test
    void serializeReproducesTheRepositoryPlainBlockStyleByteForByte() {
        ObjectNode node = yaml.parse(REPO_STYLE);
        assertThat(yaml.serialize(node)).isEqualTo(REPO_STYLE);
    }

    @Test
    void serializeNeverEmitsTheDocumentStartMarker() {
        ObjectNode node = yaml.parse(REPO_STYLE);
        assertThat(yaml.serialize(node).split("\\n")[0]).isNotEqualTo("---");
    }

    @Test
    void createEmptyNodeUsesTheKindIdentifierField() {
        assertThat(yaml.serialize(yaml.createEmptyNode(YamlMetadataKind.TASK, "prepareJdbc")))
                .isEqualTo("kind: Task\nname: prepareJdbc\n");
        assertThat(yaml.serialize(yaml.createEmptyNode(YamlMetadataKind.TASK_EXECUTION_SERVICE_GROUP, "spark-cluster")))
                .isEqualTo("kind: TaskExecutionServiceGroup\nname: spark-cluster\n");
        assertThat(yaml.serialize(yaml.createEmptyNode(YamlMetadataKind.DATA_SET, "clients")))
                .contains("keyName: clients")
                .doesNotContain("name:");
    }

    @Test
    void multilineScriptValueWithTrailingSpacesIsWrittenAsLiteralBlock() {
        String sql = "select client_id\n"
                + "     , reg_date_time\n"
                + "     , client_name\n"
                + "     , amount - sum(amount)\n"
                + "              over (\n"
                + "               partition by client_id,client_name\n"
                + "               order by reg_date_time) diff_amount\n"
                + "from  {{refCat('transaction_dds') }}\n"
                + "  where   t.reg_date_time >= timestamp '{{ intervalStartDateTime }}' \n"
                + "and\n"
                + "     t.reg_date_time < timestamp '{{ intervalEndDateTime }}'";
        ObjectNode node = yaml.parse("kind: Script\nkey: sliding_total.sql\n");
        node.put("value", sql);
        String out = yaml.serialize(node);
        assertThat(out).contains("\nvalue: |-\n  select client_id");
        assertThat(out).doesNotContain("\\n").doesNotContain("\\ ");
        assertThat(out).contains("'{{ intervalStartDateTime }}' \n");
        assertThat(yaml.parse(out).get("value").asText()).isEqualTo(sql);
    }

    @Test
    void multilineValuesWithQuotesAndBackslashesRoundTrip() {
        ObjectNode node = yaml.parse("kind: Script\nkey: escaping.sql\n");
        String value = "select \"a\"\nwhere b = 'c\\d'\nprintf(\"%s\\n\", x)\n";
        node.put("value", value);
        String out = yaml.serialize(node);
        assertThat(out).contains("value: |");
        assertThat(yaml.parse(out).get("value").asText()).isEqualTo(value);
    }

    @Test
    void multilineValuesWithTrailingNewlinesRoundTrip() {
        ObjectNode node = yaml.parse("kind: Script\nkey: newlines.sql\n");
        String value = "line1\n\nline3\n\n\n";
        node.put("value", value);
        String out = yaml.serialize(node);
        assertThat(yaml.parse(out).get("value").asText()).isEqualTo(value);
    }

    @Test
    void nestedMultilineValueInAnObjectRoundTrips() {
        ObjectNode node = yaml.parse("kind: DataSet\nkeyName: t\nconstraints:\n  pk:\n    type: primary\n");
        ((ObjectNode) node.at("/constraints/pk")).put("columns", "select a \nfrom t\n  where x\n");
        String out = yaml.serialize(node);
        assertThat(out).contains("    columns: |");
        assertThat(yaml.parse(out).at("/constraints/pk/columns").asText())
                .isEqualTo("select a \nfrom t\n  where x\n");
    }
}