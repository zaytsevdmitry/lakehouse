# Сервис веб-интерфейса (jarina-ui-svc)

Веб-интерфейс управления jarina: единая точка визуализации и администрирования сервисов, каталога данных, расписаний, состояний датасетов, Spark-подписок и метаданных-ориентированной конфигурации («Modelling»).

## Обзор

`jarina-ui-svc` — сервис, агрегирующий данные всех остальных сервисов jarina и предоставляющий единый веб-интерфейс. Для мониторинговых разделов он является тонким слоем агрегации: обращается к другим сервисам через их REST-клиенты и отдаёт результат фронтенду. Дополнительно сервис размещает поверхность **Modelling** — интерактивный редактор конфигурационных документов, хранящихся в Git-репозиториях доменов конфигурации (рабочие пространства, ветки, отправка на ревью), которая обслуживается самим сервисом. Каждый домен владеет своим репозиторием, а рабочее пространство — это набор выборов `(домен, ветка)` — см. [Домены](#домены).

Сервис состоит из двух частей:

- **бэкенд** — Spring Boot приложение, которое проксирует запросы фронтенда к сервисам jarina, отдаёт статический фронтенд и размещает бэкенд модделирования (хранилище рабочих пространств, интеграция с Git, редактирование YAML, RBAC);
- **фронтенд** — одностраничное React-приложение (Vite), собираемое в `src/main/resources/static` и раздаваемое тем же сервисом.

Разделы интерфейса:

- **Services** — граф сервисов jarina и их статус (`UP`/`DOWN`) по health-check.
- **Catalog** — дерево каталога данных: источники → схемы → датасеты; просмотр датасета (модель/DDL, линковка, ограничения) и источника данных.
- **Schedules** — список запусков расписаний за интервал, DAG запуска расписания.
- **SparkJobs** — список Spark-подписок через `jarina-task-proxy-for-spark`: создание, статус, kill, kill all, clear.
- **VCS** — журнал синхронизации GitOps-конфигурации (коммиты) и журнал объектов `jarina-config-svc`.
- **Modelling** — рабочее место по модделированию метаданных: создать рабочее пространство из одной или нескольких доменных веток, создавать конфигурационные документы любого поддерживаемого вида (форма по схеме вида, «сырой» YAML или визуальные редакторы — **ER Diagram**, **Data Lineage Diagram**, универсальный **DAG**), создавать ветки, отправлять изменения на ревью. Рабочие пространства открываются в новой вкладке браузера по deep-ссылке (`?section=modeller&workspace=<id>`).

## Архитектура

Для разделов мониторинга/чтения сервис является тонким слоем агрегации: каждый раздел интерфейса обслуживается своим контроллером, который делегирует работу REST-клиенту соответствующего сервиса jarina. Прямых обращений к базам данных для этих разделов сервис не выполняет.

Поверхность **Modelling** реализована самим UI-сервисом: он читает и записывает конфигурационные документы в рабочих пространствах (локальная ФС или S3-хранилище), взаимодействует с Git-репозиториями доменов конфигурации для этих целей (через JGit) и отдаёт метаданные редактирования по схеме. Рабочее пространство — это персональная рабочая копия пользователя, которую открывают, редактируют и, наконец, отправляют на ревью; авторитетным хранилищем является Git-репозиторий, потребляемый `jarina-config-svc` (GitOps). Репозиторий домена может быть выделен одному домену (плоские файлы в корне репозитория) или содержать несколько доменов под `domains/<имя>/` — поддерживаются обе раскладки.

Внешние взаимодействия:

- **jarina-config-svc** — каталог данных, линковка, ограничения, модели, заголовки расписаний, журналы синхронизации GitOps.
- **jarina-scheduler-svc** — запуски расписаний за интервал, DAG запуска.
- **jarina-state-svc** — состояния интервалов датасетов.
- **jarina-task-proxy-for-spark** — Spark-подписки (создание, статус, kill, clear).
- **Git-репозитории (GitOps)** — по одному репозиторию на домен конфигурации (`jarina.domains.<имя>.git.repository-url`, объявляется в общем `DomainSettings` из `jarina-domain-api`); моделирование клонирует выбранную пару `(домен, ветка)` в рабочее пространство, создаёт ветки и отправляет изменения на ревью.

Контроллеры (верхний пакет `controller`):

```
CatalogController   /api/catalog     — дерево каталога, датасеты, линковка, ограничения, скрипты
ScheduleController  /api/schedules   — запуски расписаний, заголовки, DAG
ServicesController  /api/services    — граф сервисов и их статус
SparkProxyController /api/spark-proxy — Spark-подписки
StateController     /api/states      — состояния интервалов датасетов
VcsLogController    /api/vcs         — журнал синхронизации GitOps и журнал объектов
UserController      /api/user        — профиль текущего пользователя (вкл. роль Modelling)
```

Контроллеры модделирования (`org.jarina.ui.modeller.controller`):

```
VcsController    /api/vcs          — жизненный цикл рабочих пространств, ветки, ревью, restore
EditorController /api/workspaces/{workspaceId} — CRUD файлов внутри рабочего пространства
SchemaController /api/schema       — схемы форм по видам (KindSchema) для редакторов
AdminController  /api/workspace/admin — только для WORKSPACE_ADMIN: все пространства, TTL очистки, журналы
```

Статусы сервисов вычисляются `HealthChecker`: HTTP-проверкой по `healthCheckUrl` (тип `http`) либо проверкой открытого TCP-порта (тип `tcp`). Состав сервисов, рёбра и вершины графа задаются конфигурацией `jarina.ui.services/edges/vertices`.

Фронтенд собирается Vite (каталог `frontend`), результат сборки кладётся в `src/main/resources/static`. В dev-режиме Vite проксирует `/api` на сервис (`vite.config.js`).

## Домены

**Домен** - единица изоляции конфигурации в `jarina-config-svc`: каждый домен владеет
собственным Git-репозиторием, и на каждый загруженный из него конструкт проставляется имя
домена как `domainKeyName`. Это метка владения, а не часть идентичности: `keyName` остаётся
сквозным, поэтому моделированию не нужно разрешать имена по доменам. Моделирование не
придумывает домены - оно их показывает. Сконфигурированный
репозиторий домена становится корнем панели веток, а рабочее пространство - это набор
выборов `(домен, ветка)` по этим репозиториям. См.
[config-svc: Домены](../../jarina-config-svc/doc-ru/content_configuration/domains.md).

### По одному репозиторию (и ветке) на домен

Домены объявляются в `jarina.domains` и связываются общим `DomainSettings` из
`jarina-domain-api` (каноническая справка:
[jarina-domain-api/doc-ru/readme.md](../../jarina-domain-api/doc-ru/readme.md));
`jarina.modeller` больше не хранит домены — это только хранилище пространств / аутентификация /
сессия:

```yaml
jarina:
  domains:                        # <domainKeyName>:
    platform:                     #   git.repository-url: ...
      git:
        repository-url: git://git-server:9418/platform.git
        branch: main
        credentials:              # по-доменные, никогда не сливаются
          auth-type: ssh
          ssh-private-key-path: /keys/platform_ed25519
    analytics:
      git:
        repository-url: git://git-server:9418/analytics.git
```

Правила разрешения:

| Правило | Поведение |
|---|---|
| Список доменов | `domainNames()` - имена сконфигурированных доменов, отсортированные **по алфавиту**; моделирование показывает плоский список доменов и игнорирует `priority` и дерево доменов |
| URL репозитория | `jarina.domains.<имя>.git.repository-url`; **legacy-фолбэка на один репозиторий больше нет** |
| Основная ветка | `jarina.domains.<имя>.git.branch`, по умолчанию `main` |
| Недоступный репозиторий | домен показывается с **пустым** списком веток (`VcsException` ловится по каждому домену; корень панели остаётся), это не ошибка |
| Отсутствующий URL репозитория | жёсткая ошибка при использовании (`IllegalStateException` → HTTP 400): `Domain '<имя>': no repository URL configured (property: jarina.domains.<имя>.git.repository-url)` |
| Некорректные учётные данные | ошибка на старте: `ModellerVcsCatalog` сразу строит клиент каждого сконфигурированного домена с репозиторием, поэтому неверный `auth-type` или нечитаемый ключ приводят к падению при старте |

`git.branch` - это также **целевая ветка ревью**: отправка сливается обратно в
`domainSettings.branch(domain)` своего домена.

### Панель веток - это плоский список доменов

`GET /api/vcs/branches` возвращает по одной `DomainBranchesResponse` на каждый
сконфигурированный домен - `{domain, branches[], branchMain}` - и фронтенд рисует это как
дерево с доменами в корне и ветками, отходящими от них; настроенное *дерево* доменов здесь не
отражается, потому что `domainNames()` - плоский список по алфавиту. `POST /api/vcs/branch`
принимает `{domain, branch, baseBranch}`, поэтому ветка всегда создаётся в названном
репозитории домена.

### Рабочее пространство - это набор пар `(домен, ветка)`

`POST /api/vcs/workspace` принимает `branches: [{domain, branch}, ...]`:

- требуется хотя бы один выбор, и в каждой записи должны быть непустые `domain` **и**
  `branch`;
- домен, не указанный в запросе, просто не входит в рабочее пространство;
- идентификатор пространства - это `md5(username + "|" + отсортированные пары
  "domain=branch")`, поэтому тот же пользователь, открывший тот же набор выборов, получает
  то же пространство;
- каждый выбор разворачивается в **свою** папку `<домен> (<ветка>)`, поэтому дерево файлов
  мультидоменного пространства выглядит как `platform (feature-a)/ … analytics (main)/ …`;
- список выборов сохраняется в `_workspace.json` и возвращается в `WorkspaceResponse`.

Внутри рабочего пространства `domainKeyName` **не редактируется**. Он исключён из всех схем
форм (`SchemaService.DERIVED_PROPERTIES`), потому что домен определяется репозиторием, в
который конструкт отправляется, а не содержимым файла: один и тот же YAML валиден в любом
домене. Репозиторий при этом может быть общим: если он содержит несколько доменов под
`domains/<имя>/`, рабочее пространство сохраняет только поддерево выбранного домена и
убирает префикс, поэтому в папке пространства никогда не бывает уровня `domains/`.

### Ревью отправляется по доменам

`POST /api/vcs/review/{workspaceId}` принимает только `{comment, commitMessage}` - разбивка
выводится на сервере:

- файлы пространства группируются по покрывающей их папке `<домен> (<ветка>)`; файл вне
  всех выбранных доменных папок отклоняется;
- каждая группа коммитится в свою пару `(домен, ветка)` пользователем как автором и
  техническим сервисным аккаунтом как коммитером и пушится в доменную ветку;
- домен с пустым диффом пропускается (`NO_CHANGES`);
- сводный ответ - `OK` (с URL получившихся коммитов, склеенными через запятую, если их
  несколько) или `NO_CHANGES`, если не изменилось ничего;
- **рабочее пространство удаляется только при успехе** - неудачная отправка оставляет его
  открытым;
- пользователь должен владеть пространством, иметь роль `EDITOR` для каждого выбранного
  домена, и вся отправка выполняется
  под блокировкой пространства.

### Домен как фильтр журналов VCS

Разделы VCS показывают историю синхронизации `jarina-config-svc`, которая ведётся по
доменам. Оба read-only-эндпоинта принимают фильтр `domainKeyName`, и в обеих таблицах есть
колонка `Domain Key Name`:

| Эндпоинт | Фильтры |
|---|---|
| `GET /api/vcs/logs` | `status`, `commitId`, `domainKeyName` |
| `GET /api/vcs/objects` | `commitId`, `kind`, `from`, `to`, `filePath`, `objectName`, `domainKeyName` |

Фильтр передаётся через REST-клиент в config-svc без изменений, поэтому UI показывает ровно
то, что сервис конфигурации записал для данного домена.

### Справка по настройке

| Свойство | Назначение |
|---|---|
| `jarina.domains.<имя>.git.repository-url` | Репозиторий домена; обязателен, чтобы домен был пригоден в панели веток |
| `jarina.domains.<имя>.git.branch` | Основная ветка домена (по умолчанию `main`), также цель ревью |
| `jarina.domains.<имя>.git.credentials.auth-type` | `ssh` / `token` / `basic`; если не указано, выводится из объявленных учётных данных |
| `jarina.domains.<имя>.git.credentials.ssh-private-key-path` / `.username` / `.token` / `.password` | По-доменные git-учётные данные; пустой блок означает анонимный доступ |
| `jarina.modeller.storage.*` | Хранилище пространств (см. [Конфигурация](#конфигурация)) |

Этому сервису не нужен `git.local-clone-path`: он клонирует во временные рабочие каталоги,
в отличие от `jarina-config-svc`, который держит постоянный клон каждого домена.

По-доменные настройки в поставляемом `application.yml` не объявлены; их задаёт
развёртывание, например системными свойствами
(`-Djarina.domains.platform.git.repository-url=...`, как делает `demo/compose`) или через
переменные окружения relaxed binding
(`JARINA_DOMAINS_PLATFORM_GIT_REPOSITORY_URL`, `JARINA_DOMAINS_PLATFORM_GIT_BRANCH`).

## Модули

### jarina-ui-svc

Сам сервис. Содержит:

- точку входа `JarinaUiApplication`;
- мониторинговые контроллеры (`controller`): Catalog, Schedule, Services, SparkProxy, State, VcsLog, User;
- мониторинговые сервисы (`service`): `CatalogService`, `ScheduleService`, `ServicesService`, `SparkProxyService`, `StateService`, `VcsLogService`, `HealthChecker`;
- конфигурацию `UiServiceProperties` (список сервисов, граф);
- DTO (`dto`) — представления для фронтенда (`CatalogTreeNodeDTO`, `ConstraintDTO`, `ServiceNodeDTO`, `ScheduleRequestDTO`, `DataSetStateRequestDTO`);
- пакет модделирования **`org.jarina.ui.modeller`**:
  - контроллеры (`controller`): `VcsController`, `EditorController`, `SchemaController`, `AdminController`;
  - сервисы (`service`): `VcsService`, `ReviewService`, `SchemaService`, `EditorService`, `YamlEditorService`, `EnumOptionsService`, `SyncLogService`, `AdminWorkspaceService`;
  - интеграция с VCS (`config`): `ModellerProperties` (хранилище пространств, аутентификация, сессия, логирование — домены и git-учётные данные лежат в общем `DomainSettings` из `jarina-domain-api`) и `ModellerVcsCatalog`, который через `DomainVcsFactory` строит по одному `GitVcsClient` на домен из общего модуля `jarina-domain-api`;
  - хранилище рабочих пространств (`storage`): SPI `WorkspaceStorage` с `LocalFsWorkspaceStorage` и S3 (`S3WorkspaceStorage` + минимальная подпись AWS SigV4), а также `WorkspaceManager`, `WorkspaceSeeder` и задача очистки (`WorkspaceCleanupTask`);
  - авторизация (`auth`): `DomainRole` (`VIEWER`/`EDITOR`/`CONTROL`), `ModellerRoleSupport` (разбор доменных ролей), `UserContext`, `DomainAccess`, `ForbiddenException`/`NotFoundException`;
  - DTO (`dto`): `KindSchema`, `FieldSchema`, `TreeResponse`, `FileContentResponse`, `WorkspaceResponse`, DTO ревью/restore и др.;
- `SecurityConfig` — OAuth2-логин BFF + RBAC модделирования (см. Безопасность);
- `GlobalExceptionHandler` — единая обработка ошибок;
- фронтенд (`src/main/resources/frontend`): React + Vite, включая визуальные редакторы модделирования (`ErDiagramEditor`, `DataLineageDiagramEditor`, `DagEditor` — на React Flow) и набор unit-тестов на Vitest.

Зависимости: `jarina-domain-api` (общая VCS-библиотека: `VcsClient`, его реализация на JGit, учётные данные и спецификация с фабрикой по домену), `jarina-common` (общие константы и DTO конфигурации, используемые редакторами — например enum `YamlMetadataKind`, покрывающий виды `ERDiagram` и `DataLineageDiagram`, а также соответствующие `ERDiagramDTO` / `DataLineageDiagramDTO`), `jarina-config-rest-client`, `jarina-scheduler-rest-client`, `jarina-state-rest-client`, `jarina-task-proxy-for-spark-rest-client`, `jackson-dataformat-yaml`, `org.eclipse.jgit` (+ SSH), Spring Boot OAuth2 client и resource server.

## API Endpoints

| Метод | Путь | Описание |
|---|---|---|
| GET | `/api/catalog/tree` | Дерево каталога: источники → схемы → датасеты |
| GET | `/api/catalog/dataset/{keyName}` | Датасет по ключевому имени |
| GET | `/api/catalog/dataset/{keyName}/lineage` | Линковка датасета |
| GET | `/api/catalog/dataset/{keyName}/constraints` | Ограничения датасета |
| GET | `/api/catalog/script/{key}` | SQL-скрипт по ключу |
| GET | `/api/catalog/dataset/{keyName}/model-script` | Модель (DDL) датасета |
| GET | `/api/catalog/datasource/{keyName}` | Источник данных по ключевому имени |
| POST | `/api/schedules` | Запуски расписаний за интервал (`fromDate`, `toDate`, `names`) |
| GET | `/api/schedules/headers` | Заголовки расписаний |
| GET | `/api/schedules/dag/{id}` | DAG запуска расписания по id |
| GET | `/api/services` | Список сервисов со статусами |
| GET | `/api/services/edges` | Рёбра графа сервисов |
| GET | `/api/services/vertices` | Вершины графа сервисов |
| GET | `/api/spark-proxy/submissions` | Список подписок (`limit`, `lastId`, `id`, `status`, `dateFrom`, `dateTo`) |
| GET | `/api/spark-proxy/submissions/{id}/spark-properties` | Spark-свойства подписки |
| POST | `/api/spark-proxy/submissions` | Создание подписки |
| GET | `/api/spark-proxy/submissions/status/{submissionId}` | Статус подписки |
| POST | `/api/spark-proxy/submissions/kill/{submissionId}` | Убить подписку |
| POST | `/api/spark-proxy/submissions/killall` | Убить все подписки |
| POST | `/api/spark-proxy/submissions/clear` | Очистить завершённые подписки |
| POST | `/api/states` | Состояния интервалов датасета (`dataSetKeyName`, `fromDate`, `toDate`) |
| GET | `/api/vcs/logs` | Журнал синхронизации GitOps (коммиты), фильтры `status`, `commitId`, `domainKeyName` |
| GET | `/api/vcs/objects` | Журнал объектов GitOps (изменённые конфигурационные объекты), фильтры `commitId`, `kind`, `from`, `to`, `filePath`, `objectName`, `domainKeyName` |
| GET | `/api/user` | Профиль текущего пользователя (`username`, `name`, `email`, `roles`, `domains`) |
| GET | `/api/vcs/workspaces` | Рабочие пространства текущего пользователя |
| POST | `/api/vcs/workspace` | Открыть рабочее пространство для списка выборов `{domain, branch}` (создание рабочей копии) |
| DELETE | `/api/vcs/workspace/{workspaceId}` | Удалить рабочее пространство пользователя |
| GET | `/api/vcs/branches` | Ветки, сгруппированные по доменам: по одной записи на каждый сконфигурированный репозиторий домена |
| POST | `/api/vcs/branch` | Создать ветку в репозитории домена (`domain`, `branch`, `baseBranch`) |
| POST | `/api/vcs/review/{workspaceId}` | Отправить рабочее пространство на ревью с разбивкой по доменам (`comment`, `commitMessage`); ответ `OK` или `NO_CHANGES` |
| POST | `/api/vcs/workspace/{workspaceId}/restore` | Восстановить файл/папку из состояния VCS; домен определяется по пути |
| GET | `/api/schema` | Схемы форм всех видов конфигурации |
| GET | `/api/schema/{kind}` | Схема формы одного вида конфигурации |
| GET | `/api/workspaces/{workspaceId}/tree` | Дерево файлов рабочего пространства |
| GET | `/api/workspaces/{workspaceId}/dirs` | Список каталогов |
| POST | `/api/workspaces/{workspaceId}/dirs` | Создать каталог |
| POST | `/api/workspaces/{workspaceId}/dirs/move` | Переместить каталог |
| DELETE | `/api/workspaces/{workspaceId}/dirs/{path}` | Удалить каталог (рекурсивно) |
| POST | `/api/workspaces/{workspaceId}/files` | Создать файл метаданных (`kind`, `keyName`, `directory`) |
| GET | `/api/workspaces/{workspaceId}/files/{path}` | Прочитать файл (YAML + вид + флаг редактируемости) |
| PUT | `/api/workspaces/{workspaceId}/files/{path}` | Сохранить файл (`yaml`, `keyName`) |
| POST | `/api/workspaces/{workspaceId}/files/rename` | Переименовать файл |
| POST | `/api/workspaces/{workspaceId}/files/move` | Переместить файл |
| DELETE | `/api/workspaces/{workspaceId}/files/{path}` | Удалить файл |
| GET | `/api/workspace/admin/workspaces` | Все рабочие пространства (WORKSPACE_ADMIN) |
| DELETE | `/api/workspace/admin/workspaces/{workspaceId}` | Принудительное удаление пространства (WORKSPACE_ADMIN) |
| GET | `/api/workspace/admin/settings/cleanup-ttl-hours` | TTL жизни неактивного пространства (WORKSPACE_ADMIN) |
| PUT | `/api/workspace/admin/settings/cleanup-ttl-hours` | Установить TTL очистки (WORKSPACE_ADMIN) |
| GET | `/api/workspace/admin/sync-logs` | Хвост журнала синхронизации VCS (WORKSPACE_ADMIN) |

## Конфигурация

Основные параметры (`src/main/resources/application.yml`):

```yaml
spring:
  security:
    oauth2:
      client:            # keycloak (authorization-code) + keycloak-internal (client_credentials)
      resourceserver:    # bearer JWT проверяется по certs того же realm
    threads:
      virtual:
        enabled: true

jarina:
  client:
    rest:
      config:
        server:
          url: http://localhost:8080
      state:
        server:
          url: http://localhost:8082
      scheduler:
        server:
          url: http://localhost:8081
      task-proxy-for-spark:
        server:
          url: http://localhost:8099
  ui:
    health-check-timeout-ms: 3000
    services:            # список сервисов: name, url, health-check-url, check-type
    vertices: {}         # вершины графа: ключ → имя сервиса
    edges: {}            # рёбра графа: ключ вершины → список приёмников
  modeller:
    storage:
      type: local        # [local, s3]
      root-directory: /tmp/jarina-workspaces   # когда type == local
      s3:                # когда type == s3
        endpoint: ...
        bucket: jarina-metadata-workspaces
        access-key: ...
        secret-key: ...
        region: us-east-1
      cleanup-ttl-hours: 4
    auth-strategy: jwt-rbac      # [jwt-rbac, token-exchange]
    session:
      inactivity-minutes: 30
    logging:
      sync-log-capacity: 500
  # Домены конфигурации (jarina.domains.<имя>.git.*), связываются общим
  # DomainSettings из jarina-domain-api. Домен активен, только если задан
  # repository-url; учётные данные по-доменные и никогда не сливаются.
  domains:
    platform:
      git:
        repository-url: git://git-server:9418/platform.git
        branch: main
        credentials:             # по-доменные учётные данные
          auth-type: ssh
          ssh-private-key-path: /keys/platform_ed25519
    analytics:
      git:
        repository-url: git://git-server:9418/analytics.git
        branch: main
```

| Параметр | Описание |
|---|---|
| `server.port` | Порт сервиса (8080 в demo compose; здесь пусто = значение по умолчанию) |
| `jarina.client.rest.config.server.url` | URL `jarina-config-svc` |
| `jarina.client.rest.state.server.url` | URL `jarina-state-svc` |
| `jarina.client.rest.scheduler.server.url` | URL `jarina-scheduler-svc` |
| `jarina.client.rest.task-proxy-for-spark.server.url` | URL `jarina-task-proxy-for-spark` |
| `jarina.ui.health-check-timeout-ms` | Таймаут проверки доступности сервиса |
| `jarina.ui.services[]` | Список сервисов (name, url, health-check-url, check-type: `http`/`tcp`) |
| `jarina.ui.vertices` / `edges` | Вершины / рёбра графа сервисов |
| `jarina.modeller.storage.type` | Хранилище пространств: `local` (по умолчанию) или `s3` |
| `jarina.modeller.storage.root-directory` | Локальный корень пространств (по умолчанию `/tmp/jarina-workspaces`) |
| `jarina.modeller.storage.s3.*` | S3 endpoint/bucket/ключи (тип S3) |
| `jarina.modeller.storage.cleanup-ttl-hours` | Время жизни неактивного пространства (по умолчанию 4 ч) |
| `jarina.domains.<имя>.git.repository-url` | URL репозитория домена (обязателен, чтобы домен был пригоден в панели веток) |
| `jarina.domains.<имя>.git.branch` | Основная ветка домена (по умолчанию `main`); также цель ревью |
| `jarina.domains.<имя>.git.credentials.*` | По-доменные git-учётные данные (`auth-type`, `ssh-private-key-path`, `username`, `token`, `password`); глобального аккаунта и слияния нет |
| `jarina.modeller.auth-strategy` | `jwt-rbac` (по умолчанию) или `token-exchange` |
| `jarina.modeller.session.inactivity-minutes` | Таймаут бездействия сессии пространства |
| `jarina.modeller.logging.sync-log-capacity` | Ёмкость журнала синхронизации в памяти |

## Безопасность

UI BFF аутентифицирует пользователей через Keycloak (realm `jarina`) по OAuth 2.0 **authorization code flow** (`oauth2Login()`). После успешного входа Spring Security выдает фронтенду защищенную сессионную cookie `JSESSIONID` (`HttpOnly`; в профиле `prod` также `Secure`). Запросы, изменяющие состояние, защищены от CSRF: токен передается фронтенду через cookie `XSRF-TOKEN` (доступную JS) и должен возвращаться в заголовке `X-XSRF-TOKEN`.

Тот же realm настроен и как **resource server** (bearer JWT): межсервисные вызовы аутентифицируются по учётным данным `jarina-internal-client`, JWT проверяется по эндпоинту `certs` realm.

Пути из белого списка (вход не требуется): `/healthz`, `/readyz`, `/actuator/**`, `/favicon.ico`. Все остальные запросы требуют аутентифицированной сессии; неаутентифицированные запросы браузера перенаправляются на страницу входа Keycloak, после входа пользователь возвращается на `/` (`defaultSuccessUrl`).

### Роли модделирования (RBAC)

Доступ построен **по доменам**. Роли realm несут префикс домена — `<domain>:<ROLE>`, где `ROLE` ∈ `VIEWER` / `EDITOR` / `CONTROL` (например, `platform:EDITOR`, `analytics:CONTROL`). `VIEWER` неявно входит в две другие; `EDITOR` и `CONTROL` ортогональны — `EDITOR` позволяет менять конфиги/репозиторий, но не останавливать задачи, `CONTROL` позволяет останавливать задачи, но не редактировать. Глобальная роль `WORKSPACE_ADMIN` (без домена) управляет админской поверхностью.

| Роль realm | Доступ |
|---|---|
| `<domain>:VIEWER` | Чтение датасетов/расписаний/VCS-логов этого домена (BFF фильтрует по домену), открытие пространства для чтения, удаление своего пространства |
| `<domain>:EDITOR` | + создание веток, открытие/правка/сохранение/удаление/перемещение файлов, отправка на ревью, restore — для этого домена |
| `<domain>:CONTROL` | + остановка/убийство Spark-submissions |
| `WORKSPACE_ADMIN` (глобальная, без домена) | Всё под `/api/workspace/admin/**`: все пространства, принудительное удаление, TTL очистки, журналы |

`SecurityConfig` закрывает админскую поверхность (`/api/workspace/admin/**` → `hasRole('WORKSPACE_ADMIN')`), всё остальное — `authenticated()`. Доменные проверки и проверки владения живут в сервисах (`EditorService`/`VcsService`/`ReviewService`/`SparkProxyController`/`AdminWorkspaceService`) и бросают `ForbiddenException` (→ `403`); `CatalogService`/`ScheduleService`/`VcsLogService` фильтруют по разрешённым доменам пользователя (`DomainAccess`). Фронтенд лишь *отражает* права: подсказка `readOnly` в модделировании выводится по домену из `domains` в `GET /api/user` (редактирование доступно только с ролью `EDITOR` для домена выбранного файла) и больше не зависит от владения пространством (`workspace.own`), которое бэкенд принуждает отдельно.

### Необходимые настройки

| Свойство / env | По умолчанию | Описание |
|---|---|---|
| `KEYCLOAK_ISSUER_URI` | `http://keycloak.jarina:8085/realms/jarina` | URL realm; из него строятся эндпоинты auth/token/userinfo/certs |
| `KEYCLOAK_UI_CLIENT_SECRET` | `super-secret-bff-key-1234567890` | Секрет клиента `jarina-ui-client` |
| `JARINA_UI_REDIRECT_URI` | `{baseUrl}/login/oauth2/code/{registrationId}` | OAuth2 redirect URI BFF |
| `KEYCLOAK_INTERNAL_CLIENT_SECRET` | `super-secret-internal-key-987654321` | Секрет `jarina-internal-client` (межсервисные вызовы) |
| `JARINA_DOMAINS_<ИМЯ>_GIT_REPOSITORY_URL` | — | URL репозитория домена конфигурации (relaxed binding `jarina.domains.<имя>.git.repository-url`) |
| `JARINA_DOMAINS_<ИМЯ>_GIT_BRANCH` | `main` | Ветка домена конфигурации |
| `JARINA_DOMAINS_<ИМЯ>_GIT_CREDENTIALS_SSH_PRIVATE_KEY_PATH` | — | SSH-ключ домена конфигурации |
| `JARINA_WORKSPACE_STORAGE` / `JARINA_WORKSPACE_ROOT` | `local` / `/tmp/jarina-workspaces` | Бэкенд хранилища пространств |
| `JARINA_WORKSPACE_TTL_HOURS` | `4` | TTL очистки неактивных пространств |
| `server.servlet.session.cookie.name` / `.http-only` | `JSESSIONID` / `true` | Имя сессионной cookie и флаг HttpOnly |
| `server.servlet.session.cookie.secure` | `false` (`true` в профиле `prod`) | Установите `true`, если UI работает по HTTPS |

### Настройка учетных записей и ролей в Keycloak

1. **Разверните Keycloak.** В demo-окружении compose поднимает Keycloak 26.0 с админ-консолью на `http://localhost:8085` (учетные данные из `KEYCLOAK_ADMIN`/`KEYCLOAK_ADMIN_PASSWORD`, по умолчанию `admin`/`admin_local_password`) и импортирует эталонный realm из `demo/compose/conf_infra/security/realms/jarina-realm.json`. В production используйте постоянную БД и смените все пароли/секреты по умолчанию.
2. **Роли realm'а.** В realm `jarina` определены доменные роли модделирования и глобальная админ-роль:
   - `<domain>:VIEWER` — модделирование только для чтения в этом домене;
   - `<domain>:EDITOR` — редактирование конфигурационных документов этого домена;
   - `<domain>:CONTROL` — остановка Spark-submissions;
   - `WORKSPACE_ADMIN` — админ-поверхность модделирования.

   В demo-realm заведены роли для настроенных доменов `platform`, `analytics` и `processing`. Роли попадают к сервисам в claim JWT `realm_access.roles` и преобразуются в authorities (`SecurityConfig` читает `realm_access.roles`, `roles`, `resource_access.*.roles`); роль без префикса `<domain>:` (кроме `WORKSPACE_ADMIN`) игнорируется.
3. **Клиент `jarina-ui-client`.** Confidential-клиент (*Standard Flow Enabled*, *Direct Access Grants* выключен), используемый данным BFF. Проверьте, что:
   - *Valid redirect URIs* содержат внешне видимый адрес UI: по умолчанию `http://localhost:8080/*` и `http://localhost:8080/login/oauth2/code/keycloak`;
   - *Web Origins* содержит origin UI (`http://localhost:8080`);
   - *Post Logout Redirect URIs* содержат `http://localhost:8080/*` — сюда браузер возвращается после `end_session` Keycloak при кнопке «Switch user» (см. ниже);
   - при развертывании на другом хосте/порту добавьте соответствующие redirect URI, post-logout redirect URI и web origin и задайте `JARINA_UI_REDIRECT_URI`.
4. **Создание пользователей.** Админ-консоль → realm `jarina` → *Users* → *Add user*: заполните username/email/имя, затем *Credentials* → задайте пароль (выключите *Temporary*, чтобы пароль был постоянным).
5. **Назначение ролей.** *Users* → выберите пользователя → *Role mapping* → фильтр *Filter by realm roles* → назначьте нужные доменные роли (`<domain>:VIEWER`/`EDITOR`/`CONTROL`) и, для администраторов воркспейсов, глобальную роль `WORKSPACE_ADMIN`.
6. **Service account.** Confidential-клиент `jarina-internal-client` (*Service Accounts Enabled*) используется backend-сервисами для межсервисных вызовов; его секрет должен совпадать со значением `KEYCLOAK_INTERNAL_CLIENT_SECRET` на каждом сервисе.

После настройки откройте UI - первый запрос перенаправит на страницу входа Keycloak; войти смогут только пользователи с учетной записью в realm `jarina`.

**Выход/смена пользователя.** Кнопка «Switch user» отправляет полностраничную форму
`POST /logout`: Spring Security инвалидирует сессию BFF и редиректит браузер на
`end_session_endpoint` Keycloak realm `jarina` (адрес задаётся свойством
`jarina.security.keycloak.end-session-uri`, по умолчанию выводится из
`KEYCLOAK_ISSUER_URI`) с `id_token_hint` и `post_logout_redirect_uri`, после
чего браузер падает на страницу входа Keycloak.