# Датасет (dataset)
Абстракция для определения объекта данных. 

## Поля объекта
 Поле                                                        | Назначение                                                                                                                   |
|:------------------------------------------------------------|:-----------------------------------------------------------------------------------------------------------------------------|
| keyName                                                     | Уникальный идентификатов                                                                                                     | 
| nameSpaceKeyName                                            | Принадлежность к [пространству имен](namespaces.md)                                                                          |
| dataSourceKeyName                                           | Указывает на [источник данных](datasources.md) в котором находится датасет                                                   |
| databaseSchemaName                                          | Название схемы в которой расположена таблица                                                                                 |
| tableName                                                   | Название таблицы                                                                                                             |
| description                                                 | Описание для документирования                                                                                                | 
| [scripts](scriptsReference.md#коллекция-скриптов)           | Список ссылок на скрипты, которые используются как фрагменты для построения модели датасета                                  | 
| [sqlTemplate](sqlTemplate.md)                               | Реализует адаптацию диалекта. Переопределяет элементы указанные в [драйвере](drivers.md) и [источнике данных](datasource.md) |
| [sources](#sources)                                         | Карта - ссылка на другой датасет.В качестве ключа имя датасета от которого зависит                                           |
| [columnSchema](#columnSchema)                               | Вложенный список описаний колонок                                                          |
| [constraints](#constraints)                                 | Вложенный список/карта конструктивов таблиц, где ключ имя конструктива, значение описание настроек                           |
| properties                                                  | Вложенный карта ключ-значение дополнительных параметров                                                                      |

## sources
Вложенный список зависимых датасетов

 Поле       | Назначение                                                                   |
|:-----------|:-----------------------------------------------------------------------------|
| properties | Набор свойств датасета - зависимости, которые переопределяют его собственные | 

## columnSchema
Вложенный список описаний колонок

 Поле        | Назначение                                                                                                                          |
|:------------|:------------------------------------------------------------------------------------------------------------------------------------|
| name        | Имя колонки в таблице хранилища                                                                                                     | 
| description | Описание для документирования                                                                                                       
| dataType    | Тип данных                                                                                                                          |
| nullable    | Допускается пустое значение. true/false                                                                                             |
| order       | Порядковый номер расположения колонки. Необязательное. Применимость поля зависит от исполняющего механизма и возможностей хранилища |

## constraints

Вложенный список конструктивов таблицы

**Пример**

```json
{
  "keyName": "transaction_dds",
  "nameSpaceKeyName": "DEMO",
  "dataSourceKeyName": "lakehousestorage",
  "databaseSchemaName": "default",
  "tableName": "transaction_dds",
  "description": "Details",
  "scripts": [
    {
      "key": "dataset-sql-model.transaction_dds.sql"
    }
  ],
  "sources": {
    "client_processing": {
      "properties": {
        "fetchSize": "10000"
      }
    },
    "transaction_processing": {
      "properties": {
        "fetchSize": "10000"
      }
    }
  },
  "columnSchema": [
    {
      "name": "client_name",
      "description": "Client name",
      "dataType": "string",
      "nullable": false
    },
    {
      "name": "id",
      "description": "tx id",
      "dataType": "bigint",
      "nullable": false,
      "order": 0
    },
    {
      "name": "reg_date_time",
      "description": "Transaction registration",
      "dataType": "timestamp",
      "nullable": false
    },
    {
      "name": "client_id",
      "description": "from client",
      "dataType": "string",
      "nullable": false
    },
    {
      "name": "provider_id",
      "description": "To provider",
      "dataType": "string",
      "nullable": false
    },
    {
      "name": "amount",
      "description": "Amount paid by the client",
      "dataType": "decimal",
      "nullable": false
    },
    {
      "name": "commission",
      "description": "Commission due to us",
      "dataType": "string",
      "nullable": false
    }
  ],
  "constraints": {
    "transaction_dds_pk": {
      "type": "primary",
      "columns": "id",
      "constraintLevelCheck": "dataQuality"
    }
  },
  "properties": {
   
  },
  "sqlTemplate" : {}
  
}
```

##  /v1_0/configs/datasets
GET - Выводит полный список конфигураций датасетов
POST - Примет датасет в body для сохранения
```shell
curl -X GET http://localhost:8080/v1_0/configs/datasets  |jq
```
GET - Вернет датасет в body
DELETE - удалит указанный датасет (при отсутствии зависимостей)
##  /v1_0/configs/datasets/{keyName}

```shell
curl -X GET http://localhost:8080/v1_0/configs/datasets/transaction_dds  |jq
```
