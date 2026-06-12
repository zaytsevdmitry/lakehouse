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

```commandline
minikube start --cpus 4 --memory 8192 --registry-mirror=https://dh-mirror.gitverse.ru
```

>Сборка тестировалась с применением ближайшего registry-mirror. Можно указать любой либо убрать и использовать настройку по умолчанию.

# Установка
```commandline
sh install.bash
```
В результате выполнения команды : 
- образы lakehouse* перегрузятся из локального репозитория в minikube
- соберется helm chart
- создано пространство имен lakehouse-management
- учетные записи 
  - lakehouse-app-sa - используется lakehouse-management-task-executor-service для отправки задач spark-operator
  - spark-driver-sa  - используется spark-driver для взаимодействия с оператором, создаются экзекуторы
- запустятся все сервисы
# После установки


## Проброс портов из контейнеров
Для наблюдения за сервисами можно прокинуть порты контейнеров на localhost
```commandline
sh tunnels.bash
```
> В примере команда kubectl запускается через xterm. Это нужно, чтобы иметь возможность закрыть проброс порта кликом мыши в интерфейсе рабочего стола, а не искать номера процессов чтобы их завершить.
> xterm это стандартная утилита linux. В своей операционной системе можно найти аналог.

lakehouse-management-config-service 8080 нужен для загрузки конфигурации метаданных.

## Загрузка демонстрационной конфигурации метаданных

Перейти в терминале в корне проекта в каталог demo/compose/conf.
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

# Наблюдение
Просмотр списка подов

```commandline
kubectl -n lakehouse-management get pods
```
Просмотр лога task-executor
```commandline`
kubectl -n lakehouse-management logs deployment/lakehouse-management-task-executor-service`
```
Просмотр лога драйвера, который упал с ошибкой
```commandline
kubectl -n lakehouse-management get pods|grep driver| grep Error|awk '{print $1}'|xargs -r kubectl -n lakehouse-management logs
```
kubectl -n lakehouse-management get pods -o custom-columns="NAME:.metadata.name,TASK-NAME:.metadata.annotations.lakehouse-management-task"


# Де-инсталляция
## Удаление сервисов
```commandline 
sh uninstall.bash
```
## Удаление образов
```commandline
sh remove_images.bash
```

