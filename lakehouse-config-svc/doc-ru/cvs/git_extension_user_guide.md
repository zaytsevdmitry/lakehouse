# Руководство пользователя git-расширения

Это руководство для пользователей, которые управляют конфигурацией
`lakehouse-config-svc` декларативно — через встроенное Git-расширение (подсистема CVS).
В нём описан формат репозитория (YAML), флаг `isCvsManaged`, настройка синхронизации и
сообщения об ошибках.

Для описания уровня разработчика см.
[Подсистема CVS для разработчиков платформы](cvs_for_developers.md).

## 1. Что делает Git-расширение

`lakehouse-config-svc` можно настраивать либо через REST API, либо **декларативно** из
Git-репозитория. Когда Git-расширение включено, сервис периодически:

1. выполняет pull настроенной ветки;
2. вычисляет diff между HEAD и последним успешно применённым коммитом;
3. разбирает изменённые YAML/JSON-файлы;
4. атомарно применяет весь коммит к базе данных конфигурации.

Git-репозиторий — **источник истины**: любое ваше изменение, попавшее в коммит,
применяется автоматически, а каждое применение фиксируется в журнале синхронизации.

## 2. Как включить и настроить

Все настройки находятся под префиксом `lakehouse.config.cvs.*`. Задайте их в
`application.yml` или через переменные окружения.

```yaml
lakehouse:
  config:
    cvs:
      git:
        repository-url: ${LAKEHOUSE_CONFIG_GIT_REPOSITORY_URL:}
        branch: ${LAKEHOUSE_CONFIG_GIT_BRANCH:main}
        local-clone-path: ${LAKEHOUSE_CONFIG_GIT_LOCAL_CLONE_PATH:}
        private-key-path: ${LAKEHOUSE_CONFIG_GIT_PRIVATE_KEY_PATH:}
        sync:
          enabled: ${LAKEHOUSE_CONFIG_GIT_SYNC_ENABLED:false}
          interval-ms: ${LAKEHOUSE_CONFIG_GIT_SYNC_INTERVAL_MS:30000}
          initial-delay-ms: ${LAKEHOUSE_CONFIG_GIT_SYNC_INITIAL_DELAY_MS:10000}
```

### Параметры

| Свойство | Переменная окружения | По умолчанию | Описание |
|---|---|---|---|
| `lakehouse.config.cvs.git.repository-url` | `LAKEHOUSE_CONFIG_GIT_REPOSITORY_URL` | *(пусто)* | URL репозитория конфигурации (`git://`, `ssh://`, `http(s)://` или локальный путь). **Обязателен** для включения синхронизации. |
| `lakehouse.config.cvs.git.branch` | `LAKEHOUSE_CONFIG_GIT_BRANCH` | `main` | Ветка для синхронизации. |
| `lakehouse.config.cvs.git.local-clone-path` | `LAKEHOUSE_CONFIG_GIT_LOCAL_CLONE_PATH` | *(пусто)* | Локальная директория, где сервис хранит свой клон. **Обязательна** для включения синхронизации. |
| `lakehouse.config.cvs.git.private-key-path` | `LAKEHOUSE_CONFIG_GIT_PRIVATE_KEY_PATH` | *(пусто)* | Путь к SSH-приватному ключу; нужен только для URL вида `ssh://`. Оставьте пустым для анонимного доступа. |
| `lakehouse.config.cvs.git.sync.enabled` | `LAKEHOUSE_CONFIG_GIT_SYNC_ENABLED` | `false` | Установите `true`, чтобы включить Git-расширение. |
| `lakehouse.config.cvs.git.sync.interval-ms` | `LAKEHOUSE_CONFIG_GIT_SYNC_INTERVAL_MS` | `30000` | Период цикла синхронизации (мс). |
| `lakehouse.config.cvs.git.sync.initial-delay-ms` | `LAKEHOUSE_CONFIG_GIT_SYNC_INITIAL_DELAY_MS` | `10000` | Задержка первого цикла после старта (мс). |

### Пример (переменные окружения)

```bash
LAKEHOUSE_CONFIG_GIT_REPOSITORY_URL=git://git-server:9418/config-repo.git
LAKEHOUSE_CONFIG_GIT_BRANCH=main
LAKEHOUSE_CONFIG_GIT_LOCAL_CLONE_PATH=/tmp/config-repo
LAKEHOUSE_CONFIG_GIT_SYNC_ENABLED=true
LAKEHOUSE_CONFIG_GIT_SYNC_INTERVAL_MS=30000
```

