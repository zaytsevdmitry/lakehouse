# lakehouse-config-svc

Сервис управления метаданными - единое хранилище всех конфигураций lakehouse. Является системой записи (source of truth) для метаданных, на основе которых остальные сервисы (scheduler-svc, task-executor-svc, state-svc) выполняют обработку данных.

## Обзор

`lakehouse-config-svc` хранит и обслуживает метаданные lakehouse:

- **Домены** - иерархия окружений конфигурации; каждый домен обеспечен собственным Git-репозиторием и проставляется на все объекты, загруженные из него, как `domainKeyName` (см. [Домены](#домены))
- **Драйверы** - настройки подключения к вычислительным кластерам
- **Источники данных** - подключения к внешним хранилищам (JDBC/Spark)
- **Датасеты** - описание таблиц, колонок, ограничений
- **Расписания** - периодичность обработки данных (интервалы, сценарии актов, задачи)
- **Метрики качества данных** - проверки DQ
- **Скрипты и SQL-шаблоны** - шаблоны запросов с Jinjava-подстановками
- **Линковка данных** - связи происхождения данных (lineage)
- **TaskExecutionServiceGroups** - группы исполнителей задач
- **Декларативная конфигурация из Git (GitOps/VCS)** - те же конфигурационные DTO могут быть заданы YAML-файлами в Git-репозитории и автоматически синхронизированы в БД подсистемой VCS (см. [GitOps: декларативная конфигурация из Git-репозитория (VCS)](#gitops-декларативная-конфигурация-из-git-репозиториев-vcs))

Конфигурации задаются в виде DTO, хранятся в PostgreSQL и отдаются через REST API. Изменения конфигураций транслируются в Kafka (topic `configuration_changes`) как `ConfigurationChangeDTO` (kind/keyName/action/object), чтобы scheduler-svc строил актуальные инстансы расписаний.

## Архитектура

```
┌───────────────────────┐     REST (CRUD)      ┌───────────────────────────┐
│  Admin / UI / CLI     │ ────────────────────▶│   lakehouse-config-svc    │
└───────────────────────┘                      │   (REST API /v1_0/configs)│
                                               │                           │
┌───────────────────────┐     REST (чтение)    │  ┌─────────────────────┐  │
│  scheduler-svc        │ ────────────────────▶│  │ ConfigService       │  │
│  task-executor-svc    │                      │  │ (CRUD + merge DTO)  │  │
│  state-svc            │                      │  └─────────────────────┘  │
└───────────────────────┘                      │           │               │
                                               │           ▼               │
                                               │  ┌─────────────────────┐  │
                                               │  │ PostgreSQL          │  │
                                               │  │ (schema lakehouse_  │  │
                                               │  │       config)       │  │
                                               │  └─────────────────────┘  │
                                               │           │               │
                                               │           ▼  Kafka        │
                                               │  InternalScheduler        │
                                               │  schedule_effective_      │
                                               │  changes                  │
                                               └───────────────────────────┘
```

- **Controller** - REST-эндпоинты CRUD для каждого типа метаданных + compound-эндпоинты для производных объектов.
- **Service** - бизнес-логика: валидация, приведение DTO к сущностям и обратно, объединение шаблонных и частных конфигураций через `DtoMergeUtils`.
- **Repository (JPA/Hibernate)** - персистентность в PostgreSQL.
- **InternalScheduler** - периодическая отправка изменений расписаний в Kafka.
- Метаданные связаны иерархически (домен → datasource → dataset → ...); иерархия доменов задается в `lakehouse.config.vcs.domains` (см. раздел GitOps), схема зависимостей описана в [content_configuration](content_configuration/content_configuration.md).

## GitOps: декларативная конфигурация из Git-репозиториев (VCS)

Помимо REST API `lakehouse-config-svc` умеет управлять конфигурацией декларативно: те же DTO метаданных записываются YAML-файлами в Git-репозитории, а подсистема VCS (Configuration Versioning System) по расписанию синхронизирует их в базу данных. Репозитории становятся источником истины (source of truth) и хранят полную историю изменений каждой конфигурации (подход GitOps).

Конфигурация организована в виде **доменов**. Каждый домен обеспечен собственным Git-репозиторием и синхронизируется независимо. Домен может содержать **вложенные домены** (например, `platform` ⊃ `processing` ⊃ `analytics`); домены применяются в порядке приоритета (по возрастанию, родители перед вложенными). Каждый конструкт, загруженный из репозитория домена, помечается именем домена (`domainKeyName`), что разграничивает объекты по доменам и позволяет проверять ссылки между доменами.

Сама модель доменов (дерево, принадлежность, правила изоляции, настройка) описана в
[Домены](#домены) и в [content_configuration/domains.md](content_configuration/domains.md).

Подробная документация: [Подсистема VCS для разработчиков платформы](vcs/cvs_for_developers.md) (внутреннее устройство, абстракция `VcsClient`, точки расширения, параметры разработчика) и [Руководство пользователя git-расширения](vcs/git_extension_user_guide.md) (формат YAML, `isVcsManaged`, настройка и сообщения об ошибках).

```
┌──────────────────────┐   fetch + diff по доменам  ┌───────────────────────────────┐
│  Git-репозитории     │ ──────────────────────────▶ │  GitOpsScheduler              │
│  (по одному на домен,│   pull → построение набора  │  домены в порядке приоритета  │
│  ветка main)         │   изменений → применение    └───────────────┬───────────────┘
└──────────────────────┘   в одной транзакции                        │
                                                                     ▼
                                                     ┌───────────────────────────────┐
                                                     │  Слой ConfigService           │
                                                     │  (apply/delete DTO)           │
                                                     └───────────────┬───────────────┘
                                                                     ▼
                                                     ┌───────────────────────────────┐
                                                     │  PostgreSQL                   │
                                                     │  + vcs_sync_log (SUCCESS/FAILED)│
                                                     │  + vcs_object_log (по объектам)│
                                                     └───────────────────────────────┘
```

### Домены и структура репозитория

Демостек обслуживает по одному bare-репозиторию на домен по адресу `git://git-server:9418/<domain>.git`. Репозиторий домена - это набор YAML-файлов, один конструкт в файле. Каждый файл начинается с поля `kind` (в стиле Kubernetes), которое выбирает целевой DTO; остальная часть файла связывается с этим DTO (неизвестные свойства - ошибка, значения перечислений регистронезависимы).

Демо-структура (`demo/compose/conf_git/domains/`):

```
domains/
├── platform/
│   ├── datasources/lakehousestorage.yaml
│   ├── drivers/postgres.yaml
│   ├── sql-scripts/sql-template-postgres/insertDML.yaml
│   └── scenarios/spark.yaml
├── analytics/
│   ├── datasets/transaction_dds.yaml
│   ├── quality/metrics/transaction_dds_qm.yaml
│   └── schedules/regular.yaml
└── processing/
    ├── datasets/client_processing.yaml
    └── schedules/generateSource.yaml
```

Три каталога - это три соседних Git-репозитория; демо *объявляет* их вложенной цепочкой
(`platform` ⊃ `processing` ⊃ `analytics`) - см. блок `domains` в разделе [Домены](#домены).

```yaml
kind: DataSource
keyName: processingdb
description: Remote datastore processingdb
dataSourceType: database
databaseProtocol: postgresql
service:
  host: "172.20.193.10"
  port: "5432"
  urn: postgresDB
  properties:
    user: postgresUser
    fetchSize: "10000"
```

SQL-скрипты хранятся так же через `kind: Script` и два поля - глобальный ключ скрипта `key` (слеши пути заменяются точками) и тело скрипта в литеральном `value`. Скрипты — только контент и общие между доменами по ключу: их DTO не помечается доменом, а домен в строке `SQLTemplate` наследуется от `Driver` или `Task`, который на них ссылается:

```yaml
kind: Script
key: dq.non_zero_count.sql
value: |
  select count(1) value
  from {{ refCat(targetDataSetKeyName) }}
```

### Поддерживаемые kind

Применяются в порядке зависимостей (удаление происходит в обратном порядке):

| kind | Пример файла | Первичный ключ |
|---|---|---|
| `Driver` | `drivers/postgres.yaml` | `keyName` |
| `DataSource` | `datasources/processingdb.yaml` | `keyName` |
| `Script` | `sql-scripts/dq/non_zero_count.yaml` | `key` |
| `TaskExecutionServiceGroup` | `taskexecutionservicegroups/database.yaml` | `name` |
| `Task` | `tasks/prepare-jdbc.yaml` | `name` |
| `DataSet` | `datasets/1_transaction_dds.yaml` | `keyName` |
| `ScenarioActTemplate` | `scenarios/spark-dq.yaml` | `keyName` |
| `QualityMetricsConf` | `quality/metrics/transaction_dds_qm.yaml` | `keyName` |
| `Schedule` | `schedules/regular.yaml` | `keyName` |

Kinds `ERDiagram` (`erdiagrams/`) и `DataLineageDiagram` (`datalineagediagrams/`) хранятся в репозитории, но сервисом конфигурации **не применяются**.

### Семантика синхронизации

- Каждый цикл обходит настроенные домены в порядке приоритета (родители перед вложенными, домены одного уровня по `priority` по возрастанию) и синхронизирует репозиторий каждого домена независимо: pull → diff с последним **успешно** применённым коммитом этого домена → применение в одной транзакции. На пустой базе весь head домена трактуется как набор созданных файлов. Вложенные домены иерархически зависят от родителя: если синхронизация домена завершилась ошибкой, всё его поддерево пропускается, пока родитель снова не применится успешно.
- Первое применение коммита выполняется в **одной транзакции**: созданные и изменённые конструкты применяются в порядке `kind` выше (датасеты дополнительно - в порядке зависимостей по `sources`), удаляемые - в обратном порядке, и только затем пишется маркер `SUCCESS`. Любая ошибка откатывает весь коммит.
- Каждый конструкт, применённый из репозитория домена, сохраняется с `domainKeyName` = имя домена, поэтому в строке известен его владелец. Ключ остаётся сквозным: `keyName` - это первичный ключ, поэтому между доменами он не переиспользуется (два домена, указывающие на одну таблицу, используют два разных `keyName`). Исключение - скрипты (домена нет вовсе, общие по ключу; домен в строке `SQLTemplate` наследуется от ссылающихся драйвера или задачи).
- Каждый затронутый коммитом конструкт записывается в `vcs_object_log` (`date_time_rec`, `object_name` из `keyName`, `kind`, `file_path` - путь относительно корня репозитория, `commit_id`, `domain_key_name`) - как для применённых, так и для снятых с управления файлов. Строки лога синхронизации `vcs_sync_log` также содержат `domain_key_name`.
- Коммит, не прошедший разбор YAML, валидацию или ограничение БД, фиксируется как `FAILED` (с `domain_key_name` и текстом ошибки) и **больше не повторяется**; последующий исправляющий коммит просто включает исправленное содержимое в новый diff.
- `Schedule` может ссылаться только на датасеты своего домена: иначе коммит завершится ошибкой `DataSetDomainConflictException` и будет записан как `FAILED`.
- Инфраструктурные ошибки (недоступен репозиторий, отсутствует локальный клон) только логируются и повторяются на следующем цикле. Домены без настроенного репозитория пропускаются.
- Коммиты, чья пара `(commit_id, domain_key_name)` уже есть в `vcs_sync_log`, пропускаются. Переименование файла трактуется как удаление + создание. Файлами конфигурации считаются только `*.yaml`, `*.yml` и `*.json`; всё остальное (например `load.sh`) игнорируется.

### Флаг управления VCS

Каждый конструкт, загруженный из репозитория, получает `isVcsManaged=true`; для конструктов, созданных через REST API, он остаётся `false`.

- Удаление YAML-файла из репозитория **не удаляет конструкт** - сервис лишь сбрасывает `isVcsManaged` на соответствующей сущности. Само удаление пользователь затем выполняет через REST API.
- Любое действие `POST`/`PUT`/`DELETE` через REST API над конструктом с `isVcsManaged=true` отклоняется ответом `409 Conflict` (`VcsManagedException`): чтобы изменить или удалить управляемый конструкт через REST API, сначала удалите его из репозитория. REST API также отклоняет изменения, перемещающие конструкт за пределы его домена (`DomainConflictException`).

### Конфигурация

Все параметры живут под префиксом `lakehouse.config.vcs.*` (см. также [appconf/service_configuration.md](appconf/service_configuration.md)):

| Свойство | Переменная окружения | По умолчанию | Описание |
|---|---|---|---|
| `lakehouse.config.vcs.git.sync.enabled` | `LAKEHOUSE_CONFIG_GIT_SYNC_ENABLED` | `false` | Включает бин планировщика VCS |
| `lakehouse.config.vcs.git.sync.interval-ms` | `LAKEHOUSE_CONFIG_GIT_SYNC_INTERVAL_MS` | `30000` | Период цикла |
| `lakehouse.config.vcs.git.sync.initial-delay-ms` | `LAKEHOUSE_CONFIG_GIT_SYNC_INITIAL_DELAY_MS` | `10000` | Задержка первого цикла после старта |
| `lakehouse.config.vcs.domains.<name>.git.repository-url` | `LAKEHOUSE_CONFIG_GIT_<NAME>_URL` | - | URL репозитория домена (поддерживаются `git://`, `ssh://` и `http(s)://`) |
| `lakehouse.config.vcs.domains.<name>.git.branch` | `LAKEHOUSE_CONFIG_GIT_<NAME>_BRANCH` | `main` | Синхронизируемая ветка |
| `lakehouse.config.vcs.domains.<name>.git.local-clone-path` | `LAKEHOUSE_CONFIG_GIT_<NAME>_CLONE_PATH` | - | Локальный путь, где сервис хранит клон домена |
| `lakehouse.config.vcs.domains.<name>.git.private-key-path` | `LAKEHOUSE_CONFIG_GIT_<NAME>_PRIVATE_KEY_PATH` | - | Путь к SSH-ключу (только для URL вида `ssh://`) |
| `lakehouse.config.vcs.domains.<name>.priority` | - | наименьший | Позиция домена в порядке применения (по возрастанию, родители перед вложенными) |
| `lakehouse.config.vcs.domains.<name>.domains` | - | - | Вложенные поддомены |

Для домена с именем, например, `platform` переменные окружения — `LAKEHOUSE_CONFIG_GIT_PLATFORM_URL`, `LAKEHOUSE_CONFIG_GIT_PLATFORM_BRANCH`, `LAKEHOUSE_CONFIG_GIT_PLATFORM_CLONE_PATH` и `LAKEHOUSE_CONFIG_GIT_PLATFORM_PRIVATE_KEY_PATH`.

Поставляемый `application.yml` объявляет только legacy-блок `lakehouse.config.vcs.git.*`, поэтому дерево доменов задаётся развёртыванием - см. [Домены](#домены).

### Демо

Стек `demo/compose` запускает лёгкий git-сервер (`git-server`, образ `alpine/git` с пакетом `git-daemon`), который обслуживает **по одному bare-репозиторию на домен** в персистентном томе. При старте импортирует каждый каталог `demo/compose/conf_git/domains` в свой одноимённый репозиторий (`platform`, `analytics`, `processing`) — при первом старте корневым коммитом, при последующих только изменения — и отдаёт их по `git://`. `lakehouse-config-svc` настраивается на три доменных репозитория (`git://git-server:9418/{platform,analytics,processing}.git`), ветку `main` и `sync.enabled=true`, поэтому при старте применяет всю демо-конфигурацию из git вместо REST-загрузки `load.sh`.

Демо объявляет домены **вложенной цепочкой** `platform` ⊃ `processing` ⊃ `analytics`, хотя с точки
зрения Git три репозитория соседние. Это показывает, что вложенность - решение конфигурации,
а не ограничение хранения:

```
-Dlakehouse.config.vcs.git.sync.enabled=true
-Dlakehouse.config.vcs.domains.platform.priority=0
-Dlakehouse.config.vcs.domains.platform.git.repository-url=git://git-server:9418/platform.git
-Dlakehouse.config.vcs.domains.platform.domains.processing.priority=0
-Dlakehouse.config.vcs.domains.platform.domains.processing.git.repository-url=git://git-server:9418/processing.git
-Dlakehouse.config.vcs.domains.platform.domains.processing.domains.analytics.priority=0
-Dlakehouse.config.vcs.domains.platform.domains.processing.domains.analytics.git.repository-url=git://git-server:9418/analytics.git
```

## Домены

**Домен** - единица изоляции сервиса. Домен одновременно является логической областью
метаданных и Git-репозиторием: один репозиторий на домен, один независимый цикл
синхронизации на репозиторий и имя домена, проставляемое на каждый загруженный из него
конструкт как `domainKeyName`. Домен никогда не входит в содержимое YAML - он определяется
репозиторием, из которого пришёл файл, поэтому один и тот же файл означает разное в разных
доменах.

```
lakehouse.config.vcs.domains  ──▶  orderedDomains()  ──▶  для каждого домена
                                                             fetch + diff
                                                             apply в одной транзакции
                                                             простановка domainKeyName
                                                                 │
                                                                 ▼
                                          DataSet / DataSource / Driver / Task /
                                          ScenarioActTemplate / Schedule /
                                          QualityMetricsConf / TaskExecutionServiceGroup
                                          (идентичность = domainKeyName + keyName)
```

### Дерево, порядок и принадлежность

Домены образуют дерево: `lakehouse.config.vcs.domains.<name>.domains` объявляет вложенные
поддомены. `LakehouseVCSProperties.orderedDomains()` обходит дерево в глубину, поэтому родитель
всегда применяется раньше детей; в пределах одного уровня порядок задаёт `priority` по
возрастанию, домены без `priority` идут последними, а равные приоритеты разрешаются по имени.
Вложенность - **конфигурационная** связь, а не складская: одни и те же репозитории можно
объявить соседними или цепочкой. Её единственное поведенческое следствие - распространение
ошибок: если домен не синхронизировался, всё его поддерево пропускается до успеха родителя,
поскольку дочерний домен может ссылаться на конструкты родителя. Домен без
`git.repository-url` пропускается целиком.

Домен - это атрибут владения, а не часть идентичности объекта: `keyName` остаётся
единственным первичным ключом и сквозным, поэтому один и тот же `keyName` не может
существовать в двух доменах. У скриптов (`kind: Script`) домена нет вовсе - они только
контент и общие, а домен в строке `SQLTemplate` наследуется от ссылающихся `Driver` или
`Task`.

### Правила изоляции

- `Schedule` может ссылаться только на датасеты своего домена. Ссылка на датасет другого
  домена или на созданный вручную датасет, не принадлежащий ни одному домену, проваливает
  весь коммит с `DataSetDomainConflictException`.
- REST API отклоняет обновление, которое перезаписало бы конструкт одного домена конструктом
  другого: `DomainConflictException` → `409 Conflict`. Конструкт, у которого сохранённый или
  входящий домен равен `null`, не проверяется.
- Затрагиваются только REST-операции `Save`/`Delete` доменных видов; см.
  [флаг VCS-управления](#флаг-управления-vcs).

### Как настраиваются домены

Всё связывается `LakehouseVCSProperties` под префиксом `lakehouse.config.vcs`:

```yaml
lakehouse:
  config:
    vcs:
      domains:
        platform:
          priority: 0
          git:
            repository-url: git://git-server:9418/platform.git
            branch: main
            local-clone-path: /tmp/platform
            private-key-path:            # только для ssh://
          domains:                        # вложенные поддомены
            processing:
              priority: 0
              git:
                repository-url: git://git-server:9418/processing.git
                branch: main
                local-clone-path: /tmp/processing
```

`local-clone-path` должен быть уникален для каждого домена, поскольку каждый домен держит там
свой клон. Таблица доменных свойств и шаблон переменных окружения
(`LAKEHOUSE_CONFIG_GIT_<NAME>_URL`, `..._<NAME>_BRANCH`, `..._<NAME>_CLONE_PATH`,
`..._<NAME>_PRIVATE_KEY_PATH`) приведены в разделе [Конфигурация](#конфигурация) выше.

**Legacy-конфигурация с одним репозиторием.** Свойства, существовавшие до доменов,
`lakehouse.config.vcs.git.repository-url` / `.branch` / `.local-clone-path` /
`.private-key-path`, продолжают работать: если `domains` **пуст** и `git.repository-url` не
пуст, этот единственный репозиторий публикуется как один домен с именем `default`. Как только
в `domains` есть хотя бы одна запись, legacy-блок игнорируется - дополнительного домена
`default` не возникает. Сам планировщик (`git.sync.*`) глобален и посегментно не объявляется.

### Наблюдаемость: из какого домена пришло изменение

Обе таблицы журналов VCS несут домен, и оба read-only REST-эндпоинта принимают его как
фильтр:

| Таблица / эндпоинт | Колонка / параметр |
|---|---|
| `vcs_sync_log` | `domain_key_name` |
| `vcs_object_log` | `domain_key_name` |
| `GET /v1_0/configs/vcs/logs` | `domainKeyName` (необязателен; обязательны `from`/`to`, необязательны `status`, `commitId`) |
| `GET /v1_0/configs/vcs/objectlogs` | `domainKeyName` (необязателен; обязателен `commitId` либо обе границы `from` и `to`) |

Пара `(commit_id, domain_key_name)`, для которой уже есть строка синхронизации, пропускается,
поэтому один и тот же commit id, применённый двумя разными доменами, отслеживается раздельно.

`domainKeyName` также выходит за пределы сервиса: он входит в REST-представление доменных DTO
и DTO журналов синхронизации и потребляется `lakehouse-ui-svc` (read-only-фильтр журналов VCS)
и `lakehouse-scheduler-svc` (проверка домена у `TaskExecutionServiceGroup`).

Справочник по доменам самих объектов конфигурации -
[content_configuration/domains.md](content_configuration/domains.md); внутреннее устройство
разрешения дерева - в [Подсистема VCS для разработчиков платформы](vcs/cvs_for_developers.md#6-параметры-разработчика).

## Модули

### lakehouse-config-svc

Spring Boot-приложение, реализующее REST API и хранилище метаданных. Точка входа: `org.lakehouse.config.LakehouseConfigApplication`.

### lakehouse-config-rest-client

Java-клиент (`ConfigRestClientApi`/`ConfigRestClientApiImpl`) для доступа к `lakehouse-config-svc` из других сервисов (scheduler-svc, task-executor-svc и др.). Выполняет типизированные запросы к эндпоинтам `/v1_0/configs/...` через `RestClientHelper`. Базовый URL задается свойством `lakehouse.client.rest.config.server.url`.

## API Endpoints

Описание структуры эндпоинтов и конфигураций метаданных находится в разделе [content_configuration](content_configuration/content_configuration.md).

## Конфигурация

Параметры приложения (datasource, JPA, настройки отправки расписаний в Kafka, health-эндпоинты) описаны в [appconf/service_configuration.md](appconf/service_configuration.md).

## Безопасность

`lakehouse-config-svc` защищен по OAuth 2.0 / OIDC с Keycloak в качестве identity provider (realm `lakehouse`). Spring Security настроен как **OAuth2 resource server**: каждый запрос должен содержать валидный JWT, выпущенный этим realm, иначе сервис возвращает `401`.

### Аутентификация

- **Запросы пользователей** (UI BFF, CLI, прямые вызовы API) - JWT проверяется по `spring.security.oauth2.resourceserver.jwt.issuer-uri`. Роли из claim'а `realm_access.roles` конвертируются в authorities `ROLE_<ИМЯ>` классом `KeycloakRoleConverter` и могут использоваться в `@PreAuthorize`.
- **Межсервисные вызовы** (`BearerTokenClientHttpRequestInterceptor`) - если запрос инициирован фоновой задачей (в `SecurityContext` нет пользовательского JWT), исходящий `RestClient` получает токен `client_credentials` через `OAuth2AuthorizedClientManager` по регистрации `keycloak-internal` (клиент `lakehouse-internal-client`) и добавляет его как `Authorization: Bearer`. При наличии пользовательского JWT он пробрасывается без изменений.
- Безопасность можно полностью отключить свойством `lakehouse.security.enabled=false` (все запросы становятся анонимными).

### Аудит

`AuditLoggingFilter` пишет одну строку на каждый запрос в логгер `AUDIT_LOG` (файл `logs/audit.log`):

```
User ID: <subject>, Username: <preferred_username>, Method: <method>, URI: <uri>, HTTP status: <status>
```

Для токенов, полученных через service account, вместо имени пользователя подставляется значение `lakehouse.security.audit.service-account-name` (по умолчанию `system`).

### Необходимые настройки

| Свойство / env | По умолчанию | Описание |
|---|---|---|
| `KEYCLOAK_ISSUER_URI` | `http://lakehouse-auth-svc:8080/realms/lakehouse` | URL realm'а Keycloak |
| `KEYCLOAK_INTERNAL_CLIENT_SECRET` | `super-secret-internal-key-987654321` | Секрет клиента `lakehouse-internal-client` |
| `lakehouse.security.enabled` | `true` | `false` полностью отключает безопасность |
| `lakehouse.security.audit.service-account-name` | `system` | Имя пользователя в аудите для service account токенов |
| `lakehouse.security.oauth2.internal-client-id` | `lakehouse-internal-client` | Клиент, идентифицирующий service account токены (claim `azp`) |
| `lakehouse.security.oauth2.client-registration-id` | `keycloak-internal` | OAuth2-регистрация, используемая интерцептором |

`spring.security.oauth2.resourceserver.jwt.issuer-uri` и блок `spring.security.oauth2.client` (регистрация `keycloak-internal`) преднастроены в `src/main/resources/application.yml`.

Пути из белого списка (токен не требуется): `/healthz`, `/readyz`, `/actuator/**`, `/v3/api-docs/**`, `/swagger-ui/**`. Пути Swagger активны, только пока включен Swagger; в профиле `prod` он полностью отключается (`springdoc.api-docs.enabled: false`, `springdoc.swagger-ui.enabled: false`).

### Realm Keycloak

В realm `lakehouse` должны быть:

- **`lakehouse-internal-client`** - confidential-клиент с включенными *Service Accounts* (межсервисные вызовы);
- **`lakehouse-ui-client`** - confidential-клиент со включенным *Standard Flow* (вход пользователей через UI BFF);
- роли realm'а `USER` / `ADMIN` (опционально, используются в `@PreAuthorize`).

Эталонный realm для импорта: `demo/compose/conf_infra/security/realms/lakehouse-realm.json`.