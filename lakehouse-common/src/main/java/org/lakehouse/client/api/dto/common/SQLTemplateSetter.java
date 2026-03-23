package org.lakehouse.client.api.dto.common;

public interface SQLTemplateSetter {
void setDatabaseSchemaName(String databaseSchemaName);

void setDatabaseSchemaDDLCreate(String databaseSchemaDDLCreate);

void setDatabaseSchemaExistsSQL(String databaseSchemaExistsSQL);

void setTableFullName(String tableFullName);

void setTableDDLCreate(String tableDDLCreate);

void setTableSQLExists(String tableSQLExists);

void setTableDDLDrop(String tableDDLDrop);

void setTableDDLTruncate(String tableDDLTruncate);

void setTableDDLCompact(String tableDDLCompact);

void setColumnNonNullCheckIntegrity(String columnNonNullCheckIntegrity);

void setColumnCheckIntegrity(String columnCheckIntegrity);

void setPartitionDDLExchange(String partitionDDLExchange);

void setPartitionDDLDrop(String partitionDDLDrop);

void setPartitionDDLTruncate(String partitionDDLTruncate);

void setPartitionDDLAdd(String partitionDDLAdd);

void setPartitionDDLCompact(String partitionDDLCompact);

void setConstraintDDLDrop(String constraintDDLDrop);

void setPrimaryKeyDDL(String primaryKeyDDL);

void setPrimaryKeyDDLAdd(String primaryKeyDDLAdd);

void setPrimaryKeyCheckIntegrity(String primaryKeyCheckIntegrity);

void setForeignKeyDDL(String foreignKeyDDL);

void setForeignKeyDDLAdd(String foreignKeyDDLAdd);

void setForeignKeyCheckIntegrity(String foreignKeyCheckIntegrity);

void setUniqueKeyDDL(String uniqueKeyDDL);

void setUniqueKeyDDLAdd(String uniqueKeyDDLAdd);

void setUniqueKeyCheckIntegrity(String uniqueKeyCheckIntegrity);

void setCheckConstraintDDL(String checkConstraintDDL);

void setCheckConstraintDDLAdd(String checkConstraintDDLAdd);

void setCheckConstraintCheckIntegrity(String checkConstraintCheckIntegrity);

void setColumnsCastDML(String columnsCastDML);

void setMergeDML(String mergeDML);

void setInsertDML(String insertDML);
}
