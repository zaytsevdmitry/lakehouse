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

    vcs: # GitOps (VCS) subsystem: declarative configuration from a Git repository
      git:
        repository-url: ${LAKEHOUSE_CONFIG_GIT_REPOSITORY_URL:} # e.g. git://git-server:9418/config-repo.git
        branch: ${LAKEHOUSE_CONFIG_GIT_BRANCH:main} # branch to synchronize
        local-clone-path: ${LAKEHOUSE_CONFIG_GIT_LOCAL_CLONE_PATH:} # where the service keeps its clone
        private-key-path: ${LAKEHOUSE_CONFIG_GIT_PRIVATE_KEY_PATH:} # SSH key, only for ssh:// URLs
        sync:
          enabled: ${LAKEHOUSE_CONFIG_GIT_SYNC_ENABLED:false} # enables the scheduler bean
          interval-ms: ${LAKEHOUSE_CONFIG_GIT_SYNC_INTERVAL_MS:30000} # cycle period
          initial-delay-ms: ${LAKEHOUSE_CONFIG_GIT_SYNC_INITIAL_DELAY_MS:10000} # delay of the first cycle

  health: # Service health check endpoints
    liveness-path: /healthz # Liveness probe
    readiness-path: /readyz # Readiness probe
```