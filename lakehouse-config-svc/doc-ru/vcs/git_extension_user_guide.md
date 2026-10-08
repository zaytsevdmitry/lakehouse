# Руководство пользователя git-расширения

Руководство для тех, кто управляет конфигурацией `lakehouse-config-svc` декларативно через
встроенное git-расширение (подсистему VCS). Здесь описана доменная модель и формат
репозитория (YAML), флаг `isVcsManaged`, настройка синхронизации и ожидаемые сообщения об
ошибках.

Описание для разработчиков — в [Подсистеме VCS для разработчиков платформы](cvs_for_developers.md).

## 1. Что делает git-расширение

`lakehouse-config-svc` можно конфигурировать либо через REST API, либо **декларативно** из
Git-репозиториев. Конфигурация принадлежит **доменам**; у каждого домена собственный
репозиторий. Когда git-расширение включено, сервис периодически для каждого домена:

1. подтягивает настроенную ветку репозитория домена;
2. сравнивает её HEAD с последним успешно применённым коммитом этого домена;
3. разбирает изменённые YAML/JSON-файлы;
4. применяет весь коммит атомарно к базе данных конфигурации.

Git-репозиторий домена — **источник истины**: любое ваше изменение коммита автоматически
применяется, а каждое применение фиксируется в логе синхронизации под этим доменом.

## 2. Как включить и настроить

Все настройки живут под префиксом `lakehouse.config.vcs.*`. Задаются в `application.yml`
или через переменные окружения. Список и имена доменов **задаёт пользователь**: объявите
каждый домен, у которого есть репозиторий (здесь показано обобщённо; вложенность
опциональна):

```yaml
lakehouse:
  config:
    vcs:
      git:
        sync:
          enabled: ${LAKEHOUSE_CONFIG_GIT_SYNC_ENABLED:false}
          interval-ms: ${LAKEHOUSE_CONFIG_GIT_SYNC_INTERVAL_MS:30000}
          initial-delay-ms: ${LAKEHOUSE_CONFIG_GIT_SYNC_INITIAL_DELAY_MS:10000}
      domains:
        <domain>:
          priority: 0
          git:
            repository-url: ${LAKEHOUSE_CONFIG_GIT_<DOMAIN>_URL:}
            branch: ${LAKEHOUSE_CONFIG_GIT_<DOMAIN>_BRANCH:main}
            local-clone-path: ${LAKEHOUSE_CONFIG_GIT_<DOMAIN>_CLONE_PATH:}
            private-key-path: ${LAKEHOUSE_CONFIG_GIT_<DOMAIN>_PRIVATE_KEY_PATH:}
          domains:
            <nested-domain>:
              priority: 1
              git:
                repository-url: ${LAKEHOUSE_CONFIG_GIT_<NESTED_DOMAIN>_URL:}
                branch: ${LAKEHOUSE_CONFIG_GIT_<NESTED_DOMAIN>_BRANCH:main}
                local-clone-path: ${LAKEHOUSE_CONFIG_GIT_<NESTED_DOMAIN>_CLONE_PATH:}
                private-key-path: ${LAKEHOUSE_CONFIG_GIT_<NESTED_DOMAIN>_PRIVATE_KEY_PATH:}
```

### Параметры

