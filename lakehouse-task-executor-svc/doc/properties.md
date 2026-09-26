```yaml
server:
  port: 8089
lakehouse:
  client: # External interaction settings
    rest:
      state:
        server:
          url: http://127.0.0.1:8082 # State service
      config:
        server:
          url: http://127.0.0.1:8080 # Configuration service
      scheduler:
        server:
          url: http://127.0.0.1:8081 # Scheduler service
  task-executor:
    service:
      # Intervals for sending heartbeat on the taken task to the scheduler service.
      # If not sent in time, the scheduler service will decide that something is wrong with the task
      # and move it to failed
      heart-beat-initial-delaY-ms: 5000 # delay at startup
      heart-beat-interval-ms: 5000 # interval between sends. Must be more frequent than  lakehouse.scheduler.task.retry.delay-ms in the scheduler service
      max-lock-retries: 5 # attempts to take a lock. The scheduler service may be temporarily unavailable.
      max-lock-retries-duration-ms: 5 # delay between attempts
      # Multiple executors can be in the same group.
      # This identifier will be sent to the scheduler service when locking a task,
      # so that it is possible to find out which specific instance took the task lock.
      # You can specify a pod or host name
      id: first1
    processor: # Task processor parameters
      sparkStandAloneClusterTaskProcessor:
        maxWaitToRunningStateTimeoutMs: 120000 # max time to wait for the Spark job transition to RUNNING, ms
        sparkJobStatusCheckIntervalMs: 3000 # Spark job status polling interval, ms
    # Per-domain data source settings: domains.<domainKeyName>.<dataSourceKeyName>.service-properties
    # Bound by DomainDataSourceServiceProperties (prefix lakehouse.task-executor) and exposed via
    # getServiceProperties(domainKey, dataSourceKey), which returns Optional.empty() for an unknown
    # domain or data source. NOTE: not applied to the execution path yet - the executor resolves
    # data sources through lakehouse-config-svc. See readme.md, chapter "Domains".
    domains:
      platform:
        lakehousestorage:
          service-properties: # free-form map: secret provider options, user, fetchSize, ...
            secretProvider: org.lakehouse.security.jdbc.BaoJdbcSecretProvider
            secret-key: "kv/data/lakehouse/database:password"
            vault-url: "http://openbao:8200"
            user: postgresUser
            fetchSize: "10000"
    scheduled: # Parameters for receiving tasks
      task:
        kafka:
          consumer:
            concurrency: 1 # number of task consumption threads. 1 means the process will process 1 task at a time sequentially. The functionality is provided by Spring, it has not been tested in detail.
            properties:
              bootstrap.servers: 192.1.193.20:9092
              group.id: default  # Corresponds to the taskExecutionServiceGroupName parameter from the task configuration. if this parameter and the task's taskExecutionServiceGroupName do not match, the task is ignored because another executor group should take it
              auto.offset.reset: earliest
            # The name of the topic where the scheduler service puts tasks passed for execution.
            # The name must match the one in the scheduler service
            topics: scheduled_task_msg
```

### Domain-scoped data source parameters

| Parameter | Default | Description |
|---|---|---|
| `lakehouse.task-executor.domains.<domainKeyName>.<dataSourceKeyName>.service-properties` | *(empty)* | Free-form map of connection settings for one data source of one domain |

The keys inside `service-properties` are not validated by the binding and are expected to be
the same secret provider options that `lakehouse-credential-providers-jdbc` understands in
the `service.properties` of a JDBC data source (`secretProvider`, `secret-key`, `vault-url`,
`vault-role`, `vault-k8s-auth-path`, `secret-id`, `secret-version`, `url`, `user`).

These values are currently **not used during execution**: the data sources a task opens are
always the ones returned by `lakehouse-config-svc`. The block is kept here because it is
bound and exposed by `DomainDataSourceServiceProperties`, and because the per-domain
deployment story depends on it. See readme.md, chapter "Domains".

### Task processor parameters

| Parameter | Default | Description |
|---|---|---|
| `lakehouse.task-executor.processor.sparkStandAloneClusterTaskProcessor.maxWaitToRunningStateTimeoutMs` | `120000` | Max time to wait for the Spark job transition to `RUNNING`, ms |
| `lakehouse.task-executor.processor.sparkStandAloneClusterTaskProcessor.sparkJobStatusCheckIntervalMs` | `3000` | Spark job status polling interval, ms |

### Secret resolution options for datasources (lakehouse-credential-providers-jdbc)

If the `lakehouse-credential-providers-jdbc` jar is on the classpath, the `service.properties` of a JDBC
datasource (`ServiceDTO.properties`) may contain secret provider options. `JdbcConnectionFactory` then resolves
the password at runtime and strips the security options before opening the connection:

| Option | Description |
|---|---|
| `secretProvider` | Fully qualified class name of the `SecretProvider` implementation. Its presence enables resolution |
| `secret-key` | Combined `path:key` coordinate, e.g. `kv/data/lakehouse/database:password` |
| `vault-url` | OpenBao/Vault HTTP API base URL |
| `vault-role`, `vault-k8s-auth-path` | Optional Kubernetes auth settings for OpenBao/Vault |
| `secret-id`, `secret-version` | Yandex Cloud Lockbox secret id and optional version (default `latest`) |
| `url` | Optional explicit JDBC URL; otherwise built from `host`/`port`/`urn` |
| `user` | User name; the password itself comes from the provider |

Requires the `VAULT_TOKEN` environment variable (OpenBao) or `YC_AUTH_KEY_PATH` (Lockbox). Real example:
`demo/compose/conf/datasources/processingdb.json`. Full details: [security guide](../../doc/security/security.md).