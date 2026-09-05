# Подсистема VCS для разработчиков платформы

Документ описывает, как устроена подсистема VCS (Configuration Versioning System,
система версионирования конфигурации), как её расширять и какие параметры необходимо
знать разработчику. Документ предназначен для разработчиков платформы lakehouse. Если
вы только управляете файлами конфигурации в Git-репозитории, см. вместо этого
[Руководство пользователя git-расширения](git_extension_user_guide.md).

## 1. Что такое VCS

VCS — это подсистема конфигурации-как-кода (GitOps) сервиса `lakehouse-config-svc`. Она
рассматривает Git-репозиторий как **источник истины** для метаданных конфигурации: те же
DTO конфигурации, которые принимает REST API, можно описывать в виде YAML-файлов в стиле
Kubernetes, а подсистема периодически синхронизирует их в базу данных конфигурации.

В отличие от REST API, VCS даёт:

- полную историю каждого изменения конфигурации (коммиты Git);
- декларативную, доступную для ревью конфигурацию;
- атомарное применение целого коммита;
- автоматическую защиту управляемых объектов от случайных изменений через REST API
  (флаг `isVcsManaged`).

Весь специфичный для VCS код находится в пакете `org.lakehouse.config.vcs` (плюс два
read-only контроллера `VcsSyncLogController`/`VcsObjectLogController` в
`org.lakehouse.config.controller` и `VcsManagedException` в
`org.lakehouse.config.exception`).

## 2. Как работает синхронизация

```
┌──────────────────┐   fetch + diff   ┌────────────────────────────────┐
│  Git-репозиторий │ ───────────────▶ │  GitOpsScheduler (poll)        │
│  (ветка)         │                  │  ├─ pull()                     │
└──────────────────┘                  │  ├─ getCurrentCommitId()       │
                                      │  ├─ diff против последнего     │
                                      │  │   SUCCESS-коммита           │
                                      │  └─ sync() в одной транзакции  │
                                      └───────────────┬────────────────┘
                                                      ▼
                                      ┌────────────────────────────────┐
                                      │  GitOpsChangeSetBuilder        │
                                      │  └─ GitOpsYamlParser           │
                                      └───────────────┬────────────────┘
                                                      ▼
                                      ┌────────────────────────────────┐
                                      │  GitOpsSynchronizer            │
                                      │  validate → apply → unmanage   │
                                      │  → запись object log           │
                                      │  → пометка SUCCESS/FAILED      │
                                      └───────────────┬────────────────┘
                                                      ▼
                                      ┌────────────────────────────────┐
                                      │  PostgreSQL                    │
                                      │  vcs_sync_log + vcs_object_log │
                                      └────────────────────────────────┘
```

Оркестратором является `GitOpsScheduler` (`org.lakehouse.config.vcs.component`). Бин
регистрируется только при `lakehouse.config.vcs.git.sync.enabled=true` и запускается
через `@Scheduled` с `fixedDelayString` / `initialDelayString` из того же блока свойств.
Метод `sync()` является `synchronized` и идемпотентным; его можно вызывать напрямую,
например из интеграционных тестов.

Каждый цикл:

1. Ленивый `init()` клиента `VcsClient`.
2. `pull()` отслеживаемой ветки (fetch + hard reset на ref удалённой ветки).
3. Определение текущего идентификатора коммита (HEAD).
4. Пропуск, если коммит уже есть в `vcs_sync_log` (`existsByCommitId`) или если последний
   `SUCCESS` уже указывает на HEAD.
5. Вычисление diff между последним успешно применённым коммитом и HEAD. При пустой базе
   весь HEAD рассматривается как набор созданных файлов.
6. `GitOpsChangeSetBuilder` оставляет только файлы конфигурации (расширения `.yaml`,
   `.yml`, `.json`, имя не начинается с `.`), парсит созданные/изменённые файлы из HEAD и
   удалённые — из последнего успешного коммита.
7. `GitOpsSynchronizer.sync(changeSet, head)` валидирует и применяет всё в **одной
   транзакции**. Исключения превращаются в строку `FAILED` в логе синхронизации
   (записывается в отдельной транзакции `REQUIRES_NEW`, чтобы пережить rollback).
8. Инфраструктурные сбои (`VcsClientException`: репозиторий недоступен, ошибка SSH,
   непрочитанный клон) только логируются и повторяются в следующем цикле.

