```yaml
spring:
  datasource: # datasource где будут размещены данные сервиса. Все пользовательские конфигурации метаданных
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
      send: # Настройка отправки уведомлений об изменении расписаний
        delay-ms: 10000 # Задержка между отправками
        initial-delay-ms: 20000 # Задержка первой отправки при старте сервиса
        limit: 100 # предел количества изменений за один интервал
        topic: schedule_effective_changes # имя топика для отправки изменений в расписании
        kafka:
          producer:
            properties: # https://kafka.apache.org/41/configuration/producer-configs/
              bootstrap.servers: localhost:9092

    cvs: # Подсистема GitOps (CVS): декларативная конфигурация из Git-репозитория
      git:
        repository-url: ${LAKEHOUSE_CONFIG_GIT_REPOSITORY_URL:} # например git://git-server:9418/config-repo.git
        branch: ${LAKEHOUSE_CONFIG_GIT_BRANCH:main} # синхронизируемая ветка
        local-clone-path: ${LAKEHOUSE_CONFIG_GIT_LOCAL_CLONE_PATH:} # где сервис хранит свой клон
        private-key-path: ${LAKEHOUSE_CONFIG_GIT_PRIVATE_KEY_PATH:} # SSH-ключ, только для URL вида ssh://
        sync:
          enabled: ${LAKEHOUSE_CONFIG_GIT_SYNC_ENABLED:false} # включает бин планировщика
          interval-ms: ${LAKEHOUSE_CONFIG_GIT_SYNC_INTERVAL_MS:30000} # период цикла
          initial-delay-ms: ${LAKEHOUSE_CONFIG_GIT_SYNC_INITIAL_DELAY_MS:10000} # задержка первого цикла

  health: # Эндпоинты проверки состояния сервиса
    liveness-path: /healthz # Liveness-проба
    readiness-path: /readyz # Readiness-проба
```