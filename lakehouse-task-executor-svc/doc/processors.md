# Взаимодействие с сервисом расписаний
![ServiceWorkSequence.png](uml/ServiceWorkSequence.png)

## Процесс конфигурирования
Процесс конфигурирования основан на заполнении двух важных историй
- Источники данных
  - [drivers](../../lakehouse-config-svc/doc/content_configuration/drivers.md)
    - [datasources](../../lakehouse-config-svc/doc/content_configuration/datasources.md)
      - [datasets](../../lakehouse-config-svc/doc/content_configuration/datasets.md)
- Расписания
  - [scenarioActTemplate](../../lakehouse-config-svc/doc/content_configuration/scenarioActTemplate.md)
    - [schedules.md](../../lakehouse-config-svc/doc/content_configuration/schedules.md)

Эти конфигурации должны быть заполнены и переданы в сервис config-svc.
Для работы конкретной задачи исполнителем которой является объект *TaskProcessor конфигурация источника (назначения) обогащается в объект [sources.md](../../lakehouse-config-svc/doc/content_configuration/sources.md).
Производится это на стороне сервиса конфигурации, любой разрабатываемы *TaskProcessor *TaskProcessorBody может получить его по ключевому имени датасета.

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

###  [Spark-задачи](../../lakehouse-task-spark-apps/doc/devguide.md)
  * [SparkLauncherTaskProcessor.java](../src/main/java/org/lakehouse/taskexecutor/processor/spark/SparkLauncherTaskProcessor.java) запускает тело задачи на удаленном spark standalone кластере в виде spark-job.
  * [SparkK8sOperatorTaskProcessor.java](../src/main/java/org/lakehouse/taskexecutor/processor/spark/SparkK8sOperatorTaskProcessor.java) запускает тело задачи на удаленном кластере k8s в виде spark-job 

Ничего не знают о логике задачи. Ответственность это разбор конфигурации, чтобы параметризовать spark-driver в конкретном кластере.
Не работают с локальным запуском драйвере тк это сильно утяжелит сам сервис и размоет границы его ответственности.
Конфигурация разделяется на два разные типы
- sparkConf
  - Производится обход параметров всех datasource, которые выступают в роли зависимости фильтруется вложенный объект service.properties. Отбираем все параметры начинающиеся на spark.sql.catalog.
  - Производится обход параметров целевого datasource. Тоже фильтруется вложенный объект service.properties. Отбираем все параметры начинающиеся на spark. Отобранные параметры перезапишут полученные выше, если встретятся одинаковые ключи.
  - Производится обход taskProcessorArgs. Отбираем все параметры начинающиеся на spark. Отобранные параметры перезапишут полученные выше, если встретятся одинаковые ключи.
> Почему порядок именно такой: datasource заполняется один и на множество датасетов. Поэтому содержит наиболее обобщенные параметры. Его удобно использовать для параметров по умолчанию.  taskProcessorArgs могут быть в разных задачах обслуживающих одну  и ту же таблицу(dataset) поэтому в зависимости от конкретной операции могут меняться значения и состав параметров. На пример для одной операции требуется больше памяти, а для другой больше ядер 

- Чистые атрибуты приложения 
  - тут берется просто taskProcessorArgs и отбрасываются все параметры начинающиеся на spark. так как их передача избыточна для передачи по сети.

- Манифест k8s
  - Производится обход параметров целевого datasource. Отфильтровываются параметры k8s.spark-operator.
  - Производится обход taskProcessorArgs. Отфильтровываются параметры k8s.spark-operator. Отобранные параметры перезапишут полученные выше, если встретятся одинаковые ключи.
>  Как это уже видно из названия этот вид конфигурации применяется для создания манифеста kuberflow/spark-operator и работает только с SparkK8sOperatorTaskProcessor
> В SparkLauncherTaskProcessor это не требуется так как кластер spark - standalone обходится исключительно конфигурацией spark. В манифесте для k8s мы должны рассказать больше специфичных подробностей, а иногда даже продублировать количество требуемой памяти и ядер


