# REST API

The scheduler service runs on port **8081**. All endpoints start with `/v1_0`.

## Schedules (schedule instances)

### List schedules
GET request returning a JSON body
```
http://localhost:8081/v1_0/schedule
```
```bash
curl -X GET http://localhost:8081/v1_0/schedule |jq
```
> |jq does not affect how the command works. The example is given to demonstrate the output with JSON formatting.

Example output
```json
[
  {
    "id": 1,
    "configScheduleKeyName": "regular",
    "targetExecutionDateTime": "2025-01-02T00:00:00Z",
    "status": "RUNNING"
  },
  {
    "id": 2,
    "configScheduleKeyName": "generateSourceDict",
    "targetExecutionDateTime": "2025-01-02T00:00:00Z",
    "status": "RUNNING"
  },
  {
    "id": 3,
    "configScheduleKeyName": "initial",
    "targetExecutionDateTime": "2025-02-01T00:00:00Z",
    "status": "RUNNING"
  },
  {
    "id": 4,
    "configScheduleKeyName": "generateSource",
    "targetExecutionDateTime": "2025-01-02T00:00:00Z",
    "status": "RUNNING"
  }
]
```

### List schedules within a given time interval
GET request with a JSON body `IntervalDTO` and the optional `name` parameter
```
http://localhost:8081/v1_0/schedule
```
```bash
curl -X GET http://localhost:8081/v1_0/schedule \
     -H "Content-Type: application/json" \
     -d '{"intervalStartDateTime":"2025-01-01T00:00:00Z","intervalEndDateTime":"2025-02-01T00:00:00Z"}' |jq
```
Without the `name` parameter all schedules falling within the interval are returned. With `name` set, only the schedules
of the specified configuration within the given interval.

### List schedules by configuration name with a limit
```
http://localhost:8081/v1_0/schedule/name={name}/limit={limit}
```
```bash
curl -X GET "http://localhost:8081/v1_0/schedule/name=regular/limit=10" |jq
```
Returns the latest `limit` schedules of the configuration named `name`, sorted by execution date in descending order.

### Get schedule DAG
Full structure of a schedule instance with the scenario graph and the task graph
```
http://localhost:8081/v1_0/schedule/dag/id={id}
```
```bash
curl -X GET http://localhost:8081/v1_0/schedule/dag/id=1 |jq
```
Returned object [ScheduleInstanceDAGDTO.java](../../lakehouse-common/src/main/java/org/lakehouse/client/api/dto/scheduler/ScheduleInstanceDAGDTO.java):

| Field                  | Purpose                                            |
|:-----------------------|:---------------------------------------------------|
| id                     | Schedule identifier                                |
| configScheduleKeyName  | Schedule configuration key                         |
| targetExecutionDateTime | Target execution date                             |
| status                 | Schedule status                                    |
| scenarioActs           | List of scenario (act) instances with their tasks  |
| scenarioActEdges       | Scenario dependency graph (DagEdgeDTO)             |

### Delete schedule
For example, suppose we need to delete the schedule `generateSourceDict` from the previous example. Its id=2.
Then the deletion request will look like:

```shell
curl -X DELETE http://localhost:8081/v1_0/schedule/id=2
```
HTTP CODE 200 means successful deletion.

If a configuration exists, the scheduler will create a new schedule to replace the deleted one.

## Tasks (scheduled tasks)

### List all tasks
```
http://localhost:8081/v1_0/tasks/scheduledtasks
```
```bash
curl -X GET http://localhost:8081/v1_0/tasks/scheduledtasks |jq
```
Returned object [ScheduledTaskDTO.java](../../lakehouse-common/src/main/java/org/lakehouse/client/api/dto/scheduler/tasks/ScheduledTaskDTO.java):

| Field                | Purpose                                          |
|:---------------------|:-------------------------------------------------|
| id                   | Task identifier                                  |
| name                 | Task name                                        |
| scheduleKeyName      | Schedule configuration key                       |
| scenarioActKeyName   | Scenario (act) key                               |
| dataSetKeyName       | Key of the dataset being served                  |
| status               | Task status                                      |
| targetDateTime       | Target execution date                            |
| intervalStartDateTime | Lower bound of the time window                   |
| intervalEndDateTime  | Upper bound of the time window                   |
| tryNum               | Execution attempt number                         |

### Get task by id
```
http://localhost:8081/v1_0/tasks/scheduledtasks/{id}
```
```bash
curl -X GET http://localhost:8081/v1_0/tasks/scheduledtasks/10 |jq
```

## Task locks

### Get lock by lock id
```
http://localhost:8081/v1_0/tasks/scheduledtasks/lock/{id}
```
```bash
curl -X GET http://localhost:8081/v1_0/tasks/scheduledtasks/lock/3 |jq
```
Returned object [ScheduledTaskLockDTO.java](../../lakehouse-common/src/main/java/org/lakehouse/client/api/dto/scheduler/lock/ScheduledTaskLockDTO.java):

| Field                     | Purpose                                |
|:--------------------------|:---------------------------------------|
| lockId                    | Lock identifier                        |
| scheduledTaskEffectiveDTO| Effective task configuration           |
| lastHeartBeatDateTime    | Time of the last heartbeat             |
| serviceId                | Identifier of the executor that took the task |

### Take a task by an executor
An executor takes a task into work by task id, providing its serviceId
```
http://localhost:8081/v1_0/tasks/scheduledtasks/lock/taskId/{id}/service/{serviceId}
```
```bash
curl -X GET "http://localhost:8081/v1_0/tasks/scheduledtasks/lock/taskId/10/service/task-executor-1" |jq
```
Returns a lock object. The task moves to RUNNING status.

### List all locks
```
http://localhost:8081/v1_0/tasks/scheduledtasks/locks
```
```bash
curl -X GET http://localhost:8081/v1_0/tasks/scheduledtasks/locks |jq
```

### Heartbeat
An executor notifies the scheduler that the task is still running
```
http://localhost:8081/v1_0/tasks/scheduledtasks/lock/heartbeat
```
```bash
curl -X PUT http://localhost:8081/v1_0/tasks/scheduledtasks/lock/heartbeat \
     -H "Content-Type: application/json" \
     -d '{"lockId": 3}'
```
Request body: [TaskExecutionHeartBeatDTO.java](../../lakehouse-common/src/main/java/org/lakehouse/client/api/dto/scheduler/lock/TaskExecutionHeartBeatDTO.java) (the `lockId` field).

### Release a lock (task completion)
An executor finishes the task and passes the result
```
http://localhost:8081/v1_0/tasks/scheduledtasks/release
```
```bash
curl -X PUT http://localhost:8081/v1_0/tasks/scheduledtasks/release \
     -H "Content-Type: application/json" \
     -d '{"lockId": 3, "taskResult": {"status": "SUCCESS", "causes": null}}'
```
Request body: [TaskInstanceReleaseDTO.java](../../lakehouse-common/src/main/java/org/lakehouse/client/api/dto/scheduler/lock/TaskInstanceReleaseDTO.java)
with the `lockId` and `taskResult` fields ([TaskResultDTO.java](../../lakehouse-common/src/main/java/org/lakehouse/client/api/dto/scheduler/lock/TaskResultDTO.java) with the SUCCESS, FAILED, CONF_ERROR statuses).
The task moves to a final status and the lock is removed.