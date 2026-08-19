# Scaling the task-executor-service
As a rule, all tasks executed by the task-executor-service must be lightweight.
The main task of the service is to offload responsibility from the scheduler-service. The task-executor-service is the point of parallelization of task execution.
It receives a job, adapts it for the execution environment and passes it to that environment, waiting for the result. This is not a heavy operation, but a blocking one.

## Important parameters for managing scaling
in the service instance configuration
- lakehouse.task-executor.scheduled.task.kafka.consumer.concurrency
- lakehouse.task-executor.scheduled.task.kafka.consumer.properties.group.id

```yaml
server:
  port: 8089
lakehouse:
  client:
    rest:
      state:
        server:
          url: http://127.0.0.1:8082
      config:
        server:
          url: http://127.0.0.1:8080
      scheduler:
        server:
          url: http://127.0.0.1:8081  
  task-executor:
    service:
      heart-beat-initial-delaY-ms: 5000
      heart-beat-interval-ms: 5000 
      max-lock-retries: 5
      max-lock-retries-duration-ms: 5
      id: first1  #<--- Name identifying the instance within the group. Each instance needs its own name
    scheduled:
      task:
        kafka:
          consumer:
            concurrency: 1 #<--- Number of consumption threads
            properties:
              bootstrap.servers: 192.1.193.20:9092 #<--- broker from which we receive jobs
              group.id: default #<--- Usually used to fix received offsets. The service also filters by this value which messages to execute
              auto.offset.reset: earliest 
            topics: scheduled_task_msg #<--- topic from which we receive jobs
```
in the task configuration
- taskExecutionServiceGroupName: default 

```json
{
      "name": "compact",
  
  --> "taskExecutionServiceGroupName": "default",

      "taskProcessor": "k8sSparkNativeTaskProcessor",
      "taskProcessorBody": "compactTableSQLProcessorBody",
      "importance": "critical",
      "description": "load from remote datastore",
      "taskProcessorArgs": {
        "spark.ui.enabled": "true",
        "spark.executor.memory": "1g",
        "spark.driver.memory": "1g",
        "datasource.service.protocol": "https",
        "lakehouse.client.rest.config.server.url": "http://lakehouse-management-config-service:8080",
        "k8s.spark-native.mainClass": "org.lakehouse.taskexecutor.spark.dataset.SparkProcessorApplication",
        "k8s.spark-native.appResource": "local:///opt/lakehouse-task-spark-apps/lakehouse-task-executor-spark-dataset-app-0.5.0-jar-with-dependencies.jar"
      }
    }
```
> for the service instance to take a task, the parameter values must match 
 lakehouse.task-executor.scheduled.task.kafka.consumer.properties.group.id
 and
 taskExecutionServiceGroupName
otherwise the task will be ignored, it is expected to be addressed to another task-executor-service

For example

Taken for execution:
 - lakehouse.task-executor.scheduled.task.kafka.consumer.properties.group.id = default and taskExecutionServiceGroupName = default

Not taken for execution:
 - lakehouse.task-executor.scheduled.task.kafka.consumer.properties.group.id = black and taskExecutionServiceGroupName = white

## Vertical scaling (Scale Up)
Given the lightweight nature of tasks, the first thing that can be done to increase throughput is to increase the number of task consumers
>lakehouse.task-executor.scheduled.task.kafka.consumer.properties=1

where 1 is the default value, meaning work in 1 task consumption thread. Increasing this number can achieve the required throughput

> Despite the expected lightness, you need to understand that the main memory consumers are *TaskProcessor; the code implemented in them must not inflate memory or allow leaks. You need to monitor the consumption of allocated memory and increase it

## Horizontal scaling (Scale Out)
Achieved by launching additional instances of the service. These can be JVMs running in parallel on one or multiple hosts.
in the case of k8s these can be replicas.

## Segment-based scaling.
Tasks of different natures can have stably different blocking times. For example:

|Processor group | work time | Configuration | Description|
|-|---------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----|----------------------------------------------------------------------------------------------------|
|dataset state model support processor group| milliseconds | <br>lakehouse.task-executor.scheduled.task.kafka.consumer.concurrency=1</br><br>lakehouse.task-executor.scheduled.task.kafka.consumer.properties.group.id = state</br><br>taskExecutionServiceGroupName = state</br>  | in essence a mini rest client that in an instant gets or reports a status to the state service and returns with the result. 
| Spark processors| tens of minutes | <br>lakehouse.task-executor.scheduled.task.kafka.consumer.concurrency=20</br><br>lakehouse.task-executor.scheduled.task.kafka.consumer.properties.group.id = spark</br><br>taskExecutionServiceGroupName = spark</br> |A task is created in the cluster, something is read and joined, then written                                                                                                                        |