Если при `sync.enabled=true` два обязательных параметра (`repository-url`,
`local-clone-path`) пусты, клиент откажется стартовать с ошибкой
`Git repository URL must be configured` / `Git local clone path must be configured`.

## 3. Структура репозитория

Репозиторий конфигурации — это **плоский набор файлов, один объект конфигурации на
файл**. Конфигурацией считаются только файлы `*.yaml`, `*.yml` и `*.json`; все остальные
файлы (например, `load.sh`) игнорируются. Файлы, начинающиеся с `.` (dotfiles), также
игнорируются.

Пример структуры:

```
config-repo/
├── nameSpaces/demo.yaml
├── drivers/postgres.yaml
├── datasources/processingdb.yaml
├── datasets/1_transaction_dds.yaml
├── sql-scripts/dataset-sql-model/transaction_dds.yaml
├── tasks/prepare-jdbc.yaml
├── schedules/regular.yaml
└── quality/metrics/transaction_dds_qm.yaml
```

Каждый файл конфигурации начинается с поля `kind`, которое определяет тип объекта.
Остальное содержимое файла связывается с этим типом.

### Поддерживаемые kinds

Применяются в порядке ниже; при удалении файлов используется обратный порядок.

| YAML `kind` | Пример пути | Первичный ключ |
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

## 4. Формат YAML

### Правила

- Поле `kind` **обязательно** и должно идти первым по смыслу.
- Сравнение `kind` регистронезависимо и устойчиво к дефисам/подчёркиваниям/пробелам:
  `DataSet`, `dataset` и `data-set` — это один kind.
- Значения перечислений регистронезависимы (например, `postgresql` — то же, что
  `POSTGRESQL`).
- Неизвестные свойства — **жёсткая ошибка**: отклоняется весь коммит. Держите файл в
  соответствии с полями DTO, описанными REST API / Swagger.
- Наборы данных могут ссылаться на другие наборы в `sources`; сервис применяет наборы в
  порядке зависимостей, поэтому исходные наборы применяются до своих зависимостей.

### Пример DataSource

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

### Пример NameSpace

```yaml
kind: NameSpace
keyName: demo
description: Demo namespace
```

### Пример Script

Скрипты хранят глобальный `key` (точки заменяют путь в директориях) и тело скрипта как
блок-литерал `value`:

```yaml
kind: Script
key: dq.non_zero_count.sql
value: |
  select count(1) value
  from {{ refCat(targetDataSetKeyName) }}
```

### Пример DataSet

```yaml
kind: DataSet
keyName: transaction_dds
nameSpaceKeyName: DEMO
dataSourceKeyName: lakehousestorage
databaseSchemaName: default
tableName: transaction_dds
scripts:
  - key: dataset-sql-model.transaction_dds.sql
sources:
  client_processing:
    properties:
      fetchSize: "10000"
  transaction_processing:
    properties:
      fetchSize: "10000"
columnSchema:
  - name: id
    description: tx id
    dataType: bigint
    nullable: false
    order: 0
constraints:
  transaction_dds_pk:
    type: primary
    columns: id
    constraintLevelCheck: dataQuality
```

## 5. Флаг `isCvsManaged`

Каждый объект, применённый из Git-репозитория, сохраняется с флагом
`isCvsManaged = true`. Этот флаг отличает источник создания объекта:

- объекты, созданные через REST API, имеют `isCvsManaged = false`;
- объекты, применённые из Git, имеют `isCvsManaged = true`.

### Следствия

- **Защита через REST API.** Любая попытка создать, изменить или удалить
  CVS-управляемый объект через REST API завершится ошибкой HTTP `409 Conflict`. Сначала
  нужно изменить объект в репозитории и дождаться синхронизации.
- **Удаление в два шага.** Удаление YAML-файла из репозитория **не удаляет** объект из
  базы данных. Синхронизация лишь снимает `isCvsManaged` с соответствующей сущности.
  Затем пользователь должен удалить объект через REST API.
- **Возврат владения.** После снятия флага объект снова полностью управляется через
  REST API.

Флаг никогда не читается из YAML; он вычисляется во время выполнения процесса
синхронизации.

## 6. Семантика синхронизации

- **Атомарность.** Коммит применяется внутри одной транзакции: все созданные/изменённые
  объекты, все удаления, записи object log и маркер `SUCCESS`. При любой ошибке весь
  коммит откатывается.
