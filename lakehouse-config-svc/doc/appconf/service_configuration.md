```yaml
spring:
  datasource: # datasource where the service data will be stored. All user metadata configurations
    url: jdbc:postgresql://localhost:5432/postgresDB?ApplicationName=ConfigSVC
    username: postgresUser
    password: postgresPW
    driver-class-name: org.postgresql.Driver
  jpa:
    generate-ddl: true
    ddl-auto: create-drop
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
        default_schema: lakehouse_config #
        
lakehouse:
  config:
    schedule:
      send: # Schedule change notification sending settings
        delay-ms: 10000 # Delay between sends
        initial-delay-ms: 20000 # Delay of the first send on service startup
        limit: 100 # Limit of changes per one interval
        topic: schedule_effective_changes # Topic name for sending schedule changes
        kafka:
          producer:
            properties: # https://kafka.apache.org/41/configuration/producer-configs/
              bootstrap.servers: localhost:9092

  health: # Service health check endpoints
    liveness-path: /healthz # Liveness probe
    readiness-path: /readyz # Readiness probe
```