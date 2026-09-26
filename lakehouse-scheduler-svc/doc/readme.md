# lakehouse-scheduler-svc

The lakehouse task scheduling and execution management service. It consumes schedule configuration changes from the metadata service, creates schedule instances, manages their lifecycle, resolves dependencies between scenarios and tasks, enqueues tasks and passes them to executors (task-executor-svc).

## Overview

`lakehouse-scheduler-svc` is responsible for:

- **Schedule registration** - consuming configuration changes from Kafka (topic `configuration_changes`) coming from config-svc and building schedule instances by intervals (`intervalExpression`).
- **Schedule lifecycle** - moving a schedule through the statuses NEW → RUNNING → SUCCESS/FAILED.
- **Scenarios (acts) and tasks** - creating scenario and task instances and tracking the status of each element.
- **Dependency resolution** - directed graphs `scenarioActEdges` and `dagEdges`: a task/scenario is moved to SUCCESS only after all its dependencies succeed.
- **Task queue** - passing tasks to executors via Kafka (topic `scheduled_task_msg`).
- **Task locks** - an executor takes a task via `lock/taskId/{id}/service/{serviceId}`, extends the lock with heartbeat, and returns the result via release. Protects against duplicate execution.
- **Retries** - automatic re-run of unsuccessful tasks respecting the `lag-when-failed`/`lag-when-config-failed` lags and the `maxRetries` limit.

## Architecture

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
└──────────────┘                                       │  │  reTryFailedTasks (slots)     │  │
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

- **Controllers** - REST API (see [restapi.md](restapi.md)): schedules, DAG, tasks, locks.
- **InternalScheduler** - periodic slots on a schedule: `registration` (build), `run`, `resolvedeps`, `task.retry`. Methods: `build`, `run`, `resolveDependency`, `reTryFailedTasked`.
- **BuildService** - registration of new schedule instances and their parts (scenarios, tasks, graphs).
- **ManageStateService** - moving schedule and scenario statuses, resolving scenario dependencies, finding the next interval.
- **ScheduleTaskInstanceService** - task lifecycle: queue, Kafka production, locks, heartbeat, release, retries.
- **ScheduleEffectiveService** - computing the next interval by `intervalExpression` (cron/@daily, etc.).
- **ConfigurationChangeConsumerService** - consuming configuration changes from config-svc (Kafka). Two kinds are in scope: `Schedule` (registers the schedule on a SAVE action, DELETE is logged only) and `TaskExecutionServiceGroup` (caches the group DTO on SAVE, drops it from the cache on DELETE). Any other kind is logged as out of scope and ignored.
- **ScheduledTaskDTOProducerService** - publishing tasks to executors (Kafka).
- **Factory / Repository (JPA)** - building and persisting entities.

The schedule structure, status models and class diagrams are described in [scheduling/Scheduling.md](scheduling/Scheduling.md).

## Domains

A **domain** is the unit of isolation of the configuration in `lakehouse-config-svc`: every
domain owns its own Git repository and every construct loaded from it is stamped with the
domain name as `domainKeyName`. The domain is an ownership label, not part of the identity:
`keyName` stays global, so it never repeats across domains. This service
does not manage domains - it consumes them. See
[config-svc: Domains](../../lakehouse-config-svc/doc/content_configuration/domains.md).

The scheduler owns no domain tree, no domain registry and no per-domain configuration. The
domain reaches it in exactly one place, and it is used exactly once: as a **permission check
on the task level**.

### What the scheduler receives

```
lakehouse-config-svc ──Kafka: configuration_changes──▶ ConfigurationChangeConsumerService
                                                          ├─ SCHEDULE_KIND  → BuildService.registration(dto)
                                                          │                  → ScheduleEffectiveService cache
                                                          └─ TASK_GROUP_KIND → TaskExecutionServiceGroupConfigService cache

produceScheduledTasks()  (ScheduleTaskInstanceService)
  └─ checkDomain(taskDTO, scheduleEffectiveDTO)   ← the only domain-aware code path
       scheduleEffectiveDTO.domainKeyName  ∈  taskGroup.allowedDomains ∪ {taskGroup.domainKeyName}
```

