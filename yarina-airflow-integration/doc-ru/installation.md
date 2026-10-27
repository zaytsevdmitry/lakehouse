# Установка (RU)

## Требования

| Компонент | Требование |
|:----------|:-----------|
| Apache Airflow | **2.2+** для `2.x/`; **3.1+** рекомендуется для `3.x/` (в 3.0.x есть ограничения `xcom_push`) |
| Python | 3.8 - 3.11 для Airflow 2.x; 3.9 - 3.12 для Airflow 3.x |
| HTTP-провайдер | `apache-airflow-providers-http` |
| Сетевой доступ | `yarina-state-svc`, `yarina-config-svc`, `yarina-task-proxy-for-spark` (порт `8090`), Keycloak realm `yarina` |

Установите провайдер в то же окружение, где работает Airflow:

```bash
pip install apache-airflow-providers-http
```

## Варианты установки

### Вариант A - копирование в `$AIRFLOW_HOME/plugins` (рекомендуется для DAG-only)

```bash
# Airflow 2.x
cp -r yarina-airflow-integration/2.x/plugins/. "$AIRFLOW_HOME/plugins/"

# Airflow 3.x
cp -r yarina-airflow-integration/3.x/plugins/. "$AIRFLOW_HOME/plugins/"
```

Завершающий `/.` копирует и скрытые файлы, например `.airflowignore`. В результате
должны появиться `$AIRFLOW_HOME/plugins/operators/...`,
`$AIRFLOW_HOME/plugins/hooks/...` и `$AIRFLOW_HOME/plugins/constants.py`.
После копирования перезапустите scheduler, DAG processor и webserver.

### Вариант B - отдельный пакет

Добавьте каталог версии (тот, что содержит `plugins/`) в `PYTHONPATH`:

```bash
export PYTHONPATH="/opt/yarina-airflow-integration/2.x:${PYTHONPATH}"
```

или установите модуль средствами вашего deployment-инструмента / Docker-образа.

## Проверка

Проверьте импорт (из `$AIRFLOW_HOME` или с каталогом версии в `PYTHONPATH`):

```bash
# Airflow 2.x
python -c "from plugins.operators.yarina_state_lock import YarinaStateLockOperator; \
           from plugins.operators.yarina_state_unlock import YarinaStateUnlockOperator; \
           from plugins.operators.yarina_state_check import YarinaStateCheckOperator; \
           from plugins.operators.yarina_spark_submit import YarinaSparkSubmitOperator; \
           print('yarina operators imported')"

# Airflow 3.x
python -c "from plugins.operators.yarina_state_check import YarinaStateCheckOperator; \
           from plugins.operators.yarina_spark_submit import YarinaSparkSubmitOperator; \
           print('ok')"
```

`airflow plugins` должен отработать без ошибок импорта. Модуль не регистрирует
`AirflowPlugin`, поэтому операторы не выводятся в этом списке - авторитетной
проверкой является импорт выше.

## Troubleshooting

| Симптом | Причина / решение |
|:--------|:------------------|
| `ModuleNotFoundError: No module named 'airflow.sdk'` | В Airflow 2.x скопирован `3.x/plugins`. Используйте `2.x/plugins`. |
| `ModuleNotFoundError: No module named 'airflow.models'` (3.x) | Используйте `3.x/plugins`: там `BaseOperator` импортируется из `airflow.sdk`. |
| `ModuleNotFoundError: No module named 'plugins'` | Запускайте Airflow из `$AIRFLOW_HOME` или добавьте в `PYTHONPATH` **родителя каталога `plugins/`**, а не сам `plugins/`. |
| `ModuleNotFoundError: No module named 'airflow.providers.http'` | `pip install apache-airflow-providers-http`. |
| Ошибка импорта `airflow.sdk` на Airflow 3.0.x | Обновитесь до 3.1+; ранние сборки Task SDK в 3.0 не полностью поддерживаются. |
| Предупреждение про `schedule` | Airflow < 2.4: замените `schedule=` на `schedule_interval=` в шаблоне DAG. |
| `AirflowException: Connection extra 'auth_url' is required` | Заполните `extra.auth_url` в Connection или включите `extra.security_enabled=false` для локальной разработки. |
| HTTP 404 от Keycloak | `auth_url` указывает на issuer. Нужен `{issuer}/protocol/openid-connect/token`. |
| HTTP 500 от state-svc | Известные NPE-баги бэкенда (null `lockSource`, null-даты). Сначала проверьте payload; в сообщении hook есть подсказка. |
| HTTP 500 от Spark-прокси при создании | Прокси или его scheduler завершились ошибкой. Тело ответа есть в исключении; смотрите логи `yarina-task-proxy-for-spark`. |
| `submission '...' не найден (404)` | Прокси не знает такой id. Оператор падает сразу, без ретраев; проверьте `submissionId` и базу прокси. |
| Сообщение о таймауте Spark | Задача может продолжать выполняться. Проверьте `GET /v1/submissions/status/{id}` вручную; оператор не убивает задачу. |

Оператор Spark намеренно не вызывает `/kill`, `/killall` или `/clear`. При таймауте
приложение остаётся в кластере; при необходимости остановите его вне Airflow.

## Тесты (опционально)

```bash
cd yarina-airflow-integration/2.x
PYTHONPATH=. python -m pytest plugins/tests -q

cd ../3.x
PYTHONPATH=. python -m pytest plugins/tests -q
```

Ожидаемый результат: `82 passed` для каждой поддерживаемой major-версии (Airflow
2.7.x и 3.1.x в CI). Запущенные прокси, Keycloak и база Airflow не нужны.