| Свойство | Переменная окружения | По умолчанию | Описание |
|---|---|---|---|
| `lakehouse.config.vcs.git.sync.enabled` | `LAKEHOUSE_CONFIG_GIT_SYNC_ENABLED` | `false` | Поставьте `true`, чтобы включить git-расширение. |
| `lakehouse.config.vcs.git.sync.interval-ms` | `LAKEHOUSE_CONFIG_GIT_SYNC_INTERVAL_MS` | `30000` | Период цикла синхронизации (мс). |
| `lakehouse.config.vcs.git.sync.initial-delay-ms` | `LAKEHOUSE_CONFIG_GIT_SYNC_INITIAL_DELAY_MS` | `10000` | Задержка первого цикла после старта (мс). |
| `lakehouse.config.vcs.domains.<name>.git.repository-url` | `LAKEHOUSE_CONFIG_GIT_<NAME>_URL` | *(пусто)* | URL репозитория домена (`git://`, `ssh://`, `http(s)://` или локальный путь). **Обязателен** для синхронизации домена. |
| `lakehouse.config.vcs.domains.<name>.git.branch` | `LAKEHOUSE_CONFIG_GIT_<NAME>_BRANCH` | `main` | Синхронизируемая ветка. |
| `lakehouse.config.vcs.domains.<name>.git.local-clone-path` | `LAKEHOUSE_CONFIG_GIT_<NAME>_CLONE_PATH` | *(пусто)* | Локальный каталог, где сервис хранит клон домена. |
| `lakehouse.config.vcs.domains.<name>.git.private-key-path` | `LAKEHOUSE_CONFIG_GIT_<NAME>_PRIVATE_KEY_PATH` | *(пусто)* | Путь к приватному SSH-ключу; нужен только для URL вида `ssh://`. Пусто — анонимный доступ. |
| `lakehouse.config.vcs.domains.<name>.priority` | - | наименьший | Порядок применения домена: родители перед вложенными, домены одного уровня по возрастанию значения. |
| `lakehouse.config.vcs.domains.<name>.domains` | - | - | Вложенные поддомены. |

Имя переменной окружения содержит **имя домена в верхнем регистре**: для домена
`<domain>` это `LAKEHOUSE_CONFIG_GIT_<DOMAIN>_URL`, `LAKEHOUSE_CONFIG_GIT_<DOMAIN>_BRANCH`,
`LAKEHOUSE_CONFIG_GIT_<DOMAIN>_CLONE_PATH` и `LAKEHOUSE_CONFIG_GIT_<DOMAIN>_PRIVATE_KEY_PATH`.

### Пример (переменные окружения)

```bash
LAKEHOUSE_CONFIG_GIT_SYNC_ENABLED=true
LAKEHOUSE_CONFIG_GIT_MYDATA_URL=git://git-server:9418/mydata.git
LAKEHOUSE_CONFIG_GIT_MYDATA_CLONE_PATH=/tmp/config-mydata
LAKEHOUSE_CONFIG_GIT_SYNC_INTERVAL_MS=30000
```

Домен с пустым `repository-url` просто пропускается (пишется warning).

## 3. Домены и структура репозитория

Конфигурация организована по **доменам**. У каждого домена собственный репозиторий, файлы
которого — это **плоский набор, один конструкт в файле**. Конфигурацией считаются только
`*.yaml`, `*.yml` и `*.json`; все остальные файлы (например, `load.sh`) игнорируются. Имена
файлов, начинающиеся с `.` (dotfiles), тоже игнорируются.

Каждый конструкт, загруженный из репозитория домена, сохраняется с `domainKeyName` = имя
домена, поэтому в строке известно, какому домену принадлежит конструкт. Сам ключ не
меняется: `keyName` - это первичный ключ, он сквозной, поэтому другим доменом он не
переиспользуется - два домена, указывающие на одну таблицу, используют два разных `keyName`.
Исключение - скрипты: у них домена нет вовсе, они только контент и общие по ключу.

Пример структуры (имена доменов произвольны; демостек из readme использует `platform`,
`processing` и `analytics`):

```
domains/platform/
├── datasources/processingdb.yaml
├── drivers/postgres.yaml
├── sql-scripts/dq/non_zero_count.yaml
├── scenarios/spark.yaml
└── tasks/prepare-jdbc.yaml

domains/analytics/
├── datasets/transaction_dds.yaml
├── quality/metrics/transaction_dds_qm.yaml
└── schedules/regular.yaml

domains/processing/
├── datasets/client_processing.yaml
└── schedules/generateSource.yaml
```

Каждый файл конфигурации начинается с поля `kind`, выбирающего целевой вид конструкта.
Остальная часть файла связывается с этим видом.

### Поддерживаемые kinds

Применяются в порядке ниже; при удалении файлов используется обратный порядок.

