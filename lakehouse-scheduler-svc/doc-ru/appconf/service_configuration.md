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
      change:
        kafka: # consumer для получения изменений конфигурации от сервиса конфигурации
          consumer:
            properties: # https://kafka.apache.org/41/configuration/consumer-configs/
              bootstrap.servers: localhost:9092
              group.id: scheduler
              auto.offset.reset: earliest
            topics: configuration_changes # топик с изменениями конфигурации
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
    liveness-path: /healthz # Лiveness-проба
    readiness-path: /readyz # Readiness-проба
```

## Домены

Сервис **не хранит собственной доменной конфигурации**: в YAML выше нет ни свойства
`lakehouse.scheduler.*.domains`, ни реестра доменов, ни доменного фильтра. Домен приходит как
метаданные вместе с конфигурационными объектами, прочитанными из `lakehouse-config-svc`, и
каждый домен владеет своим Git-репозиторием там же (см.
[config-svc: Домены](../../../lakehouse-config-svc/doc-ru/content_configuration/domains.md)).

На поведение доменов здесь влияют ровно две точки конфигурации, и обе живут в метаданных, а не в
этом сервисе:

| Где | Свойство / поле | Влияние |
|---|---|---|
| Метаданные config-svc | `Schedule.domainKeyName` | Домен расписания, проставляется из репозитория, из которого он загружен |
| Метаданные config-svc | `TaskExecutionServiceGroup.domainKeyName`, `TaskExecutionServiceGroup.allowedDomains` | Домены, которыми группа исполнителей владеет и которые дополнительно обслуживает |

При каждом цикле публикации `ScheduleTaskInstanceService.checkDomain(...)` сравнивает домен
расписания с `allowedDomains ∪ {domainKeyName}` группы, указанной в
`taskExecutionServiceGroupName` задачи. Несовпадение - это не ошибка расписания, а ошибка
маршрутизации: `ScheduleTaskInstance` сохраняется со статусом `CONF_ERROR` и сообщением
`Domain <домен> not allowed in <группа> taskExecutionServiceGroup`, отложенное сообщение
продюсера удаляется и в `scheduled_task_msg` ничего не отправляется. Отсутствующая группа **не
является отказом** - проверка пропускается с предупреждением, и задача публикуется.

Так как исполнители группируются только по `taskExecutionServiceGroupName` (это Kafka `group.id`),
именно эта проверка не пускает чужой домен в группу исполнителей. См.
[Домены](../readme.md#домены).