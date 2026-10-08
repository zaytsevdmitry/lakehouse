# Подсистема VCS для разработчиков платформы

Здесь описано, как устроена подсистема VCS (Configuration Versioning System), как её
расширять и какие параметры нужны разработчику. Документ предназначен для разработчиков
платформы lakehouse. Если вы только управляете конфигурацией через файлы в Git-репозиториях,
перейдите к [Руководству пользователя git-расширения](git_extension_user_guide.md).

## 1. Что такое VCS

VCS — это подсистема конфигурации-как-кода (GitOps) сервиса `lakehouse-config-svc`. Она
трактует Git-репозитории как **источник истины** для метаданных конфигурации: те же DTO,
которые принимает REST API, можно писать YAML-файлами в стиле Kubernetes, и подсистема по
расписанию синхронизирует их в базу данных конфигурации.

Конфигурация организована в виде **доменов**. Домен обеспечен собственным Git-репозиторием
и синхронизируется независимо; домен может содержать вложенные домены. Иерархия объявляется
в `lakehouse.config.vcs.domains` (см. раздел 6). Каждый конструкт, применённый из репозитория
домена, помечается именем домена (`domainKeyName`): это разграничивает объекты по доменам и
позволяет `Schedule` ссылаться только на датасеты своего домена.

В отличие от REST API, VCS даёт:

- полную историю каждого изменения конфигурации (коммиты Git);
- декларативную, проверяемую конфигурацию;
- атомарное применение всего коммита;
- автоматическую защиту управляемых конструктов от случайных правок через REST API (флаг
  `isVcsManaged`).

Весь VCS-код живёт в пакете `org.lakehouse.config.vcs` (плюс два read-only контроллера
`VcsSyncLogController`/`VcsObjectLogController` в `org.lakehouse.config.controller` и
`VcsManagedException`/`DomainConflictException`/`DataSetDomainConflictException` в
`org.lakehouse.config.exception`).

## 2. Как работает синхронизация

```
┌──────────────────────┐   fetch + diff по доменам  ┌────────────────────────────────┐
│  Git-репозитории     │ ──────────────────────────▶ │  GitOpsScheduler (poll)        │
│  (по одному на домен,│   pull() по домену          │  ├─ домены в порядке приоритета│
│  отслеживаемая ветка)│                             │  ├─ getCurrentCommitId()       │
└──────────────────────┘                             │  ├─ diff c последним SUCCESS   │
                                                     │  └─ sync() в одной транзакции │
                                                     └───────────────┬────────────────┘
                                                                     ▼
                                                     ┌────────────────────────────────┐
                                                     │  GitOpsChangeSetBuilder        │
                                                     │  └─ GitOpsYamlParser           │
                                                     └───────────────┬────────────────┘
                                                                     ▼
                                                     ┌────────────────────────────────┐
                                                     │  GitOpsSynchronizer            │
                                                     │  validate → stamp domain       │
                                                     │  → apply → unmanage            │
                                                     │  → record object logs          │
                                                     │  → mark SUCCESS/FAILED         │
                                                     └───────────────┬────────────────┘
                                                                     ▼
                                                     ┌────────────────────────────────┐
                                                     │  PostgreSQL                    │
                                                     │  vcs_sync_log + vcs_object_log │
                                                     └────────────────────────────────┘
```

Оркестратор — `GitOpsScheduler` (`org.lakehouse.config.vcs.component`). Бин регистрируется
только при `lakehouse.config.vcs.git.sync.enabled=true` и управляется `@Scheduled` с
`fixedDelayString` / `initialDelayString` из того же блока свойств. Метод `sync()`
`public` и `synchronized`, идемпотентен и может вызываться напрямую, например из
интеграционных тестов.

Каждый цикл планировщик обходит `LakehouseVCSProperties.orderedDomains()`: домены
обходятся **в глубину, родители перед вложенными доменами, домены одного уровня по
`priority` по возрастанию** (отсутствующий приоритет считается наименьшим и идёт последним).
Каждый домен синхронизируется последовательно, в собственной транзакции:

