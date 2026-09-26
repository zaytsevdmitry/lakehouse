# Сервис веб-интерфейса (lakehouse-ui-svc)

Веб-интерфейс управления lakehouse: единая точка визуализации и администрирования сервисов, каталога данных, расписаний, состояний датасетов, Spark-подписок и метаданных-ориентированной конфигурации («Modelling»).

## Обзор

`lakehouse-ui-svc` — сервис, агрегирующий данные всех остальных сервисов lakehouse и предоставляющий единый веб-интерфейс. Для мониторинговых разделов он является тонким слоем агрегации: обращается к другим сервисам через их REST-клиенты и отдаёт результат фронтенду. Дополнительно сервис размещает поверхность **Modelling** — интерактивный редактор конфигурационных документов, хранящихся в Git-репозиториях доменов конфигурации (рабочие пространства, ветки, отправка на ревью), которая обслуживается самим сервисом. Каждый домен владеет своим репозиторием, а рабочее пространство — это набор выборов `(домен, ветка)` — см. [Домены](#домены).

Сервис состоит из двух частей:

- **бэкенд** — Spring Boot приложение, которое проксирует запросы фронтенда к сервисам lakehouse, отдаёт статический фронтенд и размещает бэкенд модделирования (хранилище рабочих пространств, интеграция с Git, редактирование YAML, RBAC);
- **фронтенд** — одностраничное React-приложение (Vite), собираемое в `src/main/resources/static` и раздаваемое тем же сервисом.

Разделы интерфейса:

- **Services** — граф сервисов lakehouse и их статус (`UP`/`DOWN`) по health-check.
- **Catalog** — дерево каталога данных: источники → схемы → датасеты; просмотр датасета (модель/DDL, линковка, ограничения) и источника данных.
- **Schedules** — список запусков расписаний за интервал, DAG запуска расписания.
- **SparkJobs** — список Spark-подписок через `lakehouse-task-proxy-for-spark`: создание, статус, kill, kill all, clear.
- **VCS** — журнал синхронизации GitOps-конфигурации (коммиты) и журнал объектов `lakehouse-config-svc`.
- **Modelling** — рабочее место по модделированию метаданных: создать рабочее пространство из одной или нескольких доменных веток, создавать конфигурационные документы любого поддерживаемого вида (форма по схеме вида, «сырой» YAML или визуальные редакторы — **ER Diagram**, **Data Lineage Diagram**, универсальный **DAG**), создавать ветки, отправлять изменения на ревью. Рабочие пространства открываются в новой вкладке браузера по deep-ссылке (`?section=modeller&workspace=<id>`).

## Архитектура

Для разделов мониторинга/чтения сервис является тонким слоем агрегации: каждый раздел интерфейса обслуживается своим контроллером, который делегирует работу REST-клиенту соответствующего сервиса lakehouse. Прямых обращений к базам данных для этих разделов сервис не выполняет.

Поверхность **Modelling** реализована самим UI-сервисом: он читает и записывает конфигурационные документы в рабочих пространствах (локальная ФС или S3-хранилище), взаимодействует с Git-репозиториями доменов конфигурации для этих целей (через jgit или API GitLab/GitHub) и отдаёт метаданные редактирования по схеме. Рабочее пространство — это персональная рабочая копия пользователя, которую открывают, редактируют и, наконец, отправляют на ревью; авторитетным хранилищем является Git-репозиторий, потребляемый `lakehouse-config-svc` (GitOps). Репозиторий домена может быть выделен одному домену (плоские файлы в корне репозитория) или содержать несколько доменов под `domains/<имя>/` — поддерживаются обе раскладки.

Внешние взаимодействия:

- **lakehouse-config-svc** — каталог данных, линковка, ограничения, модели, заголовки расписаний, журналы синхронизации GitOps.
- **lakehouse-scheduler-svc** — запуски расписаний за интервал, DAG запуска.
- **lakehouse-state-svc** — состояния интервалов датасетов.
- **lakehouse-task-proxy-for-spark** — Spark-подписки (создание, статус, kill, clear).
- **Git-репозитории (GitOps)** — по одному репозиторию на домен конфигурации (`lakehouse.modeller.domains.<имя>.repository-url`, с `lakehouse.modeller.git.remote-url` как legacy-фолбэком на один репозиторий); моделирование клонирует выбранную пару `(домен, ветка)` в рабочее пространство, создаёт ветки и отправляет изменения на ревью.

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

Контроллеры модделирования (`org.lakehouse.ui.modeller.controller`):

```
VcsController    /api/vcs          — жизненный цикл рабочих пространств, ветки, ревью, restore
EditorController /api/workspaces/{workspaceId} — CRUD файлов внутри рабочего пространства
SchemaController /api/schema       — схемы форм по видам (KindSchema) для редакторов
AdminController  /api/admin        — только для админов: все пространства, TTL очистки, журналы
```

Статусы сервисов вычисляются `HealthChecker`: HTTP-проверкой по `healthCheckUrl` (тип `http`) либо проверкой открытого TCP-порта (тип `tcp`). Состав сервисов, рёбра и вершины графа задаются конфигурацией `lakehouse.ui.services/edges/vertices`.

Фронтенд собирается Vite (каталог `frontend`), результат сборки кладётся в `src/main/resources/static`. В dev-режиме Vite проксирует `/api` на сервис (`vite.config.js`).

## Домены

**Домен** - единица изоляции конфигурации в `lakehouse-config-svc`: каждый домен владеет
собственным Git-репозиторием, и на каждый загруженный из него конструкт проставляется имя
домена как `domainKeyName`. Это метка владения, а не часть идентичности: `keyName` остаётся
сквозным, поэтому моделированию не нужно разрешать имена по доменам. Моделирование не
придумывает домены - оно их показывает. Сконфигурированный
репозиторий домена становится корнем панели веток, а рабочее пространство - это набор
выборов `(домен, ветка)` по этим репозиториям. См.
[config-svc: Домены](../../lakehouse-config-svc/doc-ru/content_configuration/domains.md).

### По одному репозиторию (и ветке) на домен

`ModellerProperties` (префикс `lakehouse.modeller`) объявляет карту `domains`:

```yaml
lakehouse:
  modeller:
    domains:                        # <domainKeyName>:
      platform:                     #   repository-url: ...
        repository-url: git://git-server:9418/platform.git
        branch-main: main
      analytics:
        repository-url: git://git-server:9418/analytics.git
        branch-main: main
    git:                            # legacy-фолбэк на один репозиторий
      remote-url: ${LAKEHOUSE_GIT_URL:}
      branch-main: main
```

Правила разрешения:

| Правило | Поведение |
|---|---|
| Список доменов | `domainNames()` - ключи `domains`, отсортированные **по алфавиту**; у моделирования нет дерева доменов и `priority` |
| Нет `domains` | используется единственный домен `default`, если задан `git.remote-url`; иначе список пуст |
| URL репозитория | `repository-url` домена, иначе legacy `git.remote-url`, иначе домен непригоден |
| Основная ветка | `branch-main` домена, иначе legacy `git.branch-main`, иначе `main` |
| Недоступный репозиторий | домен показывается с **пустым** списком веток (корень панели остаётся), это не ошибка |
| Отсутствующий URL репозитория | жёсткая ошибка: `No repository URL configured for domain <имя> (lakehouse.modeller.domains.<имя>.repository-url)` |

`branch-main` - это также **целевая ветка ревью**: отправка сливается обратно в
`domainBranchMain(domain)` своего домена.

### Панель веток - это дерево доменов

`GET /api/vcs/branches` возвращает по одной `DomainBranchesResponse` на каждый
сконфигурированный домен - `{domain, branches[], branchMain}` - и фронтенд рисует это как
дерево с доменами в корне и ветками, отходящими от них. `POST /api/vcs/branch` принимает
`{domain, branch, baseBranch}`, поэтому ветка всегда создаётся в названном репозитории
домена.

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
  техническим сервисным аккаунтом как коммитером, пушится в доменную ветку и - для
  GitLab/GitHub - превращается в MR/PR против `branch-main` этого домена;
- домен с пустым диффом пропускается (`NO_CHANGES`);
- сводный ответ - `OK` (с URL MR/PR, склеенными через запятую, если их несколько) или
  `NO_CHANGES`, если не изменилось ничего;
- **рабочее пространство удаляется только при успехе** - неудачная отправка оставляет его
  открытым;
- пользователь должен владеть пространством (или быть `ADMIN`), и вся отправка выполняется
  под блокировкой пространства.

### Домен как фильтр журналов VCS

Разделы VCS показывают историю синхронизации `lakehouse-config-svc`, которая ведётся по
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
| `lakehouse.modeller.domains.<имя>.repository-url` | Репозиторий домена; обязателен, чтобы домен появился в панели веток |
| `lakehouse.modeller.domains.<имя>.branch-main` | Основная ветка домена (по умолчанию `main`), также цель ревью |
| `lakehouse.modeller.git.remote-url` | Legacy-фолбэк, используется когда `domains` пуст; домен называется `default` |
| `lakehouse.modeller.git.branch-main` | Legacy-фолбэк основной ветки (по умолчанию `main`) |

Доменные переменные окружения в поставляемом `application.yml` не объявлены; их задаёт
развёртывание, например системными свойствами
(`-Dlakehouse.modeller.domains.platform.repository-url=...`, как делает `demo/compose`) или
через переменные окружения relaxed binding.

## Модули

### lakehouse-ui-svc

Сам сервис. Содержит:

- точку входа `LakehouseUiApplication`;
- мониторинговые контроллеры (`controller`): Catalog, Schedule, Services, SparkProxy, State, VcsLog, User;
- мониторинговые сервисы (`service`): `CatalogService`, `ScheduleService`, `ServicesService`, `SparkProxyService`, `StateService`, `VcsLogService`, `HealthChecker`;
- конфигурацию `UiServiceProperties` (список сервисов, граф);
- DTO (`dto`) — представления для фронтенда (`CatalogTreeNodeDTO`, `ConstraintDTO`, `ServiceNodeDTO`, `ScheduleRequestDTO`, `DataSetStateRequestDTO`);
- пакет модделирования **`org.lakehouse.ui.modeller`**:
  - контроллеры (`controller`): `VcsController`, `EditorController`, `SchemaController`, `AdminController`;
  - сервисы (`service`): `VcsService`, `ReviewService`, `SchemaService`, `EditorService`, `YamlEditorService`, `EnumOptionsService`, `SyncLogService`, `AdminWorkspaceService`;
  - интеграция с VCS (`vcs`): SPI `VcsProvider` с реализациями `LocalGitVcsProvider`, `GitLabApiVcsProvider`, `GitHubAppVcsProvider`, `DisabledVcsProvider` и фабрикой провайдеров;
  - хранилище рабочих пространств (`storage`): SPI `WorkspaceStorage` с `LocalFsWorkspaceStorage` и S3 (`S3WorkspaceStorage` + минимальная подпись AWS SigV4), а также `WorkspaceManager`, `WorkspaceSeeder` и задача очистки (`WorkspaceCleanupTask`);
  - авторизация (`auth`): `ModellerRole` (`VIEWER < EDITOR < ADMIN`), `UserContext`, `ForbiddenException`/`NotFoundException`;
  - DTO (`dto`): `KindSchema`, `FieldSchema`, `TreeResponse`, `FileContentResponse`, `WorkspaceResponse`, DTO ревью/restore и др.;
- `SecurityConfig` — OAuth2-логин BFF + RBAC модделирования (см. Безопасность);
- `GlobalExceptionHandler` — единая обработка ошибок;
- фронтенд (`src/main/resources/frontend`): React + Vite, включая визуальные редакторы модделирования (`ErDiagramEditor`, `DataLineageDiagramEditor`, `DagEditor` — на React Flow) и набор unit-тестов на Vitest.

Зависимости: `lakehouse-common` (общие константы и DTO конфигурации, используемые редакторами — например enum `YamlMetadataKind`, покрывающий виды `ERDiagram` и `DataLineageDiagram`, а также соответствующие `ERDiagramDTO` / `DataLineageDiagramDTO`), `lakehouse-config-rest-client`, `lakehouse-scheduler-rest-client`, `lakehouse-state-rest-client`, `lakehouse-task-proxy-for-spark-rest-client`, `jackson-dataformat-yaml`, `org.eclipse.jgit` (+ SSH), Spring Boot OAuth2 client и resource server.

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
| GET | `/api/user` | Профиль текущего пользователя (`username`, `roles`, `effectiveRole`) |
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
| GET | `/api/admin/workspaces` | Все рабочие пространства (админ) |
| DELETE | `/api/admin/workspaces/{workspaceId}` | Принудительное удаление пространства (админ) |
| GET | `/api/admin/settings/cleanup-ttl-hours` | TTL жизни неактивного пространства (админ) |
| PUT | `/api/admin/settings/cleanup-ttl-hours` | Установить TTL очистки (админ) |
| GET | `/api/admin/sync-logs` | Хвост журнала синхронизации VCS (админ) |

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

lakehouse:
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
      root-directory: /tmp/lakehouse-workspaces   # когда type == local
      s3:                # когда type == s3
        endpoint: ...
        bucket: lakehouse-metadata-workspaces
        access-key: ...
        secret-key: ...
        region: us-east-1
      cleanup-ttl-hours: 4
    vcs-provider: local-git      # [local-git, gitlab-api, github-app, gerrit-ssh]
    # По одному репозиторию на домен конфигурации. Когда `domains` пуст, используется
    # legacy-блок `git.*` ниже как единственный домен `default`.
    domains:
      platform:
        repository-url: git://git-server:9418/platform.git
        branch-main: main
      analytics:
        repository-url: git://git-server:9418/analytics.git
        branch-main: main
    git:                        # legacy-фолбэк на один репозиторий
      remote-url: ${LAKEHOUSE_GIT_URL:}
      branch-main: main
    auth-strategy: jwt-rbac      # [jwt-rbac, token-exchange]
    session:
      inactivity-minutes: 30
    vcs-system-account:          # учётные данные для git-операций
      auth-type: token           # [ssh, token, basic]
      ssh-private-key-path: ...
      username: ...
      token: ...
      password: ...
    github:                      # используется при vcs-provider == github-app
      app-id: ...
      app-private-key-path: ...
      installation-id: ...
    logging:
      sync-log-capacity: 500
```

| Параметр | Описание |
|---|---|
| `server.port` | Порт сервиса (8080 в demo compose; здесь пусто = значение по умолчанию) |
| `lakehouse.client.rest.config.server.url` | URL `lakehouse-config-svc` |
| `lakehouse.client.rest.state.server.url` | URL `lakehouse-state-svc` |
| `lakehouse.client.rest.scheduler.server.url` | URL `lakehouse-scheduler-svc` |
| `lakehouse.client.rest.task-proxy-for-spark.server.url` | URL `lakehouse-task-proxy-for-spark` |
| `lakehouse.ui.health-check-timeout-ms` | Таймаут проверки доступности сервиса |
| `lakehouse.ui.services[]` | Список сервисов (name, url, health-check-url, check-type: `http`/`tcp`) |
| `lakehouse.ui.vertices` / `edges` | Вершины / рёбра графа сервисов |
| `lakehouse.modeller.storage.type` | Хранилище пространств: `local` (по умолчанию) или `s3` |
| `lakehouse.modeller.storage.root-directory` | Локальный корень пространств (по умолчанию `/tmp/lakehouse-workspaces`) |
| `lakehouse.modeller.storage.s3.*` | S3 endpoint/bucket/ключи (тип S3) |
| `lakehouse.modeller.storage.cleanup-ttl-hours` | Время жизни неактивного пространства (по умолчанию 4 ч) |
| `lakehouse.modeller.vcs-provider` | Интеграция с Git: `local-git`, `gitlab-api`, `github-app`, `gerrit-ssh` или `disabled` |
| `lakehouse.modeller.domains.<имя>.repository-url` | URL репозитория домена (обязателен, чтобы домен появился в панели веток) |
| `lakehouse.modeller.domains.<имя>.branch-main` | Основная ветка домена (по умолчанию `main`); также цель ревью |
| `lakehouse.modeller.git.remote-url` | Legacy-фолбэк, используется когда `domains` пуст; домен называется `default` |
| `lakehouse.modeller.git.branch-main` | Legacy-фолбэк основной ветки (по умолчанию `main`) |
| `lakehouse.modeller.auth-strategy` | `jwt-rbac` (по умолчанию) или `token-exchange` |
| `lakehouse.modeller.session.inactivity-minutes` | Таймаут бездействия сессии пространства |
| `lakehouse.modeller.vcs-system-account.*` | Учётные данные для git-операций (`ssh`/`token`/`basic`) |
| `lakehouse.modeller.github.*` | Настройки GitHub App (`github-app` провайдер) |
| `lakehouse.modeller.logging.sync-log-capacity` | Ёмкость журнала синхронизации в памяти |

## Безопасность

UI BFF аутентифицирует пользователей через Keycloak (realm `lakehouse`) по OAuth 2.0 **authorization code flow** (`oauth2Login()`). После успешного входа Spring Security выдает фронтенду защищенную сессионную cookie `JSESSIONID` (`HttpOnly`; в профиле `prod` также `Secure`). Запросы, изменяющие состояние, защищены от CSRF: токен передается фронтенду через cookie `XSRF-TOKEN` (доступную JS) и должен возвращаться в заголовке `X-XSRF-TOKEN`.

Тот же realm настроен и как **resource server** (bearer JWT): межсервисные вызовы аутентифицируются по учётным данным `lakehouse-internal-client`, JWT проверяется по эндпоинту `certs` realm.

Пути из белого списка (вход не требуется): `/healthz`, `/readyz`, `/actuator/**`, `/favicon.ico`. Все остальные запросы требуют аутентифицированной сессии; неаутентифицированные запросы браузера перенаправляются на страницу входа Keycloak, после входа пользователь возвращается на `/` (`defaultSuccessUrl`).

### Роли модделирования (RBAC)

Доступ к эндпоинтам модделирования предоставляется по ролям realm ниже. Настроена **иерархия ролей**, так что `ADMIN` подразумевает `EDITOR`, который подразумевает `VIEWER`:

```
LAKEHOUSE_MODELLER_ADMIN > LAKEHOUSE_MODELLER_EDITOR > LAKEHOUSE_MODELLER_VIEWER
```

| Роль realm | Доступ |
|---|---|
| `LAKEHOUSE_MODELLER_VIEWER` | Чтение рабочих пространств, редактор схем (только чтение), список веток |
| `LAKEHOUSE_MODELLER_EDITOR` | Всё, что у просмотрщика + создание веток, открытие/правка/сохранение/удаление файлов, отправка на ревью, restore |
| `LAKEHOUSE_MODELLER_ADMIN` | Всё, что у редактора + админ-поверхность: все пространства, принудительное удаление, TTL очистки, журналы |

Правила применяются в `SecurityConfig` (`/api/admin/**` → ADMIN; `/api/workspaces/**` чтение → VIEWER, запись → EDITOR; `/api/vcs/*` по операции). Внутри модделирования `UserContext`/`ModellerRole` дополнительно контролируют владение рабочим пространством (править может только владелец), а фронтенд вычисляет `readOnly` из `effectiveRole` (`GET /api/user`).

### Необходимые настройки

| Свойство / env | По умолчанию | Описание |
|---|---|---|
| `KEYCLOAK_ISSUER_URI` | `http://keycloak.lakehouse:8085/realms/lakehouse` | URL realm; из него строятся эндпоинты auth/token/userinfo/certs |
| `KEYCLOAK_UI_CLIENT_SECRET` | `super-secret-bff-key-1234567890` | Секрет клиента `lakehouse-ui-client` |
| `LAKEHOUSE_UI_REDIRECT_URI` | `{baseUrl}/login/oauth2/code/{registrationId}` | OAuth2 redirect URI BFF |
| `KEYCLOAK_INTERNAL_CLIENT_SECRET` | `super-secret-internal-key-987654321` | Секрет `lakehouse-internal-client` (межсервисные вызовы) |
| `LAKEHOUSE_VCS_PROVIDER` | `local-git` | Git-провайдер модделирования |
| `LAKEHOUSE_GIT_URL` | — | Legacy-фолбэк URL репозитория конфигурации (используется, если домены не сконфигурированы) |
| `LAKEHOUSE_GIT_BRANCH` | `main` | Основная ветка |
| `LAKEHOUSE_VCS_AUTH_TYPE` / `LAKEHOUSE_VCS_USER` / `LAKEHOUSE_VCS_TOKEN` / `LAKEHOUSE_VCS_PASSWORD` / `LAKEHOUSE_VCS_SSH_KEY_PATH` | — | Учётные данные системного аккаунта Git |
| `LAKEHOUSE_WORKSPACE_STORAGE` / `LAKEHOUSE_WORKSPACE_ROOT` | `local` / `/tmp/lakehouse-workspaces` | Бэкенд хранилища пространств |
| `LAKEHOUSE_WORKSPACE_TTL_HOURS` | `4` | TTL очистки неактивных пространств |
| `server.servlet.session.cookie.name` / `.http-only` | `JSESSIONID` / `true` | Имя сессионной cookie и флаг HttpOnly |
| `server.servlet.session.cookie.secure` | `false` (`true` в профиле `prod`) | Установите `true`, если UI работает по HTTPS |

### Настройка учетных записей и ролей в Keycloak

1. **Разверните Keycloak.** В demo-окружении compose поднимает Keycloak 26.0 с админ-консолью на `http://localhost:8085` (учетные данные из `KEYCLOAK_ADMIN`/`KEYCLOAK_ADMIN_PASSWORD`, по умолчанию `admin`/`admin_local_password`) и импортирует эталонный realm из `demo/compose/conf_infra/security/realms/lakehouse-realm.json`. В production используйте постоянную БД и смените все пароли/секреты по умолчанию.
2. **Роли realm'а.** В realm `lakehouse` определены общие роли `USER` и `ADMIN`, а также роли модделирования:
   - `LAKEHOUSE_MODELLER_VIEWER` — модделирование только для чтения;
   - `LAKEHOUSE_MODELLER_EDITOR` — редактирование конфигурационных документов;
   - `LAKEHOUSE_MODELLER_ADMIN` — админ-поверхность модделирования.

   Роли попадают к сервисам в claim JWT `realm_access.roles` и преобразуются в authorities `ROLE_…` (`SecurityConfig` читает `realm_access.roles`, `roles`, `resource_access.*.roles`).
3. **Клиент `lakehouse-ui-client`.** Confidential-клиент (*Standard Flow Enabled*, *Direct Access Grants* выключен), используемый данным BFF. Проверьте, что:
   - *Valid redirect URIs* содержат внешне видимый адрес UI: по умолчанию `http://localhost:8080/*` и `http://localhost:8080/login/oauth2/code/keycloak`;
   - *Web Origins* содержит origin UI (`http://localhost:8080`);
   - при развертывании на другом хосте/порту добавьте соответствующие redirect URI и web origin и задайте `LAKEHOUSE_UI_REDIRECT_URI`.
4. **Создание пользователей.** Админ-консоль → realm `lakehouse` → *Users* → *Add user*: заполните username/email/имя, затем *Credentials* → задайте пароль (выключите *Temporary*, чтобы пароль был постоянным).
5. **Назначение ролей.** *Users* → выберите пользователя → *Role mapping* → фильтр *Filter by realm roles* → назначьте `USER` и/или `ADMIN` и нужную роль `LAKEHOUSE_MODELLER_*` кнопкой *Assign*. Иерархия ролей автоматически покрывает `ADMIN` → `EDITOR`/`VIEWER`.
6. **Service account.** Confidential-клиент `lakehouse-internal-client` (*Service Accounts Enabled*) используется backend-сервисами для межсервисных вызовов; его секрет должен совпадать со значением `KEYCLOAK_INTERNAL_CLIENT_SECRET` на каждом сервисе.

После настройки откройте UI - первый запрос перенаправит на страницу входа Keycloak; войти смогут только пользователи с учетной записью в realm `lakehouse`.