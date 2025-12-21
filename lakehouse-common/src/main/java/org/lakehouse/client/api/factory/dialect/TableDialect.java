package org.lakehouse.client.api.factory.dialect;

import java.util.List;

public interface TableDialect {

    String getPrimaryKeyDDL();
    String getPrimaryKeyDDLAdd();
    String getPrimaryKeyDDLDrop();
    List<String> getAllConstraintsDDL();
    List<String> getForeignKeysDDL();
    List<String> getForeignKeysDDLAdd();
    List<String> getForeignKeysDDLDrop();
    List<String> getUniqueKeysDDL();
    List<String> getUniqueKeysDDLAdd();
    List<String> getUniqueKeysDDLDrop();
    List<String> getCheckKeysDDL();
    List<String> getCheckKeysDDLAdd();
    List<String> getCheckKeysDDLDrop();
    List<String> getAllConstraintsDDLAdd();
    List<String> getAllConstraintsDDLDrop();
    String getMergeOnDML();
    String getColumnsUpdateSetDML();
    String getColumnsMergeInsertValuesDML();
    String getColumnsComaSeparated();
    String getColumnsDDL();
    String getColumnsCastDML();
    String getTableDDL();
    String getDatabaseSchemaName();
    String getTableName();
}
