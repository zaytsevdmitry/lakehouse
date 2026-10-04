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
    produce: # Универсальная настройка отправки уведомлений об изменении конфигурации (outbox)
      delay-ms: 10000 # Задержка между отправками
      initial-delay-ms: 20000 # Задержка первой отправки при старте сервиса
      limit: 100 # предел количества изменений за один интервал
      topic: configuration_changes # имя топика для отправки изменений конфигурации
      kafka:
        producer:
          properties: # https://kafka.apache.org/41/configuration/producer-configs/
            bootstrap.servers: localhost:9092

    vcs: # Подсистема GitOps (VCS): декларативная конфигурация из доменных Git-репозиториев
      git:
        sync:
          enabled: ${LAKEHOUSE_CONFIG_GIT_SYNC_ENABLED:false} # включает бин планировщика
          interval-ms: ${LAKEHOUSE_CONFIG_GIT_SYNC_INTERVAL_MS:30000} # период цикла
          initial-delay-ms: ${LAKEHOUSE_CONFIG_GIT_SYNC_INITIAL_DELAY_MS:10000} # задержка первого цикла
      domains: # у каждого домена собственный репозиторий; список и имена доменов задаёт пользователь
        <domain>: # любое имя; домен может содержать вложенные домены
          priority: 0 # порядок применения: по возрастанию, родители перед вложенными
          git:
            repository-url: ${LAKEHOUSE_CONFIG_GIT_<DOMAIN>_URL:} # например git://git-server:9418/<domain>.git
            branch: ${LAKEHOUSE_CONFIG_GIT_<DOMAIN>_BRANCH:main} # синхронизируемая ветка
            local-clone-path: ${LAKEHOUSE_CONFIG_GIT_<DOMAIN>_CLONE_PATH:} # где сервис хранит клон домена
            private-key-path: ${LAKEHOUSE_CONFIG_GIT_<DOMAIN>_PRIVATE_KEY_PATH:} # SSH-ключ, только для URL вида ssh://
          domains:
            <nested-domain>: # вложенный поддомен родительского домена
              priority: 1
              git:
                repository-url: ${LAKEHOUSE_CONFIG_GIT_<NESTED_DOMAIN>_URL:}
                branch: ${LAKEHOUSE_CONFIG_GIT_<NESTED_DOMAIN>_BRANCH:main}
                local-clone-path: ${LAKEHOUSE_CONFIG_GIT_<NESTED_DOMAIN>_CLONE_PATH:}
                private-key-path: ${LAKEHOUSE_CONFIG_GIT_<NESTED_DOMAIN>_PRIVATE_KEY_PATH:}

health: # Эндпоинты проверки состояния сервиса
    liveness-path: /healthz # Лiveness-проба
    readiness-path: /readyz # Readiness-проба
```

## Домены

`lakehouse.config.vcs` связывается классом `LakehouseVCSProperties`; карта `domains` выше -
единственное место, где объявляются репозитории. Каждый домен - это один Git-репозиторий, он
забирается и применяется сам по себе, и каждый загруженный из него конструкт получает метку
`domainKeyName`. `local-clone-path` должен быть уникален на домен, а домен без
`git.repository-url` пропускается целиком.

Если `domains` пуст и задано legacy-свойство `lakehouse.config.vcs.git.repository-url`, этот
единственный репозиторий публикуется как один домен с именем `default`; как только в `domains`
появится хотя бы одна запись, legacy-блок игнорируется и **не** становится дополнительным доменом
`default`.

Сам цикл синхронизации глобален, а не посегментный по доменам:
`lakehouse.config.vcs.git.sync.*` (enabled / interval-ms / initial-delay-ms). Полное описание
дерева, порядка применения (`priority`, родители перед детьми), правил изоляции и legacy-блока:
[Домены](../content_configuration/domains.md).