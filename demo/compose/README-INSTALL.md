# Сборка

[Описание сборки образов](../../docker/readme.md)

# Запуск сервисов

Перейти в терминале в корне проекта в каталог demo/compose. Там расположен файл docker-compose.yml
Выполнить команду

```shell
docker compose up -d
```

### Настройка хост машины
Требуется строка в файле /etc/hosts хоста 
```
127.0.0.1 keycloak.lakehouse

```
### Совпадения имен
Возможны ошибки о том, что контейнеры которые должны быть запущены уже существуют. Это либо контейнеры от предыдущих попыток запуска, либо одноименные контейнеры. Нужно убедиться, что они
действительно не нужны и удалить их.

> Error response from daemon: Conflict. The container name "/broker" is already in use by container "
> 47230bbef2717dc571455f72bec3b4e3be2636d340e8dffac4c2d7e1cd4c1f5a". You have to remove (or rename) that container to be
> able to reuse that name.

```shell
docker container rm broker 
docker container rm conf-svc 
docker container rm db-dev
docker container rm demo-trino-1
docker container rm compose-trino-1
docker container rm hive-metastore
docker container rm minio-dev 
docker container rm scheduler-svc
docker container rm spark-history
docker container rm spark-master
docker container rm spark-worker-1
docker container rm state-svc
docker container rm task-executor-svc-1
docker container rm task-executor-svc-2 
docker container rm task-executor-svc-3 
docker container rm lakehouse-task-proxy4spark

```
#### Сеть
В конфигурации определена сеть
```yaml
networks:
  lakehouse_net:
    driver: bridge
    ipam:
      config:
        - subnet: 172.20.193.0/24
```
Многие файлы конфигурации могут использовать IP адрес для указания сервера.

# Загрузка демонстрационной конфигурации
### Rest-api вариант
> Приоритетно загружается CVS-git вариант, который прогрузит всю конфигурацию сам и вам ничего делать не нужно. Он заблокирует работу rest-api 

### CVS-git вариант
В этой сборке он работает по умолчанию и соответственно блокирует rest-api вариант 
Все настройки нужно производить в папке conf_git. Затем пересоздать сервис. В процессе создания сервис сам положит в репозиторий папку conf_git
Подробнее тут git-server-config-repo.md

### Зависимость ключей в конфигурациях

![зависимость ключей в конфигурациях](../../doc-ru/entities_design/logical_entities_dependency.png)


## Ссылки
[minio](http://localhost:9001/login)  

[spark-master](http://192.1.193.40:8400/)

[spark-worker](http://192.1.193.50:8401/)

[spark-history](http://localhost:18080/)

[UI](http://localhost:8084/)

## Поток управления

![flow.png](../uml/flow.png)

# Удаление 

Выполнить удаление контейнеров 

```shell
docker compose down
```

Очистить данные хранилища minio. Потребуются root привилегии тк сервис работает в контейнере под root  
```shell
bash su_cleanup.bash
```