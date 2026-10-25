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
        default_schema: yarina_config #
        
yarina:
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

    vcs: # Подсистема GitOps (VCS): сам цикл синхронизации (глобальный, не посегментный)
      sync: # не зависит от VCS-бэкенда, поэтому отдельный блок vcs.sync
        enabled: ${YARINA_CONFIG_GIT_SYNC_ENABLED:false} # включает бин планировщика
        interval-ms: ${YARINA_CONFIG_GIT_SYNC_INTERVAL_MS:30000} # период цикла
        initial-delay-ms: ${YARINA_CONFIG_GIT_SYNC_INITIAL_DELAY_MS:10000} # задержка первого цикла

  domains: # у каждого домена собственный репозиторий; список и имена доменов задаёт пользователь
    # связывается общим DomainSettings из yarina-domain-api (пространство имён yarina.domains.*)
    <domain>: # любое имя; домен может содержать вложенные домены
      priority: 0 # порядок применения: по возрастанию, родители перед вложенными
      git:
        repository-url: ${YARINA_DOMAINS_<DOMAIN>_GIT_REPOSITORY_URL:} # например git://git-server:9418/<domain>.git
        branch: ${YARINA_DOMAINS_<DOMAIN>_GIT_BRANCH:main} # синхронизируемая ветка
        local-clone-path: ${YARINA_DOMAINS_<DOMAIN>_GIT_LOCAL_CLONE_PATH:} # где сервис хранит клон домена
        credentials: # по-доменные, никогда не сливаются; пустой блок — анонимный доступ
          auth-type: ssh # необязателен: ssh | token | basic; выводится из заданных учётных данных
          ssh-private-key-path: /keys/<domain>_ed25519 # для auth-type == ssh
      domains:
        <nested-domain>: # вложенный поддомен родительского домена
          priority: 1
          git:
            repository-url: ${YARINA_DOMAINS_<NESTED_DOMAIN>_GIT_REPOSITORY_URL:}
            branch: ${YARINA_DOMAINS_<NESTED_DOMAIN>_GIT_BRANCH:main}
            local-clone-path: ${YARINA_DOMAINS_<NESTED_DOMAIN>_GIT_LOCAL_CLONE_PATH:}
            credentials: # учётные данные доменов могут отличаться внутри одного дерева
              auth-type: token
              token: ${<NESTED_DOMAIN>_GIT_TOKEN:} # для auth-type == token

health: # Эндпоинты проверки состояния сервиса
    liveness-path: /healthz # Лiveness-проба
    readiness-path: /readyz # Readiness-проба
```

## Домены

`yarina.domains` связывается общим `DomainSettings` из `yarina-domain-api`
(пространство имён `yarina.domains.<name>.*`; каноническая справка:
[yarina-domain-api/doc-ru/readme.md](../../../yarina-domain-api/doc-ru/readme.md)); карта
`domains` выше - единственное место, где объявляются репозитории. Каждый домен - это один
Git-репозиторий, он забирается и применяется сам по себе, и каждый загруженный из него
конструкт получает метку `domainKeyName`. `local-clone-path` должен быть уникален на домен,
а домен без `git.repository-url` неактивен: этот сервис пропускает его целиком, а
`yarina-ui-svc` падает при попытке использовать такой домен.

Сам цикл синхронизации глобален, а не посегментный по доменам:
`yarina.config.vcs.sync.*` (enabled / interval-ms / initial-delay-ms). Полное описание
дерева, порядка применения (`priority`, родители перед детьми), правил изоляции и того, как
видно, из какого домена пришло изменение:
[Домены](../content_configuration/domains.md).

## Доступ к Git

Каждый доменный репозиторий читается единой git-интеграцией: локальный клон на домен
ведётся через JGit и синхронизируется на каждом цикле. `repository-url` принимает любую
git-форму — `git://`, `ssh://`, `http(s)://` или локальный путь.

Аутентификация задаётся **по доменам** в `yarina.domains.<name>.git.credentials`:
`token` отправляет токен как HTTP-пароль, `basic` — HTTP Basic, `ssh` использует
`ssh-private-key-path` домена. Глобального/системного аккаунта нет и слияния нет: домен без
учётных данных обращается анонимно, а разные домены могут аутентифицироваться разными
аккаунтами. Полная настройка:
[Руководство пользователя git-расширения](../vcs/git_extension_user_guide.md).
