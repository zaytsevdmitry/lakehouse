# Шаблон SQL
Шаблон диалекта SQL(или иного командного языка).
Состоит из ключей каждый из которых является определенной способностью базы данных или движка SQL. 
Каждое значение является ссылкой на скрипт содержащий шаблон соответствующей команды.  
Не является самостоятельной конфигурацией. Входит в состав конфигураций [драйвер](drivers.md), [источник данных](datasources.md), [датасет](datasets.md) 
При использовании всех трех сущностей производится слияние шаблонов с перекрытием. 
Если искомый ключ есть в датасете, то будет взят он. Если отсутствует, то будет взят в источнике данных. Если и там отсутствует, то в драйвере.
Такой подход позволяет указать шаблоны в один раз в драйвере, а в более частных случаях перегружать значение в источнике данных или в одном частном датасете.

## Поля объекта
В качестве значения используются ключи скриптов, содержащих шаблоны для адоптации предопределенных действий в целевой системе

 Поле                                                        | Назначение шаблона                                       |
|:------------------------------------------------------------|:---------------------------------------------------------|
|databaseSchemaName| Код извлечения имени схемы бд из targetDataSet           |
|databaseSchemaDDLCreate| Создание схемы в бд                                      |
|databaseSchemaExistsSQL| Проверка существования схемы в бд                        |
|tableFullName| определение имени таблицы в формате "схема.таблица       |
|tableDDLCreate| Создание таблицы                                         |
|tableSQLExists| Проверка существования таблицы                           |
|tableDDLDrop| Удаление таблицы                                         |
|tableDDLTruncate| Сброс данных таблицы                                     |
|tableDDLCompact| Упаковка таблицы на физическом уровне                    |
|columnNonNullCheckIntegrity| Проверка соответствия конструктиву Nullable              |
|columnCheckIntegrity| Проверка соответствия конструктиву                       |
|partitionDDLExchange| Обмен партициями между таблицами (целевой и буферной)    |
|partitionDDLDrop| Удаление партиции                                        |
|partitionDDLTruncate| Сброс данных партиции                                    |
|partitionDDLAdd| Добавление партиции                                      |
|partitionDDLCompact| Упаковка партиции на физическом уровне                   |
|constraintDDLDrop| Удаление конструктива                                    |
|primaryKeyDDL| Первичный ключ для составления выражения create table    |
|primaryKeyDDLAdd| Добавление первичного ключа в существующую таблицу       |
|primaryKeyCheckIntegrity| Проверка соответствия конструктиву PK                    |
|foreignKeyDDL| Внешний  ключ для составления выражения create table     |
|foreignKeyDDLAdd| Добавление внешнего ключа в существующую таблицу         |
|foreignKeyCheckIntegrity| Проверка соответствия конструктиву FK                    |
|uniqueKeyDDL| Уникальный  ключ для составления выражения create table  |
|uniqueKeyDDLAdd| Добавление уникального ключа в существующую таблицу      |
|uniqueKeyCheckIntegrity| Проверка соответствия конструктиву UK                    |
|columnsCastDML| Приведение типа колонки                                  |
|mergeDML| Слияние модели в таблицу                                 |
|insertDML| Вставка модели в таблицу                                 |
|checkConstraintDDL| Проверочный  ключ для составления выражения create table |
|checkConstraintDDLAdd| Добавление проверочного ключа в существующую таблицу     |
|checkConstraintCheckIntegrity| Проверка соответствия конструктиву Check                 |

[код](../../../lakehouse-common/src/main/java/org/lakehouse/client/api/factory/SQLTemplateFactory.java)

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