| YAML `kind` | Пример пути | Первичный ключ |
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

Kinds `ERDiagram` и `DataLineageDiagram` хранятся в репозитории, но сервисом конфигурации
**не применяются**.

## 4. Формат YAML

### Правила

- Поле `kind` **обязательно** и должно идти первым.
- Сопоставление `kind` регистронезависимо и терпимо к дефисам/подчёркиваниям/пробелам:
  `DataSet`, `dataset` и `data-set` — один и тот же kind.
- Значения перечислений регистронезависимы (например, `postgresql` то же, что `POSTGRESQL`).
- Неизвестные свойства — **жёсткая ошибка**: весь коммит отклоняется. Держите файл в
  соответствии с полями DTO, описанными REST API / Swagger.
- Датасеты могут ссылаться на другие датасеты в `sources`; сервис применяет датасеты в
  порядке зависимостей, поэтому датасеты-источники применяются после своих зависимостей.
- Домен (`domainKeyName`) **не пишется** в YAML: он выводится во время синхронизации из
  репозитория-владельца файла.

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

### Пример Script

Скрипты хранят глобальный `key` (слеши пути заменяются точками) и тело скрипта в
литеральном блоке `value`:

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

## 5. Флаг `isVcsManaged`

Каждый конструкт, применённый из Git-репозитория, сохраняется с `isVcsManaged = true`.
Этот флаг различает источник конструкта:

- конструктам, созданным через REST API, соответствует `isVcsManaged = false`;
- конструктам, применённым из Git, — `isVcsManaged = true`.

### Следствия

- **Защита REST API.** Любая попытка создать, изменить или удалить VCS-управляемый
  конструкт через REST API отклоняется ответом `409 Conflict`. Сначала измените его в
  репозитории и дождитесь, пока синхронизация его подхватит.
- **Удаление в два шага.** Удаление YAML-файла из репозитория **не удаляет** конструкт из
  базы. Синхронизация лишь сбрасывает `isVcsManaged` на соответствующей сущности. Затем
  конструкт нужно удалить через REST API.
- **Возврат владения.** После сброса флага конструкт снова полностью управляется через REST
  API.
- **Принадлежность домену.** Конструкты дополнительно разграничены по `domainKeyName`.
  REST API отклоняет ответом `409 Conflict` (`DomainConflictException`) любое изменение,
  уводящее конструкт за пределы его домена (например, пере-привязку датасета одного домена
  к источнику другого).

Флаг никогда не читается из YAML; он выводится в рантайме процессом синхронизации.

## 6. Семантика синхронизации

- **По доменам.** Каждый репозиторий домена синхронизируется независимо, в порядке
  приоритета (родители перед вложенными). Строки лога синхронизации несут имя домена.
- **Атомарность.** Коммит применяется внутри одной транзакции: все созданные/изменённые
  конструкты, все удаления, записи лога объектов и маркер `SUCCESS`. При любой ошибке весь
  коммит откатывается.
- **Идемпотентность.** Коммиты, чья пара `(commit_id, domain)` уже имеет строку в
  `vcs_sync_log`, пропускаются. Если последний `SUCCESS` домена уже указывает на HEAD,
  ничего не делается.
- **Первый запуск.** Когда у домена нет успешных коммитов в базе, весь HEAD этого домена
  трактуется как набор созданных файлов.
- **Переименования.** Переименование файла трактуется как удаление + создание, потому что
  конструкты идентифицируются по содержимому (первичному ключу), а не по имени файла.
- **Кросс-доменная валидация.** `Schedule` может ссылаться только на датасеты своего домена
  (`domainKeyName` равен) или вовсе не доменно-привязанные. Иначе коммит завершается ошибкой
  `DataSetDomainConflictException` и записывается как `FAILED`.
- **Обработка сбоев.** Коммит, не прошедший разбор, валидацию или привязку, фиксируется как
  `FAILED` (с доменом и текстом ошибки) и **не повторяется**. Последующий исправляющий
  коммит подкатывает исправленное содержимое как часть нового diff.
