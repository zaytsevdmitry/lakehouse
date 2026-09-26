# lakehouse-scheduler-svc

Сервис планирования и управления исполнением задач lakehouse. Принимает изменения конфигураций расписаний от сервиса метаданных, формирует экземпляры расписаний (schedule instances), управляет их жизненным циклом, разрешает зависимости между сценариями и задачами, ставит задачи в очередь и передает их исполнителям (task-executor-svc).

## Обзор

`lakehouse-scheduler-svc` отвечает за:

- **Регистрацию расписаний** - получение изменений конфигураций из Kafka (topic `configuration_changes`) от config-svc и формирование экземпляров расписаний по интервалам (`intervalExpression`).
- **Жизненный цикл расписания** - перевод расписания по статусам NEW → RUNNING → SUCCESS/FAILED.
- **Сценарии (акты) и задачи** - создание экземпляров сценариев и задач, ведение статусов каждого элемента.
- **Разрешение зависимостей** - направленные графы `scenarioActEdges` и `dagEdges`: задача/сценарий переводится в SUCCESS только после успеха всех зависимостей.
- **Очередь задач** - передача задач исполнителям через Kafka (topic `scheduled_task_msg`).
- **Блокировки задач (locks)** - исполнитель берет задачу по `lock/taskId/{id}/service/{serviceId}`, продлевает блокировку heartbeat, возвращает результат через release. Защита от повторного выполнения.
- **Повторные запуски** - автоматический повторный запуск неуспешных задач с учетом лагов `lag-when-failed`/`lag-when-config-failed` и ограничения `maxRetries`.

## Архитектура

```
┌──────────────┐   Kafka: configuration_changes   ┌─────────────────────────────────────┐
│ lakehouse-   │ ─────────────────────────────────────▶│          lakehouse-scheduler-svc    │
│ config-svc   │                                       │                                     │
└──────────────┘                                       │  ┌───────────────────────────────┐  │
                                                       │  │ ConfigurationChangeConsumer- │  │
                                                       │  │ Service (consumes Schedule   │  │
                                                       │  │ changes from config-svc)     │  │
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
- **ConfigurationChangeConsumerService** - потребление изменений конфигураций из config-svc (Kafka). В области действия два kind: `Schedule` (при сохранении, action SAVE, регистрирует расписание; DELETE только логируется) и `TaskExecutionServiceGroup` (при SAVE кладёт DTO группы в кэш, при DELETE убирает из кэша). Любой другой kind логируется как вне области действия планировщика и игнорируется.
- **ScheduledTaskDTOProducerService** - публикация задач исполнителям (Kafka).
- **Factory / Repository (JPA)** - построение и персистентность сущностей.

Структура расписания, статусные модели и диаграммы классов описаны в [scheduling/Scheduling.md](scheduling/Scheduling.md).

## Домены

**Домен** - единица изоляции конфигурации в `lakehouse-config-svc`: каждый домен владеет
собственным Git-репозиторием, и на каждый загруженный из него конструкт проставляется имя
домена как `domainKeyName`. Домен - это метка владения, а не часть идентичности: `keyName`
остаётся сквозным и не повторяется между доменами. Этот сервис доменами не управляет - он их
потребляет. См.
[config-svc: Домены](../../lakehouse-config-svc/doc-ru/content_configuration/domains.md).

Планировщик не владеет ни деревом доменов, ни реестром доменов, ни доменной конфигурацией.
Домен приходит в него ровно в одном месте и используется ровно один раз: как **проверка
разрешения на уровне задачи**.

### Что получает планировщик

```
lakehouse-config-svc ──Kafka: configuration_changes──▶ ConfigurationChangeConsumerService
                                                          ├─ SCHEDULE_KIND  → BuildService.registration(dto)
                                                          │                  → кэш ScheduleEffectiveService
                                                          └─ TASK_GROUP_KIND → кэш TaskExecutionServiceGroupConfigService

produceScheduledTasks()  (ScheduleTaskInstanceService)
  └─ checkDomain(taskDTO, scheduleEffectiveDTO)   ← единственный домен-осознанный участок кода
       scheduleEffectiveDTO.domainKeyName  ∈  taskGroup.allowedDomains ∪ {taskGroup.domainKeyName}