- **`ScheduleEffectiveDTO.domainKeyName`** - the domain of the schedule, read live from the
  configuration service through `ScheduleEffectiveService.getScheduleEffectiveDTO(...)`. The
  schedule DTO is cached when a `Schedule` change is consumed, and re-read on demand.
- **`TaskExecutionServiceGroupDTO.domainKeyName` and `.allowedDomains`** - the domain the
  executor group belongs to, plus the list of additional domains the group is allowed to
  serve. The group DTO is cached in `TaskExecutionServiceGroupConfigService` when a
  `TaskExecutionServiceGroup` change is consumed, and fetched on demand.

### The domain check

`ScheduleTaskInstanceService.checkDomain(taskDTO, scheduleEffectiveDTO)` runs for every
message about to be published to `scheduled_task_msg`:

1. resolve the `TaskExecutionServiceGroup` of the task by `taskExecutionServiceGroupName`;
2. if the group is not found, log a warning and **skip** the check (the task is published);
3. build the allowed set as `taskGroup.allowedDomains` plus `taskGroup.domainKeyName` -
   a group always serves its own domain;
4. if the schedule domain is not in that set, throw
   `TaskConfigurationException("Domain <domain> not allowed in <group> taskExecutionServiceGroup")`.

The caller treats it as a configuration error: the `ScheduleTaskInstance` is set to
`CONF_ERROR` with the message in `causes`, the pending message is dropped and **the task is
not published** - it is not retried by this path. The grouping of the executors that
actually run the task stays non-domain based: an executor takes a task by matching
`taskExecutionServiceGroupName` against its Kafka `group.id`, so the domain check is what
keeps a foreign domain out of an executor group.

### Known limitations

- `ScheduleInstance.domainKeyName` is a nullable column of the `schedule_instance` table, but
  its setter is never called: `ScheduleInstanceFactory.newScheduleInstance(...)` receives the
  `ScheduleEffectiveDTO` and does not copy the domain. The column is therefore always `null`,
  and `ScheduleInstanceDTO` does not expose the domain either. The domain is always read live
  from the configuration service instead, which is why the check above works.
