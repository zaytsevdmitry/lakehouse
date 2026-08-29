# Interaction with the scheduler service
![ServiceWorkSequence.png](uml/ServiceWorkSequence.png)

## Configuration process
The configuration process is based on filling two important stories
- Data sources
  - [drivers](../../lakehouse-config-svc/doc/content_configuration/drivers.md)
    - [datasources](../../lakehouse-config-svc/doc/content_configuration/datasources.md)
      - [datasets](../../lakehouse-config-svc/doc/content_configuration/datasets.md)
- Schedules
  - [scenarioActTemplate](../../lakehouse-config-svc/doc/content_configuration/scenarioActTemplate.md)
    - [schedules.md](../../lakehouse-config-svc/doc/content_configuration/schedules.md)

These configurations must be filled in and passed to the config-svc service.
To run a specific task whose executor is the *TaskProcessor object, the source (target) configuration is enriched into the [sources.md](../../lakehouse-config-svc/doc/content_configuration/sources.md) object.
This is done on the side of the configuration service, any developed *TaskProcessor *TaskProcessorBody can obtain it by the dataset key name.

The configuration service, having received the schedule configuration, publishes it to a kafka topic, and the scheduler service listens to it and forms schedules.
Identifiers of tasks ready for execution are sent to a kafka topic listened to by task-executor-svc instances.
Having received the identifier, one task-executor-svc locks the task in scheduler-svc, receiving in response the full task description.
This is a merged version of the task consisting of the union of the assigned task template and the overridden values specified for the specific task.

Thus, for the task-executor-svc service to work, config-svc and scheduler-svc are required.
A temporary absence of one of them will lead to a task failure.
scheduler-svc will have to retry the task, which smooths over the problem of instance restarts.

# Task parameterization
## taskProcessor parameter
What executes the task.
![TaskProcessors.png](uml/TaskProcessors.png)


> The processor name is specified starting with a lowercase letter in camelCase


###  [Spark tasks](../../lakehouse-task-executor-spark-api/doc/readme.md)
  * sparkStandAloneClusterTaskProcessor [SparkStandAloneClusterTaskProcessor.java](../src/main/java/org/lakehouse/taskexecutor/processor/spark/SparkStandAloneClusterTaskProcessor.java) runs the task body on a remote spark standalone cluster as a spark-job through the REST API `/v1/submissions`.

**Architecture of Spark processors:**

```
SparkStandAloneClusterTaskProcessor
  └─ extends AbstractTaskProcessor
       └─ deploy(mainClass, appResource, serverUrl, sparkProperties, appArgs) — private method of the processor, deploy logic via the Spark REST API
```

- in earlier versions the deployment logic lived in the separate `AbstractSparkDeployTaskProcessor`
  (with `SparkRestDeployFactory` for URL building); it was later inlined into
  [SparkStandAloneClusterTaskProcessor.java](../src/main/java/org/lakehouse/taskexecutor/processor/spark/SparkStandAloneClusterTaskProcessor.java)
- the cluster endpoint is taken from the `deploy.clusterUrl` argument of `taskProcessorArgs`
  (`getMasterUrl()`); it already contains the full REST path, e.g. `http://host:port/v1/submissions`,
  and is used as the base URI for `SparkRestClientApi` (`buildSparkRestClientApi(baseURI)` on `RestClient`)
- the driver app is parameterized by the keys `deploy.mainClass` and `deploy.appResource`
  (resolved in priority: taskProcessorArgs → datasource service.properties, via `Coalesce.apply`)
- `deploy(...)` sends POST `/v1/submissions/create`, waits for the transition to `RUNNING`
  (timeout `maxWaitToRunningStateTimeoutMs`, default 120000 ms), polls the status every
  `sparkJobStatusCheckIntervalMs` (default 3000 ms) until a final status
  (`FINISHED`/`KILLED`/`FAILED`/`ERROR`) is reached, and fails on negative statuses
  (`KILLED`, `FAILED`, `ERROR`)
- exhaustion of the `DRIVER_STATE_NULL_LIMIT` (30) polls with `driverState` null/`UNKNOWN` after `RUNNING`
  aborts the task if the driver record disappeared from the master