1. Пропустить домен, если репозиторий не настроен (`git.repository-url` пуст).
2. Лениво `init()` клиента `VcsClient` для домена (`GitVcsClientFactory.create`).
3. `pull()` отслеживаемой ветки (fetch + hard reset на remote-ссылку ветки).
4. Получить идентификатор текущего коммита (HEAD).
5. Пропустить, если пара `(commit_id, domain_key_name)` уже есть в `vcs_sync_log`
   (`existsByCommitIdAndDomainKeyName`) или последняя строка `SUCCESS` домена уже
   указывает на HEAD.
6. Загрузить `CurrentDomainContext` с именем домена (сбрасывается в `finally`).
7. Вычислить diff между последним успешно применённым коммитом домена и HEAD. На пустой
   базе весь head трактуется как набор созданных файлов.
8. `GitOpsChangeSetBuilder` оставляет только файлы конфигурации (расширения `.yaml`,
   `.yml`, `.json`, имя не начинается с `.`), парсит созданные/изменённые файлы из HEAD
   и удалённые — из последнего успешного коммита.
9. `GitOpsSynchronizer.sync(changeSet, head)` валидирует и применяет всё в **одной
   транзакции**. Исключения превращаются в строку `FAILED` лога синхронизации домена
   (строка пишется через `GitOpsFailureRecorder` в отдельной транзакции `REQUIRES_NEW`,
   чтобы пережить откат).
10. Инфраструктурные сбои (`VcsClientException`: репозиторий недоступен, сбой SSH,
    нечитаемый клон) только логируются и повторяются на следующем цикле.
11. Вложенные домены иерархически зависят от родителя: если для домена падает шаг 3, 9
    или инфраструктурный шаг, весь его поддерево пропускается в этом цикле. Обход
    рекурсивный (`rootDomains()` → `nestedDomains(...)`), поэтому вложенные дети
    обрабатываются только тогда, когда родительский домен применился успешно (либо
    синхронизировать было нечего).

### Правила транзакций и порядка

`GitOpsSynchronizer` (`org.lakehouse.config.vcs.service.GitOpsSynchronizer`):

- `validateAll` запускает вид-специфичную валидацию (Schedule / ScenarioActTemplate / Task)
  **до** любой записи, так что невалидный коммит ничего не задевает.
- `stampDomain` принудительно проставляет текущий домен (`CurrentDomainContext`) на каждый
  конструкт коммита (`setDomainKeyName`). Домен **никогда** не читается из YAML; он
  выводится из синхронизируемого репозитория и побеждает всё, что лежит в распарсенных
  DTO. Исключение — скрипты: это только контент, разделяемый между доменами по ключу,
  поэтому они не проштамповываются.
- `applyAll` применяет созданные/изменённые конструкты в порядке `YamlMetadataKind.order()`;
  датасеты дополнительно упорядочиваются по зависимостям в `sources`
  (`orderDataSetsDependencyWise`, циклические ссылки откатываются к объявленному порядку).
- `unmanageAll` сбрасывает флаг `isVcsManaged` удаляемых конструктов в **обратном** порядке
  зависимостей.
- `validateAll` также запускает `checkScheduleDatasetDomains`: каждый `Schedule` может
  ссылаться только на датасеты, чей `domainKeyName` равен домену расписания (или вовсе не
  доменно-привязанные датасеты). Несовпадение бросает `DataSetDomainConflictException`,
  что откатывает весь коммит и фиксирует `FAILED`.
- После применения каждый затронутый конструкт пишется в `vcs_object_log`
  (`date_time_rec`, `object_name` = первичный ключ, `kind`, `file_path`, `commit_id`,
  `domain_key_name`).
- Только когда весь коммит успешен, строка `SUCCESS` (с `domain_key_name`) пишется в
  `vcs_sync_log`.

## 3. Абстракция VCS

Ядро абстракции — интерфейс `VcsClient` (`org.lakehouse.config.vcs.VcsClient`):

