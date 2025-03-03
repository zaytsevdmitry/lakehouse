package org.lakehouse.taskexecutor.entity;

import java.util.Objects;
import java.util.Set;

public class TableDefinition {
    private String schemaName;
    private String tableName;
    private String fullTableName;
    private String columnsComaSeparated;
    private String columnsDDL;
    private String columnsUpdateSet;
    private String columnsMergeInsertValues;
    private String columnsMergeOn;
    private String tableDDL;
    private String columnsSelectWithCast;
    private Set<String> primaryKeys;
    public TableDefinition() {
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getFullTableName() {
        return fullTableName;
    }

    public void setFullTableName(String fullTableName) {
        this.fullTableName = fullTableName;
    }

    public String getColumnsComaSeparated() {
        return columnsComaSeparated;
    }

    public void setColumnsComaSeparated(String columnsComaSeparated) {
        this.columnsComaSeparated = columnsComaSeparated;
    }

    public String getColumnsDDL() {
        return columnsDDL;
    }

    public void setColumnsDDL(String columnsDDL) {
        this.columnsDDL = columnsDDL;
    }

    public String getTableDDL() {
        return tableDDL;
    }

    public void setTableDDL(String tableDDL) {
        this.tableDDL = tableDDL;
    }

    public String getColumnsSelectWithCast() {
        return columnsSelectWithCast;
    }

    public void setColumnsSelectWithCast(String columnsSelectWithCast) {
        this.columnsSelectWithCast = columnsSelectWithCast;
    }

    public Set<String> getPrimaryKeys() {
        return primaryKeys;
    }

    public void setPrimaryKeys(Set<String> primaryKeys) {
        this.primaryKeys = primaryKeys;
    }

    public String getColumnsUpdateSet() {
        return columnsUpdateSet;
    }

    public void setColumnsUpdateSet(String columnsUpdateSet) {
        this.columnsUpdateSet = columnsUpdateSet;
    }

    public String getColumnsMergeInsertValues() {
        return columnsMergeInsertValues;
    }

    public void setColumnsMergeInsertValues(String columnsMergeInsertValues) {
        this.columnsMergeInsertValues = columnsMergeInsertValues;
    }

    public String getColumnsMergeOn() {
        return columnsMergeOn;
    }

    public void setColumnsMergeOn(String columnsMergeOn) {
        this.columnsMergeOn = columnsMergeOn;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TableDefinition that = (TableDefinition) o;
        return Objects.equals(getSchemaName(), that.getSchemaName()) && Objects.equals(getTableName(), that.getTableName()) && Objects.equals(getFullTableName(), that.getFullTableName()) && Objects.equals(getColumnsComaSeparated(), that.getColumnsComaSeparated()) && Objects.equals(getColumnsDDL(), that.getColumnsDDL()) && Objects.equals(getColumnsUpdateSet(), that.getColumnsUpdateSet()) && Objects.equals(getColumnsMergeInsertValues(), that.getColumnsMergeInsertValues()) && Objects.equals(getColumnsMergeOn(), that.getColumnsMergeOn()) && Objects.equals(getTableDDL(), that.getTableDDL()) && Objects.equals(getColumnsSelectWithCast(), that.getColumnsSelectWithCast()) && Objects.equals(getPrimaryKeys(), that.getPrimaryKeys());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSchemaName(), getTableName(), getFullTableName(), getColumnsComaSeparated(), getColumnsDDL(), getColumnsUpdateSet(), getColumnsMergeInsertValues(), getColumnsMergeOn(), getTableDDL(), getColumnsSelectWithCast(), getPrimaryKeys());
    }
}