They know nothing about the task logic. Responsibility — parsing the configuration to parameterize the spark-driver in a specific cluster.
They do not work with local driver launch, since this would heavily burden the service itself and blur the boundaries of its responsibility.

The configuration is divided into three different types:

- **sparkConf**
  - All datasources acting as dependencies are iterated. The nested `service.properties` object is filtered. All parameters starting with `spark.sql.catalog` are selected.
  - The target datasource parameters are iterated. The nested `service.properties` object is filtered. All parameters starting with `spark.` are selected. The selected parameters will override those obtained above if the same keys are encountered.
  - `taskProcessorArgs` are iterated. All parameters starting with `spark.` are selected. The selected parameters will override those obtained above if the same keys are encountered.

  > Why this exact order: a datasource is filled once and for many datasets, therefore it contains the most general parameters. It is convenient to use it for default parameters. taskProcessorArgs can differ in tasks serving the same table (dataset), therefore depending on the specific operation the values and composition of parameters may change. For example, one operation requires more memory, and another — more cores.

- **Pure application attributes**
  - `taskProcessorArgs` are taken and all parameters starting with `spark.` are discarded, since passing them over the network is redundant.

- **k8s manifest** (not implemented, reserved)
  - It is assumed to iterate the target datasource parameters with the `k8s.spark-operator` filter and iterate `taskProcessorArgs` with the same filter.

### The order of sparkConf extraction in SparkStandAloneClusterTaskProcessor.runTask()

1. **`SparkConfUtil.extractSparkConFromTaskConf(sourceConfDTO, scheduledTaskDTO)`** — collecting and merging spark properties in three passes (priority — from lowest to highest):
   1. `spark.sql.catalog.*` from all datasource dependencies (except the target)
   2. All `spark.*` from the target datasource
   3. All `spark.*` from the task's `taskProcessorArgs`
2. **`SparkConfUtil.unSparkConf(scheduledTaskDTO)`** — removing spark keys from `taskProcessorArgs` before sending them to the spark-driver
3. Resolving `mainClass` and `appResource`: taskProcessorArgs → datasource service.properties (via `Coalesce.apply`)
4. Building `appArgs`: unSparkedTaskConfig + `scheduledTaskId`, `restConfKey`, `restSchedulerKey`
5. Calling `deploy(mainClass, appResource, serverUrl, sparkProperties, appArgs)`


### Secrets in Spark tasks (lakehouse-credential-providers-jdbc / -spark)

`SparkStandAloneClusterTaskProcessor` never resolves or transmits database passwords in plaintext:

- all `spark.*` keys gathered from the datasources `service.properties` and `taskProcessorArgs`
  (including `spark.sql.catalog.*`) are forwarded to the driver as part of `sparkProperties`
  (sparkConf extraction, steps above);
- if the datasource has no explicit `spark.sql.catalog.<key>.url`, the processor builds it from
  `host`/`port`/`urn` - **without credentials**;
- the driver is parameterized to use the secure catalog `LakehouseSecureJDBCTableCatalog`
  (module `lakehouse-credential-providers-spark`), which on the Driver/Executors resolves the password
  via `SecretResolver` (module `lakehouse-credential-providers-jdbc`) using the options
  `secretProvider`, `secret-key`, `vault-url` (+ optional `vault-role`, `vault-k8s-auth-path` for OpenBao,
  or `secret-id` / `secret-version` for Lockbox);
- all security options are stripped from the catalog options before they reach the base JDBC catalog.

How to enable (in the datasource `service.properties`, prefixed keys):

```
spark.sql.catalog.processingdb                     org.lakehouse.security.catalog.LakehouseSecureJDBCTableCatalog
spark.sql.catalog.processingdb.url                 jdbc:postgresql://db-host:5432/db
spark.sql.catalog.processingdb.user                app_user
spark.sql.catalog.processingdb.secretProvider      org.lakehouse.security.jdbc.BaoJdbcSecretProvider
spark.sql.catalog.processingdb.vault-url           http://openbao:8200
spark.sql.catalog.processingdb.secret-key          kv/data/lakehouse/database:password
```

