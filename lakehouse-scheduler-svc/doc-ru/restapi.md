# REST API

Сервис шедулера работает на порту **8081**. Все эндпоинты начинаются с `/v1_0`.

## Расписания (schedule instances)

### Вывод списка расписаний
Метод запроса GET в возвратом body JSON
```
http://localhost:8081/v1_0/schedule
```
```bash
curl -X GET http://localhost:8081/v1_0/schedule |jq
```
> |jq не влияет на работу команды. Пример приведен для демонстрации вывода с форматированием JSON.

Пример вывода
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

### Вывод списка расписаний в заданном интервале времени
Метод запроса GET c телом JSON `IntervalDTO` и необязательным параметром `name`
```
http://localhost:8081/v1_0/schedule
```
```bash
curl -X GET http://localhost:8081/v1_0/schedule \
     -H "Content-Type: application/json" \
     -d '{"intervalStartDateTime":"2025-01-01T00:00:00Z","intervalEndDateTime":"2025-02-01T00:00:00Z"}' |jq
```
Без параметра `name` вернутся все расписания, попавшие в интервал. При заданном `name` - только расписания
указанной конфигурации в заданном интервале.

### Вывод списка расписаний по имени конфигурации с ограничением
```
http://localhost:8081/v1_0/schedule/name={name}/limit={limit}
```
```bash
curl -X GET "http://localhost:8081/v1_0/schedule/name=regular/limit=10" |jq
```
Вернет последние `limit` расписаний конфигурации с именем `name`, отсортированные по дате выполнения по убыванию.

### Вывод DAG расписания
Полная структура экземпляра расписания с графом сценариев и графом задач
```
http://localhost:8081/v1_0/schedule/dag/id={id}
```
```bash
curl -X GET http://localhost:8081/v1_0/schedule/dag/id=1 |jq
```
Возвращаемый объект [ScheduleInstanceDAGDTO.java](../../lakehouse-common/src/main/java/org/lakehouse/client/api/dto/scheduler/ScheduleInstanceDAGDTO.java):

| Поле                    | Назначение                                                   | 
|:------------------------|:-------------------------------------------------------------|
| id                      | Идентификатор расписания                                     |
| configScheduleKeyName   | Ключ конфигурации расписания                            |
| targetExecutionDateTime | Целевая дата выполнения                                |
| status                  | Статус расписания                                            |
| scenarioActs            | Список экземпляров сценариев (актов) с их задачами           |
| scenarioActEdges        | Граф зависимостей сценариев (DagEdgeDTO)                     |

### Удаление расписания
На пример требуется удалить расписание из предыдущего примера generateSourceDict.  Его id=2.
Тогда строка для удаления будет выглядеть так:

```shell
curl -X DELETE http://localhost:8081/v1_0/schedule/id=2
```
HTTP CODE 200 означает успешное удаление

Шедулер создаст новое расписание взамен удаленного, при наличии конфигурации.

## Задачи (scheduled tasks)

### Список всех задач
```
http://localhost:8081/v1_0/tasks/scheduledtasks
```
```bash
curl -X GET http://localhost:8081/v1_0/tasks/scheduledtasks |jq
```
Возвращаемый объект [ScheduledTaskDTO.java](../../lakehouse-common/src/main/java/org/lakehouse/client/api/dto/scheduler/tasks/ScheduledTaskDTO.java):

| Поле                | Назначение                                              |
|:--------------------|:--------------------------------------------------------|
| id                  | Идентификатор задачи                                    |
| name                | Имя задачи                                              |
| scheduleKeyName     | Ключ конфигурации расписания                            |
| scenarioActKeyName  | Ключ сценария (акта)                                    |
| dataSetKeyName      | Ключ обслуживаемого датасета                            |
| status              | Статус задачи                                           |
| targetDateTime      | Целевая дата выполнения                                 |
| intervalStartDateTime | Нижняя граница окна времени                           |
| intervalEndDateTime | Верхняя граница окна времени                            |
| tryNum              | Номер попытки выполнения                                |

### Получение задачи по id
```
http://localhost:8081/v1_0/tasks/scheduledtasks/{id}
```
```bash
curl -X GET http://localhost:8081/v1_0/tasks/scheduledtasks/10 |jq
```

## Блокировки задач (locks)

### Получение блокировки по id блокировки
```
http://localhost:8081/v1_0/tasks/scheduledtasks/lock/{id}
```
```bash
curl -X GET http://localhost:8081/v1_0/tasks/scheduledtasks/lock/3 |jq
```
Возвращаемый объект [ScheduledTaskLockDTO.java](../../lakehouse-common/src/main/java/org/lakehouse/client/api/dto/scheduler/lock/ScheduledTaskLockDTO.java):

| Поле                     | Назначение                              |
|:-------------------------|:----------------------------------------|
| lockId                   | Идентификатор блокировки                |
| scheduledTaskEffectiveDTO| Эффективная конфигурация задачи         |
| lastHeartBeatDateTime    | Время последнего heartbeat              |
| serviceId                | Идентификатор исполнителя, взявшего задачу |

### Взятие задачи исполнителем
Исполнитель забирает задачу в работу по id задачи, указывая свой serviceId
```
http://localhost:8081/v1_0/tasks/scheduledtasks/lock/taskId/{id}/service/{serviceId}
```
```bash
curl -X GET "http://localhost:8081/v1_0/tasks/scheduledtasks/lock/taskId/10/service/task-executor-1" |jq
```
Возвращает объект блокировки. Задача переходит в статус RUNNING.

### Список всех блокировок
```
http://localhost:8081/v1_0/tasks/scheduledtasks/locks
```
```bash
curl -X GET http://localhost:8081/v1_0/tasks/scheduledtasks/locks |jq
```

### Heartbeat
Исполнитель уведомляет шедулер о том, что задача еще выполняется
```
http://localhost:8081/v1_0/tasks/scheduledtasks/lock/heartbeat
```
```bash
curl -X PUT http://localhost:8081/v1_0/tasks/scheduledtasks/lock/heartbeat \
     -H "Content-Type: application/json" \
     -d '{"lockId": 3}'
```
Тело запроса: [TaskExecutionHeartBeatDTO.java](../../lakehouse-common/src/main/java/org/lakehouse/client/api/dto/scheduler/lock/TaskExecutionHeartBeatDTO.java) (поле `lockId`).

### Освобождение блокировки (завершение задачи)
Исполнитель завершает выполнение задачи и передает результат
```
http://localhost:8081/v1_0/tasks/scheduledtasks/release
```
```bash
curl -X PUT http://localhost:8081/v1_0/tasks/scheduledtasks/release \
     -H "Content-Type: application/json" \
     -d '{"lockId": 3, "taskResult": {"status": "SUCCESS", "causes": null}}'
```
Тело запроса: [TaskInstanceReleaseDTO.java](../../lakehouse-common/src/main/java/org/lakehouse/client/api/dto/scheduler/lock/TaskInstanceReleaseDTO.java)
с полями `lockId` и `taskResult` ([TaskResultDTO.java](../../lakehouse-common/src/main/java/org/lakehouse/client/api/dto/scheduler/lock/TaskResultDTO.java) со статусами SUCCESS, FAILED, CONF_ERROR).
Задача переходит в финальный статус, блокировка удаляется.