- `ScheduledTaskMsgDTO.domainKeyName` - the field that would carry the domain of a published
  task - is not populated either. A task message that arrives at an executor therefore carries
  `domainKeyName = null` today. Nothing in this service depends on it, and
  `lakehouse-task-executor-svc` resolves its data sources through the configuration service
  regardless (see [task-executor-svc: Domains](../../lakehouse-task-executor-svc/doc/readme.md#domains)).
- There are no domain-scoped settings in this service: no `lakehouse.scheduler.*.domains.*`
  property exists, and no REST endpoint takes a domain parameter. All application parameters
  are listed in [appconf/service_configuration.md](appconf/service_configuration.md).

## Modules

### lakehouse-scheduler-svc

Spring Boot application implementing the scheduler. Entry point: `org.lakehouse.scheduler.LakehouseSchedulerApp`.

### lakehouse-scheduler-rest-client

Java client (`SchedulerRestClientApi`/`SchedulerRestClientApiImpl`) for accessing `lakehouse-scheduler-svc` from other services (task-executor-svc and others). Performs typed requests to the `/v1_0/...` endpoints via `RestClientHelper`. The base URL is set by the `lakehouse.client.rest.scheduler.server.url` property.

## API Endpoints

All service endpoints are described in [restapi.md](restapi.md). The service runs on port **8081**; all endpoints start with `/v1_0`.

Endpoint coverage across controllers has been verified:

| Controller                   | Endpoints                                          |
|:-----------------------------|:---------------------------------------------------|
| ScheduleInstanceController   | `GET/POST /v1_0/schedule`, `GET /v1_0/schedule/name={name}/limit={limit}`, `DELETE /v1_0/schedule/id={id}` |
| ScheduleInstanceDAGController| `GET /v1_0/schedule/dag/id={id}`                   |
| ScheduledTaskController      | `GET /v1_0/tasks/scheduledtasks`, `GET /v1_0/tasks/scheduledtasks/{id}` |
| ScheduledTaskLockController  | `GET /v1_0/tasks/scheduledtasks/lock/{id}`, `GET /v1_0/tasks/scheduledtasks/lock/taskId/{id}/service/{serviceId}`, `PUT /v1_0/tasks/scheduledtasks/lock/heartbeat`, `PUT /v1_0/tasks/scheduledtasks/release`, `GET /v1_0/tasks/scheduledtasks/locks` |

All controller endpoints are described in [restapi.md](restapi.md).

## Configuration

Application parameters (port, datasource, JPA, config-svc client, Kafka producer/consumer, slot periodicity, retries, health endpoints) are described in [appconf/service_configuration.md](appconf/service_configuration.md).

## Security

`lakehouse-scheduler-svc` is protected with OAuth 2.0 / OIDC and uses Keycloak as the identity provider (realm `lakehouse`). Spring Security is configured as an **OAuth2 resource server**: every request must carry a valid JWT issued by the realm, otherwise the service returns `401`.

### Authentication

- **User requests** (UI BFF, CLI, direct API calls) - the JWT is validated via `spring.security.oauth2.resourceserver.jwt.issuer-uri`. Roles from the `realm_access.roles` claim are converted to `ROLE_<NAME>` authorities by `KeycloakRoleConverter` and can be used with `@PreAuthorize`.
- **Service-to-service calls** (`BearerTokenClientHttpRequestInterceptor`) - when a request originates from a background task (no user JWT in the `SecurityContext`), the outgoing `RestClient` obtains a `client_credentials` token through the `OAuth2AuthorizedClientManager` using the `keycloak-internal` registration (client `lakehouse-internal-client`) and attaches it as `Authorization: Bearer`. When a user JWT is present, it is propagated unchanged.
- Security can be switched off entirely with `lakehouse.security.enabled=false` (all requests become anonymous).

### Audit logging

`AuditLoggingFilter` writes one line per request to the `AUDIT_LOG` logger (file `logs/audit.log`):

```
User ID: <subject>, Username: <preferred_username>, Method: <method>, URI: <uri>, HTTP status: <status>
```

Tokens obtained with the service account are logged with the configured `lakehouse.security.audit.service-account-name` (default `system`) instead of the username.

### Required settings

| Property / env | Default | Description |
|---|---|---|
| `KEYCLOAK_ISSUER_URI` | `http://lakehouse-auth-svc:8080/realms/lakehouse` | Keycloak realm URL |
| `KEYCLOAK_INTERNAL_CLIENT_SECRET` | `super-secret-internal-key-987654321` | Secret of `lakehouse-internal-client` |
| `lakehouse.security.enabled` | `true` | Set `false` to disable security completely |
| `lakehouse.security.audit.service-account-name` | `system` | Username logged for service account tokens |
| `lakehouse.security.oauth2.internal-client-id` | `lakehouse-internal-client` | Client identifying service account tokens (`azp` claim) |
| `lakehouse.security.oauth2.client-registration-id` | `keycloak-internal` | OAuth2 client registration used by the interceptor |

`spring.security.oauth2.resourceserver.jwt.issuer-uri` and the `spring.security.oauth2.client` block (registration `keycloak-internal`) are preconfigured in `src/main/resources/application.yml`.

Whitelisted paths (no token required): `/healthz`, `/readyz`, `/actuator/**`, `/v3/api-docs/**`, `/swagger-ui/**`. The Swagger paths are active only while Swagger is enabled; under the `prod` profile they are fully disabled (`springdoc.api-docs.enabled: false`, `springdoc.swagger-ui.enabled: false`).

### Keycloak realm

The `lakehouse` realm must contain:

- **`lakehouse-internal-client`** - confidential client with *Service Accounts Enabled* (service-to-service calls);
- Realm roles `USER` / `ADMIN` (optional, used with `@PreAuthorize`).

The reference realm import is in `demo/compose/conf_infra/security/realms/lakehouse-realm.json`.