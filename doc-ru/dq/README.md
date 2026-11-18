# DQ: как писать тестсеты и пороги

Документ описывает подсистему контроля качества данных (Data Quality, DQ): формат
QM-конфигов, тестсетов и порогов, контекст шаблонов, диагностику ошибок и эталонные
проверки. Материал рассчитан на аналитика или дата-инженера, который пишет правила
проверки, не читая код движка.

## 1. Граница ответственности

Движок DQ предоставляет **механизм и контекст**:

* загружает QM-конфиг и его testSets/thresholds;
* рендерит SQL-скрипты Jinja-шаблонизатором;
* передаёт в шаблоны ключи датасетов, интервалы и справочники;
* выполняет SQL в Spark, приводит результаты к общему виду;
* сравнивает измеренные значения с ожиданиями и агрегирует severity;
* сохраняет статусы и измеренные значения метрик;
* выполняет физические integrity-проверки (PK, FK, UK, CHECK, nullable, schema drift).

Пользователь предоставляет **правила**:

* SQL-тестсеты, возвращающие `subMetricName` и `value`;
* пороги и ожидания (`expectation`) к этим тестсетам;
* при необходимости — переопределённые SQL-шаблоны integrity-проверок под свою БД.

Движок не «додумывает» бизнес-правила и не переписывает пользовательские конфиги:
если правило или тип колонки заданы неверно, вы получите диагностическую ошибку с
контекстом, а не молчаливое исправление.

## 2. Структура QM-конфига

Минимальный конфиг:

```json
{
  "dataSetKeyName": "transaction_dds",
  "keyName": "transaction_dds_qm",
  "enabled": true,
  "save": true,
  "dqThresholdViolationLevel": "error",
  "testSets": {
    "non_zero_count": {
      "type": "sparkSQL",
      "scripts": [{ "key": "dq.non_zero_count.sql" }]
    }
  },
  "metric": {
    "type": "sparkSQL",
    "scripts": [{ "key": "dq.non_zero_count.sql" }]
  },
  "thresholds": {
    "non_zero_count_th": {
      "type": "sparkSQL",
      "scripts": [{ "key": "dq.non_zero_count_th.sql" }]
    }
  }
}
```

Порядок выполнения:

1. Выполняются все `testSets`, их результаты регистрируются как временные
   представления с именами ключей тестсетов. Это нужно, чтобы в одном прогоне можно
   было ссылаться на значения других тестсетов (относительные пороги).
2. Выполняется `metric` (если задан) и при `save: true` сохраняются его значения.
3. Выполняются `thresholds`.
4. Если нарушен хотя бы один порог с `ERROR`, выбрасывается `TaskFailedException`.
   Если нарушены только `WARN`, статус метрики становится `FAILED`, но пайплайн
   продолжается.

`save: true` сохраняет измеренные значения (не только статус) через
`MetricDQValueProducerService` — для истории и трендов.

## 3. Формат sparkSQL-тестсета

Тестсет — это SQL-запрос, который возвращает таблицу. Для порогов с `expectation`
обязательна колонка, указанная в `valueColumn` (по умолчанию `value`). Рекомендуемый
формат — две колонки:

```sql
select 'commission_not_numeric' as subMetricName
     , count(1) as value
from {{ refCat(targetDataSetKeyName) }}
where reg_date_time >= timestamp '{{ intervalStartDateTime }}'
  and reg_date_time <  timestamp '{{ intervalEndDateTime }}'
  and (commission is null
       or not (cast(commission as string) rlike '^-?[0-9]+([.][0-9]+)?$'))
```

* `subMetricName` — имя измерения; используется при сохранении значений и в
  диагностике.
* `value` — измеренное число. Движок приводит его к `BigDecimal`.
* Если `subMetricName` нет, движок подставляет ключ тестсета или порога
  (для `metric` — `metric`).
* Для `integrity`-тестсетов движок дополнительно нормализует `severity`
  (`ERROR`/`WARN`).

### Контекст шаблонов

В SQL-скриптах доступны глобальные переменные из `SourceConfDTO` и `ScheduledTaskDTO`,
а также вспомогательные ключи:

| Переменная | Значение |
|---|---|
| `targetDataSetKeyName` | ключ целевого датасета |
| `dataSets` | карта датасетов, доступных в прогоне |
| `intervalStartDateTime`, `intervalEndDateTime` | границы интервала из расписания |
| `intervalStartDate`, `intervalEndDate` | те же границы в формате `YYYY-MM-DD` |
| `qualityMetricsConfKeyName` | ключ текущего QM-конфига |
| `refCat('<dataSetKeyName>')` | платформенно-корректная ссылка на таблицу датасета |

