

```yaml
spring:
  datasource:
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
        default_schema: lakehouse_config

lakehouse:
  config:
    schedule:
      send: # Настройка отправки уведомлений об изменении расписаний
        delay-ms: 10000 # Задержка между отправками
        initial-delay-ms: 20000 # Задержка первой отправки при старте сервиса
        limit: 100 # предел количества изменений за один интервал
      kafka:
        producer:
          schedule:
            send:
              topic: schedule_changes # имя топика для отправки изменений в расписании
            delete:
              topic: schedule_deletes # не реализовано еще todo
          bootstrap-servers: localhost:9092
          topic: schedules_effective

```