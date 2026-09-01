# Реестр функциональных возможностей проекта (на основе документации)

Реестр составлен по всем Markdown-документам репозитория (за исключением зеркал `doc-ru/`, `node_modules/` и чисто сборочных инструкций). Для каждой сущности/сервиса указаны реализованные фичи и источник — файл(ы) документации.

---

## Модуль: Платформа lakehouse в целом (metadata-driven управление данными)
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
- **Хранение конфигураций**: config-svc хранит источники данных, датасеты, расписания, сценарии, драйверы; публикует изменения расписаний в Kafka (`schedule_effective_changes`).
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

## Модуль: lakehouse-config-svc — сервис управления конфигурациями (source of truth)
*Путь к исходному файлу: `./lakehouse-config-svc/doc/readme.md`, `./lakehouse-config-svc/doc/content_configuration/*.md`*

### Реализованные фичи:
- **Единый источник истины метаданных**: CRUD-хранилище всех конфигураций в PostgreSQL (схема `lakehouse_config`) с REST API `/v1_0/configs`; на его основе работают scheduler-svc, task-executor-svc, state-svc.
- **Публикация изменений расписаний в Kafka**: топик `schedule_effective_changes` (с задержками/лимитами и первоначальной рассылкой при старте).
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
- **Безопасность и аудит**: Keycloak OAuth2/JWT (resource server), роли → `ROLE_<NAME>` для `@PreAuthorize`, сервис-сервисные вызовы по `client_credentials`, `AuditLoggingFilter` (JSON в `AUDIT_LOG`), health `/healthz`/`/readyz`.

---

## Модуль: lakehouse-config-svc — VCS (Configuration Versioning System / GitOps)
*Путь к исходному файлу: `./lakehouse-config-svc/doc/vcs/vcs_for_developers.md`, `./lakehouse-config-svc/doc/vcs/git_extension_user_guide.md`*

### Реализованные фичи:
- **Git как source of truth**: тот же набор DTO, что и REST API, записывается Kubernetes-style YAML/JSON файлами; подсистема периодически синхронизирует их в БД конфигураций (конфигурация-как-код, GitOps).
- **Полная история изменений** каждой конфигурации (git-коммиты), декларативная проверяемая конфигурация, атомарное применение целого коммита.
- **GitOpsScheduler**: периодический цикл pull → diff против последнего успешного коммита → sync в одной транзакции; метод `sync()` synchronized и идемпотентен.
- **Атомарность и журнал**: применение валидация→apply→unmanage → запись `vcs_object_log`; только при полном успехе коммита пишется `SUCCESS` в `vcs_sync_log`; ошибки — `FAILED` (отдельная `REQUIRES_NEW`-транзакция), инфраструктурные сбои — повтор на следующем цикле.
- **Абстракция `VcsClient`**: `init/pull/getCurrentCommitId/getDiff/readFileContent`; реализация `GitVcsClient` на JGit (SSH publickey, rename = delete+create, пустой diff трактует всю ветку как созданную); смена транспорта без правок остального конвейера.
- **`GitOpsYamlParser`/`ConfigKind`**: обязательный `kind` (регистр/разделители нечувствительны), регистро-независимые enum, неизвестные свойства — жёсткая ошибка; 10 видов конфигураций с порядком применения.
- **`isVcsManaged`**: метка владения конструктива VCS; REST создание/изменение/удаление управляемого — HTTP `409 Conflict`; удаление файла из репозитория лишь снимает метку (двухшаговое удаление); трёхсторонний контракт `save/saveVcs/unmanage` в сервисах.
- **Параметры**: `lakehouse.config.vcs.*` (repository-url, branch, local-clone-path, private-key-path, sync.enabled/interval/initial-delay).
- **Read-only REST**: `GET /v1_0/configs/vcs/logs` (история синхронизации) и `GET /v1_0/configs/vcs/objectlogs` (журнал затронутых объектов), потребляются UI.
- **Тесты**: `GitOpsIntegrationTest`, `GitVcsClientTest`, `GitOpsChangeSetBuilderTest`, `GitOpsSchedulerUnitTest`, `GitOpsYamlParserTest`, `TestGitRepository`.