### Правила транзакции и порядка

`GitOpsSynchronizer` (`org.lakehouse.config.vcs.service.GitOpsSynchronizer`):

- `applyAll` применяет созданные/изменённые объекты в порядке `ConfigKind.order()`;
  наборы данных дополнительно упорядочиваются по зависимостям в `sources`
  (`orderDataSetsDependencyWise`; при циклических ссылках — возврат к заявленному
  порядку).
- `unmanageAll` снимает флаг `isVcsManaged` с удаляемых объектов в **обратном** порядке
  зависимостей.
- После применения каждый затронутый объект записывается в `vcs_object_log`
  (`date_time_rec`, `object_name` = первичный ключ, `kind`, `file_path`, `commit_id`).
- Только после успешного применения всего коммита пишется строка `SUCCESS` в
  `vcs_sync_log`.

## 3. Абстракция VCS

Ядро абстракции — интерфейс `VcsClient` (`org.lakehouse.config.vcs.VcsClient`):

| Метод | Описание |
|---|---|
| `void init()` | Гарантирует наличие локальной копии и её привязку к настроенному remote. |
| `void pull()` | Fetch отслеживаемой ветки и hard reset локального checkout на неё. |
| `String getCurrentCommitId()` | Текущий commit id (HEAD ветки) после `pull()`. |
| `List<VcsDiffEntry> getDiff(String baseCommitId)` | Файлы, изменённые между `baseCommitId` и HEAD. |
| `Optional<String> readFileContent(String commitId, String path)` | Содержимое файла в заданном коммите. |

Вспомогательные типы в том же пакете:

- `VcsDiffEntry` — `record VcsDiffEntry(String path, VcsChangeType type)`.
- `VcsChangeType` — `enum { CREATED, UPDATED, DELETED }`.
- `VcsClientException` — runtime-исключение для **инфраструктурных** сбоев; оно не
  считается неуспешной синхронизацией, цикл повторится позже.

### Встроенная Git-реализация

`GitVcsClient` (`org.lakehouse.config.vcs.client`) — единственная встроенная
реализация, построена на **JGit**.

- `init()` применяет SSH-настройки (только если задан `privateKeyPath`) и клонирует
  remote, если нет локального `.git`, иначе открывает локальный репозиторий.
- `pull()` выполняет fetch `+refs/heads/*:refs/remotes/origin/*` и сбрасывает локальный
  checkout на выбранный ref ветки.
- `getDiff()` использует `DiffFormatter` с включённым детектированием переименований.
  **Переименование сообщается как DELETE + CREATE**, поскольку объекты конфигурации
  идентифицируются по содержимому, а не по пути файла.
- Когда `baseCommitId` пуст, все дерево сообщается как CREATED.
- SSH поддерживает аутентификацию `publickey` одним приватным ключом; ключ используется
  только когда задан `privateKeyPath`.

Поскольку весь конвейер выше клиента потребляет только абстракцию `VcsClient`, замена
транспорта (SVN, Mercurial, REST-сервис и т.п.) не требует изменений в синхронизаторе,
построителе набора изменений, планировщике или слое персистентности.

## 4. Разбор декларативного YAML

`GitOpsYamlParser` (`org.lakehouse.config.vcs.yaml.GitOpsYamlParser`) связывает YAML-файл
с DTO:

- файл должен начинаться с поля `kind` (стиль Kubernetes);
- значение `kind` выбирает целевой класс DTO (перечисление `ConfigKind`);
- сравнение `kind` регистронезависимо и устойчиво к дефисам/подчёркиваниям/пробелам, так
  что `DataSet`, `dataset` и `data-set` — это один и тот же kind;
- поля-перечисления десериализуются регистронезависимо (например, `postgresql` ==
  `POSTGRESQL`);
- неизвестные свойства — **жёсткая ошибка**, чтобы декларативное описание было строгим.

`ConfigKind` (`org.lakehouse.config.vcs.yaml.ConfigKind`) определяет распознаваемые kinds
с их YAML-значением, классом DTO и порядком зависимостей `order`:

| ConfigKind | YAML `kind` | DTO | order |
|---|---|---|---|
| `NAME_SPACE` | `NameSpace` | `NameSpaceDTO` | 1 |
| `DRIVER` | `Driver` | `DriverDTO` | 2 |
| `DATA_SOURCE` | `DataSource` | `DataSourceDTO` | 3 |
| `SCRIPT` | `Script` | `ScriptContent` | 4 |
| `TASK_EXECUTION_SERVICE_GROUP` | `TaskExecutionServiceGroup` | `TaskExecutionServiceGroupDTO` | 5 |
| `TASK` | `Task` | `TaskDTO` | 6 |
| `DATA_SET` | `DataSet` | `DataSetDTO` | 7 |
| `SCENARIO_ACT_TEMPLATE` | `ScenarioActTemplate` | `ScenarioActTemplateDTO` | 8 |
| `QUALITY_METRICS_CONF` | `QualityMetricsConf` | `QualityMetricsConfDTO` | 9 |
| `SCHEDULE` | `Schedule` | `ScheduleDTO` | 10 |

Первичный ключ каждого kind извлекается в `GitOpsYamlParser.resolveKey()` (например,
`keyName`, `name` или `key`). `ScriptContent` — специальный record
`(String key, String value)` для скриптов, тело которых — инлайн-литерал.

## 5. Контракт управления флагом `isVcsManaged`

Каждая конкретная сущность конфигурации несёт булев флаг `isVcsManaged`
(`@Column(nullable=false)`, по умолчанию `false`). Он помечает объекты, которыми владеет
VCS:

`NameSpace`, `Schedule`, `TaskExecutionServiceGroup`, `SQLTemplate`, `Script`, `Task`,
`DataSet`, `TemplateScenarioAct`, `Driver`, `DataSource`, `QualityMetricsConf`.

Сервис каждой сущности реализует **тройной контракт** (см. `NameSpaceService`,
`ScriptService`, `TaskService`, `DataSetService`, `DriverService`, `DataSourceService`,
`ScheduleService`, `ScenarioActTemplateService`, `TaskExecutionServiceGroupService`,
`QualityMetricsConfService`):

1. Пользовательские `save(...)` / `deleteById(...)` вызывают `rejectIfVcsManaged(key,
   operation)` и бросают `VcsManagedException` (HTTP `409 Conflict`), если объект
   управляется.
2. `saveVcs(...)` сохраняет объект и выставляет `isVcsManaged = true`. Именно этот метод
   вызывается из `GitOpsSynchronizer.apply()`.
3. `unmanage(...)` сбрасывает флаг в `false`. Вызывается из
   `GitOpsSynchronizer.unmanage()`, когда YAML-файл удаляется из репозитория.

`Task` и `Driver` дополнительно каскадно распространяют флаг на связанные `SQLTemplate`
через `SQLTemplateService.markTaskManaged` / `markDriverManaged`.

Флаг является **производным во время выполнения** и никогда не читается из YAML: YAML
включает/выключает только саму синхронизацию. Флаг — это также механизм защиты
управляемых объектов от случайных изменений через REST API.

## 6. Параметры разработчика

Все параметры VCS живут под префиксом `lakehouse.config.vcs.*` и связываются классом
`GitVcsConfigurationProperties` (`org.lakehouse.config.vcs.configuration`). Они
внедряются в бин `GitVcsClient` классом `GitVcsConfiguration` (управляется
`@ConditionalOnProperty(lakehouse.config.vcs.git.sync.enabled=true)`).

| Свойство | Переменная окружения | По умолчанию | Назначение |
|---|---|---|---|
| `lakehouse.config.vcs.git.repository-url` | `LAKEHOUSE_CONFIG_GIT_REPOSITORY_URL` | *(пусто)* | URL удалённого репозитория (`git://`, `ssh://`, `http(s)://`, локальный) |
| `lakehouse.config.vcs.git.branch` | `LAKEHOUSE_CONFIG_GIT_BRANCH` | `main` | Отслеживаемая ветка |
| `lakehouse.config.vcs.git.local-clone-path` | `LAKEHOUSE_CONFIG_GIT_LOCAL_CLONE_PATH` | *(пусто)* | Локальная директория сервиса для его клона |
| `lakehouse.config.vcs.git.private-key-path` | `LAKEHOUSE_CONFIG_GIT_PRIVATE_KEY_PATH` | *(пусто)* | Путь к SSH-ключу, только для `ssh://` |
| `lakehouse.config.vcs.git.sync.enabled` | `LAKEHOUSE_CONFIG_GIT_SYNC_ENABLED` | `false` | Главный выключатель планировщика VCS |
| `lakehouse.config.vcs.git.sync.interval-ms` | `LAKEHOUSE_CONFIG_GIT_SYNC_INTERVAL_MS` | `30000` | Фиксированная задержка между циклами |
| `lakehouse.config.vcs.git.sync.initial-delay-ms` | `LAKEHOUSE_CONFIG_GIT_SYNC_INITIAL_DELAY_MS` | `10000` | Задержка первого цикла после старта |

