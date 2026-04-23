# Источники данных (datasources)
Служит для определения источников данных. Расширяет конфигурацию driver.
## Поля объекта
| Поле                                | Назначение                                                                                                                                                         |
|:------------------------------------|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| keyName                             | Уникальный идентификатов                                                                                                                                           | 
| driverKeyName                       | Указывает на конфигурацию-драйвер, чьим экземпляром является текущий источник                                                                                      |
| catalogKeyName                      | Указывает на каталог. Все датасеты связанные с источником будут определять местонахождение таблицы в указанном каталоге( если каталог поддерживается исполнителем) |
| [service](#Вложенный-объект-сервис) | Описывает параметры подключения, для создания сессии или передачи комманд                                                                                          |
| description                         | Описание для документирования                                                                                                                                      | 
| [sqlTemplate](sqlTemplate.md)       | Реализует адаптацию диалекта. Переопределяет элементы указанные в [драйвере](drivers.md)                                                                           |

## Вложенный объект сервис
| Поле       | Назначение                                              |
|:-----------|:--------------------------------------------------------|
| host       | Сетевой адрес узла источника                            | 
| port       | Сетевой адрес порта источника                           | 
| urn        | Точка подключения. Возможное имя базы данных, или иное  |
| properties | Прочие параметры подключения в виде карты ключ-значение |




**Пример**
```json
{
  "keyName": "processingdb",
  "driverKeyName":"postgres",
  "catalogKeyName":"processing",
  "service":
    {
      "host": "192.1.193.10",
      "port": "5432",
      "urn": "postgresDB",
      "properties": {
        "password": "postgresPW",
        "user": "postgresUser",
        "fetchSize": "10000",
        "spark.sql.catalog.processing": "org.apache.spark.sql.execution.datasources.v2.jdbc.JDBCTableCatalog",
        "spark.sql.catalog.processing.url": "{{driver.connectionTemplates['jdbc']}}",
        "spark.sql.catalog.processing.user": "postgresUser",
        "spark.sql.catalog.processing.password": "postgresPW",
        "spark.sql.catalog.processing.type": "hive"
      }
    },
  "description": "Remote datastore processingdb",
  "sqlTemplate" : {}
}



```

##  /v1_0/configs/datasources
GET - Выводит список источников

##  /v1_0/configs/datasources/{keyName}                                                            

Манипуляция конкретным объектом по ключу