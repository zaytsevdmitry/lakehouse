# Реестр функциональных возможностей проекта (на основе документации)

Реестр составлен по всем Markdown-документам репозитория (за исключением зеркал `doc-ru/`, `node_modules/` и чисто сборочных инструкций). Для каждой сущности/сервиса указаны реализованные фичи и источник — файл(ы) документации.

---

## Модуль: Платформа yarina в целом (metadata-driven управление данными)
*Путь к исходному файлу: `./README.md`, `./README_ru.md`, `./CHANGELOG.md`*

### Реализованные фичи:
- **Metadata-driven ETL/ELT-конвейеры**: универсальный конвейер вместо написания кода под каждый источник — изменения структур данных и правил трансформаций выполняются правкой внешних конфигураций (SQL/JSON) без переписывания кода.
- **Единый источник истины по метаданным**: централизованные метаданные (datasources, datasets, schedules, scenarios, quality metrics) обеспечивают data lineage, контроль доступа и комплаенс.
- **Интеллектуальная автоматизация**: метаданные запускают автоматические трансформации и событийно-ориентированные процессы.
- **Состав платформы**: config-svc, scheduler-svc, task-executor-svc, state-svc, Spark-приложения (dataset/DQ), UI, CLI, task-proxy-for-spark, безопасность (Keycloak, OpenBao), деплой Docker/K8s.
- **Планируемые улучшения (todo.md)**: контроль времени выполнения в статусе QUEUED, надёжная доставка эффективной конфигурации расписания в Kafka, унификация параметров datasource/task, запуск следующего экземпляра расписания до завершения предыдущего, устойчивость Spark-задач к исключениям.

---

## Модуль: Системный дизайн и архитектура
*Путь к исходному файлу: `./doc/system_design/system_design.md`*

### Реализованные фичи:
- **Управление изменениями данных на основе метаданных**: набор сервисов, управляющих обработкой данных по schedules/datasets/scenarios.
- **Хранение конфигураций**: config-svc хранит источники данных, датасеты, расписания, сценарии, драйверы; публикует изменения конфигураций в Kafka (`configuration_changes`).
- **Планирование**: scheduler-svc строит экземпляры расписаний/задач, разрешает зависимости, ставит задачи в очередь, управляет блокировками (lock/heartbeat/release).
- **Состояния интервалов датасетов**: state-svc хранит состояния (LOCKED/SUCCESS), ищет «дыры» (интервалы без SUCCESS) для проверки готовности зависимостей и исключения конфликтов.
- **Исполнение задач**: task-executor-svc берёт задачи из Kafka, блокирует их, исполняет (процессоры JDBC/state/spark), уведомляет heartbeat и освобождает блокировку с результатом.
- **Проксирование Spark-задач**: task-proxy-for-spark принимает spark-submit по REST, ведёт очередь в PostgreSQL, отправляет в кластеры (Standalone/K8s/YARN) и отслеживает статусы.
- **Полный цикл «конфигурация → задача»**: публикация изменений → построение задачи → очередь → блокировка → LOCKED → выполнение → SUCCESS → heartbeat/release.
- **Открытый вопрос**: осиротевший lock при удалении расписания (`./doc-ru/questions.md`).

---

## Модуль: Модель сущностей (data model)
*Путь к исходному файлу: `./doc/entities_design/entities_design.md`*

### Реализованные фичи:
- **NameSpace**: пространство имён/проект — высокоуровневая группировка объектов.
- **DataSource**: источник данных с `DataSourceSvcItem` (host/port/urn), key-value свойствами, `Driver`, `ConnectionTemplate`.
- **DataSet**: датасет — схема, таблица/endPoint, свои свойства; колонки (имя/тип/nullable/комментарий/порядок); констрейнты `DataSetConstraint` (PK/FK, тип, enabled, уровни проверки runtime/construct); источники/зависимости `DataSetSource`.
- **SQLTemplate**: привязка скрипта к датасету + источнику + драйверу (SQL-диалект).
- **Schedule**: расписание (intervalExpression, start/end, enabled).
- **TemplateScenarioAct / TemplateTask / TemplateTaskEdge**: шаблон сценария (набор задач + DAG).
- **ScenarioAct / ScenarioActTask / ScenarioActEdge**: экземпляр сценария (датасет + расписание + шаблон, интервалы start/end).
- **TaskExecutionServiceGroup**: группа выполнения задач.
- **Script / ScriptReference**: скрипты и их привязки.
- **QualityMetricsConf**: конфигурация проверок качества (наборы тестов/метрик, порог нарушения, enabled/save).
- **Логические зависимости**: script → dataset/sqlTemplate/quality-metric; scenarioActTemplate → schedule; taskExecutionService → scenarioActTemplate; dataset → schedule/quality-metric; dataSource → dataset; sqlTemplate → driver/datasource/dataset.

---

## Модуль: yarina-config-svc — сервис управления конфигурациями (source of truth)
*Путь к исходному файлу: `./yarina-config-svc/doc/readme.md`, `./yarina-config-svc/doc/content_configuration/*.md`*

