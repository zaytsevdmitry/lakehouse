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
-  двух мест хранения данных бд Postgres и файловая система.
-  пяти датасетов
-  расписания, сценарий которого применяет шаблоны последовательностей задач.
-  проекта (пространства имен, он один DEMO)
-  группы исполнителей (она одна, default)