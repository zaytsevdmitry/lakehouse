# lakehouse-config-svc

Сервис управления метаданными - единое хранилище всех конфигураций lakehouse. Является системой записи (source of truth) для метаданных, на основе которых остальные сервисы (scheduler-svc, task-executor-svc, state-svc) выполняют обработку данных.

## Обзор

`lakehouse-config-svc` хранит и обслуживает метаданные lakehouse:

- **Пространства имен** - логическое разделение окружений
- **Драйверы** - настройки подключения к вычислительным кластерам
- **Источники данных** - подключения к внешним хранилищам (JDBC/Spark)
- **Датасеты** - описание таблиц, колонок, ограничений
- **Расписания** - периодичность обработки данных (интервалы, сценарии актов, задачи)
- **Метрики качества данных** - проверки DQ
- **Скрипты и SQL-шаблоны** - шаблоны запросов с Jinjava-подстановками
- **Линковка данных** - связи происхождения данных (lineage)
- **TaskExecutionServiceGroups** - группы исполнителей задач
- **Декларативная конфигурация из Git (GitOps/CVS)** - те же конфигурационные DTO могут быть заданы YAML-файлами в Git-репозитории и автоматически синхронизированы в БД подсистемой CVS (см. [GitOps: декларативная конфигурация из Git-репозитория (CVS)](#gitops-декларативная-конфигурация-из-git-репозитория-cvs))

Конфигурации задаются в виде DTO, хранятся в PostgreSQL и отдаются через REST API. Изменения расписаний транслируются в Kafka (topic `schedule_effective_changes`), чтобы scheduler-svc строил актуальные инстансы расписаний.

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
- Метаданные связаны иерархически (namespace → datasource → dataset → ...), схема зависимостей описана в [content_configuration](content_configuration/content_configuration.md).

## GitOps: декларативная конфигурация из Git-репозитория (CVS)

Помимо REST API `lakehouse-config-svc` умеет управлять конфигурацией декларативно: те же DTO метаданных записываются YAML-файлами в Git-репозиторий, а подсистема CVS (Configuration Versioning System) по расписанию синхронизирует их в базу данных. Репозиторий становится источником истины (source of truth) и хранит полную историю изменений каждой конфигурации (подход GitOps).

```
┌──────────────────┐   fetch + diff   ┌───────────────────────────────┐
│  Git-репозиторий │ ───────────────▶ │  GitOpsScheduler             │
│  (ветка main)    │                  │  pull → построение набора     │
└──────────────────┘                  │  изменений → применение       │
                                      │  в одной транзакции           │
                                      └───────────────┬───────────────┘
                                                      │
                                                      ▼
                                      ┌───────────────────────────────┐
                                      │  Слой ConfigService           │
                                      │  (apply/delete DTO)           │
                                      └───────────────┬───────────────┘
                                                      ▼
                                      ┌───────────────────────────────┐
                                      │  PostgreSQL                   │
                                      │  + cvs_sync_log (SUCCESS/FAILED)│
                                      │  + cvs_object_log (по объектам)│
                                      └───────────────────────────────┘
```

### Структура репозитория

Репозиторий конфигурации - это набор YAML-файлов, один конструкт в файле. Каждый файл начинается с поля `kind` (в стиле Kubernetes), которое выбирает целевой DTO; остальная часть файла связывается с этим DTO (неизвестные свойства - ошибка, значения перечислений регистронезависимы).

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

SQL-скрипты хранятся так же через `kind: Script` и два поля - глобальный ключ скрипта `key` (слеши пути заменяются точками) и тело скрипта в литеральном `value`:

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
| `NameSpace` | `nameSpaces/demo.yaml` | `keyName` |
| `Driver` | `drivers/postgres.yaml` | `keyName` |
| `DataSource` | `datasources/processingdb.yaml` | `keyName` |
| `Script` | `sql-scripts/dq/non_zero_count.yaml` | `key` |
| `TaskExecutionServiceGroup` | `taskexecutionservicegroups/database.yaml` | `name` |
| `Task` | `tasks/prepare-jdbc.yaml` | `name` |
| `DataSet` | `datasets/1_transaction_dds.yaml` | `keyName` |
| `ScenarioActTemplate` | `scenarios/spark-dq.yaml` | `keyName` |
| `QualityMetricsConf` | `quality/metrics/transaction_dds_qm.yaml` | `keyName` |
| `Schedule` | `schedules/regular.yaml` | `keyName` |

### Семантика синхронизации

- Каждый цикл подтягивает настроенную ветку и сравнивает её head с последним **успешно** применённым коммитом (`cvs_sync_log` со статусом `SUCCESS`); на пустой базе весь head трактуется как набор созданных файлов.
- Первое применение коммита выполняется в **одной транзакции**: созданные и изменённые конструкты применяются в порядке `kind` выше (датасеты дополнительно - в порядке зависимостей по `sources`), удаляемые - в обратном порядке, и только затем пишется маркер `SUCCESS`. Любая ошибка откатывает весь коммит.
- Каждый затронутый коммитом конструкт записывается в `cvs_object_log` (`date_time_rec`, `object_name` из `keyName`, `kind`, `file_path` - путь относительно корня репозитория, `commit_id`) - как для применённых, так и для снятых с управления файлов.
- Коммит, не прошедший разбор YAML, валидацию или ограничение БД, фиксируется как `FAILED` вместе с текстом ошибки и **больше не повторяется**; последующий исправляющий коммит просто включает исправленное содержимое в новый diff.
- Инфраструктурные ошибки (недоступен репозиторий, отсутствует локальный клон) только логируются и повторяются на следующем цикле.
- Коммиты, чей id уже есть в `cvs_sync_log`, пропускаются. Переименование файла трактуется как удаление + создание. Файлами конфигурации считаются только `*.yaml`, `*.yml` и `*.json`; всё остальное (например `load.sh`) игнорируется.

### Флаг управления CVS

Каждый конструкт, загруженный из репозитория, получает `isCvsManaged=true`; для конструктов, созданных через REST API, он остаётся `false`.

- Удаление YAML-файла из репозитория **не удаляет конструкт** - сервис лишь сбрасывает `isCvsManaged` на соответствующей сущности. Само удаление пользователь затем выполняет через REST API.
- Любое действие `POST`/`PUT`/`DELETE` через REST API над конструктом с `isCvsManaged=true` отклоняется ответом `409 Conflict` (`CvsManagedException`): чтобы изменить или удалить управляемый конструкт через REST API, сначала удалите его из репозитория.

### Конфигурация

Все параметры живут под префиксом `lakehouse.config.cvs.*` (см. также [appconf/service_configuration.md](appconf/service_configuration.md)):

| Свойство | Переменная окружения | По умолчанию | Описание |
|---|---|---|---|
| `lakehouse.config.cvs.git.repository-url` | `LAKEHOUSE_CONFIG_GIT_REPOSITORY_URL` | - | URL репозитория конфигурации (поддерживаются `git://`, `ssh://` и `http(s)://`) |
| `lakehouse.config.cvs.git.branch` | `LAKEHOUSE_CONFIG_GIT_BRANCH` | `main` | Синхронизируемая ветка |
| `lakehouse.config.cvs.git.local-clone-path` | `LAKEHOUSE_CONFIG_GIT_LOCAL_CLONE_PATH` | - | Локальный путь, где сервис хранит свой клон |
| `lakehouse.config.cvs.git.private-key-path` | `LAKEHOUSE_CONFIG_GIT_PRIVATE_KEY_PATH` | - | Путь к SSH-ключу (только для URL вида `ssh://`) |
| `lakehouse.config.cvs.git.sync.enabled` | `LAKEHOUSE_CONFIG_GIT_SYNC_ENABLED` | `false` | Включает бин планировщика CVS |
| `lakehouse.config.cvs.git.sync.interval-ms` | `LAKEHOUSE_CONFIG_GIT_SYNC_INTERVAL_MS` | `30000` | Период цикла |
| `lakehouse.config.cvs.git.sync.initial-delay-ms` | `LAKEHOUSE_CONFIG_GIT_SYNC_INITIAL_DELAY_MS` | `10000` | Задержка первого цикла после старта |

### Демо

Стек `demo/compose` запускает лёгкий git-сервер (`git-server`, образ `alpine/git` с пакетом `git-daemon`), который держит bare-репозиторий в персистентном томе, импортирует `demo/compose/conf_git` (YAML-зеркало `demo/compose/conf`) в ветку `main` — при первом старте корневым коммитом, при последующих только изменения — и отдаёт его по `git://`. `lakehouse-config-svc` настраивается на `git://git-server:9418/config-repo.git`, ветку `main` и `sync.enabled=true`, поэтому при старте применяет всю демо-конфигурацию из git одной транзакцией вместо REST-загрузки `load.sh`.

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