`refCat` — функция Jinja, а не поле контекста: используйте её для всех обращений к
таблицам, чтобы шаблон работал и в Spark/Iceberg, и во внешней БД.

Для integrity-проверок дополнительно доступны:

| Переменная | Значение |
|---|---|
| `constraintName` | имя ограничения |
| `constraint` | объект ограничения (`columns`, `checkExpr`, `reference`, ...) |
| `constraintColumns` | список колонок ограничения |
| `constraintSeverity` | `ERROR` или `WARN` для данного типа ограничения |
| `referenceDataSetKeyName`, `referenceConstraintName` | ссылка FK |
| `sourceColumns`, `referenceColumns` | колонки источника и справочника |
| `sourceColumnTypes`, `referenceColumnTypes` | типы колонок из `columnSchema` |
| `sourceCastTypes` | типы, к которым нужно привести колонки источника |
| `foreignTypeMismatch`, `foreignTypeMismatchMessage` | признак и текст несовпадения типов FK |

## 4. Формат expectation (порог)

`expectation` — необязательное поле порога. Если его нет, сохраняется прежнее
поведение: **любая непустая выборка означает нарушение** (`count() > 0`). Это
гарантирует обратную совместимость существующих QM-конфигов.

Если `expectation` есть, движок:

1. выполняет тестсет;
2. берёт значение из колонки `valueColumn`;
3. сравнивает его с порогом через `operator`;
4. считает порог нарушенным только если сравнение не выполнено.

```json
{
  "expectation": {
    "valueColumn": "value",
    "operator": "=",
    "threshold": 0,
    "severity": "ERROR"
  }
}
```

Поля:

* `valueColumn` — имя числовой колонки результата (по умолчанию `value`).
* `operator` — один из `>`, `>=`, `<`, `<=`, `=`, `!=` (допускаются `==`, `<>`).
* `threshold` — число или объект относительного порога (см. ниже).
* `severity` — `ERROR` или `WARN`. Переопределяет `dqThresholdViolationLevel`
  конкретного QM-конфига. Регистр не важен: `error`, `ERROR`, `warn`, `WARN`
  эквивалентны. Исторический `INFO` считается неблокирующим, как `WARN`.
* Если `severity` не задан, используется `dqThresholdViolationLevel` конфига, а при
  его отсутствии — `ERROR`.

### Относительные пороги

Порог можно задать относительно значения другого тестсета или метрики **текущего
прогона**:

```json
{
  "expectation": {
    "valueColumn": "value",
    "operator": ">=",
    "threshold": {
      "relativeTo": "non_zero_count",
      "factor": 0.5
    },
    "severity": "WARN"
  }
}
```

Движок ищет baseline по ключу `relativeTo` среди значений, измеренных в этом же
прогоне:

* ключ тестсета (`non_zero_count`);
* ключ тестсета и `subMetricName` (`non_zero_count.client_count`);
* ключ QM-конфига для `metric` (например, `transaction_dds_qm`).

Baseline умножается на `factor`, после чего применяется `operator`. Если baseline
не найден, выбрасывается `TaskConfigurationException` с указанием QM, порога и
датасета. История прошлых прогонов для относительных порогов не используется —
это сознательное ограничение текущей реализации.

## 5. Severity и остановка пайплайна

| Severity | Статус метрики | Пайплайн |
|---|---|---|
| `ERROR` | `FAILED` | останавливается (`TaskFailedException`) |
| `WARN` | `FAILED` | продолжается |
| `INFO` (legacy) | `FAILED` | продолжается, как `WARN` |

Если нарушено несколько порогов, `ERROR` имеет приоритет: достаточно одного
`ERROR`, чтобы остановить пайплайн.

Для integrity-проверок severity задаётся типом ограничения:

| Проверка | Severity |
|---|---|
| Primary key | `ERROR` |
| Unique key | `ERROR` |
| Check constraint | `ERROR` |
| Nullable column | `ERROR` |
| Foreign key | `WARN` |
| Schema drift | `WARN` |

FK имеет `WARN`, потому что «осиротевшие» записи на стыке загрузок допустимы в
пределах tolerance. При необходимости правило можно ужесточить через собственный
порог/`expectation`.

## 6. Integrity-проверки

Integrity-тестсет (`"type": "integrity"`) не требует SQL-скриптов: движок сам
обходит ограничения целевого датасета и колонки, для которых задан `columnSchema`.
Результат приводится к общему виду `subMetricName / value / severity`, поэтому
integrity-проверки можно объединять обычным порогом:

