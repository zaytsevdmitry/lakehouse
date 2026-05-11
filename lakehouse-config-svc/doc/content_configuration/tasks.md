# Задачи (tasks)
Задача это одно атомарное действие в составе множества, направленных на изменение состояния датасета, или сопутствующие действия.
Описание задачи применяется в шаблонизации сценариев либо непосредственно в сценарии
Не используется как самостоятельный объект

## Поля объекта
 Поле                            | Назначение                                                                                                                                        | 
|:--------------------------------|:--------------------------------------------------------------------------------------------------------------------------------------------------|
| name                            | Уникальное имя внутри конфигурации                                                                                                                |
| taskExecutionServiceGroupName   | Ссылка на целевой исполняющий механизм                                                                                                            |
| taskProcessor                   | Имя класса в исполняющем механизме                                                                                                                |
| taskProcessorArgs               | Набор аргументов которые будут переданы в исполняющий механизм                                                                                    |
| taskProcessorBody               | Имя класса в исполняющем механизме, в случае если он имеет модульную структуру. Напимер если логика может быть переиспользована разными системами |
| description                     | Описание для документирования                                                                                                                     | 
| importance                      | todo                                                                                                                                              |

**Фрагмент с описанием задачи**
```json

{
      "name": "load",
      "taskExecutionServiceGroupName": "spark-cluster",
      "taskProcessor": "sparkLauncherTaskProcessor",
      "taskProcessorBody": "mergeSQLProcessorBody",
      "importance": "critical",
      "description": "load from remote datastore",
      "taskProcessorArgs": {
        "spark.ui.enabled": "true",
        "spark.executor.memory": "1g",
        "spark.driver.memory": "1g",
        "protocol": "http",
        "lakehouse.client.rest.config.server.url": "http://192.1.193.80:8080",
        "deploy.mainClass": "org.lakehouse.taskexecutor.spark.dataset.SparkProcessorApplication",
        "deploy.appResource": "/opt/lakehouse-task-spark-apps/lakehouse-task-executor-spark-dataset-app-0.4.0-jar-with-dependencies.jar"
      }
    }
```
     