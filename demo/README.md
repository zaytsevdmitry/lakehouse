# Демонстрационная сборка 
## Подготовка
В idea :
- обеспечить наличие java 17
- Установить maven (mvn)
- Установить docker и docker compose


Перед сборкой нужно убедится что установлены java 17 по умолчанию и mvn

> \[ERROR\] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.13.0:compile (default-compile) on project lakehouse-common: Fatal error compiling: error: release version 17 not supported -> [Help 1]

```
@localhost:~/projects/my/lakehouse/buld> java -version
openjdk version "17.0.13" 2024-10-15
OpenJDK Runtime Environment Temurin-17.0.13+11 (build 17.0.13+11)
OpenJDK 64-Bit Server VM Temurin-17.0.13+11 (build 17.0.13+11, mixed mode, sharing)
```
Если это не так, то нужно добавить команды в терминал

```
export JAVA_HOME=[путь к папке с установленной java17]
export PATH=$JAVA_HOME/bin/:$PATH
```
Снова проверить версию > java -version
ожидается что в выводе будет java 17

## Сборка образа
Файл build.bash запускает сборку проекта, образа и раскладывает артефакты по папкам

## Запуск сервисов
Перейти в терминале в корне проекта в каталог demo. Там расположен файл docker-compose.yml
Выполнить команду

```
docker compose down; docker compose up --build
```

Возможны ошибки о том, что контейнеры которые должны быть запущены уже существуют. Нужно убедиться, что они действительно не нужны и удалить их.

> Error response from daemon: Conflict. The container name "/broker" is already in use by container "47230bbef2717dc571455f72bec3b4e3be2636d340e8dffac4c2d7e1cd4c1f5a". You have to remove (or rename) that container to be able to reuse that name.

``` 
docker container rm broker 
docker container rm db
docker container rm task-executor-svc-1
docker container rm task-executor-svc-2 
docker container rm task-executor-svc-3 
docker container rm conf-svc 
docker container rm scheduler-svc 
```

## Загрузка демонстрационной конфигурации
Перейти в терминале в корне проекта в каталог demo/conf.
Выполнить файл load.bash
Он загрузит демонстрационные данные в сервис конфигурации. Через несколько секунд после этого сервис исполнитель начнет выполнять демонстрационные задачи

## Сценарий демонстрации
Предположим есть 
- два источника
- объединяющая источники детальная трансформация 
- две разные агрегации на основе детальной трансформации

Наполнение данными источников, производится в одном том же расписании, что и трансформация по какой-то, нужной кому-то логике. 
Конфигурация содержит описания
-  двух мест хранения данных бд Postgres и файловая система [datastores](conf/datastores).
-  пяти датасетов [datasets](conf/datasets) и скриптов описывающих трансформацию данных [dataset-sql-model](conf/dataset-sql-model)
-  двух шаблонов сценариев [scenario-act-templates](conf/scenario-act-templates):
  - [default](conf/scenario-act-templates/default.json) применяется в расписаниях [regular](conf/schedules/regular.json) и [initial](conf/schedules/initial.json). Обслуживает логику Spark задач
  - [source.json](conf/scenario-act-templates/source.json) применяется в расписаниях [generateSource](conf/schedules/generateSource.json) и [generateSourceDict](conf/schedules/generateSourceDict.json). Обслуживает логику jdbc задач
- расписаний, сценарий которого применяет шаблоны последовательностей задач.
  - [generateSource](conf/schedules/generateSource.json) формирует данные в таблице платежных транзакций [transaction_processing](conf/datasets/transaction_processing.json) в postgres [processingdb](conf/datastores/processingdb.json)
  - [generateSourceDict](conf/schedules/generateSourceDict.json) просто перезаписывает справочник клиентов [client_processing](conf/datasets/client_processing.json) в postgres [processingdb](conf/datastores/processingdb.json)
  - [regular](conf/schedules/regular.json) имитирует ежедневное соединение двух таблиц [transaction_dds.sql](conf/dataset-sql-model/transaction_dds.sql) из расписаний выше в одну на spark [transaction_dds](conf/datasets/transaction_dds.json) сохраняя на диск [lakehousestorage](conf/datastores/lakehousestorage.json)
    - transaction_dds послужит источником [aggregation_pay_per_client_total_mart.sql](conf/dataset-sql-model/aggregation_pay_per_client_total_mart.sql) для витрины [aggregation_pay_per_client_total_mart.json](conf/datasets/aggregation_pay_per_client_total_mart.json) которая будет сохранена в [lakehousestorage](conf/datastores/lakehousestorage.json)
    - transaction_dds послужит источником [aggregation_pay_per_client_daily_mart.sql](conf/dataset-sql-model/aggregation_pay_per_client_daily_mart.sql) для витрины [aggregation_pay_per_client_daily_mart.json](conf/datasets/aggregation_pay_per_client_daily_mart.json) которая будет сохранена в [lakehousestorage](conf/datastores/lakehousestorage.json)
  - [initial](conf/schedules/initial.json) ежемесячная версия [regular](conf/schedules/regular.json). Будет заблокирована сбором generateSource и generateSourceDict до тех пор, пока они не будут собраны за первый месяц 
- проекта (пространства имен, он один [demo](conf/projects/demo.json))
-  двух групп исполнителей [taskexecutionservicegroups](conf/taskexecutionservicegroups)
  - [state-exe](conf/taskexecutionservicegroups/state-exe.json) для работы с задачами "состояний" 1 экземпляр
  - [default](conf/taskexecutionservicegroups/default.json) обрабатывает задачи с данными 2 конкурирующих экземпляра

### Источники собираются каждый день, трансформации тоже каждый день
![regular.png](uml/regular.png)




### Источники собираются каждый день, трансформации каждый месяц
![initial.png](uml/initial.png)