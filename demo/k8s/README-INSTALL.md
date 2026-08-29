# Сборка

[Описание сборки образов](../../docker/readme.md)

# Подготовка
- Установить minikube 
- обновить или просто скачать kubectl v1.36.0

```commandline
 curl -LO https://dl.k8s.io/release/v1.36.0/bin/linux/amd64/kubectl
```

>Пример приведен для linux. [Тут](https://kubernetes.io/docs/tasks/tools/) можно подобрать под свою операционную систему.
Файл нужно расположить "поближе" в переменной окружения PATH. На пример $HOME/bin

```commandline
mv kubectl $HOME/bin/
```
- установить helm
  - для удобства можно добавить helm-bash-completion или другой удобный
- Запустить minikube

```shell
minikube start --cpus 4 --memory 8192 --registry-mirror=https://dh-mirror.gitverse.ru
```

>Сборка тестировалась с применением ближайшего registry-mirror. Можно указать любой либо убрать и использовать настройку по умолчанию.


# Установка
```shell
sh install.bash
```
В результате выполнения команды: 
- образы lakehouse* перегрузятся из локального репозитория в minikube  ~ 3-5 минут в зависимости от локальной системы
- соберется helm chart
- создано пространство имен lakehouse-management
- учетные записи 
  - lakehouse-app-sa - используется lakehouse-task-proxy4spark для отправки задачи посредством submit
  - spark-driver-sa  - используется spark-driver , создаются экзекуторы
- запустятся все сервисы
# После установки


## Проброс портов из контейнеров
Для наблюдения за сервисами можно прокинуть порты контейнеров на localhost
```shell
sh tunnels.bash
```
> В примере команда kubectl запускается через xterm. Это нужно, чтобы иметь возможность закрыть проброс порта кликом мыши в интерфейсе рабочего стола, а не искать номера процессов чтобы их завершить.
> xterm это стандартная утилита linux. В своей операционной системе можно найти аналог.

lakehouse-management-config-service 8080 нужен для загрузки конфигурации метаданных.

## Загрузка демонстрационной конфигурации метаданных

Перейти в терминале в корне проекта в каталог demo/k8s/conf.
Выполнить файл load.bash
Он загрузит демонстрационные данные в сервис конфигурации. Через несколько секунд после этого сервис исполнитель начнет
выполнять демонстрационные задачи

```shell
cd ./conf
sh load.sh
```

Если сервис конфигураций еще не доступен, скрипт "подождет" готовности сервиса
```
server is 127.0.0.1:8080/v1_0/configs
pwd is /home/dm2/IdeaProjects/lakehouse/demo/conf
Waiting Config-SVC: The request failed. Sleeping...zzZ
Retry Config-SVC
Waiting Config-SVC: The request failed. Sleeping...zzZ

```
и загрузит конфигурацию. В конце должно появиться сообщение
```
All configurations loaded
```

# Наблюдение
Просмотр списка подов

```shell
kubectl -n lakehouse-management get pods -o custom-columns="NAME:.metadata.name,STATUS:.status.phase,TASK-NAME:.metadata.annotations.lakehouse-management-task"

```
Просмотр лога task-executor
```shell
kubectl -n lakehouse-management logs deployment/lakehouse-management-task-executor-service
```
Просмотр task-proxy4spark
```shell
kubectl -n lakehouse-management logs deployment/lakehouse-task-proxy4spark
```

Просмотр лога драйвера, который упал с ошибкой
```shell
kubectl -n lakehouse-management get pods|grep task| grep Error|awk '{print $1}'|xargs -r kubectl -n lakehouse-management logs
```
Ускоряем работу
```shell
kubectl -n lakehouse-management scale deployment lakehouse-management-task-executor-service --replicas=4
````
## Keycloak
Сборка использует Keycloak для аутентификации. Необходимо добавить запись в `/etc/hosts` (или `%SystemRoot%\System32\drivers\etc\hosts` в Windows):

```
127.0.0.1 keycloak
```

> Без этой записи браузер не сможет перейти на страницу входа Keycloak (issuer URI = `http://keycloak:8085/realms/lakehouse`).

После проброса портов Keycloak доступен по адресам:
- Админ-консоль: http://keycloak:8085 (учетные данные: `admin` / `admin_local_password`)
- OIDC endpoint: http://keycloak:8085/realms/lakehouse

Вход в Lakehouse UI: http://localhost:8084 — перенаправит на Keycloak.

Демонстрационные пользователи (realm `lakehouse`):

| Логин | Пароль | Роль |
|:------|:-------|:-----|
| `de_view` | `de_view` | USER |
| `de_editor` | `de_editor` | ADMIN |

> Секреты клиентов по умолчанию в файлах values.yaml предназначены только для демонстрации.
> В production используйте переменные окружения `KEYCLOAK_INTERNAL_CLIENT_SECRET` и `KEYCLOAK_UI_CLIENT_SECRET`.

# Де-инсталляция
## Удаление сервисов
```shell 
sh uninstall.bash
```
## Удаление образов
```shell
sh remove_images.bash
```

