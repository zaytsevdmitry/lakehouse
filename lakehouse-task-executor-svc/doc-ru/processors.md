# Взаимодействие с сервисом расписаний
![ServiceWorkSequence.png](uml/ServiceWorkSequence.png)

## Процесс конфигурирования
Процесс конфигурирования основан на заполнении двух важных историй
- Источники данных
  - [drivers](../../lakehouse-config-svc/doc-ru/content_configuration/drivers.md)
    - [datasources](../../lakehouse-config-svc/doc-ru/content_configuration/datasources.md)
      - [datasets](../../lakehouse-config-svc/doc-ru/content_configuration/datasets.md)
- Расписания
  - [scenarioActTemplate](../../lakehouse-config-svc/doc-ru/content_configuration/scenarioActTemplate.md)
    - [schedules.md](../../lakehouse-config-svc/doc-ru/content_configuration/schedules.md)

Эти конфигурации должны быть заполнены и переданы в сервис config-svc.
Для работы конкретной задачи исполнителем которой является объект *TaskProcessor конфигурация источника (назначения) обогащается в объект [sources.md](../../lakehouse-config-svc/doc-ru/content_configuration/sources.md).
Производится это на стороне сервиса конфигурации, любой разрабатываемый *TaskProcessor *TaskProcessorBody может получить его по ключевому имени датасета.

Сервис конфигурации получив конфигурацию расписания публикует его в kafka topic, а сервис расписаний прослушивает его и формирует расписания.
Идентификаторы задач готовых к выполнению отправляются в kafka topic, который прослушивают экземпляры task-executor-svc.
Получив идентификатор один task-executor-svc блокирует задачу в scheduler-svc, получая в ответ полное описание задачи. 
Это слитый вариант задачи состоящий из объединения назначенного шаблона задачи и перегруженных значений указанных для конкретной задачи.

Таким образом для работы сервиса task-executor-svc требуется наличие config-svc и scheduler-svc.
Временное отсутствие одного из них приведет к аварии задачи. 
scheduler-svc должен будет повторить задачу, это сглаживает проблему перезапуска экземпляров.

# Параметризация задачи
## параметр taskProcessor
То что исполняет задачу.
![TaskProcessors.png](uml/TaskProcessors.png)


> Имя процессора указывается с буквенного символа в нижнем регистре в camelCase


###  [Spark-задачи](../../lakehouse-task-executor-spark-api/doc/readme.md)
  * sparkStandAloneClusterTaskProcessor [SparkStandAloneClusterTaskProcessor.java](../src/main/java/org/lakehouse/taskexecutor/processor/spark/SparkStandAloneClusterTaskProcessor.java) запускает тело задачи на удаленном spark standalone кластере в виде spark-job через REST API `/v1/submissions`.

**Архитектура Spark-процессоров:**

```
SparkStandAloneClusterTaskProcessor
  └─ extends AbstractTaskProcessor
       └─ deploy(mainClass, appResource, serverUrl, sparkProperties, appArgs) — приватный метод процессора, логика деплоя через Spark REST API
```

- в ранних версиях логика деплоя жила в отдельном `AbstractSparkDeployTaskProcessor`
  (с `SparkRestDeployFactory` для формирования URL); позже она была инлайнирована в
  [SparkStandAloneClusterTaskProcessor.java](../src/main/java/org/lakehouse/taskexecutor/processor/spark/SparkStandAloneClusterTaskProcessor.java)
- адрес кластера берётся из аргумента `deploy.clusterUrl` в `taskProcessorArgs`
  (`getMasterUrl()`); он уже содержит полный путь REST, например `http://host:port/v1/submissions`,
  и используется как базовый URI для `SparkRestClientApi` (`buildSparkRestClientApi(baseURI)` на `RestClient`)
- приложение-драйвер параметризуется ключами `deploy.mainClass` и `deploy.appResource`
  (разрешаются в приоритете: taskProcessorArgs → datasource service.properties, через `Coalesce.apply`)
- `deploy(...)` отправляет POST `/v1/submissions/create`, ожидает перехода в `RUNNING`
  (таймаут `maxWaitToRunningStateTimeoutMs`, по умолчанию 120000 мс), опрашивает статус каждые
  `sparkJobStatusCheckIntervalMs` (по умолчанию 3000 мс) до финального статуса
  (`FINISHED`/`KILLED`/`FAILED`/`ERROR`) и падает на негативных статусах
  (`KILLED`, `FAILED`, `ERROR`)
- превышение лимита `DRIVER_STATE_NULL_LIMIT` (30) опросов с `driverState` null/`UNKNOWN` после `RUNNING`
  прерывает задачу, если запись о драйвере исчезла у мастера