### Реализованные фичи:
- **Единый источник истины метаданных**: CRUD-хранилище всех конфигураций в PostgreSQL (схема `yarina_config`) с REST API `/v1_0/configs`; на его основе работают scheduler-svc, task-executor-svc, state-svc.
- **Публикация изменений конфигураций в Kafka**: transaction outbox, топик `configuration_changes` (envelope `ConfigurationChangeDTO`: kind/keyName/action SAVE|DELETE/object, с задержками/лимитами и первоначальной рассылкой при старте).
- **Составные (effective/compound) эндпоинты**: сборка «эффективной» конфигурации расписания со слиянием шаблона сценария; конфигурация конкретной задачи; сборка всех скриптов модели датасета в один итоговый скрипт; «срез» источников (`/v1_0/configs/compound/sources/dataset/{keyName}`); построение составных объектов по ключу.
- **Иерархия и правила зависимостей**: верхние объекты (namespace → datasource → dataset) грузятся раньше; удаление верхнего требует удаления нижних; каскадные метаданные удаляются/приводятся к новой форме вместе с верхним элементом.
- **NameSpace**: логическое объединение датасетов (проект/разделение в mesh-подходе).
- **Driver**: конфигурация адаптации различных СУБД с похожей функциональностью через шаблонизацию SQL-диалекта (около 30 шаблонов: DDL схем/таблиц/партиций, PK/FK/unique/check, mergeDML/insertDML).
- **DataSource**: описание источника (host/port/urn/properties), типы database/iceberg; задание учётных данных и параметров сессии как key-value.
- **DataSet**: абстракция объекта данных (namespace, data source, schema/table, скрипты-фрагменты модели, зависимости `sources`, `columnSchema`, констрейнты, properties, `partitionStmt`).
- **Data lineage**: направленный граф зависимостей датасета (вершины + рёбра) на всех уровнях глубины — GET `/v1_0/configs/lineage/datasets/{keyName}`.
- **Quality metric**: конфигурация проверок качества (enabled/save, порог нарушения, testSets, thresholds, источники для проверки).
- **Scenario act template**: переиспользуемый шаблон акта сценария (список задач + DAG рёбра; одноимённые задачи переопределяются, остальные добавляются).
- **Schedule**: правила периодичности (intervalExpression: cron/@daily), обязательное время старта, опциональный стоп, enabled; составной объект из действий (scenarioActs) и их DAG; вычисляемые границы интервалов через Jinjava (`intervalStart`/`intervalEnd`).
- **Scripts**: динамические шаблоны-фрагменты с подстановками Jinjava (`{{ refCat('...') }}`, `{{ intervalStartDateTime }}`).
- **Script reference / collection**: переиспользование и составные (композитные) скрипты, хранение больших скриптов без экранирования; сортировка по `order`.
- **SQL template**: словарь SQL-диалекта (~31 шаблон команд), слияние driver→task с приоритетом задачи; tableDDLCompact, merge/insert DML и др.
- **Task executor service group**: маркер маршрутизации задач к группам исполнителей.
- **Task**: атомарное действие (процессор + аргументы, критичность critical/warn, maxRetries, driverKeyName, sqlTemplate, модульное тело процессора), переиспользуемый шаблон задачи.
- **Java REST-клиент** `ConfigRestClientApi` для доступа из других сервисов.
- **VCS (Configuration Versioning System / GitOps)**: Git как source of truth, периодическая синхронизация конфигураций и `isVcsManaged` (см. модуль VCS ниже).
- **Домены**: конфигурационный домен как единица изоляции и как собственный Git-репозиторий; `domainKeyName` проставляется при синхронизации, правила изоляции и настройка `yarina.domains.*` из общего модуля `yarina-domain-api` (см. фичу «Домены и репозитории»).
- **Безопасность и аудит**: Keycloak OAuth2/JWT (resource server), роли → `ROLE_<NAME>` для `@PreAuthorize`, сервис-сервисные вызовы по `client_credentials`, `AuditLoggingFilter` (JSON в `AUDIT_LOG`), health `/healthz`/`/readyz`.

---

## Модуль: yarina-config-svc — VCS (Configuration Versioning System / GitOps)
*Путь к исходному файлу: `./yarina-config-svc/doc/vcs/vcs_for_developers.md`, `./yarina-config-svc/doc/vcs/git_extension_user_guide.md`*

