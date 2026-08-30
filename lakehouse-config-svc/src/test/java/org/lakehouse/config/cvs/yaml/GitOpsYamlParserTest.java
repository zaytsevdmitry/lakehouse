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

import org.junit.jupiter.api.Test;
import org.lakehouse.client.api.constant.DatabaseProtocol;
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.NameSpaceDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GitOpsYamlParserTest {

    private final GitOpsYamlParser parser = new GitOpsYamlParser();

    @Test
    void parsesNameSpace() {
        ParsedConfig parsed = parser.parse("""
                kind: NameSpace
                keyName: fin
                description: Finance namespace
                """);

        assertThat(parsed.kind()).isEqualTo(ConfigKind.NAME_SPACE);
        assertThat(parsed.dto()).isInstanceOf(NameSpaceDTO.class);
        NameSpaceDTO dto = (NameSpaceDTO) parsed.dto();
        assertThat(dto.getKeyName()).isEqualTo("fin");
        assertThat(dto.getDescription()).isEqualTo("Finance namespace");
        assertThat(parser.resolveKey(parsed)).isEqualTo("fin");
    }

    @Test
    void parsesScriptContent() {
        ParsedConfig parsed = parser.parse("""
                kind: Script
                key: sql/merge
                value: |
                  merge into target using source;
                """);

        assertThat(parsed.kind()).isEqualTo(ConfigKind.SCRIPT);
        assertThat(parsed.dto()).isInstanceOf(ScriptContent.class);
        assertThat(((ScriptContent) parsed.dto()).key()).isEqualTo("sql/merge");
        assertThat(((ScriptContent) parsed.dto()).value()).contains("merge into target using source;");
        assertThat(parser.resolveKey(parsed)).isEqualTo("sql/merge");
    }

    @Test
    void bindsEnumsCaseInsensitivelyAndToleratesDashInKind() {
        ParsedConfig parsed = parser.parse("""
                kind: data-source
                keyName: mysql
                description: MySql
                databaseProtocol: mysql
                dataSourceType: database
                service:
                  host: localhost
                """);

        assertThat(parsed.kind()).isEqualTo(ConfigKind.DATA_SOURCE);
        assertThat(parsed.dto()).isInstanceOf(DataSourceDTO.class);
        DataSourceDTO dto = (DataSourceDTO) parsed.dto();
        assertThat(dto.getDataSourceType()).isEqualTo(Types.DataSourceType.database);
        assertThat(dto.getDatabaseProtocol()).isEqualTo(DatabaseProtocol.MYSQL);
        assertThat(parser.resolveKey(parsed)).isEqualTo("mysql");
    }

    @Test
    void resolvesKindTolerantToSeparatorsAndCase() {
        assertThat(ConfigKind.fromYamlValue("DataSet")).isEqualTo(ConfigKind.DATA_SET);
        assertThat(ConfigKind.fromYamlValue("data-set")).isEqualTo(ConfigKind.DATA_SET);
        assertThat(ConfigKind.fromYamlValue("dataset")).isEqualTo(ConfigKind.DATA_SET);
        assertThat(ConfigKind.fromYamlValue("DATA SET")).isEqualTo(ConfigKind.DATA_SET);
        assertThat(ConfigKind.fromYamlValue("task_execution_service_group"))
                .isEqualTo(ConfigKind.TASK_EXECUTION_SERVICE_GROUP);
        assertThat(ConfigKind.fromYamlValue("scenario-act-template"))
                .isEqualTo(ConfigKind.SCENARIO_ACT_TEMPLATE);
    }

    @Test
    void rejectsBlankKind() {
        assertThatThrownBy(() -> ConfigKind.fromYamlValue(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ConfigKind.fromYamlValue(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resolveKeyUsesRowKeysOfEveryConstruct() {
        assertThat(parser.resolveKey(parse("kind: NameSpace\nkeyName: ns"))).isEqualTo("ns");
        assertThat(parser.resolveKey(parse("kind: Driver\nkeyName: drv"))).isEqualTo("drv");
        assertThat(parser.resolveKey(parse("kind: DataSource\nkeyName: ds"))).isEqualTo("ds");
        assertThat(parser.resolveKey(parse("kind: Script\nkey: scr\nvalue: v"))).isEqualTo("scr");
        assertThat(parser.resolveKey(parse("kind: TaskExecutionServiceGroup\nname: grp"))).isEqualTo("grp");
        assertThat(parser.resolveKey(parse("kind: Task\nname: tsk"))).isEqualTo("tsk");
        assertThat(parser.resolveKey(parse("kind: DataSet\nkeyName: set-1"))).isEqualTo("set-1");
        assertThat(parser.resolveKey(parse("kind: ScenarioActTemplate\nkeyName: scen"))).isEqualTo("scen");
        assertThat(parser.resolveKey(parse("kind: QualityMetricsConf\nkeyName: qm"))).isEqualTo("qm");
        assertThat(parser.resolveKey(parse("kind: Schedule\nkeyName: sch\nintervalExpression: '* * ? * *'"))).isEqualTo("sch");
    }

    @Test
    void rejectsMissingKind() {
        assertThatThrownBy(() -> parser.parse("keyName: ns"))
                .isInstanceOf(CvsConfigParseException.class)
                .hasMessageContaining("kind");
    }

    @Test
    void rejectsUnknownKind() {
        assertThatThrownBy(() -> parser.parse("kind: NoSuchThing\nkeyName: ns"))
                .isInstanceOf(CvsConfigParseException.class)
                .hasMessageContaining("Unknown configuration kind");
    }

    @Test
    void rejectsUnknownProperties() {
        assertThatThrownBy(() -> parser.parse("kind: NameSpace\nkeyName: ns\nbogusProperty: 1"))
                .isInstanceOf(CvsConfigParseException.class);
    }

    @Test
    void rejectsBlankAndNonMapDocuments() {
        assertThatThrownBy(() -> parser.parse("")).isInstanceOf(CvsConfigParseException.class);
        assertThatThrownBy(() -> parser.parse("   \n")).isInstanceOf(CvsConfigParseException.class);
        assertThatThrownBy(() -> parser.parse("- just\n- a\n- list\n")).isInstanceOf(CvsConfigParseException.class);
    }

    private ParsedConfig parse(String yaml) {
        return parser.parse(yaml);
    }
}