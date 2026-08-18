# Задачи (tasks)
Задача это одно атомарное действие в составе множества, направленных на изменение состояния датасета, или сопутствующие действия.
Описание задачи применяется в шаблонизации сценариев либо непосредственно в сценарии.
Также может сохраняться как самостоятельная конфигурация - шаблон задачи, используемый повторно.

## Поля объекта
|  Поле                         | Назначение                                                                                                                                              | 
|:------------------------------|:--------------------------------------------------------------------------------------------------------------------------------------------------------|
| name                          | Уникальное имя внутри конфигурации                                                                                                                      |
| template                      | Ссылка на [шаблон действия сценария](scenarioActTemplate.md), при наличии которого одноименные задачи переопределяются, остальные добавляются             |
| taskExecutionServiceGroupName | Ссылка на целевой исполняющий механизм                                                                                                                  |
| taskProcessor                 | Имя класса в исполняющем механизме                                                                                                                      |
| taskProcessorArgs             | Набор аргументов которые будут переданы в исполняющий механизм                                                                                          |
| taskProcessorBody             | Имя класса в исполняющем механизме, в случае если он имеет модульную структуру. Например если логика может быть повторно использована разными системами |
| importance                    | Критичность задачи. `critical` - abort when error, `warn` - pass when error                                                                              |
| maxRetries                    | Максимальное число повторов неуспешной задачи. Положительное значение ограничивает повторы (сравнивается с числом попыток). `null`, `0` и отрицательные значения - бесконечные повторы |
| driverKeyName                 | Указывает на конфигурацию-драйвер, чей экземпляр используется для выполнения задачи                                                                     |
| [sqlTemplate](sqlTemplate.md) | Реализует адаптацию диалекта. Переопределяет элементы указанные в [драйвере](drivers.md)                                                                 |
| description                   | Описание для документирования                                                                                                                           | 

**Фрагмент с описанием задачи**
```json

{
      "name": "load",
      "taskExecutionServiceGroupName": "spark-cluster",
      "taskProcessor": "sparkStandAloneClusterTaskProcessor",
      "taskProcessorBody": "mergeSQLProcessorBody",
      "importance": "critical",
      "maxRetries": 2,
      "description": "load from remote datastore",
      "taskProcessorArgs": {
        "spark.ui.enabled": "true",
        "spark.executor.memory": "1g",
        "spark.driver.memory": "1g",
        "protocol": "http",
        "lakehouse.client.rest.config.server.url": "http://192.1.193.80:8080",
        "deploy.mainClass": "org.lakehouse.taskexecutor.spark.dataset.SparkProcessorApplication",
        "deploy.appResource": "/opt/lakehouse-task-spark-apps/lakehouse-task-executor-spark-dataset-app-0.5.0-jar-with-dependencies.jar"
      }
    }
```

##  /v1_0/configs/tasks
Список задач (шаблонов)
##  /v1_0/configs/tasks/{name}
Манипуляция конкретной задачей по имени
     