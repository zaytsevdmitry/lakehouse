package org.lakehouse.client.api.dto.common;

public interface SQLTemplateGetter {
    String getDatabaseSchemaName();

    String getDatabaseSchemaDDLCreate();

    String getDatabaseSchemaExistsSQL();

    String getTableFullName();

    String getTableDDLCreate();

    String getTableSQLExists();

    String getTableDDLDrop();

    String getTableDDLTruncate();

    String getTableDDLCompact();

    String getColumnNonNullCheckIntegrity();

    String getColumnCheckIntegrity();

    String getPartitionDDLExchange();

    String getPartitionDDLDrop();

    String getPartitionDDLTruncate();

    String getPartitionDDLAdd();

    String getPartitionDDLCompact();

    String getConstraintDDLDrop();

    String getPrimaryKeyDDL();

    String getPrimaryKeyDDLAdd();

    String getPrimaryKeyCheckIntegrity();

    String getForeignKeyDDL();

    String getForeignKeyDDLAdd();

    String getForeignKeyCheckIntegrity();

    String getUniqueKeyDDL();

    String getUniqueKeyDDLAdd();

    String getUniqueKeyCheckIntegrity();

    String getCheckConstraintDDL();

    String getCheckConstraintDDLAdd();

    String getCheckConstraintCheckIntegrity();

    String getColumnsCastDML();

    String getMergeDML();

    String getInsertDML();
}