Ничего не знают о логике задачи. Ответственность — разбор конфигурации, чтобы параметризовать spark-driver в конкретном кластере.
Не работают с локальным запуском драйвера, т.к. это сильно утяжелит сам сервис и размоет границы его ответственности.

Конфигурация разделяется на три разных типа:

- **sparkConf**
  - Производится обход параметров всех datasource, которые выступают в роли зависимости. Фильтруется вложенный объект `service.properties`. Отбираются все параметры, начинающиеся на `spark.sql.catalog`.
  - Производится обход параметров целевого datasource. Фильтруется вложенный объект `service.properties`. Отбираются все параметры, начинающиеся на `spark.`. Отобранные параметры перезапишут полученные выше, если встретятся одинаковые ключи.
  - Производится обход `taskProcessorArgs`. Отбираются все параметры, начинающиеся на `spark.`. Отобранные параметры перезапишут полученные выше, если встретятся одинаковые ключи.

  > Почему порядок именно такой: datasource заполняется один и на множество датасетов, поэтому содержит наиболее обобщённые параметры. Его удобно использовать для параметров по умолчанию. taskProcessorArgs могут быть разными в задачах, обслуживающих одну и ту же таблицу (dataset), поэтому в зависимости от конкретной операции могут меняться значения и состав параметров. Например, для одной операции требуется больше памяти, а для другой — больше ядер.

- **Чистые атрибуты приложения**
  - Берутся `taskProcessorArgs` и отбрасываются все параметры, начинающиеся на `spark.`, т.к. их передача избыточна для сети.

- **Манифест k8s** (не реализован, зарезервировано)
  - Предполагается обход параметров целевого datasource с фильтром `k8s.spark-operator` и обход `taskProcessorArgs` с тем же фильтром.

### Порядок извлечения sparkConf в SparkStandAloneClusterTaskProcessor.runTask()

1. **`SparkConfUtil.extractSparkConFromTaskConf(sourceConfDTO, scheduledTaskDTO)`** — сбор и слияние spark-свойств в три прохода (приоритет — от низшего к высшему):
   1. `spark.sql.catalog.*` из всех datasource-зависимостей (кроме целевого)
   2. Все `spark.*` из целевого datasource
   3. Все `spark.*` из `taskProcessorArgs` задачи
2. **`SparkConfUtil.unSparkConf(scheduledTaskDTO)`** — удаление spark-ключей из `taskProcessorArgs` перед отправкой в spark-driver
3. Разрешение `mainClass` и `appResource`: taskProcessorArgs → datasource service.properties (через `Coalesce.apply`)
4. Формирование `appArgs`: unSparkedTaskConfig + `scheduledTaskId`, `restConfKey`, `restSchedulerKey`
5. Вызов `deploy(mainClass, appResource, serverUrl, sparkProperties, appArgs)`


### Секреты в Spark-задачах (lakehouse-credential-providers-jdbc / -spark)

`SparkStandAloneClusterTaskProcessor` никогда не резолвит и не передаёт пароли БД в открытом виде:

- все ключи `spark.*`, собранные из `service.properties` datasource и `taskProcessorArgs`
  (включая `spark.sql.catalog.*`), передаются драйверу в составе `sparkProperties`
  (извлечение sparkConf, шаги выше);
- если у datasource нет явного `spark.sql.catalog.<key>.url`, процессор строит его из
  `host`/`port`/`urn` — **без учётных данных**;
- драйвер параметризуется безопасным каталогом `LakehouseSecureJDBCTableCatalog`
  (модуль `lakehouse-credential-providers-spark`), который на Driver/Executors резолвит пароль
  через `SecretResolver` (модуль `lakehouse-credential-providers-jdbc`) по опциям
  `secretProvider`, `secret-key`, `vault-url` (+ опциональные `vault-role`, `vault-k8s-auth-path` для OpenBao,
  или `secret-id` / `secret-version` для Lockbox);
- все опции безопасности вычищаются из опций каталога до передачи базовому JDBC-каталогу.

Как включить (в `service.properties` datasource, ключи с префиксом):

```
spark.sql.catalog.processingdb                     org.lakehouse.security.catalog.LakehouseSecureJDBCTableCatalog
spark.sql.catalog.processingdb.url                 jdbc:postgresql://db-host:5432/db
spark.sql.catalog.processingdb.user                app_user
spark.sql.catalog.processingdb.secretProvider      org.lakehouse.security.jdbc.BaoJdbcSecretProvider
spark.sql.catalog.processingdb.vault-url           http://openbao:8200
spark.sql.catalog.processingdb.secret-key          kv/data/lakehouse/database:password
```

