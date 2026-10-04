# Application parameters

```yaml
server:
  port: 8081 # service port

spring:
  datasource: # datasource where the service data is stored
    url: jdbc:postgresql://localhost:5432/postgresDB?ApplicationName=SchedulerSVC
    username: postgresUser
    password: postgresPW
    driver-class-name: org.postgresql.Driver
  jpa:
    generate-ddl: true
    ddl-auto: update
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    show-sql: false
    hibernate:
      transaction:
        jta:
          platform: org.hibernate.service.jta.platform.internal.JBossStandAloneJtaPlatform
    properties:
      jakarta:
        persistence:
          create-database-schemas: true
      hibernate:
        default_schema: lakehouse_scheduler #

lakehouse:
  client:
    rest:
      config: # REST client for accessing the configuration service
        server:
          url: http://localhost:8080
  scheduler:
    schedule:
      task:
        kafka: # producer for sending tasks to executors
          producer:
            topic: scheduled_task_msg # topic name for sending tasks to executors
            properties: # https://kafka.apache.org/41/configuration/producer-configs/
              bootstrap.servers: localhost:9092
    config:
      change:
        kafka: # consumer for receiving configuration changes from the configuration service
          consumer:
            properties: # https://kafka.apache.org/41/configuration/consumer-configs/
              bootstrap.servers: localhost:9092
              group.id: scheduler
              auto.offset.reset: earliest
            topics: configuration_changes # topic with configuration changes
            concurrency: 1 # number of consumption threads
    registration: # Periodicity of registration (building) of new schedules
      delay-ms: 6000
      initial-delay-ms: 5000
    run: # Periodicity of schedule run and processing
      delay-ms: 1200
      initial-delay-ms: 3000
    resolvedeps: # Periodicity of dependency resolution (moving to SUCCESS)
      delay-ms: 1500
      initial-delay-ms: 10000
    task:
      retry: # Re-run of unsuccessful tasks
        delay-ms: 14000
        initial-delay-ms: 10000
        lag-when-failed: 10000 # delay of re-run for FAILED tasks
        lag-when-config-failed: 240000 # delay of re-run for CONF_ERROR tasks

  health: # Service health check endpoints
    liveness-path: /healthz # Liveness probe
    readiness-path: /readyz # Readiness probe
```

## Domains

The service holds **no domain configuration of its own**: there is no `lakehouse.scheduler.*.domains`
property, no domain registry and no domain filter in the YAML above. A domain is metadata that
arrives with the configuration objects read from `lakehouse-config-svc`, and each domain owns its
own Git repository there (see
[config-svc: Domains](../../../lakehouse-config-svc/doc/content_configuration/domains.md)).

Only two configuration points affect how domains behave here, and both live in the metadata
rather than in this service:

| Where | Property / field | Effect |
|---|---|---|
| config-svc metadata | `Schedule.domainKeyName` | Domain of the schedule, stamped from the repository it was loaded from |
| config-svc metadata | `TaskExecutionServiceGroup.domainKeyName`, `TaskExecutionServiceGroup.allowedDomains` | Domains the executor group owns and may additionally serve |

On every publication cycle `ScheduleTaskInstanceService.checkDomain(...)` compares the domain of
the schedule against `allowedDomains ∪ {domainKeyName}` of the task's
`taskExecutionServiceGroupName`. A mismatch is not an error of the schedule but of the routing:
the `ScheduleTaskInstance` is stored as `CONF_ERROR` with the message
`Domain <domain> not allowed in <group> taskExecutionServiceGroup`, the pending producer message
is deleted and nothing is sent to `scheduled_task_msg`. A missing group is **not** a rejection -
the check is skipped with a warning and the task is published.

Because executors are grouped only by `taskExecutionServiceGroupName` (the Kafka `group.id`),
this check is what keeps a foreign domain out of an executor group. See
[Domains](../readme.md#domains).