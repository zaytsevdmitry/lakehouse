# Шаблон SQL
Шаблон диалекта SQL(или иного командного языка).
Состоит из ключей каждый из которых является определенной способностью базы данных или движка SQL. 
Каждое значение является ссылкой на скрипт содержащий шаблон соответствующей команды.  
Не является самостоятельной конфигурацией. Входит в состав конфигураций [драйвер](./drivers.md), [источник данных](datasources.md), [датасет](datasets.md) 
При использовании всех трех сущностей производится слияние шаблонов с перекрытием. 
Если искомый ключ есть в датасете, то будет взят он. Если отсутствует, то будет взят в источнике данных. Если и там отсутствует, то в драйвере.
Такой подход позволяет указать шаблоны в один раз в драйвере, а в более частных случаях перегружать значение в источнике данных или в одном частном датасете.

[код](../../lakehouse-common/src/main/java/org/lakehouse/client/api/factory/SQLTemplateFactory.java)

Пример
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
Здесь **databaseSchemaDDLCreate** функциональность позволяющая создавать схемы в базах данных. Под схемой здесь понимается имя которое, объединяет множество таблиц в базе данных.
**"sql-template-postgres.databaseSchemaDDLCreate.sql"** это ссылка на скрипт содержащий шаблон команды создания схемы

**Пример содержимого скрипта sql-template-postgres.databaseSchemaDDLCreate.sql**
```
{%set targetDataSet=dataSets[targetDataSetKeyName]%}
create schema {{ targetDataSet.databaseSchemaName }}
```