- **Идемпотентность.** Коммиты, чей id уже есть в `cvs_sync_log`, пропускаются. Если
  последний `SUCCESS` уже указывает на HEAD, ничего не делается.
- **Первая синхронизация.** Когда в базе ещё нет успешного коммита, весь HEAD
  репозитория рассматривается как набор созданных файлов.
- **Переименования.** Переименование файла трактуется как удаление + создание, поскольку
  объекты идентифицируются по содержимому (первичному ключу), а не по имени файла.
- **Обработка ошибок.** Коммит, который не удалось разобрать, провалидировать или
  связать с DTO, записывается как `FAILED` с текстом ошибки и **не повторяется**.
  Следующий исправляющий коммит включит исправленное содержимое в новый diff.
- **Инфраструктурные ошибки** (репозиторий недоступен, нет клона, сбой SSH) только
  логируются и повторяются в следующем цикле.
- **Прогресс виден** в UI панели «CVS → CVSLog»: строки `SUCCESS` хранят применённый
  commit id, строки `FAILED` — сообщение об ошибке; object log перечисляет каждый
  затронутый объект по коммиту.

## 7. Проверка состояния синхронизации

Read-only REST-эндпоинты (также отображаются в UI):

- `GET /v1_0/configs/cvs/logs?from=...&to=...&status=...&commitId=...`
  Возвращает историю журнала синхронизации (`id`, `commitId`, `syncDateTime`, `status`,
  `errorMessage`). `from` и `to` обязательны; `status` (`SUCCESS`/`FAILED`) и `commitId`
  опциональны.
- `GET /v1_0/configs/cvs/objectlogs?commitId=...&kind=...&from=...&to=...&filePath=...&objectName=...`
  Возвращает пообъектный журнал (`id`, `dateTimeRec`, `objectName`, `kind`, `filePath`,
  `commitId`). Нужно указать либо `commitId`, либо обе даты `from` и `to`.

## 8. Сообщения об ошибках

### Ошибки конфигурации (записываются как `FAILED` с текстом в `cvs_sync_log`)

| Сообщение | Значение |
|---|---|
| `Missing required field 'kind'` | В YAML-файле нет поля `kind`. |
| `YAML document is empty` | Файл пуст или состоит из пробелов. |
| `YAML document cannot be parsed as a configuration map` | Некорректный синтаксис YAML или корень не является mapping. |
| `Configuration kind must not be blank` | `kind` есть, но пуст. |
| `Unknown configuration kind: <value>` | Значение `kind` не соответствует ни одному поддерживаемому типу. |
| `Cannot bind YAML document to <kind>` | Оставшиеся поля не удаётся связать с DTO, например неизвестное свойство или неверное значение. |

Поскольку неизвестные свойства мешают связыванию, большинство проблем валидации
проявляются как `Cannot bind YAML document to <kind>`. Исходное сообщение Jackson
включается как причина.

### Инфраструктурные ошибки (логируются, повторяются в следующем цикле)

| Сообщение | Значение |
|---|---|
| `Git repository URL must be configured` | `repository-url` пуст при включённой синхронизации. |
| `Git local clone path must be configured` | `local-clone-path` пуст при включённой синхронизации. |
| `Cannot init CVS client for repository <url>` | Не удалось клонировать/открыть репозиторий или настроить SSH. |
| `Cannot pull repository <url>` | Не удалось выполнить fetch или reset. |
| `No reachable commit on branch <branch>` | На ветке нет коммитов. |
| `Cannot resolve current commit on branch <branch>` | Не удалось прочитать ref ветки. |
| `Cannot compute diff against <baseCommitId>` | Не удалось вычислить diff. |
| `Cannot read file <path> at commit <commitId>` | Не удалось прочитать blob файла конфигурации. |
| `CVS client is not initialized; call init() first` | Метод вызван до `init()`. |
| `SSH private key is not readable: <key>` | Настроенный приватный ключ отсутствует или не читается. |
| `Cannot create SSH session factory for key <key>` | Не удалось настроить SSH для ключа. |

### Ошибки REST API

| HTTP-статус | Шаблон сообщения | Значение |
|---|---|---|
| `409 Conflict` | `Configuration construct '<keyName>' is managed via CVS (git) and cannot be <created or updated|deleted> through the REST API. Remove it from the configuration repository (git) first.` | Пользователь пытается изменить или удалить CVS-управляемый объект через REST. |
| `400 Bad Request` | `Either commitId or both from and to must be provided` | Запрос object log не содержит ни `commitId`, ни пары `from`+`to`. |