Requires: the jars `lakehouse-credential-providers-jdbc` and `lakehouse-credential-providers-spark` on the driver
classpath, network access to the secret store, and `VAULT_TOKEN` (or Kubernetes Service Account auth). More details
in the [security guide](../../doc/security/security.md).


### Working with the dataset state model
Requires state-svc availability. A temporary absence will lead to a task failure.
scheduler-svc will have to retry the task, which smooths over the problem of instance restarts.

  * [LockedStateTaskProcessor.java](../src/main/java/org/lakehouse/taskexecutor/processor/state/LockedStateTaskProcessor.java) Moves the dataset increment to the Locked status — locked. This shows other processes that they CANNOT work with the dataset data interval.
  * [SuccessStateTaskProcessor.java](../src/main/java/org/lakehouse/taskexecutor/processor/state/SuccessStateTaskProcessor.java) Moves the dataset increment to the Success status — successful. This shows other processes that they CAN work with the dataset data interval.
  * [DependencyCheckStateTaskProcessor.java](../src/main/java/org/lakehouse/taskexecutor/processor/state/DependencyCheckStateTaskProcessor.java) Checks the dataset status. Used to check the state of dependencies and the current dataset.

### Working with databases (JDBC)
[JdbcTaskProcessor.java](../src/main/java/org/lakehouse/taskexecutor/processor/jdbc/JdbcTaskProcessor.java)
- always works through a jdbc driver that must be placed on the classpath
- knows nothing about the syntax of the database it works with, because this is the responsibility of the corresponding [sqlTemplate.md](../../lakehouse-config-svc/doc/content_configuration/sqlTemplate.md)
- is responsible only for determining the `taskProcessorBody` from the task parameter and running it
- opens the connection through `JdbcConnectionFactory` (`lakehouse-task-executor-api`): with
  `lakehouse-credential-providers-jdbc` on the classpath, if the target datasource `service.properties`
  contains `secretProvider`, the password is resolved from OpenBao/Lockbox (`secret-key`, `vault-url`,
  etc.), the security options are stripped and the resolved value is injected as `password`; without
  `secretProvider` the behavior is unchanged

## TaskProcessorBody
Code that is extracted from the TaskProcessor for reuse by other TaskProcessors or execution outside the application.

![TaskProcessorBody.png](uml/TaskProcessorBody.png)

TaskProcessorBody implementations using SQLTemplate templates are compatible with Spark tasks and [JdbcTaskProcessor.java](../src/main/java/org/lakehouse/taskexecutor/processor/jdbc/JdbcTaskProcessor.java):
* [AppendSQLProcessorBody.java](../../lakehouse-task-executor-api/src/main/java/org/lakehouse/taskexecutor/api/processor/body/sql/AppendSQLProcessorBody.java)
  Places the model into an insert and executes the query
* [CompactTableSQLProcessorBody.java](../../lakehouse-task-executor-api/src/main/java/org/lakehouse/taskexecutor/api/processor/body/sql/CompactTableSQLProcessorBody.java)
  Executes the command from the tableDDLCompact template
* [CreateTableSQLProcessorBody.java](../../lakehouse-task-executor-api/src/main/java/org/lakehouse/taskexecutor/api/processor/body/sql/CreateTableSQLProcessorBody.java)
  Executes the command from the tableDDLCreate template, the schema will also be created from the schema template
* [MergeSQLProcessorBody.java](../../lakehouse-task-executor-api/src/main/java/org/lakehouse/taskexecutor/api/processor/body/sql/MergeSQLProcessorBody.java)
  Places the model into a merge and executes the query

These bodies know nothing about the environment where they will run, nor about the language syntax.
They only know which template to extract, build a unique jinja context, and pass it all to an abstract execution environment that renders the template and executes it in a specific environment (RDBMS, TRINO or SPARK).

[SparkTaskProcessorDQBody.java](../../lakehouse-task-executor-spark-dq-app/src/main/java/org/lakehouse/taskexecutor/spark/dq/service/SparkTaskProcessorDQBody.java)
is compatible only with [SparkStandAloneClusterTaskProcessor.java](../src/main/java/org/lakehouse/taskexecutor/processor/spark/SparkStandAloneClusterTaskProcessor.java).