package org.lakehouse.client.api.dto.common;

import java.util.Objects;

public class SQLTemplateDTO implements SQLTemplateGetter , SQLTemplateSetter {
    
    private String databaseSchemaName;
    private String databaseSchemaDDLCreate;
    private String databaseSchemaExistsSQL;

    private String tableFullName;
    private String tableDDLCreate;
    private String tableSQLExists;
    private String tableDDLDrop;
    private String tableDDLTruncate;
    private String tableDDLCompact;


    private String columnNonNullCheckIntegrity;
    private String columnCheckIntegrity;


    private String partitionDDLExchange;
    private String partitionDDLDrop;
    private String partitionDDLTruncate;
    private String partitionDDLAdd;
    private String partitionDDLCompact;

    private String constraintDDLDrop;

    private String primaryKeyDDL;
    private String primaryKeyDDLAdd;
    private String primaryKeyCheckIntegrity;

    private String foreignKeyDDL;
    private String foreignKeyDDLAdd;
    private String foreignKeyCheckIntegrity;

    private String uniqueKeyDDL;
    private String uniqueKeyDDLAdd;
    private String uniqueKeyCheckIntegrity;

    private String CheckConstraintDDL;
    private String CheckConstraintDDLAdd;
    private String CheckConstraintCheckIntegrity;



    private String columnsCastDML;
    private String mergeDML;
    private String insertDML;

    @Override
    public String getDatabaseSchemaName() {
        return databaseSchemaName;
    }

    @Override
    public void setDatabaseSchemaName(String databaseSchemaName) {
        this.databaseSchemaName = databaseSchemaName;
    }

    @Override
    public String getDatabaseSchemaDDLCreate() {
        return databaseSchemaDDLCreate;
    }

    @Override
    public void setDatabaseSchemaDDLCreate(String databaseSchemaDDLCreate) {
        this.databaseSchemaDDLCreate = databaseSchemaDDLCreate;
    }

    @Override
    public String getDatabaseSchemaExistsSQL() {
        return databaseSchemaExistsSQL;
    }

    @Override
    public void setDatabaseSchemaExistsSQL(String databaseSchemaExistsSQL) {
        this.databaseSchemaExistsSQL = databaseSchemaExistsSQL;
    }

    @Override
    public String getTableFullName() {
        return tableFullName;
    }

    @Override
    public void setTableFullName(String tableFullName) {
        this.tableFullName = tableFullName;
    }

    @Override
    public String getTableDDLCreate() {
        return tableDDLCreate;
    }

    @Override
    public void setTableDDLCreate(String tableDDLCreate) {
        this.tableDDLCreate = tableDDLCreate;
    }

    @Override
    public String getTableSQLExists() {
        return tableSQLExists;
    }

    @Override
    public void setTableSQLExists(String tableSQLExists) {
        this.tableSQLExists = tableSQLExists;
    }

    @Override
    public String getTableDDLDrop() {
        return tableDDLDrop;
    }

    @Override
    public void setTableDDLDrop(String tableDDLDrop) {
        this.tableDDLDrop = tableDDLDrop;
    }

    @Override
    public String getTableDDLTruncate() {
        return tableDDLTruncate;
    }

    @Override
    public void setTableDDLTruncate(String tableDDLTruncate) {
        this.tableDDLTruncate = tableDDLTruncate;
    }

    @Override
    public String getTableDDLCompact() {
        return tableDDLCompact;
    }

    @Override
    public void setTableDDLCompact(String tableDDLCompact) {
        this.tableDDLCompact = tableDDLCompact;
    }

    @Override
    public String getColumnNonNullCheckIntegrity() {
        return columnNonNullCheckIntegrity;
    }

    @Override
    public void setColumnNonNullCheckIntegrity(String columnNonNullCheckIntegrity) {
        this.columnNonNullCheckIntegrity = columnNonNullCheckIntegrity;
    }

    @Override
    public String getColumnCheckIntegrity() {
        return columnCheckIntegrity;
    }

    @Override
    public void setColumnCheckIntegrity(String columnCheckIntegrity) {
        this.columnCheckIntegrity = columnCheckIntegrity;
    }

    @Override
    public String getPartitionDDLExchange() {
        return partitionDDLExchange;
    }

    @Override
    public void setPartitionDDLExchange(String partitionDDLExchange) {
        this.partitionDDLExchange = partitionDDLExchange;
    }

    @Override
    public String getPartitionDDLDrop() {
        return partitionDDLDrop;
    }

    @Override
    public void setPartitionDDLDrop(String partitionDDLDrop) {
        this.partitionDDLDrop = partitionDDLDrop;
    }

