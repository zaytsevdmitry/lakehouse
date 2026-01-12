package org.lakehouse.client.api.dto.common;

import java.util.Objects;

public class SQLTemplateDTO {
    
    private String databaseSchemaName;
    private String databaseSchemaDDLCreate;
    private String databaseSchemaExistsSQL;

    private String tableFullName;
    private String tableDDLCreate;
    private String tableSQLExists;
    private String tableDDLDrop;
    private String tableDDLTruncate;
    private String tableDDLCompact;

    private String partitionDDLExchange;
    private String partitionDDLDrop;
    private String partitionDDLTruncate;
    private String partitionDDLAdd;
    private String partitionDDLCompact;

    private String constraintDDLDrop;

    private String primaryKeyDDL;
    private String primaryKeyDDLAdd;

    private String foreignKeyDDL;
    private String foreignKeyDDLAdd;

    private String uniqueKeyDDL;
    private String uniqueKeyDDLAdd;

    private String CheckConstraintDDL;
    private String CheckConstraintDDLAdd;



    private String columnsCastDML;
    private String mergeDML;
    private String insertDML;
    public String getDatabaseSchemaDDLCreate() {
        return databaseSchemaDDLCreate;
    }

    public void setDatabaseSchemaDDLCreate(String databaseSchemaDDLCreate) {
        this.databaseSchemaDDLCreate = databaseSchemaDDLCreate;
    }

    public String getDatabaseSchemaExistsSQL() {
        return databaseSchemaExistsSQL;
    }

    public void setDatabaseSchemaExistsSQL(String databaseSchemaExistsSQL) {
        this.databaseSchemaExistsSQL = databaseSchemaExistsSQL;
    }

    public String getTableDDLCreate() {
        return tableDDLCreate;
    }

    public void setTableDDLCreate(String tableDDLCreate) {
        this.tableDDLCreate = tableDDLCreate;
    }

    public String getTableSQLExists() {
        return tableSQLExists;
    }

    public void setTableSQLExists(String tableSQLExists) {
        this.tableSQLExists = tableSQLExists;
    }

    public String getTableDDLDrop() {
        return tableDDLDrop;
    }

    public void setTableDDLDrop(String tableDDLDrop) {
        this.tableDDLDrop = tableDDLDrop;
    }

    public String getTableDDLTruncate() {
        return tableDDLTruncate;
    }

    public void setTableDDLTruncate(String tableDDLTruncate) {
        this.tableDDLTruncate = tableDDLTruncate;
    }

    public String getTableDDLCompact() {
        return tableDDLCompact;
    }

    public void setTableDDLCompact(String tableDDLCompact) {
        this.tableDDLCompact = tableDDLCompact;
    }

    public String getPartitionDDLExchange() {
        return partitionDDLExchange;
    }

    public void setPartitionDDLExchange(String partitionDDLExchange) {
        this.partitionDDLExchange = partitionDDLExchange;
    }

    public String getPartitionDDLDrop() {
        return partitionDDLDrop;
    }

    public void setPartitionDDLDrop(String partitionDDLDrop) {
        this.partitionDDLDrop = partitionDDLDrop;
    }

    public String getPartitionDDLTruncate() {
        return partitionDDLTruncate;
    }

    public void setPartitionDDLTruncate(String partitionDDLTruncate) {
        this.partitionDDLTruncate = partitionDDLTruncate;
    }

    public String getPartitionDDLAdd() {
        return partitionDDLAdd;
    }

    public void setPartitionDDLAdd(String partitionDDLAdd) {
        this.partitionDDLAdd = partitionDDLAdd;
    }

    public String getConstraintDDLDrop() {
        return constraintDDLDrop;
    }

    public void setConstraintDDLDrop(String constraintDDLDrop) {
        this.constraintDDLDrop = constraintDDLDrop;
    }

    public String getPrimaryKeyDDL() {
        return primaryKeyDDL;
    }

    public void setPrimaryKeyDDL(String primaryKeyDDL) {
        this.primaryKeyDDL = primaryKeyDDL;
    }

    public String getPrimaryKeyDDLAdd() {
        return primaryKeyDDLAdd;
    }

    public void setPrimaryKeyDDLAdd(String primaryKeyDDLAdd) {
        this.primaryKeyDDLAdd = primaryKeyDDLAdd;
    }

    public String getForeignKeyDDL() {
        return foreignKeyDDL;
    }

    public void setForeignKeyDDL(String foreignKeyDDL) {
        this.foreignKeyDDL = foreignKeyDDL;
    }

    public String getForeignKeyDDLAdd() {
        return foreignKeyDDLAdd;
    }

    public void setForeignKeyDDLAdd(String foreignKeyDDLAdd) {
        this.foreignKeyDDLAdd = foreignKeyDDLAdd;
    }

    public String getUniqueKeyDDL() {
        return uniqueKeyDDL;
    }

    public void setUniqueKeyDDL(String uniqueKeyDDL) {
        this.uniqueKeyDDL = uniqueKeyDDL;
    }