---

## Модуль: lakehouse-scheduler-svc — планировщик
*Путь к исходному файлу: `./lakehouse-scheduler-svc/doc/readme.md`, `./lakehouse-scheduler-svc/doc/scheduling/Scheduling.md`, `./lakehouse-scheduler-svc/doc/restapi.md`*

### Реализованные фичи:
- **Регистрация расписаний**: потребление изменений из Kafka (`schedule_effective_changes`) и построение экземпляров расписаний по `intervalExpression`.
- **Жизненный цикл расписания**: статусы NEW → RUNNING → SUCCESS/FAILED; выключение `enabled` останавливает и создание новых, и обработку статусов запущенных.
- **Сценарии и задачи**: создание экземпляров сценариев/задач, трекинг статуса каждого элемента; параметризация границ интервалов (Jinjava-выражения).
- **Разрешение зависимостей**: направленные графы `scenarioActEdges` и `dagEdges` — задача/сценарий завершаются только после успеха всех зависимостей; некритичные сбои не блокируют последующие.
- **Очередь задач**: передача задач исполнителям через Kafka (топик `scheduled_task_msg`).
- **Замки (locks)**: взятие задачи исполнителем, продление heartbeat, release с результатом; защита от повторного исполнения.
- **Ретри**: автоматический повтор неуспешных задач (задержки `lag-when-failed`/`lag-when-config-failed`, лимит `maxRetries`).
- **Внутренний планировщик**: периодические слоты registration (build) / run / resolvedeps / task.retry.
- **REST API**: расписания (список / по имени+limit / DAG / удаление), задачи, замки, lock/heartbeat/release; Java-клиент `SchedulerRestClientApi`.
- **Безопасность и аудит**: Keycloak OAuth2/JWT, `client_credentials` для сервис-сервисных вызовов, аудит-логирование.

---

## Модуль: lakehouse-state-svc — сервис состояний интервалов датасетов
*Путь к исходному файлу: `./lakehouse-state-svc/doc/readme.md`, `./lakehouse-state-svc/doc/restapi.md`*

### Реализованные фичи:
- **Хранение состояний интервалов**: покрытие временного ряда датасета интервалами со статусами LOCKED/SUCCESS.
- **Защита от конфликтов**: запись отклоняется, если новый `lockSource` не совпадает с записанным для незакрытых интервалов (`LockedStateRuntimeException`).
- **Поиск «пробелов»**: интервалы без SUCCESS (не обработаны/LOCKED) в заданном окне — сигнал для запуска задач; при отсутствии состояний возвращается весь запрошенный интервал.
- **Компактная запись состояния**: пересекающиеся интервалы ужимаются/разбиваются при записи (без пересечений), дубликаты исключаются уникальным ограничением.
- **REST API**: POST `/v1_0/state/dataset/wrong` (поиск пробелов), PUT `/v1_0/state/dataset` (запись состояния), GET `/v1_0/state/dataset` (вывод состояний).
- **Java REST-клиент** `StateRestClientApi` для scheduler-svc и task-executor-svc.
- **Безопасность и аудит**: Keycloak OAuth2/JWT, `client_credentials`, аудит-логирование.

---

## Модуль: lakehouse-task-executor-svc — исполнитель задач
*Путь к исходному файлу: `./lakehouse-task-executor-svc/doc/readme.md`, `./lakehouse-task-executor-svc/doc/processors.md`, `./lakehouse-task-executor-svc/doc/properties.md`, `./lakehouse-task-executor-svc/doc/scaling.md`*

