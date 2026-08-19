# lakehouse-scheduler-svc

Сервис планирования и управления исполнением задач lakehouse. Принимает изменения конфигураций расписаний от сервиса метаданных, формирует экземпляры расписаний (schedule instances), управляет их жизненным циклом, разрешает зависимости между сценариями и задачами, ставит задачи в очередь и передает их исполнителям (task-executor-svc).

## Обзор

`lakehouse-scheduler-svc` отвечает за:

- **Регистрацию расписаний** - получение изменений расписаний из Kafka (topic `schedule_effective_changes`) от config-svc и формирование экземпляров расписаний по интервалам (`intervalExpression`).
- **Жизненный цикл расписания** - перевод расписания по статусам NEW → RUNNING → SUCCESS/FAILED.
- **Сценарии (акты) и задачи** - создание экземпляров сценариев и задач, ведение статусов каждого элемента.
- **Разрешение зависимостей** - направленные графы `scenarioActEdges` и `dagEdges`: задача/сценарий переводится в SUCCESS только после успеха всех зависимостей.
- **Очередь задач** - передача задач исполнителям через Kafka (topic `scheduled_task_msg`).
- **Блокировки задач (locks)** - исполнитель берет задачу по `lock/taskId/{id}/service/{serviceId}`, продлевает блокировку heartbeat, возвращает результат через release. Защита от повторного выполнения.
- **Повторные запуски** - автоматический повторный запуск неуспешных задач с учетом лагов `lag-when-failed`/`lag-when-config-failed` и ограничения `maxRetries`.

## Архитектура

```
┌──────────────┐   Kafka: schedule_effective_changes   ┌─────────────────────────────────────┐
│ lakehouse-   │ ─────────────────────────────────────▶│          lakehouse-scheduler-svc    │
│ config-svc   │                                       │                                     │
└──────────────┘                                       │  ┌───────────────────────────────┐  │
                                                       │  │ ScheduleConfigConsumerService │  │
                                                       │  │ (consumes schedule changes)   │  │
                                                       │  └───────────────────────────────┘  │
┌──────────────┐                                       │  ┌───────────────────────────────┐  │
│ Admin / UI / │  REST (8081)                          │  │ InternalScheduler             │  │
│ CLI          │ ───────────────────────────────────-─▶│  │  build / run / resolveDeps /  │  │
└──────────────┘                                       │  │  reTryFailedTasks (слоты)     │  │
                                                       │  └───────────────┬───────────────┘  │
                                                       │                  ▼                  │
                                                       │  ┌───────────────────────────────┐  │
                                                       │  │ BuildService / ManageState-   │  │
                                                       │  │ Service / ScheduleTaskInstance│  │
                                                       │  │ Service                       │  │
                                                       │  └───────────────┬───────────────┘  │
                                                       │                  ▼                  │
                                                       │  ┌───────────────────────────────┐  │
                                                       │  │ PostgreSQL                    │  │
                                                       │  │ (schema lakehouse_scheduler)  │  │
                                                       │  └───────────────┬───────────────┘  │
                                                       └──────────────────┼──────────────────┘
                                                                          ▼ Kafka: scheduled_task_msg
                                                       ┌──────────────────────────────┐
                                                       │       task-executor-svc      │
                                                       │  (lock / heartbeat / release)│
                                                       └──────────────────────────────┘
```

- **Controllers** - REST API (см. [restapi.md](restapi.md)): расписания, DAG, задачи, блокировки.
- **InternalScheduler** - периодические слоты по расписанию: `registration` (build), `run`, `resolvedeps`, `task.retry`. Методы: `build`, `run`, `resolveDependency`, `reTryFailedTasked`.
- **BuildService** - регистрация новых экземпляров расписаний и их составных частей (сценарии, задачи, графы).
- **ManageStateService** - перевод статусов расписаний и сценариев, разрешение зависимостей сценариев, поиск следующего интервала.
- **ScheduleTaskInstanceService** - жизненный цикл задач: очередь, продюсирование в Kafka, блокировки, heartbeat, release, повторные запуски.
- **ScheduleEffectiveService** - вычисление следующего интервала по `intervalExpression` (cron/@daily и т.п.).
- **ScheduleConfigConsumerService** - потребление изменений расписаний из config-svc (Kafka).
- **ScheduledTaskDTOProducerService** - публикация задач исполнителям (Kafka).
- **Factory / Repository (JPA)** - построение и персистентность сущностей.

Структура расписания, статусные модели и диаграммы классов описаны в [scheduling/Scheduling.md](scheduling/Scheduling.md).

## Модули

### lakehouse-scheduler-svc

Spring Boot-приложение, реализующее планировщик. Точка входа: `org.lakehouse.scheduler.LakehouseSchedulerApp`.

### lakehouse-scheduler-rest-client

Java-клиент (`SchedulerRestClientApi`/`SchedulerRestClientApiImpl`) для доступа к `lakehouse-scheduler-svc` из других сервисов (task-executor-svc и др.). Выполняет типизированные запросы к эндпоинтам `/v1_0/...` через `RestClientHelper`. Базовый URL задается свойством `lakehouse.client.rest.scheduler.server.url`.

## API Endpoints

Описание всех эндпоинтов сервиса приведено в [restapi.md](restapi.md). Сервис работает на порту **8081**, все эндпоинты начинаются с `/v1_0`.

Проверено покрытие эндпоинтов контроллерами:

| Контроллер                      | Эндпоинты                                          |
|:--------------------------------|:---------------------------------------------------|
| ScheduleInstanceController      | `GET/POST /v1_0/schedule`, `GET /v1_0/schedule/name={name}/limit={limit}`, `DELETE /v1_0/schedule/id={id}` |
| ScheduleInstanceDAGController   | `GET /v1_0/schedule/dag/id={id}`                   |
| ScheduledTaskController         | `GET /v1_0/tasks/scheduledtasks`, `GET /v1_0/tasks/scheduledtasks/{id}` |
| ScheduledTaskLockController     | `GET /v1_0/tasks/scheduledtasks/lock/{id}`, `GET /v1_0/tasks/scheduledtasks/lock/taskId/{id}/service/{serviceId}`, `PUT /v1_0/tasks/scheduledtasks/lock/heartbeat`, `PUT /v1_0/tasks/scheduledtasks/release`, `GET /v1_0/tasks/scheduledtasks/locks` |

Все эндпоинты контроллеров описаны в [restapi.md](restapi.md).

## Конфигурация

Параметры приложения (порт, datasource, JPA, клиент config-svc, Kafka producer/consumer, периодичность слотов, повторные запуски, health-эндпоинты) описаны в [appconf/service_configuration.md](appconf/service_configuration.md).