    public String getUniqueKeyDDLAdd() {
        return uniqueKeyDDLAdd;
    }

    public void setUniqueKeyDDLAdd(String uniqueKeyDDLAdd) {
        this.uniqueKeyDDLAdd = uniqueKeyDDLAdd;
    }

    public String getCheckConstraintDDL() {
        return CheckConstraintDDL;
    }

    public void setCheckConstraintDDL(String checkConstraintDDL) {
        CheckConstraintDDL = checkConstraintDDL;
    }

    public String getCheckConstraintDDLAdd() {
        return CheckConstraintDDLAdd;
    }

    public void setCheckConstraintDDLAdd(String checkConstraintDDLAdd) {
        CheckConstraintDDLAdd = checkConstraintDDLAdd;
    }
    public String getColumnsCastDML() {
        return columnsCastDML;
    }

    public void setColumnsCastDML(String columnsCastDML) {
        this.columnsCastDML = columnsCastDML;
    }

    public String getMergeDML() {
        return mergeDML;
    }

    public void setMergeDML(String mergeDML) {
        this.mergeDML = mergeDML;
    }

    public String getInsertDML() {
        return insertDML;
    }

    public void setInsertDML(String insertDML) {
        this.insertDML = insertDML;
    }

    public String getTableFullName() {
        return tableFullName;
    }

    public void setTableFullName(String tableFullName) {
        this.tableFullName = tableFullName;
    }

    public String getDatabaseSchemaName() {
        return databaseSchemaName;
    }

    public void setDatabaseSchemaName(String databaseSchemaName) {
        this.databaseSchemaName = databaseSchemaName;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        SQLTemplateDTO that = (SQLTemplateDTO) o;
        return Objects.equals(getDatabaseSchemaName(), that.getDatabaseSchemaName()) && Objects.equals(getDatabaseSchemaDDLCreate(), that.getDatabaseSchemaDDLCreate()) && Objects.equals(getDatabaseSchemaExistsSQL(), that.getDatabaseSchemaExistsSQL()) && Objects.equals(getTableFullName(), that.getTableFullName()) && Objects.equals(getTableDDLCreate(), that.getTableDDLCreate()) && Objects.equals(getTableSQLExists(), that.getTableSQLExists()) && Objects.equals(getTableDDLDrop(), that.getTableDDLDrop()) && Objects.equals(getTableDDLTruncate(), that.getTableDDLTruncate()) && Objects.equals(getTableDDLCompact(), that.getTableDDLCompact()) && Objects.equals(getPartitionDDLExchange(), that.getPartitionDDLExchange()) && Objects.equals(getPartitionDDLDrop(), that.getPartitionDDLDrop()) && Objects.equals(getPartitionDDLTruncate(), that.getPartitionDDLTruncate()) && Objects.equals(getPartitionDDLAdd(), that.getPartitionDDLAdd()) && Objects.equals(getPartitionDDLCompact(), that.getPartitionDDLCompact()) && Objects.equals(getConstraintDDLDrop(), that.getConstraintDDLDrop()) && Objects.equals(getPrimaryKeyDDL(), that.getPrimaryKeyDDL()) && Objects.equals(getPrimaryKeyDDLAdd(), that.getPrimaryKeyDDLAdd()) && Objects.equals(getForeignKeyDDL(), that.getForeignKeyDDL()) && Objects.equals(getForeignKeyDDLAdd(), that.getForeignKeyDDLAdd()) && Objects.equals(getUniqueKeyDDL(), that.getUniqueKeyDDL()) && Objects.equals(getUniqueKeyDDLAdd(), that.getUniqueKeyDDLAdd()) && Objects.equals(getCheckConstraintDDL(), that.getCheckConstraintDDL()) && Objects.equals(getCheckConstraintDDLAdd(), that.getCheckConstraintDDLAdd()) && Objects.equals(getColumnsCastDML(), that.getColumnsCastDML()) && Objects.equals(getMergeDML(), that.getMergeDML()) && Objects.equals(getInsertDML(), that.getInsertDML());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDatabaseSchemaName(), getDatabaseSchemaDDLCreate(), getDatabaseSchemaExistsSQL(), getTableFullName(), getTableDDLCreate(), getTableSQLExists(), getTableDDLDrop(), getTableDDLTruncate(), getTableDDLCompact(), getPartitionDDLExchange(), getPartitionDDLDrop(), getPartitionDDLTruncate(), getPartitionDDLAdd(), getPartitionDDLCompact(), getConstraintDDLDrop(), getPrimaryKeyDDL(), getPrimaryKeyDDLAdd(), getForeignKeyDDL(), getForeignKeyDDLAdd(), getUniqueKeyDDL(), getUniqueKeyDDLAdd(), getCheckConstraintDDL(), getCheckConstraintDDLAdd(), getColumnsCastDML(), getMergeDML(), getInsertDML());
    }

    public String getPartitionDDLCompact() {
        return partitionDDLCompact;
    }

    public void setPartitionDDLCompact(String partitionDDLCompact) {
        this.partitionDDLCompact = partitionDDLCompact;
    }

}