    @Override
    public String getPartitionDDLTruncate() {
        return partitionDDLTruncate;
    }

    @Override
    public void setPartitionDDLTruncate(String partitionDDLTruncate) {
        this.partitionDDLTruncate = partitionDDLTruncate;
    }

    @Override
    public String getPartitionDDLAdd() {
        return partitionDDLAdd;
    }

    @Override
    public void setPartitionDDLAdd(String partitionDDLAdd) {
        this.partitionDDLAdd = partitionDDLAdd;
    }

    @Override
    public String getPartitionDDLCompact() {
        return partitionDDLCompact;
    }

    @Override
    public void setPartitionDDLCompact(String partitionDDLCompact) {
        this.partitionDDLCompact = partitionDDLCompact;
    }

    @Override
    public String getConstraintDDLDrop() {
        return constraintDDLDrop;
    }

    @Override
    public void setConstraintDDLDrop(String constraintDDLDrop) {
        this.constraintDDLDrop = constraintDDLDrop;
    }

    @Override
    public String getPrimaryKeyDDL() {
        return primaryKeyDDL;
    }

    @Override
    public void setPrimaryKeyDDL(String primaryKeyDDL) {
        this.primaryKeyDDL = primaryKeyDDL;
    }

    @Override
    public String getPrimaryKeyDDLAdd() {
        return primaryKeyDDLAdd;
    }

    @Override
    public void setPrimaryKeyDDLAdd(String primaryKeyDDLAdd) {
        this.primaryKeyDDLAdd = primaryKeyDDLAdd;
    }

    @Override
    public String getPrimaryKeyCheckIntegrity() {
        return primaryKeyCheckIntegrity;
    }

    @Override
    public void setPrimaryKeyCheckIntegrity(String primaryKeyCheckIntegrity) {
        this.primaryKeyCheckIntegrity = primaryKeyCheckIntegrity;
    }

    @Override
    public String getForeignKeyDDL() {
        return foreignKeyDDL;
    }

    @Override
    public void setForeignKeyDDL(String foreignKeyDDL) {
        this.foreignKeyDDL = foreignKeyDDL;
    }

    @Override
    public String getForeignKeyDDLAdd() {
        return foreignKeyDDLAdd;
    }

    @Override
    public void setForeignKeyDDLAdd(String foreignKeyDDLAdd) {
        this.foreignKeyDDLAdd = foreignKeyDDLAdd;
    }

    @Override
    public String getForeignKeyCheckIntegrity() {
        return foreignKeyCheckIntegrity;
    }

    @Override
    public void setForeignKeyCheckIntegrity(String foreignKeyCheckIntegrity) {
        this.foreignKeyCheckIntegrity = foreignKeyCheckIntegrity;
    }

    @Override
    public String getUniqueKeyDDL() {
        return uniqueKeyDDL;
    }

    @Override
    public void setUniqueKeyDDL(String uniqueKeyDDL) {
        this.uniqueKeyDDL = uniqueKeyDDL;
    }

    @Override
    public String getUniqueKeyDDLAdd() {
        return uniqueKeyDDLAdd;
    }

    @Override
    public void setUniqueKeyDDLAdd(String uniqueKeyDDLAdd) {
        this.uniqueKeyDDLAdd = uniqueKeyDDLAdd;
    }

    @Override
    public String getUniqueKeyCheckIntegrity() {
        return uniqueKeyCheckIntegrity;
    }

    @Override
    public void setUniqueKeyCheckIntegrity(String uniqueKeyCheckIntegrity) {
        this.uniqueKeyCheckIntegrity = uniqueKeyCheckIntegrity;
    }

    @Override
    public String getCheckConstraintDDL() {
        return CheckConstraintDDL;
    }

    @Override
    public void setCheckConstraintDDL(String checkConstraintDDL) {
        CheckConstraintDDL = checkConstraintDDL;
    }

    @Override
    public String getCheckConstraintDDLAdd() {
        return CheckConstraintDDLAdd;
    }

    @Override
    public void setCheckConstraintDDLAdd(String checkConstraintDDLAdd) {
        CheckConstraintDDLAdd = checkConstraintDDLAdd;
    }

    @Override
    public String getCheckConstraintCheckIntegrity() {
        return CheckConstraintCheckIntegrity;
    }

    @Override
    public void setCheckConstraintCheckIntegrity(String checkConstraintCheckIntegrity) {
        CheckConstraintCheckIntegrity = checkConstraintCheckIntegrity;
    }

    @Override
    public String getColumnsCastDML() {
        return columnsCastDML;
    }