### Реализованные фичи:
- **Git как source of truth**: тот же набор DTO, что и REST API, записывается Kubernetes-style YAML/JSON файлами; подсистема периодически синхронизирует их в БД конфигураций (конфигурация-как-код, GitOps).
- **Полная история изменений** каждой конфигурации (git-коммиты), декларативная проверяемая конфигурация, атомарное применение целого коммита.
- **GitOpsScheduler**: периодический цикл pull → diff против последнего успешного коммита → sync в одной транзакции; метод `sync()` synchronized и идемпотентен.
- **Атомарность и журнал**: применение валидация→apply→unmanage → запись `vcs_object_log`; только при полном успехе коммита пишется `SUCCESS` в `vcs_sync_log`; ошибки — `FAILED` (отдельная `REQUIRES_NEW`-транзакция), инфраструктурные сбои — повтор на следующем цикле.
- **Абстракция `VcsClient`**: `init/pull/getCurrentCommitId/getDiff/readFileContent`; реализация `GitVcsClient` на JGit (SSH publickey, rename = delete+create, пустой diff трактует всю ветку как созданную); смена транспорта без правок остального конвейера.
- **`GitOpsYamlParser` + `ConfigurationProduceResolver` SPI**: обязательный `kind` (регистр/разделители нечувствительны), регистро-независимые enum, неизвестные свойства — жёсткая ошибка. Виды конфигураций (9) разбираются не enum'ом с таблицей DTO/порядка, а по одному `@Component`-резолверу на вид (`getKind()` + `resolve(keyName)`), собираемым в `ConfigurationProduceResolverRegistry.findByKind(...)`; дубликаты kind → ошибка на старте. Вид `NameSpace` удалён.
- **`isVcsManaged`**: метка владения конструктива VCS; REST создание/изменение/удаление управляемого — HTTP `409 Conflict`; удаление файла из репозитория лишь снимает метку (двухшаговое удаление); трёхсторонний контракт `save/saveVcs/unmanage` в сервисах.
- **Параметры**: `yarina.config.vcs.sync.{enabled, interval-ms, initial-delay-ms}` — только цикл синхронизации (`YarinaVCSProperties`); сами домены и их git-настройки живут в `yarina.domains.*` (`DomainSettings` из `yarina-domain-api`, см. фичу «Домены и репозитории»).
- **Read-only REST**: `GET /v1_0/configs/vcs/logs` (история синхронизации) и `GET /v1_0/configs/vcs/objectlogs` (журнал затронутых объектов), потребляются UI.
- **Тесты**: `GitOpsIntegrationTest`, `GitVcsClientTest`, `GitOpsChangeSetBuilderTest`, `GitOpsSchedulerUnitTest`, `GitOpsYamlParserTest`, `TestGitRepository`.

---

## Модуль: yarina-domain-api — общая модель доменов и Git-доступа
*Путь к исходному файлу: `./yarina-domain-api/doc-ru/readme.md` (EN: `./yarina-domain-api/doc/readme.md`)*

### Реализованные фичи:
- **Единое пространство имён**: `yarina.domains.<name>.{priority, git.{repository-url, branch, local-clone-path, credentials.{auth-type, ssh-private-key-path, username, token, password}}, domains}`; `DomainSettings` нормализует имя домена (trim + lower case), коллизия регистра — ошибка на старте, обход дерева — `rootDomains()`/`nestedDomains(parent)`/`orderedDomains()` (родитель перед детьми, по `priority` по возрастанию, без `priority` → `Integer.MAX_VALUE` → в конец).
- **Домен активен, только если задан `git.repository-url`**: без него config-svc пропускает домен, ui-svc падает при использовании; legacy-свойства `yarina.config.vcs.git.*` и синтезированный домен `default` удалены.
- **По-доменные учётные данные**: `GitCredentialsSettings.effectiveAuthType()` — явный `auth-type` → из ключа → по наличию учётных данных → анонимный доступ; валидация требует ключ для `ssh`, токен для `token`, пару username/password для `basic`; слияния нет, глобального аккаунта нет (`yarina.config.vcs.system-account` и `yarina.modeller.vcs-system-account` удалены).
- **Транспорт**: `DomainVcsSpec` (спецификация: url/branch/clone path/credentials) + `DomainVcsFactory` — `clientFor(...)` для путей чтения с постоянным клоном, `repositoryFor(...)` для транзиентных клонов и ревью; `VcsClient` SPI с реализацией `GitVcsClient` (JGit), `VcsException`/`VcsReviewResult`/`VcsDiffEntry`.
- **Два потребителя**: `yarina-config-svc` (цикл синхронизации `yarina.config.vcs.sync.*`, `buildSpec(domain, true)` — требует `git.local-clone-path`) и `yarina-ui-svc` (`buildSpec(domain, false)`, панель веток по `domainNames()`).

---

## Фича: Домены и репозитории (конфигурационные домены)
*Путь к исходному файлу: `./yarina-domain-api/doc-ru/readme.md`, `./yarina-config-svc/doc/content_configuration/domains.md`, `./yarina-ui-svc/doc/readme.md`, `./yarina-scheduler-svc/doc/readme.md`, `./yarina-task-executor-svc/doc/readme.md`*

