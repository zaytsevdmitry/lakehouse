# Параметры приложения

```yaml
server:
  port: 8081 # порт сервиса

spring:
  datasource: # datasource где будут размещены данные сервиса
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
      config: # клиент REST для обращения к сервису конфигурации
        server:
          url: http://localhost:8080
  scheduler:
    schedule:
      task:
        kafka: # producer для отправки задач исполнителям
          producer:
            topic: scheduled_task_msg # имя топика для отправки задач исполнителям
            properties: # https://kafka.apache.org/41/configuration/producer-configs/
              bootstrap.servers: localhost:9092
    config:
      schedule:
        kafka: # consumer для получения изменений расписаний от сервиса конфигурации
          consumer:
            properties: # https://kafka.apache.org/41/configuration/consumer-configs/
              bootstrap.servers: localhost:9092
              group.id: scheduler
              auto.offset.reset: earliest
            topics: schedule_effective_changes # топик с изменениями расписаний
            concurrency: 1 # число потоков потребления
    registration: # Периодичность регистрации (формирования) новых расписаний
      delay-ms: 6000
      initial-delay-ms: 5000
    run: # Периодичность запуска и обработки расписаний
      delay-ms: 1200
      initial-delay-ms: 3000
    resolvedeps: # Периодичность разрешения зависимостей (перевод в SUCCESS)
      delay-ms: 1500
      initial-delay-ms: 10000
    task:
      retry: # Повторный запуск неуспешных задач
        delay-ms: 14000
        initial-delay-ms: 10000
        lag-when-failed: 10000 # задержка повторного запуска для FAILED задач
        lag-when-config-failed: 240000 # задержка повторного запуска для CONF_ERROR задач

  health: # Эндпоинты проверки состояния сервиса
    liveness-path: /healthz # Liveness-проба
    readiness-path: /readyz # Readiness-проба
```