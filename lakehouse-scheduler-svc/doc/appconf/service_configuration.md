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
      schedule:
        kafka: # consumer for receiving schedule changes from the configuration service
          consumer:
            properties: # https://kafka.apache.org/41/configuration/consumer-configs/
              bootstrap.servers: localhost:9092
              group.id: scheduler
              auto.offset.reset: earliest
            topics: schedule_effective_changes # topic with schedule changes
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