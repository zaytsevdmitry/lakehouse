package org.lakehouse.taskexecutor.factory;

import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.dataset.ColumnDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetConstraintDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.task.TableDefinition;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class TableDefinitionFactory {
    public TableDefinition buildTableDefinition(DataSetDTO dataSetDTO, DataSourceDTO dataSourceDTO) {
        TableDefinition result = new TableDefinition();
        String[] table = dataSetDTO.getFullTableName().split("\\.");

        if (table.length == 1)
            result.setTableName(table[0]);
        if (table.length == 2) {
            result.setSchemaName(table[0]);
            result.setTableName(table[1]);
        }

        result.setFullTableName(dataSetDTO.getFullTableName());

        result.setColumnsDDL(columnsDDL(dataSetDTO.getColumnSchema()));

        result.setColumnsComaSeparated(columnsCS(dataSetDTO.getColumnSchema()));

        result.setTableDDL(
                tableDDL(
                        dataSetDTO.getFullTableName(),
                        dataSetDTO.getColumnSchema(),
                        dataSetDTO.getProperties()));

        result.setColumnsSelectWithCast(columnsCast(dataSetDTO.getColumnSchema()));
        result.setColumnsMergeInsertValues(getColumnsMergeInsertValues(dataSetDTO.getColumnSchema()));
        result.setColumnsMergeOn(getMergeOn(dataSetDTO.getConstraints()));
        result.setPrimaryKeys(getPrimaryKeys(dataSetDTO.getConstraints()));
        result.setColumnsUpdateSet(getColumnsUpdateSet(dataSetDTO.getColumnSchema()));
        return result;
    }


    private String getMergeOn(List<DataSetConstraintDTO> constraints) {
        //todo aliases of table queryed in merge - t it's target q is a model script
        return getPrimaryKeys(constraints).stream()
                .map(key ->
                        String.format("t.".concat(key).concat(" = q.").concat(key)))
                .collect(Collectors.joining(" and "));

    }

    private String getColumnsUpdateSet(List<ColumnDTO> columns) {
        //todo aliases of table queryed in merge - t it's target q is a model script
        return columns
                .stream()
                .map(col -> String.format("t.%s = q.%s\n", col.getName(), col.getName()))
                .collect(Collectors.joining(", "));
    }

    private String getColumnsMergeInsertValues(List<ColumnDTO> columns) {
        //todo aliases of table queryed in merge - t it's target q is a model script
        return columns
                .stream()
                .map(col -> String.format("q.%s", col.getName()))
                .collect(Collectors.joining(", "));
    }

    private Set<String> getPrimaryKeys(List<DataSetConstraintDTO> constraints) {

        return constraints.stream()
                .filter(c ->
                        c.getType().equals(Types.Constraint.primary))
                .flatMap(dataSetConstraintDTO ->
                        Stream.of(dataSetConstraintDTO.getColumns().split("\\,")))
                .collect(Collectors.toSet());
    }

    private String columnsCS(List<ColumnDTO> columnSchema) {
        return columnSchema
                .stream()
                // todo skip identity col      .filter(columnDTO -> !columnDTO.getDataType().equalsIgnoreCase("serial"))
                .map(ColumnDTO::getName)
                .collect(Collectors.joining(", "));
    }


    private String columnsDDL(List<ColumnDTO> columnSchema) {
        StringJoiner columns = new StringJoiner(",");

        columnSchema.stream().map(columnDTO ->
                String.format("%s %s", columnDTO.getName(), columnDTO.getDataType())).forEach(columns::add);

        return columns.toString();
    }

    private String columnsCast(List<ColumnDTO> columnSchema) {
        StringJoiner columns = new StringJoiner(",");

        columnSchema.stream().map(columnDTO ->
                        String.format(
                                "cast( %s as  %s) as %s",
                                columnDTO.getName(),
                                columnDTO.getDataType(),
                                columnDTO.getName()))
                .forEach(columns::add);

        return columns.toString();
    }

    private String tableDDL(String tableName, List<ColumnDTO> columnSchema, Map<String, String> properties) {
        // todo storage parameters ?
        // todo constraints
        return String.format(
                "create table %s\n (%s)\n %s",
                tableName,
                columnsDDL(columnSchema),
                // todo storage parameters ?
                // todo constraints
                properties
                        .entrySet()
                        .stream()
                        .map(sse -> String.format("%s %s", sse.getKey(), sse.getValue()))
        );
    }
}