### Реализованные фичи:
- **Домен = единица изоляции конфигурации = Git-репозиторий**: каждый домен владеет собственным репозиторием, забирается и применяется самостоятельно, а каждый загруженный из него конструкт получает метку `domainKeyName` (`GitOpsSynchronizer.stampDomainOn(...)`). Домен **не пишется в YAML** — он выводится из репозитория и всегда побеждает значение разобранного DTO.
- **Дерево доменов**: домен может объявлять вложенные домены (`yarina.domains.<name>.domains`), обход в глубину (`orderedDomains()`), родитель всегда применяется раньше детей; внутри одного уровня сортировка по `priority` по возрастанию, без `priority` → `Integer.MAX_VALUE` (в конец), равные приоритесы — по имени. Вложенность — конфигурационное, а не складское отношение; единственное поведенческое следствие — распространение отказа: при сбое синхронизации домена всё его поддерево пропускается до успеха родителя.
- **`keyName` остаётся глобальным**: это единственный первичный ключ (`KeyEntityAbstract.keyName`), он **не** входит в составной ключ с `domainKeyName`, поэтому один и тот же `keyName` не может существовать в двух доменах. Домен — атрибут владения, а не часть идентичности.
- **Скрипты общие между доменами**: `SQLTemplate` не штампуется доменом; домен на строке выводится из ссылающихся `Driver`/`Task` (`SQLTemplateEntitySpecifier.domainOf()`). Конструкт, созданный через REST API, домена не имеет (`domainKeyName = null`).
- **Правила изоляции**: `Schedule` может ссылаться только на датасеты своего домена — иначе весь коммит падает с `DataSetDomainConflictException` (`checkScheduleDatasetDomains()`), потому что применимость коммита не должна зависеть от репозитория, которым коммит не управляет; REST не даёт перезаписать конструкт одного домена конструктом другого — `DomainConflictException` → `409 Conflict` (`rejectDomainConflict(...)` в сервисах `DataSet`, `DataSource`, `Driver`, `Task`, `ScenarioActTemplate`, `Schedule`, `QualityMetricsConf`, `TaskExecutionServiceGroup`); удаление файла из репозитория конструкта не удаляет — только снимает `isVcsManaged`.
- **Legacy-режим одного репозитория удалён**: свойства `yarina.config.vcs.git.repository-url`/`.branch`/`.local-clone-path`/`.private-key-path` и синтезированный домен `default` больше не существуют; без `git.repository-url` у домена синхронизация пропускается (config-svc) или домен непригоден (ui-svc). Цикл синхронизации остался отдельным: `yarina.config.vcs.sync.*` (`enabled` по умолчанию `false`).
- **Настройка доменов в config-svc**: `yarina.domains.<name>.{priority, domains.*, git.repository-url, git.branch (main), git.local-clone-path, git.credentials.{auth-type, ssh-private-key-path, username, token, password}}`; домен без `git.repository-url` пропускается целиком, имя домена нормализуется (trim + lower case), дубликат по регистру — ошибка на старте. Учётные данные по-доменные и никогда не сливаются (глобального аккаунта нет). Цикл синхронизации глобален: `yarina.config.vcs.sync.{enabled, interval-ms, initial-delay-ms}`. Переменные окружения: `YARINA_CONFIG_GIT_SYNC_ENABLED|_INTERVAL_MS|_INITIAL_DELAY_MS` и `YARINA_DOMAINS_<NAME>_GIT_REPOSITORY_URL|_BRANCH|_LOCAL_CLONE_PATH|_GIT_CREDENTIALS_SSH_PRIVATE_KEY_PATH`.
- **Настройка доменов в ui-svc (Modelling)**: те же `yarina.domains.<name>.git.*` через общий `DomainSettings` (`yarina.modeller` хранит только storage/auth/session/logging); `git.branch` домена (по умолчанию `main`) является целью ревью (MR/PR). Панель веток показывает плоский список доменов: `GET /api/vcs/branches` возвращает по `DomainBranchesResponse {domain, branches[], branchMain}` на домен из `domainNames()` (имена отсортированы по алфавиту, дерево и `priority` не учитываются), `POST /api/vcs/branch` создаёт ветку в именованном домене. Недоступный репозиторий — пустой список веток, а отсутствующий `repository-url` — жёсткая ошибка (`Domain '<name>': no repository URL configured (property: yarina.domains.<name>.git.repository-url)`, HTTP 400). Workspace — набор пар `(domain, branch)` с id `md5(username + "|" + отсортированные "domain=branch")`, каждая пара чекаутится в свою папку `<domain> (<branch>)`; ревью сабмитится **по доменам** (группировка файлов по папкам, отдельный коммит/ push в каждый домен, пустой diff → `NO_CHANGES`). `domainKeyName` в workspace не редактируется (`SchemaService.DERIVED_PROPERTIES`); если репозиторий содержит несколько доменов под `domains/<name>/`, workspace хранит только поддерево выбранного домена и срезает префикс. Fallback-домен `default` удалён, а `domainRemoteUrl()`/`domainBranchMain()` больше не существуют: список доменов берётся из общего `DomainSettings.domainNames()`.
- **Настройка доменов в scheduler-svc**: собственной доменной конфигурации нет; планировщик применяет `ScheduleTaskInstanceService.checkDomain(...)` — сравнивает домен расписания с `allowedDomains ∪ {domainKeyName}` группы `TaskExecutionServiceGroup`; при несовпадении `ScheduleTaskInstance` → `CONF_ERROR`, сообщение продюсера удаляется, в Kafka ничего не уходит; отсутствующая группа — не отказ (предупреждение, задача публикуется). Так как исполнители группируются только по `taskExecutionServiceGroupName` (Kafka `group.id`), эта проверка не пускает чужой домен в группу.
- **Настройка доменов в task-executor-svc**: `yarina.task-executor.domains.<domainKeyName>.<dataSourceKeyName>.service-properties` (`DomainDataSourceServiceProperties`) — свободная карта настроек подключения источника конкретного домена; `getServiceProperties(domainKey, dataSourceKey)` даёт `Optional.empty()` для неизвестного домена/источника. Карта **применяется на пути исполнения**: `ExecuteService.prepareProperties(...)` сливает её в `DataSourceDTO.getService().getProperties()` (семантика `putAll` — перекрывает значения config-svc, удобно чтобы не держать секреты в Git), откуда она доходит до `JdbcConnectionFactory` и резолвера секретов.
- **Наблюдаемость**: домен есть в обеих таблицах журналов (`vcs_sync_log.domain_key_name`, `vcs_object_log.domain_key_name`) и в фильтрах REST (`GET /v1_0/configs/vcs/logs`, `.../vcs/objectlogs` — `domainKeyName`); UI использует `domainKeyName` как read-only фильтр журналов VCS.
- **Тесты**: `ScheduleTaskInstanceServiceDomainCheckTest` (3 сценария: чужой домен отклоняется и задача не публикуется, домен из `allowedDomains` принимается, собственный домен группы принимается), `DomainDataSourceServicePropertiesTest` (биндинг динамической карты через `Environment`, изоляция записей по домену, сквозной доступ к бину через `ExecuteService`).