```

- **`ScheduleEffectiveDTO.domainKeyName`** - домен расписания, читаемый из сервиса
  конфигурации через `ScheduleEffectiveService.getScheduleEffectiveDTO(...)`. DTO расписания
  кэшируется при обработке изменения kind `Schedule` и перечитывается по необходимости.
- **`TaskExecutionServiceGroupDTO.domainKeyName` и `.allowedDomains`** - домен, которому
  принадлежит группа исполнителей, и список дополнительных доменов, которые группа вправе
  обслуживать. DTO группы кэшируется в `TaskExecutionServiceGroupConfigService` при
  обработке изменения kind `TaskExecutionServiceGroup` и запрашивается по необходимости.

### Проверка домена

`ScheduleTaskInstanceService.checkDomain(taskDTO, scheduleEffectiveDTO)` выполняется для
каждого сообщения перед публикацией в `scheduled_task_msg`:

1. resolve группу `TaskExecutionServiceGroup` задачи по `taskExecutionServiceGroupName`;
2. если группа не найдена, залогировать предупреждение и **пропустить** проверку (задача
   публикуется);
3. собрать разрешённое множество как `taskGroup.allowedDomains` плюс
   `taskGroup.domainKeyName` - группа всегда обслуживает собственный домен;
4. если домен расписания не входит в это множество, бросить
   `TaskConfigurationException("Domain <домен> not allowed in <группа> taskExecutionServiceGroup")`.

Вызывающий трактует это как ошибку конфигурации: `ScheduleTaskInstance` переводится в
`CONF_ERROR`, причина пишется в `causes`, отложенное сообщение удаляется и **задача не
публикуется** - этот путь её не повторяет. Группировка исполнителей, которые фактически
выполняют задачу, остаётся недоменной: исполнитель берёт задачу по совпадению
`taskExecutionServiceGroupName` с Kafka `group.id`, поэтому именно проверка домена не даёт
чужому домену попасть в группу исполнителей.

### Известные ограничения

- `ScheduleInstance.domainKeyName` - nullable-колонка таблицы `schedule_instance`, но её
  сеттер нигде не вызывается: `ScheduleInstanceFactory.newScheduleInstance(...)` получает
  `ScheduleEffectiveDTO` и не копирует домен. Колонка всегда `null`, и `ScheduleInstanceDTO`
  домен тоже не выставляет. Домен всегда читается заново из сервиса конфигурации - поэтому
  проверка выше и работает.
- `ScheduledTaskMsgDTO.domainKeyName` - поле, которое должно было бы нести домен
  опубликованной задачи, - тоже не заполняется. Сообщение о задаче, приходящее к исполнителю,
  поэтому сегодня несёт `domainKeyName = null`. Ничто в этом сервисе от этого не зависит, а
  `lakehouse-task-executor-svc` в любом случае резолвит источники данных через сервис
  конфигурации (см. [task-executor-svc: Домены](../../lakehouse-task-executor-svc/doc-ru/readme.md#домены)).
- Доменных настроек в этом сервисе нет: свойства `lakehouse.scheduler.*.domains.*`
  не существуют, и ни один REST-эндпоинт не принимает параметр домена. Все параметры
  приложения перечислены в [appconf/service_configuration.md](appconf/service_configuration.md).

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

## Безопасность

`lakehouse-scheduler-svc` защищен по OAuth 2.0 / OIDC с Keycloak в качестве identity provider (realm `lakehouse`). Spring Security настроен как **OAuth2 resource server**: каждый запрос должен содержать валидный JWT, выпущенный этим realm, иначе сервис возвращает `401`.

### Аутентификация

- **Запросы пользователей** (UI BFF, CLI, прямые вызовы API) - JWT проверяется по `spring.security.oauth2.resourceserver.jwt.issuer-uri`. Роли из claim'а `realm_access.roles` конвертируются в authorities `ROLE_<ИМЯ>` классом `KeycloakRoleConverter` и могут использоваться в `@PreAuthorize`.
- **Межсервисные вызовы** (`BearerTokenClientHttpRequestInterceptor`) - если запрос инициирован фоновой задачей (в `SecurityContext` нет пользовательского JWT), исходящий `RestClient` получает токен `client_credentials` через `OAuth2AuthorizedClientManager` по регистрации `keycloak-internal` (клиент `lakehouse-internal-client`) и добавляет его как `Authorization: Bearer`. При наличии пользовательского JWT он пробрасывается без изменений.
- Безопасность можно полностью отключить свойством `lakehouse.security.enabled=false` (все запросы становятся анонимными).

### Аудит

`AuditLoggingFilter` пишет одну строку на каждый запрос в логгер `AUDIT_LOG` (файл `logs/audit.log`):

```
User ID: <subject>, Username: <preferred_username>, Method: <method>, URI: <uri>, HTTP status: <status>
```

Для токенов, полученных через service account, вместо имени пользователя подставляется значение `lakehouse.security.audit.service-account-name` (по умолчанию `system`).

### Необходимые настройки

| Свойство / env | По умолчанию | Описание |
|---|---|---|
| `KEYCLOAK_ISSUER_URI` | `http://lakehouse-auth-svc:8080/realms/lakehouse` | URL realm'а Keycloak |
| `KEYCLOAK_INTERNAL_CLIENT_SECRET` | `super-secret-internal-key-987654321` | Секрет клиента `lakehouse-internal-client` |
| `lakehouse.security.enabled` | `true` | `false` полностью отключает безопасность |
| `lakehouse.security.audit.service-account-name` | `system` | Имя пользователя в аудите для service account токенов |
| `lakehouse.security.oauth2.internal-client-id` | `lakehouse-internal-client` | Клиент, идентифицирующий service account токены (claim `azp`) |
| `lakehouse.security.oauth2.client-registration-id` | `keycloak-internal` | OAuth2-регистрация, используемая интерцептором |

`spring.security.oauth2.resourceserver.jwt.issuer-uri` и блок `spring.security.oauth2.client` (регистрация `keycloak-internal`) преднастроены в `src/main/resources/application.yml`.

Пути из белого списка (токен не требуется): `/healthz`, `/readyz`, `/actuator/**`, `/v3/api-docs/**`, `/swagger-ui/**`. Пути Swagger активны, только пока включен Swagger; в профиле `prod` он полностью отключается (`springdoc.api-docs.enabled: false`, `springdoc.swagger-ui.enabled: false`).

### Realm Keycloak

В realm `lakehouse` должны быть:

- **`lakehouse-internal-client`** - confidential-клиент с включенными *Service Accounts* (межсервисные вызовы);
- роли realm'а `USER` / `ADMIN` (опционально, используются в `@PreAuthorize`).

Эталонный realm для импорта: `demo/compose/conf_infra/security/realms/lakehouse-realm.json`.