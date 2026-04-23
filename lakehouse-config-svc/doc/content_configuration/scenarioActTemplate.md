# Шаблон действия сценария
Предназначен для шаблонизации при повторном использовании действий сценария. В нем можно определить из каких типовых задач должено состоять действие, в каком порядке они должны быть выполнены, с какими параметрами.



## Поля объекта
 Поле                                           | Назначение                                                                                        | 
|:-----------------------------------------------|:--------------------------------------------------------------------------------------------------|
| keyName                                        | Уникальный идентификатор                                                                          |
| tasks                                          | Список задач, при наличии шаблона переопределяет одноименные задачи, остальные добавляет в список |
| dagEdges                                       | Направленный граф задач, при наличии шаблон , переопределяет и встраивается в шаблон              |



```json
{
  "keyName": "database",
  "description": "Database scenario",
  "tasks": [
    {
      "name": "begin",
      "taskExecutionServiceGroupName": "state-service",
      "taskProcessor": "lockedStateTaskProcessor",
      "importance": "critical",
      "description": "Made dataset interval status Locked"
    },
    {
      "name": "prepare",
      "taskExecutionServiceGroupName": "database",
      "taskProcessor": "jdbcTaskProcessor",
      "taskProcessorBody": "createTableSQLProcessorBody",
      "importance": "critical",
      "description": "Create table if not exists",
      "executionModuleArgs": {
      }
    },
    {
      "name": "load",
      "taskExecutionServiceGroupName": "database",
      "taskProcessor": "jdbcTaskProcessor",
      "taskProcessorBody": "mergeSQLProcessorBody",
      "importance": "critical",
      "description": "load data",
      "executionModuleArgs": {
      }
    },
    {
      "name": "finally",
      "taskExecutionServiceGroupName": "state-service",
      "taskProcessor": "successStateTaskProcessor",
      "importance": "critical",
      "description": "Made dataset interval status SUCCESS"
    }
  ],
  "dagEdges": [
    {
      "from": "begin",
      "to": "prepare"
    },
    {
      "from": "prepare",
      "to": "load"
    },
    {
      "from": "load",
      "to": "finally"
    }
  ]
}

```


##  /v1_0/configs/scenarios
Список сценариев
##  /v1_0/configs/scenarios/{keyName}
Манипуляция конкретным объектом по ключу