---

## Модуль: yarina-scheduler-svc — планировщик
*Путь к исходному файлу: `./yarina-scheduler-svc/doc/readme.md`, `./yarina-scheduler-svc/doc/scheduling/Scheduling.md`, `./yarina-scheduler-svc/doc/restapi.md`*

### Реализованные фичи:
- **Регистрация расписаний**: потребление изменений из Kafka (`configuration_changes`, kind `Schedule`, action `SAVE`) и построение экземпляров расписаний по `intervalExpression`.
- **Жизненный цикл расписания**: статусы NEW → RUNNING → SUCCESS/FAILED; выключение `enabled` останавливает и создание новых, и обработку статусов запущенных.
- **Сценарии и задачи**: создание экземпляров сценариев/задач, трекинг статуса каждого элемента; параметризация границ интервалов (Jinjava-выражения).
- **Разрешение зависимостей**: направленные графы `scenarioActEdges` и `dagEdges` — задача/сценарий завершаются только после успеха всех зависимостей; некритичные сбои не блокируют последующие.
- **Очередь задач**: передача задач исполнителям через Kafka (топик `scheduled_task_msg`).
- **Замки (locks)**: взятие задачи исполнителем, продление heartbeat, release с результатом; защита от повторного исполнения.
- **Ретри**: автоматический повтор неуспешных задач (задержки `lag-when-failed`/`lag-when-config-failed`, лимит `maxRetries`).
- **Внутренний планировщик**: периодические слоты registration (build) / run / resolvedeps / task.retry.
- **Проверка домена при публикации**: домен расписания сверяется с `allowedDomains ∪ {domainKeyName}` группы `TaskExecutionServiceGroup` (`ScheduleTaskInstanceService.checkDomain(...)`); несовпадение → `CONF_ERROR` без отправки в Kafka (см. фичу «Домены и репозитории»).
- **REST API**: расписания (список / по имени+limit / DAG / удаление), задачи, замки, lock/heartbeat/release; Java-клиент `SchedulerRestClientApi`.
- **Безопасность и аудит**: Keycloak OAuth2/JWT, `client_credentials` для сервис-сервисных вызовов, аудит-логирование.

---

## Модуль: yarina-state-svc — сервис состояний интервалов датасетов
*Путь к исходному файлу: `./yarina-state-svc/doc/readme.md`, `./yarina-state-svc/doc/restapi.md`*

### Реализованные фичи:
- **Хранение состояний интервалов**: покрытие временного ряда датасета интервалами со статусами LOCKED/SUCCESS.
- **Защита от конфликтов**: запись отклоняется, если новый `lockSource` не совпадает с записанным для незакрытых интервалов (`LockedStateRuntimeException`).
- **Поиск «пробелов»**: интервалы без SUCCESS (не обработаны/LOCKED) в заданном окне — сигнал для запуска задач; при отсутствии состояний возвращается весь запрошенный интервал.
- **Компактная запись состояния**: пересекающиеся интервалы ужимаются/разбиваются при записи (без пересечений), дубликаты исключаются уникальным ограничением.
- **REST API**: POST `/v1_0/state/dataset/wrong` (поиск пробелов), PUT `/v1_0/state/dataset` (запись состояния), GET `/v1_0/state/dataset` (вывод состояний).
- **Java REST-клиент** `StateRestClientApi` для scheduler-svc и task-executor-svc.
- **Безопасность и аудит**: Keycloak OAuth2/JWT, `client_credentials`, аудит-логирование.

---

## Модуль: yarina-task-executor-svc — исполнитель задач
*Путь к исходному файлу: `./yarina-task-executor-svc/doc/readme.md`, `./yarina-task-executor-svc/doc/processors.md`, `./yarina-task-executor-svc/doc/properties.md`, `./yarina-task-executor-svc/doc/scaling.md`*

