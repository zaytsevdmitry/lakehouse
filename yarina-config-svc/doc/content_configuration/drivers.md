# Driver
Configuration that adapts different storage implementations with similar functionality relative to each other.
Such as inserting rows, creating and dropping tables, merging data, etc. Adaptation is achieved by templating the SQL dialect (or another command language). 
## Object fields
| Field                             | Purpose                                                                                                                               |
|:----------------------------------|:--------------------------------------------------------------------------------------------------------------------------------------|
| keyName                           | Unique identifier                                                                                                                     | 
| [sqlTemplate](sqlTemplate.md)     | Implements dialect adaptation.                                                                                                        |
| description                       | Description for documentation                                                                                                         | 

The driver defines the base dialect template, which is overridden at more specific levels ([data source](datasources.md), [task](tasks.md)).
A reference to the driver is made in a [task](tasks.md) via the driverKeyName field.

**Example**
```json
{
  "keyName" : "postgres",
  "description" : null,
  "sqlTemplate" : {
    "databaseSchemaName" : "sql-template-postgres.databaseSchemaName.sql",
    "databaseSchemaDDLCreate" : "sql-template-postgres.databaseSchemaDDLCreate.sql",
    "databaseSchemaExistsSQL" : "sql-template-postgres.databaseSchemaExistsSQL.sql",
    "tableFullName" : "sql-template-postgres.tableFullName.sql",
    "tableDDLCreate" : "sql-template-postgres.tableDDLCreate.sql",
    "tableSQLExists" : "sql-template-postgres.tableSQLExists.sql",
    "tableDDLDrop" : "sql-template-postgres.tableDDLDrop.sql",
    "tableDDLTruncate" : "sql-template-postgres.tableDDLTruncate.sql",
    "tableDDLCompact" : "sql-template-postgres.tableDDLCompact.sql",
    "columnNonNullCheckIntegrity" : "sql-template-postgres.columnNonNullCheckIntegrity.sql",
    "columnCheckIntegrity" : "sql-template-postgres.columnCheckIntegrity.sql",
    "partitionDDLExchange" : "sql-template-postgres.partitionDDLExchange.sql",
    "partitionDDLDrop" : "sql-template-postgres.partitionDDLDrop.sql",
    "partitionDDLTruncate" : "sql-template-postgres.partitionDDLTruncate.sql",
    "partitionDDLAdd" : "sql-template-postgres.partitionDDLAdd.sql",
    "partitionDDLCompact" : "sql-template-postgres.partitionDDLCompact.sql",
    "constraintDDLDrop" : "sql-template-postgres.constraintDDLDrop.sql",
    "primaryKeyDDL" : "sql-template-postgres.primaryKeyDDL.sql",
    "primaryKeyDDLAdd" : "sql-template-postgres.primaryKeyDDLAdd.sql",
    "primaryKeyCheckIntegrity" : "sql-template-postgres.primaryKeyCheckIntegrity.sql",
    "foreignKeyDDL" : "sql-template-postgres.foreignKeyDDL.sql",
    "foreignKeyDDLAdd" : "sql-template-postgres.foreignKeyDDLAdd.sql",
    "foreignKeyCheckIntegrity" : "sql-template-postgres.foreignKeyCheckIntegrity.sql",
    "uniqueKeyDDL" : "sql-template-postgres.uniqueKeyDDL.sql",
    "uniqueKeyDDLAdd" : "sql-template-postgres.uniqueKeyDDLAdd.sql",
    "uniqueKeyCheckIntegrity" : "sql-template-postgres.uniqueKeyCheckIntegrity.sql",
    "columnsCastDML" : "sql-template-postgres.columnsCastDML.sql",
    "mergeDML" : "sql-template-postgres.mergeDML.sql",
    "insertDML" : "sql-template-postgres.insertDML.sql",
    "checkConstraintDDL" : "sql-template-postgres.checkConstraintDDL.sql",
    "checkConstraintDDLAdd" : "sql-template-postgres.checkConstraintDDLAdd.sql",
    "checkConstraintCheckIntegrity" : "sql-template-postgres.checkConstraintCheckIntegrity.sql"
  }
}
```


##  /v1_0/configs/drivers
Returns a list of objects
##  /v1_0/configs/drivers/{keyName}
Manipulates a specific object by key