Эталонная настройка — в `src/main/resources/application.yml`; в демостеке значения
объявлены в `demo/k8s`/`demo/compose` (лёгкий `git-server`, обслуживающий
`git://git-server:9418/config-repo.git`, ветка `main`, `sync.enabled=true`).

## 7. Ручной вызов планировщика

`GitOpsScheduler.sync()` публичен и `synchronized`, поэтому его можно вызывать напрямую
(например, из интеграционных тестов или административного триггера). Он:

- пропускает коммиты, у которых уже есть строка в `vcs_sync_log`;
- записывает ошибки конфигурации/валидации как `FAILED` (чтобы проблемный коммит не
  повторялся вечно);
- оставляет инфраструктурные `VcsClientException` на повтор следующего цикла.

## 8. Как расширять абстракцию VCS

### 8.1 Добавление нового бэкенда VCS

Реализуйте пять методов `VcsClient` (`init`, `pull`, `getCurrentCommitId`, `getDiff`,
`readFileContent`), переиспользуйте `VcsDiffEntry`/`VcsChangeType` и бросайте
`VcsClientException` при транзиентных инфраструктурных сбоях. По желанию выставите
`VcsClient` `@Bean` в `@Configuration`, управляемый `@ConditionalOnProperty`, как в
`GitVcsConfiguration`. Остальной конвейер (построитель набора изменений,
синхронизатор, планировщик, персистентность) от транспорта не зависит.

### 8.2 Добавление нового типа конфигурационного объекта

1. Добавьте запись в перечисление `ConfigKind` с YAML-значением, классом DTO и
   порядком зависимостей `order`.
2. Убедитесь, что у сущности есть булево поле `isVcsManaged` с геттером и сеттером.
3. Реализуйте тройной контракт в сервисе: `save(...)`/`deleteById(...)`, вызывающие
   `rejectIfVcsManaged(...)` и бросающие `VcsManagedException`; `saveVcs(...)`,
   выставляющий флаг; `unmanage(...)`, снимающий его.
4. В `GitOpsSynchronizer` добавьте `case <KIND> -> <xService>.saveVcs(...)` в `apply()` и
   соответствующий `<xService>.unmanage(key)` в `unmanage()`.
5. В `GitOpsYamlParser.resolveKey()` добавьте извлечение первичного ключа для нового
   kind.
6. По желанию добавьте валидацию, специфичную для `ConfigKind`, вызываемую из
   `GitOpsSynchronizer.validate()`.
7. Если kind требует особого порядка (как наборы данных по `sources`), расширьте логику
   порядка в `applyAll()` / `orderDataSetsDependencyWise()`.

### 8.3 Тесты и ссылки

Эталонные тесты находятся в `src/test/java/org/lakehouse/config/vcs/`:
`GitOpsIntegrationTest`, `GitVcsClientTest`, `GitOpsChangeSetBuilderTest`,
`GitOpsSchedulerUnitTest`, `GitOpsYamlParserTest` и хелпер `TestGitRepository`.

## 9. Read-only REST-эндпоинты

Потребляются UI (`lakehouse-ui-svc`) и возвращают историю синхронизации:

- `GET /v1_0/configs/vcs/logs` — `VcsSyncLogController` (`VcsSyncLogDTO`): `from`, `to`
  (обязательны), опционально `status`, `commitId`. Строка `SUCCESS` хранит применённый
  commit id, строка `FAILED` — сообщение об ошибке.
- `GET /v1_0/configs/vcs/objectlogs` — `VcsObjectLogController` (`VcsObjectLogDTO`):
  опционально `commitId`, `kind`, `from`, `to`, `filePath`, `objectName`; обязательно
  либо `commitId`, либо обе даты `from` и `to`.