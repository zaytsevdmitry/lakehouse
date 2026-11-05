# Примеры (RU)

Полные шаблоны DAG доступны для каждой major-версии:

* [examples/2.x/yarina_split_lock_lifecycle_dag.py](../examples/2.x/yarina_split_lock_lifecycle_dag.py)
* [examples/3.x/yarina_split_lock_lifecycle_dag.py](../examples/3.x/yarina_split_lock_lifecycle_dag.py)
* [examples/2.x/yarina_spark_submit_dag.py](../examples/2.x/yarina_spark_submit_dag.py)
* [examples/3.x/yarina_spark_submit_dag.py](../examples/3.x/yarina_spark_submit_dag.py)

## Жизненный цикл

```text
check_dependencies -> lock_interval -> etl -> unlock_interval
```

1. **check_dependencies** (`YarinaStateCheckOperator`)
   * резолвит датасеты-источники (из `source_datasets` или из
     `yarina-config-svc`);
   * вызывает `POST /v1_0/state/dataset/wrong` для каждого источника и для
     целевого датасета;
   * падает на чужом `LOCKED` (и, опционально, на «дырах»).
2. **lock_interval** (`YarinaStateLockOperator`)
   * отправляет `PUT /v1_0/state/dataset` со `status=LOCKED`;
   * использует `lock_source="airflow-run-{{ run_id }}"`;
   * кладёт lockSource в XCom как `active_lock_source`.
3. **etl** - ваша Bash/Python/Spark-задача. Если она падает, Airflow
   останавливает run, а интервал остаётся `LOCKED` (удобно для диагностики и
   безопасных повторов).
4. **unlock_interval** (`YarinaStateUnlockOperator`)
   * отправляет `PUT /v1_0/state/dataset` с тем же `lock_source` и
     `status=SUCCESS`.

LockSource уникален для каждого DAG run, поэтому два параллельных run не могут
молча перезаписать друг друга. Пока интервал заблокирован, state-svc отклоняет
запись `SUCCESS` с другим lockSource.

## `force_unlock` и когда он нужен

В штатных DAG'ах оставляйте `force_unlock=False`.

Используйте `force_unlock=True` **только для аварийного восстановления**,
например когда:

* DAG перезапустили с новым `run_id`, а интервал всё ещё `LOCKED` от прошлого run;
* lockSource потерян и освободить интервал штатно невозможно.

При `force_unlock=True` оператор определяет lockSource в таком порядке:

1. явный `lock_source` (если он отличается от `airflow-executor`);
2. XCom `active_lock_source` задачи `lock_task_id`;
3. единственный `LOCKED` lockSource, полученный из `yarina-state-svc`;
4. `airflow-executor` как fallback (если `LOCKED` нет, запись `SUCCESS` всё равно
   пройдёт).

Оператор пишет **WARNING** при каждом использовании `force_unlock` или при
обнаружении чужого лока. Предпочитайте отдельный maintenance-DAG или ручной run
с `force_unlock=True` постоянному включению флага.

## Отправка Spark-задачи

`YarinaSparkSubmitOperator` отправляет Spark-приложение через
`yarina-task-proxy-for-spark` и ждёт терминального статуса:

```text
POST /v1/submissions/create -> poll GET /v1/submissions/status/{submissionId} -> FINISHED
```

Минимальная задача:

```python
YarinaSparkSubmitOperator(
    task_id="submit_spark_job",
    app_resource="s3a://bucket/app.jar",
    main_class="com.example.Main",
    app_args=["--date", "{{ ds }}"],
    spark_properties={
        "spark.app.name": "airflow-{{ run_id }}",
        "spark.executor.instances": "4",
    },
    environment_variables={"AWS_REGION": "eu-west-1"},
    http_conn_id="yarina_spark_proxy_default",
    poll_interval_seconds=15,
    timeout_seconds=3600,
)
```

Задача кладёт `spark_submission_id` (proxy id, например `"42"`) в XCom. Она
завершается успешно на `FINISHED`, падает на `FAILED`/`KILLED` и сразу падает при
HTTP 404 на запрос статуса. Полная таблица статусов - в
[spark-operator.md](spark-operator.md).

> **Без kill:** оператор никогда не вызывает `/kill`, `/killall` или `/clear`. При
> таймауте Spark-приложение может продолжать выполняться; используйте
> `submissionId` из исключения, чтобы проверить
> `GET /v1/submissions/status/{id}` вручную.

Spark-задачу можно использовать как шаг `etl` внутри жизненного цикла интервалов,
например после `lock_interval` и перед `unlock_interval`.

## Автоматический резолв источников

По умолчанию (`auto_resolve_sources=True`, `source_datasets=None`) check-оператор
вызывает:

```text
GET /v1_0/configs/datasets/{dataset_key_name}
```

и берёт `DataSetDTO.sources.keys()` минус сам датасет. Это эквивалент
`SourceConfDTO.getTargetDataSet().getSources().keySet()` в
`DependencyCheckStateTaskProcessor`.

Явный список нужен, когда:

* config-svc недоступен из Airflow worker;
* DAG должен фиксировать зависимости независимо от изменений метаданных;
* нужно проверить только часть источников:

```python
YarinaStateCheckOperator(
    task_id="check_dependencies",
    dataset_key_name="transaction_dds",
    interval_start="{{ data_interval_start.isoformat() }}",
    interval_end="{{ data_interval_end.isoformat() }}",
    lock_source="airflow-run-{{ run_id }}",
    source_datasets=["client_processing", "transaction_processing"],
    auto_resolve_sources=False,
)
```

## Настройка поведения при ошибках

| Параметр | По умолчанию | Значение |
|:---------|:-------------|:---------|
| `fail_on_foreign_lock` | `True` | Падать, если целевой интервал заблокирован другим run |
| `fail_on_holes` | `False` | Падать, если интервал источника не покрыт `SUCCESS` |

Частая практика - держать `fail_on_holes=False`, когда upstream-датасеты
загружаются другими расписаниями, и включать флаг для строго зависимых дневных
цепочек.

## Шаблоны интервалов

В шаблонах используются:

```jinja
interval_start = "{{ data_interval_start.isoformat() }}"
interval_end   = "{{ data_interval_end.isoformat() }}"
lock_source    = "airflow-run-{{ run_id }}"
```

Все три поля входят в `template_fields`, поэтому рендерятся перед выполнением.
Полученные timestamp'ы содержат смещение timezone (например
`2026-01-01T00:00:00+00:00`), которое принимается сервисами.

## Диагностика ошибок

| Ошибка | Вероятная причина |
|:-------|:------------------|
| `Connection extra 'auth_url' is required` | Пустой `extra` Connection; укажите токен-эндпоинт |
| Keycloak 404 с подсказкой про `protocol/openid-connect/token` | `auth_url` указывает на issuer |
| HTTP 500 с подсказкой про NPE | Известный баг state-svc: null `lockSource` или null-даты |
| `lock source mismatch` / чужой лок | Интервал держит другой DAG run; подождите или используйте `force_unlock` в аварии |
| `Dataset '...' not found` | Неверный `dataset_key_name` или config-svc недоступен |
| Spark `submission '...' не найден (404)` | Прокси не знает id; оператор не делает retry |
| Таймаут Spark | Задача может продолжать выполняться; проверьте URL статуса из исключения вручную |
| Spark create отклонён | Смотрите `message` прокси и payload запроса в исключении |