### Работа со статусной моделью датасета
Требует доступности state-svc. Временное отсутствие  приведет к аварии задачи.
scheduler-svc должен будет повторить задачу, это сглаживает проблему перезапуска экземпляров.

  * [LockedStateTaskProcessor.java](../src/main/java/org/lakehouse/taskexecutor/processor/state/LockedStateTaskProcessor.java) Переводит инкремент датасета в статус - Locked - заблокирован. Это показывает другим процессам, что они НЕ могут работать с интервалом данных датасета.
  * [SuccessStateTaskProcessor.java](../src/main/java/org/lakehouse/taskexecutor/processor/state/SuccessStateTaskProcessor.java) Переводит инкремент датасета в статус - Success  - успешен.  Это показывает другим процессам, что они могут работать с интервалом данных датасета.
  * [DependencyCheckStateTaskProcessor.java](../src/main/java/org/lakehouse/taskexecutor/processor/state/DependencyCheckStateTaskProcessor.java) Проверяет статус датасета. Применяется для проверки состояния зависимостей и текущего датасета.
### Работа с базами данных(JDBC). Особенности работы [JdbcTaskProcessor.java](../src/main/java/org/lakehouse/taskexecutor/processor/jdbc/JdbcTaskProcessor.java)
- всегда работает через jdbc драйвер, который должен быть помещен в classpath
- ничего не знает про синтаксис бд с корой работает, потому что это ответственность соответствующего [sqlTemplate.md](../../lakehouse-config-svc/doc/content_configuration/sqlTemplate.md)
- отвечает только за определение из параметра задачи taskProcessorBody и его запуск

## TaskProcessorBody
Код, который выносится из TaskProcessor для пере-использования другими TaskProcessor или исполнения вне приложения

![TaskProcessorBody.png](uml/TaskProcessorBody.png)

TaskProcessorBody использующие шаблоны SQLTemplate совместимы с Spark-задачами и [JdbcTaskProcessor.java](../src/main/java/org/lakehouse/taskexecutor/processor/jdbc/JdbcTaskProcessor.java)
* [AppendSQLProcessorBody.java](../../lakehouse-task-executor-api/src/main/java/org/lakehouse/taskexecutor/api/processor/body/sql/AppendSQLProcessorBody.java)
  Помещает модель в insert и выполняет запрос
* [CompactTableSQLProcessorBody.java](../../lakehouse-task-executor-api/src/main/java/org/lakehouse/taskexecutor/api/processor/body/sql/CompactTableSQLProcessorBody.java)
  Выполняет команду из шаблона tableDDLCompact
* [CreateTableSQLProcessorBody.java](../../lakehouse-task-executor-api/src/main/java/org/lakehouse/taskexecutor/api/processor/body/sql/CreateTableSQLProcessorBody.java)
  Выполняет команду из шаблона tableDDLCreate схему тоже создаст из шаблона схемы
* [MergeSQLProcessorBody.java](../../lakehouse-task-executor-api/src/main/java/org/lakehouse/taskexecutor/api/processor/body/sql/MergeSQLProcessorBody.java)
  Помещает модель в merge и выполняет запрос
Эти body, ничего не знают про окружение где они будут работать, синтаксис языка.
Они знают только какой шаблон надо извлечь, формируют уникальный jinja контекст, передают это все в абстрактную среду исполнения котора рендерит темплейт и исполняет его в конкретном окружении (RDBMS,TRINO или SPARK)

[SparkTaskProcessorDQBody.java](../../lakehouse-task-executor-spark-dq-app/src/main/java/org/lakehouse/taskexecutor/spark/dq/service/SparkTaskProcessorDQBody.java)
совместим только [SparkLauncherTaskProcessor.java](../src/main/java/org/lakehouse/taskexecutor/processor/spark/SparkLauncherTaskProcessor.java) и
[SparkK8sOperatorTaskProcessor.java](../src/main/java/org/lakehouse/taskexecutor/processor/spark/SparkK8sOperatorTaskProcessor.java)