| Метод | Описание |
|---|---|
| `void init()` | Убедиться, что локальная копия существует и указывает на настроенный remote. |
| `void pull()` | Сделать fetch отслеживаемой ветки и hard-reset локального чекаута на неё. |
| `String getCurrentCommitId()` | Идентификатор текущего HEAD после `pull()`. |
| `List<VcsDiffEntry> getDiff(String baseCommitId)` | Файлы, изменённые между `baseCommitId` и HEAD. |
| `Optional<String> readFileContent(String commitId, String path)` | Содержимое файла на заданном коммите. |

Вспомогательные типы значения в том же пакете:

- `VcsDiffEntry` — `record VcsDiffEntry(String path, VcsChangeType type)`.
- `VcsChangeType` — `enum { CREATED, UPDATED, DELETED }`.
- `VcsClientException` — runtime-исключение для **инфраструктурных** сбоев; оно не
  трактуется как неудачная синхронизация, поэтому цикл повторяется позже.

### Встроенная реализация на Git

`GitVcsClient` (`org.lakehouse.config.vcs.client`) — единственная встроенная реализация и
построена на **JGit**. Клиент создаётся **по домену** фабрикой `GitVcsClientFactory`
(`org.lakehouse.config.vcs.configuration`) с именем домена и его настройками `git.*`.

- `init()` применяет SSH-настройки (только когда задан `privateKeyPath`) и клонирует
  remote, если локального `.git` нет; иначе открывает локальный репозиторий.
- `pull()` делает fetch `+refs/heads/*:refs/remotes/origin/*` и сбрасывает локальный
  чекаут на fetched-ссылку ветки.
- `getDiff()` использует `DiffFormatter` со включённым определением переименований.
  **Переименование сообщается как DELETE + CREATE**, потому что конструкты
  идентифицируются по содержимому, а не по пути файла.
- Если `baseCommitId` пуст, всё дерево сообщается как CREATED.
- SSH-транспорт поддерживает аутентификацию `publickey` одним приватным ключом; ключ
  используется только когда задан `privateKeyPath`.

Поскольку весь конвейер выше клиента потребляет только абстракцию `VcsClient`, замена
транспорта (SVN, Mercurial, REST-сервис, ...) не требует изменений ни в синхронизаторе,
ни в сборщике набора изменений, ни в планировщике, ни в персистентности.

## 4. Декларативный разбор YAML

`GitOpsYamlParser` (`org.lakehouse.config.vcs.yaml.GitOpsYamlParser`) связывает YAML-файл
с DTO **в два этапа**:

1. `parsePreliminary(content)` возвращает `PreliminaryConfig(kind, body)` — читает только
   поле `kind`.
2. `parseFull(PreliminaryConfig)` связывает остальные поля с DTO.

Правила:

- файл должен начинаться с поля `kind` (стиль Kubernetes);
- значение `kind` выбирает целевой класс DTO (enum `YamlMetadataKind`);
- сопоставление `kind` регистронезависимо и терпимо к дефисам/подчёркиваниям/пробелам,
  поэтому `DataSet`, `dataset` и `data-set` принимаются одинаково;
- enum-поля десериализуются регистронезависимо (например `postgresql` == `POSTGRESQL`);
- неизвестные свойства — **жёсткая ошибка**, чтобы декларативное описание было строгим.

`YamlMetadataKind` (в `lakehouse-common`, `org.lakehouse.client.api.constant.YamlMetadataKind`)
определяет распознаваемые виды с их YAML-значением, классом DTO, идентифицирующим полем и
порядком зависимостей `order`. `isConfig()` показывает, является ли вид объектом
конфигурации, применяемым сервисом конфигурации; такие виды, как `ERDiagram`/
`DataLineageDiagram`, хранятся в репозитории, но **не применяются** (`isConfig() == false`):