### Реализованные фичи:
- **Приём и исполнение задач**: потребление из Kafka (`scheduled_task_msg`), без собственного знания о том, какие задачи когда выполняются.
- **Фильтрация по группе исполнения**: инстанс берёт только задачи с совпадающими `taskExecutionServiceGroupName` и `group.id` консьюмера.
- **Доменные настройки источников**: `yarina.task-executor.domains.<domainKeyName>.<dataSourceKeyName>.service-properties` перекрывают (`putAll`) `service.properties` из config-svc для конкретного домена и доходят до резолвера секретов (см. фичу «Домены и репозитории»).
- **Блокировка и живость**: блокировка задачи в scheduler-svc (получение слитой конфигурации шаблона и задачи), фоновый heartbeat, возврат результата (SUCCESS/FAILED/CONF_ERROR).
- **Параллелизм**: ограничение concurrency консьюмера и пулом потоков.
- **Три класса TaskProcessor**:
  - **Spark** (`sparkStandAloneClusterTaskProcessor`): запуск тела задачи на удалённом standalone-кластере через REST `/v1/submissions`, параметризация sparkConf (приоритет: catalog-зависимости → целевой datasource → taskProcessorArgs), ожидание RUNNING и опрос до финального статуса, аварийное прерывание при потере driver-записи.
  - **State-model**: `LockedStateTaskProcessor` (LOCKED — блокировка), `SuccessStateTaskProcessor` (SUCCESS — разрешение), `DependencyCheckStateTaskProcessor` (проверка зависимостей и текущего статуса).
  - **JDBC** (`JdbcTaskProcessor`): выполнение тела задачи через JDBC-драйвер (синтаксис БД — зона sqlTemplate), соединение с рантайм-резолвингом пароля из секрет-стора.
- **SQL-тела задач на шаблонах** (переиспользуются и в Spark, и в JDBC): `AppendSQLProcessorBody`, `MergeSQLProcessorBody`, `CreateTableSQLProcessorBody`, `CompactTableSQLProcessorBody`; DQ-тело `SparkTaskProcessorDQBody` — только Spark.
- **Библиотека контрактов** `yarina-task-executor-api`: интерфейс `TaskProcessor`, тела задач, абстракции источников данных, SQLTemplate-резолвер.
- **Масштабирование**: вертикальное (concurrency), горизонтальное (несколько инстансов через Kafka group), сегментное (разделение групп исполнения по типу задач — state/миллисекунды vs Spark/минуты).
- **Секреты в Spark-задачах**: URL без учётных данных, драйвер на защищённом `YarinaSecureJDBCTableCatalog`, пароли резолвятся на Driver/Executors, опции безопасности вырезаются перед передачей в JDBC.
- **REST-клиент управления** `yarina-task-executor-rest-client`; health `/healthz`/`/readyz`; безопасность и аудит как у остальных сервисов.

---

## Модуль: yarina-task-executor-spark-api / yarina-task-executor-spark-dataset-app — контракты и Spark-приложение
*Путь к исходному файлу: `./yarina-task-executor-spark-api/doc/readme.md`, `./yarina-task-executor-spark-dataset-app/README.md`*

### Реализованные фичи:
- **Выделение распределённой подгруппы задач**: сущность «тело задачи» (ProcessBody) — логика исполняется на стороне кластера через собственные сущности.
- **BodyStarter**: подбирает и конфигурирует требуемый ProcessBody для исполнения.
- **ProcessBody**: переиспользуемая логика задачи, выносится за пределы конкретного TaskProcessor.
- *(README spark-dataset-app пуст — функциональное описание отсутствует.)*

---

## Модуль: yarina-task-proxy-for-spark — прокси Spark-сабмитов
*Путь к исходному файлу: `./yarina-task-proxy-for-spark/doc/readme.md`*

### Реализованные фичи:
- **REST-прокси для Spark Submit**: приём задач в формате Spark Standalone REST API (`/v1/submissions/create`).
- **Очередь в PostgreSQL** с конкурентным потреблением нескольких инстансов (`FOR UPDATE SKIP LOCKED`).
- **Адаптерный паттерн кластеров**: Standalone, Kubernetes, YARN, MESOS (заглушка); адаптер выбирается при старте.
- **Двойная модель ID**: внешний `submissionId` (для клиента) и внутренний `realSubmissionId` (реальный driver ID из вывода spark-submit).
- **Три параллельных планировщика**: SubmissionScheduler (раздача spark-submit на Virtual Threads), ClusterStatusScheduler (синхронизация статусов с кластером), CleanupScheduler (очистка завершённых, включая удаление driver-pod в k8s).
- **Управление задачами**: create, status, kill, killall, clear.
- **Маппинг статусов**: стандартная модель Spark REST (WAITING/RUNNING/FINISHED/FAILED/KILLED/UNKNOWN) из состояния кластера (pod phase, YARN state, driver state).
- **Prometheus-метрики**: нагрузка сабмитов, результаты, длительность p50/p95/p99.
- **Observability/безопасность**: Actuator, OAuth2/OIDC с Keycloak, аудит-логирование.

---

## Модуль: yarina-credential-providers — безопасный резолвинг секретов (JDBC + Spark)
*Путь к исходному файлу: `./yarina-credential-providers-spark/doc/README.md`, `./doc/security/security.md`*

