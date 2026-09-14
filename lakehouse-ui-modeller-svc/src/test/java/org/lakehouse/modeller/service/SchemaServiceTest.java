package org.lakehouse.modeller.service;

import org.junit.jupiter.api.Test;
import org.lakehouse.modeller.dto.FieldSchema;
import org.lakehouse.modeller.dto.KindSchema;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaServiceTest {

    private final SchemaService service = new SchemaService();

    @Test
    void exposesAllSupportedKindsWithDirectories() {
        List<KindSchema> all = service.all();
        assertThat(all).hasSize(12);
        assertThat(all).extracting(KindSchema::kind)
                .containsExactlyInAnyOrder("NameSpace", "Driver", "ERDiagram", "DataSet", "DataSource",
                        "QualityMetricsConf", "ScenarioActTemplate", "Schedule",
                        "TaskExecutionServiceGroup", "Task", "MetricDQ", "Script");
        assertThat(service.schema("NameSpace").directory()).isEqualTo("nameSpaces");
        assertThat(service.schema("Driver").directory()).isEqualTo("drivers");
        assertThat(service.schema("ERDiagram").directory()).isEqualTo("erdiagrams");
        assertThat(service.schema("DataSet").directory()).isEqualTo("datasets");
        assertThat(service.schema("DataSource").directory()).isEqualTo("datasources");
        assertThat(service.schema("QualityMetricsConf").directory()).isEqualTo("quality");
        assertThat(service.schema("ScenarioActTemplate").directory()).isEqualTo("scenarios");
        assertThat(service.schema("Schedule").directory()).isEqualTo("schedules");
        assertThat(service.schema("TaskExecutionServiceGroup").directory()).isEqualTo("taskexecutionservicegroups");
        assertThat(service.schema("Task").directory()).isEqualTo("tasks");
        assertThat(service.schema("MetricDQ").directory()).isEqualTo("config/dq");
        assertThat(service.schema("Script").directory()).isEqualTo("scripts");
    }

    @Test
    void kindLookupIsCaseAndPunctuationInsensitive() {
        assertThat(service.schema("NameSpace")).isEqualTo(service.schema("namespace"));
        assertThat(service.schema("metric_dq")).isEqualTo(service.schema("MetricDQ"));
        assertThat(service.schema("unknown")).isNull();
    }

    @Test
    void erDiagramExposesKeyNameFirstAndTheDataSetPlacementsList() {
        KindSchema schema = service.schema("ERDiagram");
        assertThat(schema.dtoClass()).contains("ERDiagramDTO");
        List<FieldSchema> fields = schema.fields();
        assertThat(fields.get(0).name()).isEqualTo("keyName");
        assertThat(fields.get(0).keyName()).isTrue();
        FieldSchema dataSets = field(fields, "dataSets");
        assertThat(dataSets.type()).isEqualTo("list");
        assertThat(dataSets.item().children()).extracting(FieldSchema::name).contains("keyName", "x", "y");
    }

    @Test
    void namespaceCarriesMandatoryKeyNameFirstAndDescription() {
        KindSchema schema = service.schema("NameSpace");
        assertThat(schema.dtoClass()).contains("NameSpaceDTO");
        List<FieldSchema> fields = schema.fields();
        assertThat(fields.get(0).name()).isEqualTo("keyName");
        assertThat(fields.get(0).keyName()).isTrue();
        assertThat(fields.get(0).label()).isEqualTo("Key Name");
        assertThat(fields).extracting(FieldSchema::name).contains("description");
    }

    @Test
    void dataSetNameSpaceKeyNameIsReadOnlyBackedByNameSpacePicker() {
        KindSchema schema = service.schema("DataSet");
        FieldSchema nameSpace = field(schema.fields(), "nameSpaceKeyName");
        assertThat(nameSpace.readOnly()).isTrue();
        assertThat(nameSpace.picker()).isEqualTo("nameSpace");
        assertThat(nameSpace.clearable()).isTrue();
    }

    @Test
    void dataSetDataSourceKeyNameIsReadOnlyBackedByDataSourcePicker() {
        KindSchema schema = service.schema("DataSet");
        FieldSchema dataSource = field(schema.fields(), "dataSourceKeyName");
        assertThat(dataSource.readOnly()).isTrue();
        assertThat(dataSource.picker()).isEqualTo("dataSource");
        assertThat(dataSource.clearable()).isFalse();
    }

    @Test
    void taskGeneralPickFieldsAreReadOnlyBackedByCatalogPickers() {
        KindSchema schema = service.schema("Task");
        FieldSchema template = field(schema.fields(), "template");
        assertThat(template.readOnly()).isTrue();
        assertThat(template.picker()).isEqualTo("task");
        assertThat(template.clearable()).isFalse();

        FieldSchema group = field(schema.fields(), "taskExecutionServiceGroupName");
        assertThat(group.readOnly()).isTrue();
        assertThat(group.picker()).isEqualTo("taskExecutionServiceGroup");
        assertThat(group.clearable()).isFalse();

        FieldSchema driver = field(schema.fields(), "driverKeyName");
        assertThat(driver.readOnly()).isTrue();
        assertThat(driver.picker()).isEqualTo("driver");
        assertThat(driver.clearable()).isTrue();
    }

    @Test
    void scheduleModelsTheVisualDagAndScenarioActs() {
        KindSchema schema = service.schema("Schedule");
        assertThat(schema.dtoClass()).contains("ScheduleDTO");
        List<FieldSchema> fields = schema.fields();
        FieldSchema edges = field(fields, "scenarioActEdges");
        assertThat(edges.type()).isEqualTo("dag");
        assertThat(edges.children()).extracting(FieldSchema::name).containsExactlyInAnyOrder("from", "to");

        FieldSchema acts = field(fields, "scenarioActs");
        assertThat(acts.type()).isEqualTo("list");
        assertThat(acts.item().type()).isEqualTo("object");
        assertThat(acts.item().children()).extracting(FieldSchema::name).contains("scenarioActTemplate");

        assertThat(field(fields, "enabled").type()).isEqualTo("boolean");
        assertThat(field(fields, "intervalExpression").type()).isEqualTo("string");
    }

    @Test
    void complexBeansMapCollectionsAndTemplateObjects() {
        KindSchema dataset = service.schema("DataSet");
        assertThat(field(dataset.fields(), "columnSchema").type()).isEqualTo("list");
        assertThat(field(dataset.fields(), "sources").type()).isEqualTo("map");
        assertThat(field(dataset.fields(), "columnSchema").item().type()).isEqualTo("object");

        KindSchema driver = service.schema("Driver");
        FieldSchema sqlTemplate = field(driver.fields(), "sqlTemplate");
        assertThat(sqlTemplate.type()).isEqualTo("object");
        assertThat(sqlTemplate.children()).extracting(FieldSchema::name).contains("tableDDLCreate", "mergeDML");
    }

    @Test
    void columnSchemaPutsNameFirstInTheEditForm() {
        KindSchema schema = service.schema("DataSet");
        FieldSchema columns = field(schema.fields(), "columnSchema");
        assertThat(columns.item().children().get(0).name()).isEqualTo("name");
        assertThat(columns.item().children()).extracting(FieldSchema::name).contains("dataType", "nullable");
    }

    @Test
    void sqlTemplateFieldsAreReadOnlyScriptPickersWithClear() {
        List<FieldSchema> sqlTemplates = new ArrayList<>();
        sqlTemplates.add(field(service.schema("Driver").fields(), "sqlTemplate"));
        sqlTemplates.add(field(service.schema("Task").fields(), "sqlTemplate"));
        FieldSchema tasks = field(service.schema("ScenarioActTemplate").fields(), "tasks");
        sqlTemplates.add(field(tasks.item().children(), "sqlTemplate"));

        for (FieldSchema sqlTemplate : sqlTemplates) {
            assertThat(sqlTemplate.type()).isEqualTo("object");
            assertThat(sqlTemplate.children()).isNotEmpty();
            assertThat(sqlTemplate.children()).allSatisfy((f) -> {
                assertThat(f.readOnly()).isTrue();
                assertThat(f.picker()).isEqualTo("scriptKey");
                assertThat(f.clearable()).isTrue();
            });
        }
    }

    @Test
    void recordDtoTypesAreResolved() {
        KindSchema metricDq = service.schema("MetricDQ");
        assertThat(metricDq.dtoClass()).contains("MetricDQStatusDTO");
        List<FieldSchema> fields = metricDq.fields();
        assertThat(field(fields, "metricId").type()).isEqualTo("integer");
        assertThat(field(fields, "metricKeyName").type()).isEqualTo("string");
    }

    @Test
    void dataSourceEnumFieldsRenderAsStrings() {
        KindSchema schema = service.schema("DataSource");
        assertThat(schema.dtoClass()).contains("DataSourceDTO");
        List<FieldSchema> fields = schema.fields();
        assertThat(field(fields, "keyName").keyName()).isTrue();
        assertThat(field(fields, "databaseProtocol").type()).isEqualTo("string");
        assertThat(field(fields, "dataSourceType").type()).isEqualTo("string");
        assertThat(field(fields, "service").type()).isEqualTo("object");
    }

    @Test
    void dataSourceServiceTabOrderHostPortUrnProperties() {
        KindSchema schema = service.schema("DataSource");
        FieldSchema service = field(schema.fields(), "service");
        assertThat(service.children()).extracting(FieldSchema::name)
                .isEqualTo(List.of("host", "port", "urn", "properties"));
        assertThat(field(service.children(), "properties").type()).isEqualTo("map");
    }

    @Test
    void scriptExposesKeyAsUniqueIdentifierAndCodeValue() {
        KindSchema schema = service.schema("Script");
        assertThat(schema.dtoClass()).contains("ScriptDTO");
        List<FieldSchema> fields = schema.fields();
        assertThat(fields).extracting(FieldSchema::name).isEqualTo(List.of("key", "value"));
        FieldSchema key = field(fields, "key");
        assertThat(key.keyName()).isTrue();
        assertThat(key.uniqueAcrossKind()).isTrue();
        assertThat(key.picker()).isNull();
        assertThat(field(fields, "value").type()).isEqualTo("code");
    }

    @Test
    void scenarioActTemplateExposesTasksAndDag() {
        KindSchema schema = service.schema("ScenarioActTemplate");
        assertThat(schema.dtoClass()).contains("ScenarioActTemplateDTO");
        List<FieldSchema> fields = schema.fields();
        FieldSchema edges = field(fields, "dagEdges");
        assertThat(edges.type()).isEqualTo("dag");
        FieldSchema tasks = field(fields, "tasks");
        assertThat(tasks.type()).isEqualTo("list");
        assertThat(tasks.item().children()).extracting(FieldSchema::name).contains("name");
    }

    @Test
    void taskKindsResolveTheirSchedulesAndDirectories() {
        KindSchema group = service.schema("TaskExecutionServiceGroup");
        assertThat(group.dtoClass()).contains("TaskExecutionServiceGroupDTO");
        assertThat(field(group.fields(), "name").keyName()).isTrue();
        assertThat(field(group.fields(), "description").type()).isEqualTo("string");

        KindSchema task = service.schema("Task");
        assertThat(task.dtoClass()).contains("TaskDTO");
        assertThat(task.fields()).extracting(FieldSchema::name)
                .contains("name", "template", "taskProcessor", "sqlTemplate", "taskProcessorArgs");
    }

    @Test
    void taskItemsFollowTheRequestedColumnOrder() {
        KindSchema schema = service.schema("ScenarioActTemplate");
        FieldSchema tasks = field(schema.fields(), "tasks");
        assertThat(tasks.item().children()).extracting(FieldSchema::name)
                .isEqualTo(List.of("name", "template", "taskProcessor", "taskProcessorBody",
                        "taskExecutionServiceGroupName", "description", "driverKeyName",
                        "importance", "maxRetries", "sqlTemplate", "taskProcessorArgs"));
    }

    @Test
    void qualityMetricsConfResolvesValueMapsAndEnums() {
        KindSchema schema = service.schema("QualityMetricsConf");
        assertThat(schema.dtoClass()).contains("QualityMetricsConfDTO");
        List<FieldSchema> fields = schema.fields();
        assertThat(field(fields, "dqThresholdViolationLevel").type()).isEqualTo("string");
        assertThat(field(fields, "sources").type()).isEqualTo("map");
        assertThat(field(fields, "thresholds").type()).isEqualTo("map");
        assertThat(field(fields, "metric").type()).isEqualTo("object");
    }

    @Test
    void dataSetConstraintsCarryEnumsPickersAndConditionalVisibility() {
        KindSchema schema = service.schema("DataSet");
        FieldSchema constraints = field(schema.fields(), "constraints");
        assertThat(constraints.type()).isEqualTo("map");
        assertThat(constraints.children()).extracting(FieldSchema::name)
                .isEqualTo(List.of("constraintLevelCheck", "type", "columns",
                        "checkExpr", "reference", "enabled",
                        "tableConstraintDDLCreateOverride", "tableConstraintDDLAddOverride"));

        FieldSchema type = field(constraints.children(), "type");
        assertThat(type.enumValues()).containsExactly("primary", "foreign", "unique", "check");
        assertThat(type.enumDefault()).isEqualTo("primary");

        FieldSchema level = field(constraints.children(), "constraintLevelCheck");
        assertThat(level.enumValues()).containsExactly("dataQuality", "construct", "none");

        FieldSchema checkExpr = field(constraints.children(), "checkExpr");
        assertThat(checkExpr.visibleField()).isEqualTo("type");
        assertThat(checkExpr.visibleValue()).isEqualTo("check");

        FieldSchema reference = field(constraints.children(), "reference");
        assertThat(reference.type()).isEqualTo("object");
        assertThat(reference.visibleField()).isEqualTo("type");
        assertThat(reference.visibleValue()).isEqualTo("foreign");
        assertThat(reference.children()).extracting(FieldSchema::name)
                .isEqualTo(List.of("dataSetKeyName", "constraintName", "onDelete", "onUpdate"));
        assertThat(field(reference.children(), "dataSetKeyName").readOnly()).isTrue();
        assertThat(field(reference.children(), "dataSetKeyName").picker()).isEqualTo("datasetConstraint");
        assertThat(field(reference.children(), "constraintName").readOnly()).isTrue();
        assertThat(field(reference.children(), "onDelete").enumValues())
                .containsExactly("setNull", "default", "restrict", "noAction", "cascade");
        assertThat(field(reference.children(), "onUpdate").enumValues())
                .containsExactly("setNull", "default", "restrict", "noAction", "cascade");

        FieldSchema columns = field(constraints.children(), "columns");
        assertThat(columns.readOnly()).isTrue();
        assertThat(columns.picker()).isEqualTo("columns");
    }

    @Test
    void dataSetScriptReferencesPickKeyFromTheScriptPicker() {
        KindSchema schema = service.schema("DataSet");
        FieldSchema scripts = field(schema.fields(), "scripts");
        assertThat(scripts.type()).isEqualTo("list");
        assertThat(scripts.item().children()).extracting(FieldSchema::name).isEqualTo(List.of("key", "order"));
        FieldSchema key = field(scripts.item().children(), "key");
        assertThat(key.readOnly()).isTrue();
        assertThat(key.picker()).isEqualTo("scriptKey");
    }

    private static FieldSchema field(List<FieldSchema> fields, String name) {
        return fields.stream().filter(f -> f.name().equals(name)).findFirst().orElse(null);
    }
}