| YamlMetadataKind | YAML `kind` | DTO | order | Применяется |
|---|---|---|---|---|
| `DRIVER` | `Driver` | `DriverDTO` | 2 | да |
| `DATA_SOURCE` | `DataSource` | `DataSourceDTO` | 3 | да |
| `SCRIPT` | `Script` | `ScriptDTO` | 4 | да |
| `TASK_EXECUTION_SERVICE_GROUP` | `TaskExecutionServiceGroup` | `TaskExecutionServiceGroupDTO` | 5 | да |
| `TASK` | `Task` | `TaskDTO` | 6 | да |
| `DATA_SET` | `DataSet` | `DataSetDTO` | 7 | да |
| `SCENARIO_ACT_TEMPLATE` | `ScenarioActTemplate` | `ScenarioActTemplateDTO` | 8 | да |
| `QUALITY_METRICS_CONF` | `QualityMetricsConf` | `QualityMetricsConfDTO` | 9 | да |
| `SCHEDULE` | `Schedule` | `ScheduleDTO` | 10 | да |
| `ER_DIAGRAM` | `ERDiagram` | `ERDiagramDTO` | 11 | нет |
| `METRIC_DQ` | `MetricDQ` | `MetricDQStatusDTO` | 12 | да |
| `DATA_LINEAGE_DIAGRAM` | `DataLineageDiagram` | `DataLineageDiagramDTO` | 13 | нет |

Первичный ключ каждого вида извлекается методом `GitOpsYamlParser.resolveKey()` (например,
`keyName`, `name` или `key`). `ParsedConfig` — связанный record `(YamlMetadataKind kind,
Object dto)`.

## 5. Контракт управления флагом `isVcsManaged`

Каждая конкретная сущность конфигурации несёт булев `isVcsManaged`
(`@Column(nullable=false)`, по умолчанию `false`). Он помечает конструкты, принадлежащие
VCS:

`Schedule`, `TaskExecutionServiceGroup`, `SQLTemplate`, `Script`, `Task`, `DataSet`,
`TemplateScenarioAct`, `Driver`, `DataSource`, `QualityMetricsConf`.

Сущности дополнительно несут `domainKeyName` (домен-владелец конструкции, проставляется во
время синхронизации); исключения — `TaskExecutionServiceGroup` и `Script`: они не
доменно-привязаны — скрипты это общий контент, группы исполнения задач это настройка уровня
платформы.

Каждый сервис сущности реализует **трёхсторонний контракт** (см. `ScriptService`,
`TaskService`, `DataSetService`, `DriverService`, `DataSourceService`, `ScheduleService`,
`ScenarioActTemplateService`, `TaskExecutionServiceGroupService`,
`QualityMetricsConfService`):

1. Пользовательский `save(...)` / `deleteById(...)` вызывает `rejectIfVcsManaged(key,
   operation)` и бросает `VcsManagedException` (HTTP `409 Conflict`), когда конструкт
   управляемый. Доменно-осведомлённые сервисы дополнительно вызывают
   `rejectDomainConflict(...)` и бросают `DomainConflictException` (HTTP `409 Conflict`),
   когда запрос пытается увести конструкт за пределы его домена (например, пере-привязать
   датасет к источнику другого домена).
2. `saveVcs(...)` сохраняет конструкт и выставляет `isVcsManaged = true`. Этот метод
   вызывает `GitOpsSynchronizer.apply()`.
3. `unmanage(...)` сбрасывает флаг в `false`. Вызывается `GitOpsSynchronizer.unmanage()`,
   когда YAML-файл удалён из репозитория.

`Task` и `Driver` дополнительно каскадируют флаг на свои `SQLTemplate` через
`SQLTemplateService.markTaskManaged` / `markDriverManaged`.

Флаг **выводится в рантайме** и никогда не читается из YAML: YAML лишь включает/выключает
синхронизацию целиком. Это также механизм, защищающий управляемые конструкты от случайных
правок через REST API.

## 6. Параметры разработчика