### Реализованные фичи:
- **Динамическое получение секретов в рантайме** (Spark 3.5.x, на Driver и Executors, без Spring): пароли JDBC и S3-ключи вместо хардкода в `spark-defaults.conf`.
- **Провайдеры**: OpenBao/HashiCorp Vault (токен `VAULT_TOKEN` или Kubernetes ServiceAccount JWT) и Yandex Cloud Lockbox (Instance Metadata или файл authorized key).
- **Защита от утечек**: секреты никогда не пишутся в логи, поддержка redaction Spark UI.
- **YarinaSecureJDBCTableCatalog**: защищённая замена Spark `JDBCTableCatalog` (опции секретности вырезаются до передачи в базовый каталог; поддержка S3A).
- **S3-провайдеры**: `BaoS3CredentialsProvider`, `YcLockboxS3CredentialsProvider` для Hadoop S3A.
- **Кэш секретов**: in-memory с TTL 5 минут на JVM — защита секрет-API при тысячах партиций.
- **YarinaSecurityContext**: статический контекст для получения секретов внутри распределённых замыканий (foreachPartition и т.п.), клиент ленивый по JVM и не сериализуется.
- **Использование вне Spark**: тот же контракт опций в `JdbcConnectionFactory` (`yarina-task-executor-api`) для не-Spark JDBC-подключений; обратная совместимость без провайдера.

---

## Модуль: yarina-ui-svc — веб-консоль
*Путь к исходному файлу: `./yarina-ui-svc/doc/readme.md`, `./yarina-ui-svc/doc/arch/architecture.md`*

### Реализованные фичи:
- **Единая веб-консоль управления**: тонкий BFF без собственного состояния (БД), агрегирует данные всех сервисов; также хостит поверхность **Modelling** — редактор конфигурационных документов (см. модуль ниже).
- **Services**: граф сервисов и их статусы UP/DOWN (HTTP/TCP-пробы).
- **Catalog**: дерево каталога данных (источники → схемы → датасеты), просмотр датасета (модель/DDL, lineage, ограничения) и источника.
- **Schedules**: инстансы запусков расписаний за интервал и DAG экземпляра расписания.
- **SparkJobs**: управление сабмитами через task-proxy-for-spark (create/status/kill/killall/clear, просмотр spark-свойств).
- **Состояния интервалов датасетов**: просмотр через state-svc.
- **Аутентификация**: Keycloak OAuth2 authorization code flow, роли USER/ADMIN, CSRF-защита (XSRF-TOKEN → X-XSRF-TOKEN), сервисная страница логина, whitelist `/healthz`/`/readyz`/`/actuator/**`.
- **VCSLog**: панель «VCS» — история синхронизации конфигураций (SUCCESS/FAILED с ошибками) и журнал затронутых объектов из config-svc.
- **Мониторинговые контроллеры**: `CatalogController`, `ScheduleController`, `ServicesController`, `SparkProxyController`, `StateController`, `VcsLogController`, `UserController` (`/api/…`), статусы через `HealthChecker` (HTTP/TCP-пробы, граф из `yarina.ui.services/vertices/edges`).
- **SPA-фронтенд**: React/Vite, собранный в ресурсы сервиса (`src/main/resources/static`); содержит визуальные редакторы модделирования (`ErDiagramEditor`, `DataLineageDiagramEditor`, `DagEditor` — React Flow) и Vitest-набор unit-тестов (см. модуль Modelling).

---

## Модуль: yarina-ui-svc — Modelling (metadata-modelling workbench)
*Путь к исходному файлу: `./yarina-ui-svc/doc/readme.md`, `./yarina-ui-svc/doc/arch/architecture.md`*

