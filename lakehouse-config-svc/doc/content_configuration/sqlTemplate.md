# SQL template
SQL dialect template (or another command language).
Consists of keys each of which is a certain capability of the database or SQL engine. 
Each value is a reference to a script containing the template of the corresponding command.  
It is not a standalone configuration. It is part of the [driver](drivers.md) and [task](tasks.md) configurations.
When both entities are used, the templates are merged with overlap. 
If the searched key exists in the task, it is taken. If absent, it is taken from the driver.
This approach allows specifying templates once in the driver, and overriding the value in a specific task for more specific cases.

## Object fields
As values, script keys are used that contain templates for adapting predefined actions in the target system

| Field                                                        | Template purpose                                     |
|:-------------------------------------------------------------|:-----------------------------------------------------|
|databaseSchemaName                                            | Code for extracting the database schema name from targetDataSet           |
|databaseSchemaDDLCreate                                       | Creating a schema in the database                                       |
|databaseSchemaExistsSQL                                       | Checking whether a schema exists in the database                        |
|tableFullName                                                 | Defining the table name in the "schema.table" format                   |
|tableDDLCreate                                                | Creating a table                                                        |
|tableSQLExists                                                | Checking whether a table exists                                          |
|tableDDLDrop                                                   | Dropping a table                                                        |
|tableDDLTruncate                                              | Clearing table data                                                      |
|tableDDLCompact                                               | Compacting a table at the physical level                                 |
|columnNonNullCheckIntegrity                                   | Checking compliance with the Nullable construct                          |
|columnCheckIntegrity                                          | Checking compliance with the construct                                   |
|partitionDDLExchange                                          | Exchanging partitions between tables (target and buffer)                 |
|partitionDDLDrop                                               | Dropping a partition                                                     |
|partitionDDLTruncate                                          | Clearing partition data                                                  |
|partitionDDLAdd                                               | Adding a partition                                                       |
|partitionDDLCompact                                           | Compacting a partition at the physical level                             |
|constraintDDLDrop                                              | Dropping a construct                                                     |
|primaryKeyDDL                                                  | Primary key for composing the create table expression                    |
|primaryKeyDDLAdd                                               | Adding a primary key to an existing table                                |
|primaryKeyCheckIntegrity                                       | Checking compliance with the PK construct                                 |
|foreignKeyDDL                                                  | Foreign key for composing the create table expression                    |
|foreignKeyDDLAdd                                               | Adding a foreign key to an existing table                                |
|foreignKeyCheckIntegrity                                       | Checking compliance with the FK construct                                 |
|uniqueKeyDDL                                                   | Unique key for composing the create table expression                     |
|uniqueKeyDDLAdd                                                | Adding a unique key to an existing table                                 |
|uniqueKeyCheckIntegrity                                        | Checking compliance with the UK construct                                 |
|columnsCastDML                                                 | Column type casting                                                     |
|mergeDML                                                       | Merging the model into the table                                        |
|insertDML                                                      | Inserting the model into the table                                      |
|checkConstraintDDL                                             | Check key for composing the create table expression                      |
|checkConstraintDDLAdd                                          | Adding a check key to an existing table                                  |
|checkConstraintCheckIntegrity                                  | Checking compliance with the Check construct                             |

[code](../../../lakehouse-task-executor-api/src/main/java/org/lakehouse/taskexecutor/api/factory/SQLTemplateFactory.java)

Example
````json
{
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
````
Here **databaseSchemaDDLCreate** is the functionality allowing creation of schemas in databases. A schema here means a name that groups a set of tables in a database.
**"sql-template-postgres.databaseSchemaDDLCreate.sql"** is a reference to a script containing the schema creation command template

**Example content of the script sql-template-postgres.databaseSchemaDDLCreate.sql**
```
{%set targetDataSet=dataSets[targetDataSetKeyName]%}
create schema {{ targetDataSet.databaseSchemaName }}
```