Иерархию конфигурации связывает `LakehouseVCSProperties`
(`org.lakehouse.config.vcs.LakehouseVCSProperties`, префикс `lakehouse.config.vcs`).
`orderedDomains()` возвращает уплощённый список доменов в порядке зависимостей: родители
впереди, домены одного уровня по `priority` по возрастанию (отсутствующий приоритет
считается наименьшим). `DomainProperties.isRepositoryConfigured()` сообщает, настроен ли
у домена `repository-url`; клиенты `VcsClient` по доменам создаются по требованию через
`GitVcsClientFactory`.

| Свойство | Переменная окружения | По умолчанию | Значение |
|---|---|---|---|
| `lakehouse.config.vcs.git.sync.enabled` | `LAKEHOUSE_CONFIG_GIT_SYNC_ENABLED` | `false` | Главный выключатель планировщика VCS |
| `lakehouse.config.vcs.git.sync.interval-ms` | `LAKEHOUSE_CONFIG_GIT_SYNC_INTERVAL_MS` | `30000` | Фиксированная задержка планировщика между циклами |
| `lakehouse.config.vcs.git.sync.initial-delay-ms` | `LAKEHOUSE_CONFIG_GIT_SYNC_INITIAL_DELAY_MS` | `10000` | Задержка первого цикла после старта |
| `lakehouse.config.vcs.domains.<name>.priority` | - | наименьший | Позиция в порядке применения (по возрастанию, родители перед детьми) |
| `lakehouse.config.vcs.domains.<name>.git.repository-url` | `LAKEHOUSE_CONFIG_GIT_<NAME>_URL` | *(пусто)* | URL remote-репозитория (`git://`, `ssh://`, `http(s)://`, локальный) |
| `lakehouse.config.vcs.domains.<name>.git.branch` | `LAKEHOUSE_CONFIG_GIT_<NAME>_BRANCH` | `main` | Отслеживаемая ветка |
| `lakehouse.config.vcs.domains.<name>.git.local-clone-path` | `LAKEHOUSE_CONFIG_GIT_<NAME>_CLONE_PATH` | *(пусто)* | Локальный каталог сервиса для клона домена |
| `lakehouse.config.vcs.domains.<name>.git.private-key-path` | `LAKEHOUSE_CONFIG_GIT_<NAME>_PRIVATE_KEY_PATH` | *(пусто)* | Путь к приватному SSH-ключу, только для `ssh://` |
| `lakehouse.config.vcs.domains.<name>.domains` | - | - | Вложенные поддомены |

Переменные окружения для домена `platform`: `LAKEHOUSE_CONFIG_GIT_PLATFORM_URL`,
`LAKEHOUSE_CONFIG_GIT_PLATFORM_BRANCH`, `LAKEHOUSE_CONFIG_GIT_PLATFORM_CLONE_PATH`,
`LAKEHOUSE_CONFIG_GIT_PLATFORM_PRIVATE_KEY_PATH`.

Поставляемый `src/main/resources/application.yml` объявляет только **legacy**-блок
`lakehouse.config.vcs.git.*`, поэтому по умолчанию домены не сконфигурированы, а
непустой `git.repository-url` публикуется как единственный домен `default`. Демостек
`demo/compose` поэтому задаёт дерево доменов явно системными свойствами
(`docker-compose.yaml`), вкладывая `platform` (`priority: 0`) с ребёнком `processing`
(`priority: 0`), внутри которого — `analytics` (`priority: 0`):

```
-Dlakehouse.config.vcs.git.sync.enabled=true
-Dlakehouse.config.vcs.domains.platform.priority=0
-Dlakehouse.config.vcs.domains.platform.git.repository-url=git://git-server:9418/platform.git
-Dlakehouse.config.vcs.domains.platform.git.branch=main
-Dlakehouse.config.vcs.domains.platform.git.local-clone-path=/tmp/platform
-Dlakehouse.config.vcs.domains.platform.domains.processing.priority=0
-Dlakehouse.config.vcs.domains.platform.domains.processing.git.repository-url=git://git-server:9418/processing.git
...
```

