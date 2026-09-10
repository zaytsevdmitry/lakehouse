package org.lakehouse.modeller.service;

import org.junit.jupiter.api.Test;
import org.lakehouse.modeller.yaml.ConfigKind;
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
        assertThat(yaml.serialize(yaml.createEmptyNode(ConfigKind.TASK, "prepareJdbc")))
                .isEqualTo("kind: Task\nname: prepareJdbc\n");
        assertThat(yaml.serialize(yaml.createEmptyNode(ConfigKind.TASK_EXECUTION_SERVICE_GROUP, "spark-cluster")))
                .isEqualTo("kind: TaskExecutionServiceGroup\nname: spark-cluster\n");
        assertThat(yaml.serialize(yaml.createEmptyNode(ConfigKind.DATA_SET, "clients")))
                .contains("keyName: clients")
                .doesNotContain("name:");
    }
}