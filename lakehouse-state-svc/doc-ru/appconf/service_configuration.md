# Параметры приложения

```yaml
server:
  port: 8082 # порт сервиса

spring:
  datasource: # datasource где будут размещены данные сервиса
    url: jdbc:postgresql://localhost:5432/postgresDB?ApplicationName=stateSVC
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
        default_schema: lakehouse_state #

lakehouse:
  health: # Эндпоинты проверки состояния сервиса
    liveness-path: /healthz # Liveness-проба
    readiness-path: /readyz # Readiness-проба
```