Требуется: jar-файлы `lakehouse-credential-providers-jdbc` и `lakehouse-credential-providers-spark` в classpath
драйвера, сетевой доступ к хранилищу секретов и `VAULT_TOKEN` (или Kubernetes Service Account). Подробнее в
[руководстве по безопасности](../../doc-ru/security/security.md).


### Работа со статусной моделью датасета
Требует доступности state-svc. Временное отсутствие приведет к аварии задачи.
scheduler-svc должен будет повторить задачу, это сглаживает проблему перезапуска экземпляров.

  * [LockedStateTaskProcessor.java](../src/main/java/org/lakehouse/taskexecutor/processor/state/LockedStateTaskProcessor.java) Переводит инкремент датасета в статус Locked — заблокирован. Это показывает другим процессам, что они НЕ могут работать с интервалом данных датасета.
  * [SuccessStateTaskProcessor.java](../src/main/java/org/lakehouse/taskexecutor/processor/state/SuccessStateTaskProcessor.java) Переводит инкремент датасета в статус Success — успешен. Это показывает другим процессам, что они могут работать с интервалом данных датасета.
  * [DependencyCheckStateTaskProcessor.java](../src/main/java/org/lakehouse/taskexecutor/processor/state/DependencyCheckStateTaskProcessor.java) Проверяет статус датасета. Применяется для проверки состояния зависимостей и текущего датасета.

### Работа с базами данных (JDBC)
[JdbcTaskProcessor.java](../src/main/java/org/lakehouse/taskexecutor/processor/jdbc/JdbcTaskProcessor.java)
- всегда работает через jdbc драйвер, который должен быть помещен в classpath
- ничего не знает про синтаксис БД, с которой работает, потому что это ответственность соответствующего [sqlTemplate.md](../../lakehouse-config-svc/doc-ru/content_configuration/sqlTemplate.md)
- отвечает только за определение из параметра задачи `taskProcessorBody` и его запуск
- открывает подключение через `JdbcConnectionFactory` (`lakehouse-task-executor-api`): при наличии
  `lakehouse-credential-providers-jdbc` в classpath, если `service.properties` целевого datasource
  содержит `secretProvider`, пароль резолвится из OpenBao/Lockbox (`secret-key`, `vault-url` и т.д.),
  опции безопасности вычищаются, а разрешённое значение инжектируется как `password`; без
  `secretProvider` поведение не меняется

## TaskProcessorBody
Код, который выносится из TaskProcessor для пере-использования другими TaskProcessor или исполнения вне приложения.

![TaskProcessorBody.png](uml/TaskProcessorBody.png)

TaskProcessorBody, использующие шаблоны SQLTemplate, совместимы с Spark-задачами и [JdbcTaskProcessor.java](../src/main/java/org/lakehouse/taskexecutor/processor/jdbc/JdbcTaskProcessor.java):
* [AppendSQLProcessorBody.java](../../lakehouse-task-executor-api/src/main/java/org/lakehouse/taskexecutor/api/processor/body/sql/AppendSQLProcessorBody.java)
  Помещает модель в insert и выполняет запрос
* [CompactTableSQLProcessorBody.java](../../lakehouse-task-executor-api/src/main/java/org/lakehouse/taskexecutor/api/processor/body/sql/CompactTableSQLProcessorBody.java)
  Выполняет команду из шаблона tableDDLCompact
* [CreateTableSQLProcessorBody.java](../../lakehouse-task-executor-api/src/main/java/org/lakehouse/taskexecutor/api/processor/body/sql/CreateTableSQLProcessorBody.java)
  Выполняет команду из шаблона tableDDLCreate, схему тоже создаст из шаблона схемы
* [MergeSQLProcessorBody.java](../../lakehouse-task-executor-api/src/main/java/org/lakehouse/taskexecutor/api/processor/body/sql/MergeSQLProcessorBody.java)
  Помещает модель в merge и выполняет запрос

Эти body ничего не знают про окружение, где они будут работать, и синтаксис языка.
Они знают только какой шаблон надо извлечь, формируют уникальный jinja контекст, передают это всё в абстрактную среду исполнения, которая рендерит шаблон и исполняет его в конкретном окружении (RDBMS, TRINO или SPARK).

[SparkTaskProcessorDQBody.java](../../lakehouse-task-executor-spark-dq-app/src/main/java/org/lakehouse/taskexecutor/spark/dq/service/SparkTaskProcessorDQBody.java)
совместим только с [SparkStandAloneClusterTaskProcessor.java](../src/main/java/org/lakehouse/taskexecutor/processor/spark/SparkStandAloneClusterTaskProcessor.java).
