```yaml
server:
  port: 8089
lakehouse:
  client: # Настройки внешнего взаимодействия
    rest:
      state:
        server:
          url: http://127.0.0.1:8082 # Сервис состояний
      config:
        server:
          url: http://127.0.0.1:8080 # Сервис конфигураций
      scheduler:
        server:
          url: http://127.0.0.1:8081 # Сервис расписаний
  task-executor:
    service:
      # Интервалы отправки herdbeat по взятой задаче сервису расписаний. 
      # Если вовремя не отправить , сервис расписаний решит что с задачей что-то не так 
      # и переведет ее в failed
      heart-beat-initial-delaY-ms: 5000 # задержка при старте  
      heart-beat-interval-ms: 5000 # интервал между отправками. Должен быть чаще чем  lakehouse.scheduler.task.retry.delay-ms в сервисе расписаний
      max-lock-retries: 5 # попытки взять блокировку. Сервис расписаний может быть временно недоступен.  
      max-lock-retries-duration-ms: 5 # задержка между попытками
      # Несколько исполнителей могут находиться в одной группе. 
      # Этот идентификатор отправится сервису расписаний при блокировке задачи,
      # чтобы можно было узнать какой конкретный экземпляр взял блокировку задачи.
      # Можно указать имя пода или хоста 
      id: first1
    processor: # Параметры процессоров задач
      sparkStandAloneClusterTaskProcessor:
        maxWaitToRunningStateTimeoutMs: 120000 # максимальное время ожидания перехода Spark-задачи в состояние RUNNING, мс
        sparkJobStatusCheckIntervalMs: 3000 # интервал опроса статуса Spark-задачи, мс
    scheduled: # Параметры для получения задач
      task:
        kafka:
          consumer:
            concurrency: 1 # количество потоков потребления задач 1 значит процесс будет последовательно обрабатывать 1 задачу за раз. Функциональность предоставлена Spring, это детально не тестировалось.
            properties:
              bootstrap.servers: 192.1.193.20:9092
              group.id: default  # Соответствует параметру taskExecutionServiceGroupName из конфигурации задач. если этот параметр и taskExecutionServiceGroupName у задачи не совпадают, задача игнорируется тк ее должна взять другая группа исполнителей
              auto.offset.reset: earliest
            # Имя топика куда сервис расписаний поставляет задачи переданные на выполнение.
            # Имя должно совпадать с именем у сервиса расписаний  
            topics: scheduled_task_msg 
```

### Параметры процессоров задач

| Параметр | По умолчанию | Описание |
|---|---|---|
| `lakehouse.task-executor.processor.sparkStandAloneClusterTaskProcessor.maxWaitToRunningStateTimeoutMs` | `120000` | Максимальное время ожидания перехода Spark-задачи в состояние `RUNNING`, мс |
| `lakehouse.task-executor.processor.sparkStandAloneClusterTaskProcessor.sparkJobStatusCheckIntervalMs` | `3000` | Интервал опроса статуса Spark-задачи, мс |

### Опции разрешения секретов для datasource (lakehouse-credential-providers-jdbc)

Если jar `lakehouse-credential-providers-jdbc` находится в classpath, в `service.properties` JDBC-источника
(`ServiceDTO.properties`) могут присутствовать опции провайдера секретов. `JdbcConnectionFactory` резолвит пароль
в рантайме и вычищает опции безопасности до открытия подключения:

| Опция | Описание |
|---|---|
| `secretProvider` | Полное имя класса реализации `SecretProvider`. Его наличие включает разрешение |
| `secret-key` | Комбинированная координата `path:key`, например `kv/data/lakehouse/database:password` |
| `vault-url` | Базовый URL HTTP API OpenBao/Vault |
| `vault-role`, `vault-k8s-auth-path` | Опциональные настройки Kubernetes auth для OpenBao/Vault |
| `secret-id`, `secret-version` | Идентификатор секрета Yandex Cloud Lockbox и опциональная версия (по умолчанию `latest`) |
| `url` | Опциональный явный JDBC URL; иначе строится из `host`/`port`/`urn` |
| `user` | Имя пользователя; сам пароль приходит из провайдера |

Требуется переменная окружения `VAULT_TOKEN` (OpenBao) или `YC_AUTH_KEY_PATH` (Lockbox). Реальный пример:
`demo/compose/conf/datasources/processingdb.json`. Подробнее: [руководство по безопасности](../../doc-ru/security/security.md).