# Драйвер (driver)
Конфигурация позволяющая адаптировать разные реализации хранилищ с подобным по отношению к друг другу функционалом.
Таким как вставка строк, создание и удаление таблиц, слияние данных и тд. Адоптация достигается путем шаблонизации диалекта SQL(или иного командного языка). 
## Поля объекта
 Поле                          | Назначение                                                                                                                               |
|:------------------------------|:-----------------------------------------------------------------------------------------------------------------------------------------|
| keyName                       | Уникальный идентификатов                                                                                                                 | 
| [sqlTemplate](sqlTemplate.md) | Реализует адаптацию диалекта.                                                                                                            |
| description                   | Описание для документирования                                                                                                            | 

Драйвер задает базовый шаблон диалекта, который переопределяется на более частных уровнях ([источник данных](datasources.md), [задача](tasks.md)).
Ссылка на драйвер производится в [задаче](tasks.md) через поле driverKeyName.

**Пример**
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
Выводит список объектов
##  /v1_0/configs/drivers/{keyName}
Манипуляция конкретным объектом по ключу