    @Override
    public void setColumnsCastDML(String columnsCastDML) {
        this.columnsCastDML = columnsCastDML;
    }

    @Override
    public String getMergeDML() {
        return mergeDML;
    }

    @Override
    public void setMergeDML(String mergeDML) {
        this.mergeDML = mergeDML;
    }

    @Override
    public String getInsertDML() {
        return insertDML;
    }

    @Override
    public void setInsertDML(String insertDML) {
        this.insertDML = insertDML;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        SQLTemplateDTO that = (SQLTemplateDTO) o;
        return Objects.equals(getDatabaseSchemaName(), that.getDatabaseSchemaName()) && Objects.equals(getDatabaseSchemaDDLCreate(), that.getDatabaseSchemaDDLCreate()) && Objects.equals(getDatabaseSchemaExistsSQL(), that.getDatabaseSchemaExistsSQL()) && Objects.equals(getTableFullName(), that.getTableFullName()) && Objects.equals(getTableDDLCreate(), that.getTableDDLCreate()) && Objects.equals(getTableSQLExists(), that.getTableSQLExists()) && Objects.equals(getTableDDLDrop(), that.getTableDDLDrop()) && Objects.equals(getTableDDLTruncate(), that.getTableDDLTruncate()) && Objects.equals(getTableDDLCompact(), that.getTableDDLCompact()) && Objects.equals(getColumnNonNullCheckIntegrity(), that.getColumnNonNullCheckIntegrity()) && Objects.equals(getColumnCheckIntegrity(), that.getColumnCheckIntegrity()) && Objects.equals(getPartitionDDLExchange(), that.getPartitionDDLExchange()) && Objects.equals(getPartitionDDLDrop(), that.getPartitionDDLDrop()) && Objects.equals(getPartitionDDLTruncate(), that.getPartitionDDLTruncate()) && Objects.equals(getPartitionDDLAdd(), that.getPartitionDDLAdd()) && Objects.equals(getPartitionDDLCompact(), that.getPartitionDDLCompact()) && Objects.equals(getConstraintDDLDrop(), that.getConstraintDDLDrop()) && Objects.equals(getPrimaryKeyDDL(), that.getPrimaryKeyDDL()) && Objects.equals(getPrimaryKeyDDLAdd(), that.getPrimaryKeyDDLAdd()) && Objects.equals(getPrimaryKeyCheckIntegrity(), that.getPrimaryKeyCheckIntegrity()) && Objects.equals(getForeignKeyDDL(), that.getForeignKeyDDL()) && Objects.equals(getForeignKeyDDLAdd(), that.getForeignKeyDDLAdd()) && Objects.equals(getForeignKeyCheckIntegrity(), that.getForeignKeyCheckIntegrity()) && Objects.equals(getUniqueKeyDDL(), that.getUniqueKeyDDL()) && Objects.equals(getUniqueKeyDDLAdd(), that.getUniqueKeyDDLAdd()) && Objects.equals(getUniqueKeyCheckIntegrity(), that.getUniqueKeyCheckIntegrity()) && Objects.equals(getCheckConstraintDDL(), that.getCheckConstraintDDL()) && Objects.equals(getCheckConstraintDDLAdd(), that.getCheckConstraintDDLAdd()) && Objects.equals(getCheckConstraintCheckIntegrity(), that.getCheckConstraintCheckIntegrity()) && Objects.equals(getColumnsCastDML(), that.getColumnsCastDML()) && Objects.equals(getMergeDML(), that.getMergeDML()) && Objects.equals(getInsertDML(), that.getInsertDML());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDatabaseSchemaName(), getDatabaseSchemaDDLCreate(), getDatabaseSchemaExistsSQL(), getTableFullName(), getTableDDLCreate(), getTableSQLExists(), getTableDDLDrop(), getTableDDLTruncate(), getTableDDLCompact(), getColumnNonNullCheckIntegrity(), getColumnCheckIntegrity(), getPartitionDDLExchange(), getPartitionDDLDrop(), getPartitionDDLTruncate(), getPartitionDDLAdd(), getPartitionDDLCompact(), getConstraintDDLDrop(), getPrimaryKeyDDL(), getPrimaryKeyDDLAdd(), getPrimaryKeyCheckIntegrity(), getForeignKeyDDL(), getForeignKeyDDLAdd(), getForeignKeyCheckIntegrity(), getUniqueKeyDDL(), getUniqueKeyDDLAdd(), getUniqueKeyCheckIntegrity(), getCheckConstraintDDL(), getCheckConstraintDDLAdd(), getCheckConstraintCheckIntegrity(), getColumnsCastDML(), getMergeDML(), getInsertDML());
    }
}