- **Инфраструктурные ошибки** (репозиторий недоступен, нет клона, сбой SSH) только
  логируются и повторяются на следующем цикле.
- **Ход процесса виден** в UI на панели "VCS → VCSLog": строки `SUCCESS` хранят применённый
  commit id, строки `FAILED` — текст ошибки; лог объектов перечисляет каждый затронутый
  объект по коммиту; и там, и там отслеживается домен.

## 7. Проверка состояния синхронизации

Read-only REST-эндпоинты (также отображаются в UI):

- `GET /v1_0/configs/vcs/logs?from=...&to=...&status=...&commitId=...`
  Возвращает историю лога синхронизации (`id`, `commitId`, `syncDateTime`, `status`,
  `domainKeyName`, `errorMessage`). `from` и `to` обязательны; `status`
  (`SUCCESS`/`FAILED`) и `commitId` опциональны.
- `GET /v1_0/configs/vcs/objectlogs?commitId=...&kind=...&from=...&to=...&filePath=...&objectName=...`
  Возвращает лог по объектам (`id`, `dateTimeRec`, `objectName`, `kind`, `filePath`,
  `commitId`, `domainKeyName`). Нужен либо `commitId`, либо пара `from` и `to`.

## 8. Сообщения об ошибках

### Ошибки конфигурации (пишутся как `FAILED` с текстом в `vcs_sync_log`)

| Сообщение | Значение |
|---|---|
| `Missing required field 'kind'` | В YAML-файле нет поля `kind`. |
| `YAML document is empty` | Файл пуст или пустой. |
| `YAML document cannot be parsed as a configuration map` | Некорректный YAML или корень — не отображение. |
| `Configuration kind must not be blank` | `kind` есть, но пустой. |
| `Unknown configuration kind: <value>` | Значение `kind` не соответствует ни одному виду. |
| `Cannot bind YAML document to <kind>` | Остальные поля не связываются с DTO, например неизвестное свойство или невалидное значение. |
| `Schedule '<schedule>' refers to data set '<dataset>' of domain '<other>' instead of '<domain>'` | Расписание ссылается на датасет другого домена (`DataSetDomainConflictException`). |

Поскольку неизвестные свойства не связываются, большинство проблем валидации проявляется
как `Cannot bind YAML document to <kind>`. Лежащее в основе сообщение Jackson приводится
как причина (cause).

### Инфраструктурные ошибки (логируются, повторяются на следующем цикле)

| Сообщение | Значение |
|---|---|
| `Cannot init VCS client for repository <url>` | Сбой клонирования/открытия или настройки SSH. |
| `Cannot pull repository <url>` | Сбой fetch или reset. |
| `No reachable commit on branch <branch>` | В ветке нет коммитов. |
| `Cannot resolve current commit on branch <branch>` | Сбой чтения ссылки ветки. |
| `Cannot compute diff against <baseCommitId>` | Сбой вычисления diff. |
| `Cannot read file <path> at commit <commitId>` | Сбой чтения blob файла конфигурации. |
| `VCS client is not initialized; call init() first` | Метод вызван до `init()`. |
| `SSH private key is not readable: <key>` | Настроенный приватный ключ не существует или не читается. |
| `Cannot create SSH session factory for key <key>` | Сбой настройки SSH для ключа. |

### Ошибки REST API

| HTTP-статус | Шаблон сообщения | Значение |
|---|---|---|
| `409 Conflict` | `Configuration construct '<keyName>' is managed via VCS (git) and cannot be <created or updated|deleted> through the REST API. Remove it from the configuration repository (git) first.` | Пользователь пытается изменить или удалить VCS-управляемый конструкт через REST (`VcsManagedException`). |
| `409 Conflict` | `Configuration construct '<keyName>' belongs to domain '<domain>' and cannot be <reason> through the REST API.` | Запрос увёл бы конструкт за пределы его домена (`DomainConflictException`). |
| `400 Bad Request` | `Either commitId or both from and to must be provided` | Запрос лога объектов не содержит ни `commitId`, ни пары `from`+`to`. |