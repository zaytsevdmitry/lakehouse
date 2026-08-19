# Scenario act template
Intended for templating when scenario acts are reused. It defines what typical tasks the act should consist of, in what order they should be executed, and with what parameters.



## Object fields
| Field    | Purpose                                                                                        | 
|:---------|:-----------------------------------------------------------------------------------------------|
| keyName  | Unique identifier                                                                              |
| tasks    | List of tasks; when a template is present, overrides same-named tasks, adds the rest to the list|
| dagEdges | Directed task graph; when a template is present, overrides and embeds into the template        |



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
      "taskProcessorArgs": {
      }
    },
    {
      "name": "load",
      "taskExecutionServiceGroupName": "database",
      "taskProcessor": "jdbcTaskProcessor",
      "taskProcessorBody": "mergeSQLProcessorBody",
      "importance": "critical",
      "description": "load data",
      "taskProcessorArgs": {
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
List of scenarios
##  /v1_0/configs/scenarios/{keyName}
Manipulates a specific object by key