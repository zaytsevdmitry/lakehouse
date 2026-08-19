# Tasks
A task is one atomic action within a set aimed at changing the dataset state, or a related action.
The task description is used in scenario templating or directly in a scenario.
It can also be saved as a standalone configuration - a task template used repeatedly.

## Object fields
| Field                        | Purpose                                                                                                                                              | 
|:-----------------------------|:-----------------------------------------------------------------------------------------------------------------------------------------------------|
| name                         | Unique name within the configuration                                                                                                                 |
| template                     | Reference to the [scenario act template](scenarioActTemplate.md); when present, same-named tasks are overridden, the rest are added                  |
| taskExecutionServiceGroupName| Reference to the target executing engine                                                                                                             |
| taskProcessor                | Class name in the executing engine                                                                                                                   |
| taskProcessorArgs            | Set of arguments that will be passed to the executing engine                                                                                         |
| taskProcessorBody            | Class name in the executing engine, in case it has a modular structure. E.g. when the logic can be reused by different systems                         |
| importance                   | Task criticality. `critical` - abort when error, `warn` - pass when error                                                                             |
| maxRetries                   | Maximum number of retries of a failed task. A positive value limits the retries (compared with the attempt number). `null`, `0` and negative values - unlimited retries |
| driverKeyName                | Points to the driver configuration whose instance is used to execute the task                                                                        |
| [sqlTemplate](sqlTemplate.md)| Implements dialect adaptation. Overrides elements specified in the [driver](drivers.md)                                                               |
| description                  | Description for documentation                                                                                                                         | 

**Fragment with a task description**
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
List of tasks (templates)
##  /v1_0/configs/tasks/{name}
Manipulates a specific task by name