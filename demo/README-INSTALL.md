# Демонстрационная сборка

## Подготовка

В idea :

- обеспечить наличие java 17
- Установить maven (mvn) и плагины
- Установить docker и docker compose

Перед сборкой нужно убедится что установлены java 17 по умолчанию и mvn

> \[ERROR\] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.13.0:compile (default-compile) on
> project lakehouse-common: Fatal error compiling: error: release version 17 not supported -> [Help 1]

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

Снова проверить версию
```commandline
> java -version
```
Ожидается что в выводе будет java 17

## Сборка образа

Файл build.bash запускает сборку проекта, образа и раскладывает артефакты по папкам

## Запуск сервисов

Перейти в терминале в корне проекта в каталог demo. Там расположен файл docker-compose.yml
Выполнить команду

```
docker compose down; docker compose up
```
#### Совпадения имен
Возможны ошибки о том, что контейнеры которые должны быть запущены уже существуют. Это либо контейнеры от предыдущих попыток запуска, либо одноименные контейнеры. Нужно убедиться, что они
действительно не нужны и удалить их.

> Error response from daemon: Conflict. The container name "/broker" is already in use by container "
> 47230bbef2717dc571455f72bec3b4e3be2636d340e8dffac4c2d7e1cd4c1f5a". You have to remove (or rename) that container to be
> able to reuse that name.

``` 
docker container rm broker 
docker container rm db-dev
docker container rm demo-trino-1
docker container rm hive-metastore
docker container rm spark-history
docker container rm spark-master
docker container rm spark-worker-1
docker container rm task-executor-svc-1
docker container rm task-executor-svc-2 
docker container rm task-executor-svc-3 
docker container rm task-executor-svc-4 
docker container rm conf-svc 
docker container rm scheduler-svc 
```
#### Сеть
В конфигурации определена сеть
```yaml
networks:
  lakehouse_net:
    driver: bridge
    ipam:
      config:
        - subnet: 192.1.193.0/24
```
Многие файлы конфигурации могут использовать IP адрес для указания сервера.

## Загрузка демонстрационной конфигурации

Перейти в терминале в корне проекта в каталог demo/conf.
Выполнить файл load.bash
Он загрузит демонстрационные данные в сервис конфигурации. Через несколько секунд после этого сервис исполнитель начнет
выполнять демонстрационные задачи

Если сервис конфигураций еще не доступен, скрипт "подождет" готовности сервиса 
```commandline
server is 127.0.0.1:8080/v1_0/configs
pwd is /home/dm2/IdeaProjects/lakehouse/demo/conf
Waiting Config-SVC: The request failed. Sleeping...zzZ
Retry Config-SVC
Waiting Config-SVC: The request failed. Sleeping...zzZ

```
и загрузит конфигурацию. В конце должно появиться сообщение 
```commandline
All configurations loaded
```

### Зависимость ключей в конфигурациях

![зависимость ключей в конфигурациях](../doc/entities_design/logical_entities_dependency.png)


## Ссылки
[minio](http://localhost:9001/login)  

[spark-master](http://192.1.193.40:8400/)

[spark-worker](http://192.1.193.50:8401/)

[spark-history](http://localhost:18080/)