### Реализованные фичи:
- **Приём и исполнение задач**: потребление из Kafka (`scheduled_task_msg`), без собственного знания о том, какие задачи когда выполняются.
- **Фильтрация по группе исполнения**: инстанс берёт только задачи с совпадающими `taskExecutionServiceGroupName` и `group.id` консьюмера.
- **Блокировка и живость**: блокировка задачи в scheduler-svc (получение слитой конфигурации шаблона и задачи), фоновый heartbeat, возврат результата (SUCCESS/FAILED/CONF_ERROR).
- **Параллелизм**: ограничение concurrency консьюмера и пулом потоков.
- **Три класса TaskProcessor**:
  - **Spark** (`sparkStandAloneClusterTaskProcessor`): запуск тела задачи на удалённом standalone-кластере через REST `/v1/submissions`, параметризация sparkConf (приоритет: catalog-зависимости → целевой datasource → taskProcessorArgs), ожидание RUNNING и опрос до финального статуса, аварийное прерывание при потере driver-записи.
  - **State-model**: `LockedStateTaskProcessor` (LOCKED — блокировка), `SuccessStateTaskProcessor` (SUCCESS — разрешение), `DependencyCheckStateTaskProcessor` (проверка зависимостей и текущего статуса).
  - **JDBC** (`JdbcTaskProcessor`): выполнение тела задачи через JDBC-драйвер (синтаксис БД — зона sqlTemplate), соединение с рантайм-резолвингом пароля из секрет-стора.
- **SQL-тела задач на шаблонах** (переиспользуются и в Spark, и в JDBC): `AppendSQLProcessorBody`, `MergeSQLProcessorBody`, `CreateTableSQLProcessorBody`, `CompactTableSQLProcessorBody`; DQ-тело `SparkTaskProcessorDQBody` — только Spark.
- **Библиотека контрактов** `lakehouse-task-executor-api`: интерфейс `TaskProcessor`, тела задач, абстракции источников данных, SQLTemplate-резолвер.
- **Масштабирование**: вертикальное (concurrency), горизонтальное (несколько инстансов через Kafka group), сегментное (разделение групп исполнения по типу задач — state/миллисекунды vs Spark/минуты).
- **Секреты в Spark-задачах**: URL без учётных данных, драйвер на защищённом `LakehouseSecureJDBCTableCatalog`, пароли резолвятся на Driver/Executors, опции безопасности вырезаются перед передачей в JDBC.
- **REST-клиент управления** `lakehouse-task-executor-rest-client`; health `/healthz`/`/readyz`; безопасность и аудит как у остальных сервисов.

---

## Модуль: lakehouse-task-executor-spark-api / lakehouse-task-executor-spark-dataset-app — контракты и Spark-приложение
*Путь к исходному файлу: `./lakehouse-task-executor-spark-api/doc/readme.md`, `./lakehouse-task-executor-spark-dataset-app/README.md`*

### Реализованные фичи:
- **Выделение распределённой подгруппы задач**: сущность «тело задачи» (ProcessBody) — логика исполняется на стороне кластера через собственные сущности.
- **BodyStarter**: подбирает и конфигурирует требуемый ProcessBody для исполнения.
- **ProcessBody**: переиспользуемая логика задачи, выносится за пределы конкретного TaskProcessor.
- *(README spark-dataset-app пуст — функциональное описание отсутствует.)*

---

## Модуль: lakehouse-task-proxy-for-spark — прокси Spark-сабмитов
*Путь к исходному файлу: `./lakehouse-task-proxy-for-spark/doc/readme.md`*

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

## Модуль: lakehouse-credential-providers — безопасный резолвинг секретов (JDBC + Spark)
*Путь к исходному файлу: `./lakehouse-credential-providers-spark/doc/README.md`, `./doc/security/security.md`*