```json
{
  "metric": {
    "type": "integrity",
    "description": "PK, FK, nullable columns and schema drift"
  },
  "thresholds": {
    "integrity_violations_th": {
      "type": "sparkSQL",
      "scripts": [{ "key": "dq.integrity_violations_th.sql" }]
    }
  }
}
```

`dq.integrity_violations_th.sql` читает текущую метрику по имени QM-конфига:

```sql
select *
from {{ qualityMetricsConfKeyName }}
where value > 0
```

Порог без `expectation` считает нарушением любую строку. Severity берётся из
колонки `severity` результата: если хотя бы одна строка имеет `ERROR`, порог
блокирующий; иначе — `WARN`.

### FK и несовпадение типов

Перед выполнением FK-проверки движок сравнивает типы колонок источника и
справочника по `columnSchema`. При несовпадении выводится явная диагностика, а не
«Spark job failed»:

```
FK transaction_processing_client_fk: type mismatch — source client_id (string) vs reference client_processing.id (integer). Constraint transaction_processing_client_fk (foreign), dataset transaction_dds, column(s) client_id: ...
```

Стандартный шаблон FK приводит колонки источника к типам справочника
(`sourceCastTypes`), чтобы проверка работала при совместимых, но разных типах
(`varchar`/`string`, `int4`/`integer`). Если приведение невозможно, исправьте
`columnSchema` или переопределите шаблон.

### Переопределение шаблонов под БД

Шаблоны integrity-проверок лежат в `sql-scripts/sql-template-<dbtype>/`:

* `primaryKeyCheckIntegrity.sql`
* `foreignKeyCheckIntegrity.sql`
* `uniqueKeyCheckIntegrity.sql`
* `checkConstraintCheckIntegrity.sql`
* `columnNonNullCheckIntegrity.sql`

Например, шаблон FK для Spark/Iceberg:

```sql
select '{{ constraintName }}' as subMetricName
     , count(1) as value
     , '{{ constraintSeverity }}' as severity
from (
  select {% for c in sourceColumns %}cast(src.`{{ c }}` as {{ sourceCastTypes[loop.index0] }}) as `{{ c }}`{% if not loop.last %}, {% endif %}{% endfor %}
  from {{ refCat(targetDataSetKeyName) }} src
) src_values
left anti join (
  select {% for c in referenceColumns %}refs.`{{ c }}` as `{{ c }}`{% if not loop.last %}, {% endif %}{% endfor %}
  from {{ refCat(referenceDataSetKeyName) }} refs
) ref_values
  on {% for c in sourceColumns %}src_values.`{{ c }}` = ref_values.`{{ referenceColumns[loop.index0] }}`{% if not loop.last %} and {% endif %}{% endfor %}
```

Для PostgreSQL используется `NOT EXISTS` и кавычки-идентификаторы:

```sql
where not exists (
  select 1
  from {{ refCat(referenceDataSetKeyName) }} refs
  where {% for c in sourceColumns %}src_values."{{ c }}" = refs."{{ referenceColumns[loop.index0] }}"{% if not loop.last %} and {% endif %}{% endfor %}
)
```

Правила переопределения:

* шаблон обязан вернуть колонку `value`;
* `subMetricName` и `severity` опциональны — движок подставит имя ограничения и
  severity по типу;
* используйте `refCat(...)`, а не жёсткие имена схем;
* проверяйте `foreignTypeMismatch`, если для вашей БД нужна особая обработка типов.

## 7. Диагностика ошибок

* Ошибка SQL-тестсета содержит QM-ключ, ключ тестсета, датасет и исходное сообщение:
  `QM transaction_dds_qm, testSet commission_numeric_th, dataset transaction_dds: ...`.
* Ошибка рендеринга шаблона оборачивается в ту же диагностику.
* Ошибка integrity-проверки содержит имя constraint’а, тип, датасет и колонки:
  `Constraint transaction_processing_client_fk (foreign), dataset transaction_dds, column(s) client_id: ...`.
* Отрендеренный SQL пишется в лог на уровне `DEBUG`, чтобы не засорять прод-логи.
* Неизвестный `operator`, отсутствующий `valueColumn` или недоступный baseline
  относительного порога — это `TaskConfigurationException` с контекстом, а не
  невнятное падение Spark.

## 8. Эталонные проверки (demo)

Эталонные QM-конфиги лежат в `demo/conf/quality/metrics/`; SQL-скрипты — в
`demo/conf/sql-scripts/dq/`. Те же конфиги есть в `yarina-config-svc/demo/quality-metrics/`
и в GitOps-наборе `demo/conf_git/`, но набор включённых проверок отличается:

* в `demo/conf` (и `demo/conf_git`) эталонный `transaction_dds_qm` содержит
  legacy-порог и проверки C1/C3/C4;
* в `yarina-config-svc/demo` исходный `transaction_dds_qm` оставлен legacy-конфигом
  для регрессии обратной совместимости, а гибридные пороги собраны в
  `transaction_dds_qm_hybrid`.

### C1. Валидность

* `transaction_dds_qm` → `commission_numeric_th`, `provider_id_format_th`,
  `reg_date_time_range_th` (в `yarina-config-svc/demo` — `transaction_dds_qm_hybrid`).
* Скрипты: `dq.commission_is_numeric.sql`, `dq.provider_id_format.sql`,
  `dq.reg_date_time_range.sql`.
* Порог: `= 0`, `ERROR`.

### C2. Согласованность «агрегат vs факт»

* `aggregation_pay_per_client_daily_mart_qm` → `sum_amount_reconciliation_th`,
  `sum_commission_reconciliation_th`.
* Скрипты: `dq.mart_sum_amount_reconciliation.sql`,
  `dq.mart_sum_commission_reconciliation.sql`.
* Порог: `= 0`, `ERROR`; сравнение с допуском `0.01`.

### C3. Согласованность денормализации

* `transaction_dds_qm` → `client_name_consistency_th` (в
  `yarina-config-svc/demo` — `transaction_dds_qm_hybrid`).
* Скрипт: `dq.client_name_consistency.sql`.
* Порог: `= 0`, `WARN` — расхождение денормализации является сигналом, а не
  остановкой.

### C4. Аномалия объёма

* `transaction_dds_qm` → `volume_anomaly_th`; отдельный
  `transaction_dds_qm_hybrid` в `yarina-config-svc/demo` содержит только
  гибридные пороги.
* Использует `dq.non_zero_count.sql` как baseline `non_zero_count`.
* Порог: `>=` `{ "relativeTo": "non_zero_count", "factor": 0.5 }`, `WARN`.
* В демо baseline — тот же счётчик текущего прогона, поэтому порог демонстрирует
  механизм, а не ловит реальную аномалию. Для рабочего правила объявите отдельный
  тестсет/метрику с ожидаемым объёмом (например, из справочника ожидаемых
  объёмов) и сошлитесь на него в `relativeTo`.

### C5. Schema drift

* Включён в `transaction_dds_qm_const`, `client_processing_qm_const` и
  `aggregation_pay_per_client_daily_mart_qm_const` как часть integrity-проверки.
* Движок сравнивает `columnSchema` с фактическими колонками таблицы и возвращает
  число отсутствующих колонок (`schema_drift`), severity `WARN`.
* Альтернатива для не-Spark БД — `dq.schema_drift.sql` (пример через
  `describe table`/`information_schema`).

### Интеграционные проверки

* `transaction_dds_qm` использует legacy-порог `non_zero_count_th` **без**
  `expectation` — это регрессия обратной совместимости.
* `transaction_dds_qm_hybrid` содержит только пороги с `expectation` (абсолютные и
  относительный).
* `transaction_dds_qm_const` проверяет PK, FK, nullable и schema drift.
* `client_processing_qm_const` — PK/UK/nullable/schema drift.
* `aggregation_pay_per_client_daily_mart_qm_const` — PK/nullable/schema drift.

## 9. Чек-лист нового тестсета

1. Определите измерение и правило: что считаем нарушением.
2. Напишите SQL с `subMetricName` и `value`, используя `refCat` и интервалы.
3. Проверьте запрос вручную на исторических данных.
4. Добавьте `expectation` с оператором, порогом и severity.
5. Для аномалий объёма используйте относительный порог от уже объявленного
   тестсета/метрики.
6. Для физических ограничений используйте `type: integrity` и отдельный
   integrity-порог.
7. Убедитесь, что `save: true`, если значения нужны для истории и трендов.

## 10. Ограничения текущей реализации

* Относительные пороги используют только значения текущего прогона; истории
  прошлых запусков нет.
* Проверки внешних справочников (точность reference data) не входят в DQ-движок.
* Нечёткие дубликаты (fuzzy duplicates) не поддерживаются.
* Идемпотентность загрузки и восстановление после сбоев — ответственность
  пайплайна, а не DQ-проверок.
* Метрики хранятся в рамках текущего прогона и отправляются в Kafka; отдельного
  хранилища истории движок не создаёт.