Все три репозитория обслуживает лёгкий `git-server`
(`git://git-server:9418/{platform,processing,analytics}.git`, ветка `main`).

## 7. Ручной вызов планировщика

`GitOpsScheduler.sync()` `public` и `synchronized`, поэтому его можно вызывать напрямую
(например, из интеграционных тестов или административного триггера). Он:

- обходит все настроенные домены в порядке приоритета и пропускает домены без настроенного
  репозитория;
- пропускает пары `(commit_id, domain_key_name)`, уже имеющие строку в `vcs_sync_log`;
- фиксирует ошибки конфигурации/валидации как `FAILED` для домена, где они случились (чтобы
  провинившийся коммит не повторялся вечно);
- оставляет инфраструктурные `VcsClientException` для повтора на следующем цикле.

## 8. Как расширять абстракцию VCS

### 8.1 Добавить новый VCS-бэкенд

Реализуйте пять методов `VcsClient` (`init`, `pull`, `getCurrentCommitId`, `getDiff`,
`readFileContent`), используйте `VcsDiffEntry`/`VcsChangeType` и бросайте
`VcsClientException` при транзиентных инфраструктурных сбоях. Зарегистрируйте клиент по
домену в `GitVcsClientFactory` (или выставите `VcsClient` как `@Bean` в `@Configuration`
с `@ConditionalOnProperty`). Остальной конвейер (сборщик набора изменений, синхронизатор,
планировщик, персистентность) от транспорта не зависит.

### 8.2 Добавить новый вид конструкта конфигурации

1. Добавьте запись в enum `YamlMetadataKind` в `lakehouse-common`: YAML-значение, класс DTO
   и `order` зависимостей.
2. Убедитесь, что соответствующая сущность имеет булево поле `isVcsManaged` с геттером и
   сеттером и, если конструкт доменно-привязан, поле `domainKeyName`.
3. Реализуйте трёхсторонний контракт в сервисе: `save(...)`/`deleteById(...)`, которые
   вызывают `rejectIfVcsManaged(...)` и бросают `VcsManagedException`; `saveVcs(...)`,
   который выставляет флаг и домен; `unmanage(...)`, который их сбрасывает.
4. В `GitOpsSynchronizer.apply()` добавьте ветку `case <KIND> -> <xService>.saveVcs(...)` и
   парный `<xService>.unmanage(key)` в `unmanage()`. Если конструкт несёт домен, добавьте его
   в `stampDomainOn(...)`.
5. В `GitOpsYamlParser.resolveKey()` добавьте извлечение первичного ключа для нового вида.
6. По желанию добавьте вид-специфичную валидацию, вызываемую из
   `GitOpsSynchronizer.validate()`.
7. Если виду нужен особый порядок (как датасетам по их `sources`), расширьте логику порядка
   в `applyAll()` / `orderDataSetsDependencyWise()`.

### 8.3 Тесты и примеры

Эталонные тесты — в `src/test/java/org/lakehouse/config/vcs/`:
`GitOpsIntegrationTest`, `GitVcsClientTest`, `GitOpsChangeSetBuilderTest`,
`GitOpsSchedulerUnitTest`, `GitOpsYamlParserTest` и хелпер `TestGitRepository`.

## 9. Read-only REST-эндпоинты

Используются UI (`lakehouse-ui-svc`) и возвращают историю синхронизации:

- `GET /v1_0/configs/vcs/logs` — `VcsSyncLogController` (`VcsSyncLogDTO`): `from`, `to`
  (обязательные), опционально `status`, `commitId`. Строка несёт `domainKeyName`; строка
  `SUCCESS` несёт применённый commit id, строка `FAILED` — текст ошибки.
- `GET /v1_0/configs/vcs/objectlogs` — `VcsObjectLogController` (`VcsObjectLogDTO`):
  опционально `commitId`, `kind`, `from`, `to`, `filePath`, `objectName`; нужен либо
  `commitId`, либо пара `from` и `to`. Каждая строка несёт `domainKeyName`.