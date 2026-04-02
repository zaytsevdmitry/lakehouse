# Драйвер (driver)
Конфигурация позволяющая адаптировать разные реализации хранилищ с подобным по отношению к друг другу функционалом.
Таким как вставка строк, создание и удаление таблиц, слияние данных и тд. Адоптация достигается путем шаблонизации. 
Предусмотрено два шаблона: шаблон строки подключения и диалекта SQL(или иного командного языка). 

 Поле                          | Назначение                                                                                                                               |
|:------------------------------|:-----------------------------------------------------------------------------------------------------------------------------------------|
| keyName                       | Уникальный идентификатов                                                                                                                 | 
| connectionTemplates           | Содержит список возможных шаблонов образования строки подключения. Поддерживаются ключи spark, jdbc                                      |0
| [sqlTemplate](sqlTemplate.md) | Реализует адаптацию диалекта.                                                                                                            |
| description                   | Описание для документирования                                                                                                            | 
| dataSourceType                | Тип источника. <br/>**database** - для баз данных,<br/> **[iceberg](https://iceberg.apache.org/)** - для таблиц в формате файлов iceberg | 

connectionTemplates применяется в механизме разрешения конфигурации. В контекст будет помещено поле [service из datasource](datasources.md#вложенный-объект-сервис)
```json
  "connectionTemplates" : {
    "jdbc" : "jdbc:postgresql://{{service.host}}:{{service.port}}/{{service.urn}}",
   "spark" : "{%set service=dataSources[dataSets[targetDataSetKeyName].dataSourceKeyName].service%}{{taskProcessorArgs.protocol}}://{{service.host}}:{{service.port}}"
},

```

**Пример**
```json
{
  "keyName" : "postgres",
  "description" : null,
  "connectionTemplates" : {
    "jdbc" : "jdbc:postgresql://{{service.host}}:{{service.port}}/{{service.urn}}"
  },
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
  },
  "dataSourceType" : "database"
}
```