### Реализованные фичи:
- **Динамическое получение секретов в рантайме** (Spark 3.5.x, на Driver и Executors, без Spring): пароли JDBC и S3-ключи вместо хардкода в `spark-defaults.conf`.
- **Провайдеры**: OpenBao/HashiCorp Vault (токен `VAULT_TOKEN` или Kubernetes ServiceAccount JWT) и Yandex Cloud Lockbox (Instance Metadata или файл authorized key).
- **Защита от утечек**: секреты никогда не пишутся в логи, поддержка redaction Spark UI.
- **LakehouseSecureJDBCTableCatalog**: защищённая замена Spark `JDBCTableCatalog` (опции секретности вырезаются до передачи в базовый каталог; поддержка S3A).
- **S3-провайдеры**: `BaoS3CredentialsProvider`, `YcLockboxS3CredentialsProvider` для Hadoop S3A.
- **Кэш секретов**: in-memory с TTL 5 минут на JVM — защита секрет-API при тысячах партиций.
- **LakehouseSecurityContext**: статический контекст для получения секретов внутри распределённых замыканий (foreachPartition и т.п.), клиент ленивый по JVM и не сериализуется.
- **Использование вне Spark**: тот же контракт опций в `JdbcConnectionFactory` (`lakehouse-task-executor-api`) для не-Spark JDBC-подключений; обратная совместимость без провайдера.

---

## Модуль: lakehouse-ui-svc — веб-консоль
*Путь к исходному файлу: `./lakehouse-ui-svc/doc/readme.md`*

### Реализованные фичи:
- **Единая веб-консоль управления**: тонкий BFF без собственного состояния (БД), агрегирует данные всех сервисов.
- **Services**: граф сервисов и их статусы UP/DOWN (HTTP/TCP-пробы).
- **Catalog**: дерево каталога данных (источники → схемы → датасеты), просмотр датасета (модель/DDL, lineage, ограничения) и источника.
- **Schedules**: инстансы запусков расписаний за интервал и DAG экземпляра расписания.
- **SparkJobs**: управление сабмитами через task-proxy-for-spark (create/status/kill/killall/clear, просмотр spark-свойств).
- **Состояния интервалов датасетов**: просмотр через state-svc.
- **Аутентификация**: Keycloak OAuth2 authorization code flow, роли USER/ADMIN, CSRF-защита, сервисная страница логина.
- **VCSLog**: панель «VCS» — история синхронизации конфигураций (SUCCESS/FAILED с ошибками) и журнал затронутых объектов из config-svc.
- **SPA-фронтенд**: React/Vite, собранный в ресурсы сервиса.

---

## Модуль: Безопасность платформы
*Путь к исходному файлу: `./doc/security/security.md`*

### Реализованные фичи:
- **Единый IdP (Keycloak)**: единый realm `lakehouse`, OAuth2/OIDC, JWT; роли `USER`/`ADMIN`; два клиента — ui (интерактивный) и internal (сервисные аккаунты).
- **Безопасность сервис-к-сервису**: 5 бэкенд-сервисов как OAuth2 Resource Server (проверка JWT по JWKS); ретрансляция JWT пользователя или `client_credentials` для фоновых обработок; whitelist для health/actuator/OpenAPI; возможность отключения безопасности.
- **UI/BFF**: authorization code flow, HTTP-сессия (JSESSIONID), CSRF (XSRF-TOKEN).
- **Авторизация Spark-приложений**: драйверы аутентифицируются по `client_credentials` для обратных вызовов; секреты/токены не передаются аргументами задач; маскирование секретов в логах Spark (redaction).
- **Секреты подключений**: пароли БД и ключи S3 не хранятся в конфигурации — резолвинг в рантайме из OpenBao/Vault или Yandex Cloud Lockbox; безопасный `JDBCTableCatalog`; кэш TTL 5 мин; обратная совместимость.
- **Аудит**: `AuditLoggingFilter` — структурированная JSON-запись на каждый входящий запрос (User ID, Username, Method, URI, статус); сервисные аккаунты логируются как `system`.

---

*Составлено на основе Markdown-документов проекта (EN-версии; `doc-ru/` — зеркала, `node_modules/`, `target/` исключены, всего EN-файлов: 55, включая зеркала: 93).*