### Реализованные фичи:
- **Рабочее место по модделированию**: создание рабочего пространства (workspace) из ветки центрального Git-репозитория, создание и редактирование конфигурационных документов любого поддерживаемого вида (форма по схеме / «сырой» YAML / визуальные редакторы), создание веток, отправка изменений на ревью; открытие по deep-ссылке `?section=modeller&workspace=<id>`.
- **Хранилище workspace'ов**: `WorkspaceStorage` SPI — `LocalFsWorkspaceStorage` (локальная ФС, `yarina.modeller.storage.root-directory`) и `S3WorkspaceStorage` (S3 + собственный минимальный AWS SigV4-подпись); `WorkspaceManager`, `WorkspaceSeeder`, фоновый `WorkspaceCleanupTask` (TTL неактивности, по умолчанию 4 ч); сессия неактивности `yarina.modeller.session.inactivity-minutes`.
- **Интеграция с Git**: единый `VcsClient` SPI с единственной реализацией `GitVcsClient` (JGit) — ветки, создание ветки, чтение файлов ветки и push на ревью; учётные данные — только по-доменные (`yarina.domains.<name>.git.credentials.*`, auth `ssh`/`token`/`basic`, без глобального аккаунта и без слияния), анонимный доступ, если блок пуст; центральный Git-репозиторий как источник истины, потребляемый GitOps в `yarina-config-svc`.
- **Домены и репозитории**: `yarina.domains.<name>.{git.repository-url, git.branch, git.credentials.*, priority, domains}` из общего модуля `yarina-domain-api` — по репозиторию на домен, панель веток как плоский список доменов, workspace как набор пар `(domain, branch)`, ревью по доменам, `domainKeyName` не редактируется в формах (см. фичу «Домены и репозитории»).
- **Файловый CRUD в workspace**: `EditorController` — дерево/каталоги (`/api/workspaces/{id}/tree`, dirs), создание документа по `kind`+`keyName`+`directory`, чтение (YAML + kind + флаг редактируемости), сохранение через `YamlEditorService` (PUT `yaml`/`keyName`), rename/move/delete файлов и каталогов, restore из VCS-состояния (`WorkspaceSeeder`/`ReviewService`).
- **Форма по схеме вида**: `SchemaController` отдаёт `KindSchema`/`FieldSchema` для всех видов (`/api/schema`), `EnumOptionsService`; `FormEditor` рендерит форму по схеме, переключатель форма ↔ «сырой» YAML (jackson-dataformat-yaml) с подсветкой синтаксиса (`CodeEditor`).
- **Визуальные редакторы (React Flow)**: ER-диаграммы (`kind: ERDiagram`, каталог `erdiagrams`), диаграмма lineage (`kind: DataLineageDiagram`, каталог `datalineagediagrams` — узлы DataSet, рёбра из `sources`, отсутствующие узлы пунктиром) и универсальный DAG-редактор; на канвасе диаграмм — тулбар Add/Edit/Remove, модалка добавления узла через picker, подтверждение удаления, встроенное редактирование (форма/YAML) по кнопке Edit, переключатель fullscreen. Виды `ERDiagram`/`DataLineageDiagram` — не конфигурационные (`isConfig=false`): хранятся в репозитории, но не применяются config-svc.
- **Ревью и восстановление**: сабмит workspace'а на ревью (коммит + комментарий, `POST /api/vcs/review/{workspaceId}`), restore файла/каталога из состояния VCS.
- **RBAC модделирования (domain-driven)**: роли Keycloak realm `<domain>:<ROLE>` (`VIEWER`/`EDITOR`/`CONTROL`; `VIEWER` неявно входит в две другие, `EDITOR`/`CONTROL` ортогональны) плюс глобальная `WORKSPACE_ADMIN`; владение workspace'ом (правит только владелец); `readOnly` выводится по домену из `domains` (`GET /api/user`), а не из глобальной роли; admin-поверхность (`AdminController`, `/api/workspace/admin/**`) — все workspace'ы, force-delete, TTL очистки, «хвост» sync-логов.
- **Kinds в shared enum `YamlMetadataKind`**: `ER_DIAGRAM` (order 11) и `DATA_LINEAGE_DIAGRAM` (order 13), идентификатор `keyName`, директории `erdiagrams`/`datalineagediagrams`.
- **Тесты фронтенда**: Vitest 3 + React Testing Library (jsdom), настроены в `vite.config.js` (`test/setup.js` — полифиллы React Flow); 44 теста: `DataLineageDiagramEditor` (24 — граф↔документ, позиции узлов, drag, Add/Edit/Remove + модалки, fullscreen), `FormEditor` (7), `EditorView` (6, вкл. multi-domain workspace leave-on-review и переключение `readOnly` по домену), `WorkspacePicker` (7, вкл. preselect ветки домена и включение/выключение New branch по `EDITOR`).

---

## Модуль: Безопасность платформы
*Путь к исходному файлу: `./doc/security/security.md`*

### Реализованные фичи:
- **Единый IdP (Keycloak)**: единый realm `yarina`, OAuth2/OIDC, JWT; доменные роли `<domain>:VIEWER`/`EDITOR`/`CONTROL` и глобальная `WORKSPACE_ADMIN`; два клиента — ui (интерактивный) и internal (сервисные аккаунты).
- **Безопасность сервис-к-сервису**: 5 бэкенд-сервисов как OAuth2 Resource Server (проверка JWT по JWKS); ретрансляция JWT пользователя или `client_credentials` для фоновых обработок; whitelist для health/actuator/OpenAPI; возможность отключения безопасности.
- **UI/BFF**: authorization code flow, HTTP-сессия (JSESSIONID), CSRF (XSRF-TOKEN).
- **Авторизация Spark-приложений**: драйверы аутентифицируются по `client_credentials` для обратных вызовов; секреты/токены не передаются аргументами задач; маскирование секретов в логах Spark (redaction).
- **Секреты подключений**: пароли БД и ключи S3 не хранятся в конфигурации — резолвинг в рантайме из OpenBao/Vault или Yandex Cloud Lockbox; безопасный `JDBCTableCatalog`; кэш TTL 5 мин; обратная совместимость.
- **Аудит**: `AuditLoggingFilter` — структурированная JSON-запись на каждый входящий запрос (User ID, Username, Method, URI, статус); сервисные аккаунты логируются как `system`, но инициированные пользователем действия BFF пишет под реальным Keycloak-username (запись действий человека как `system` запрещена).

---

*Составлено на основе Markdown-документов проекта (EN-версии; `doc-ru/` — зеркала, `node_modules/`, `target/` исключены, всего EN-файлов: 58, включая зеркала: 96). Реестр отражён на версию **0.11.0** (после tags **0.9.0** — credential providers, **0.10.0** — VCS/GitOps; актуальное состояние — поверхность Modelling в UI и фича «Домены и репозитории»: конфигурационный домен = Git-репозиторий, сквозная `domainKeyName`-разметка в config-svc / ui-svc / scheduler-svc / task-executor-svc).*