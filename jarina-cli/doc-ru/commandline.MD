# Командная строка (jarina-cli)

## Обзор

`jarina-cli` — интерактивный клиент командной строки для управления jarina. Он выполняет операции над метаданными конфигурации (namespace, datastore, dataset, шаблоны сценариев, расписания, группы сервисов выполнения задач) через REST API `jarina-config-svc`, а также операции планировщика (просмотр запланированных задач, блокировка задач) через REST API `jarina-scheduler-svc`.

Клиент построен на Spring Boot (`CommandLineClient`, режим без веб-приложения) и использует `jarina-config-rest-client` и `jarina-scheduler-rest-client` для обращения к сервисам.

## Конфигурация

Параметры подключения задаются в `src/main/resources/application.yml`:

```yaml
jarina:
  client:
    rest:
      config:
        server:
          url: http://127.0.0.1:8080
      scheduler:
        server:
          url: http://127.0.0.1:8081
```

| Параметр | Назначение | По умолчанию |
|---|---|---|
| `jarina.client.rest.config.server.url` | URL сервиса конфигурации (`jarina-config-svc`) | `http://127.0.0.1:8080` |
| `jarina.client.rest.scheduler.server.url` | URL сервиса планировщика (`jarina-scheduler-svc`) | `http://127.0.0.1:8081` |

## Запуск

```bash
java -jar jarina-cli-<version>.jar
```

При запуске выводится приглашение `Provide the Java Scanner char input:`. Команды вводятся интерактивно; каждая команда завершается строкой, содержащей разделитель `/`.

## Формат команд

```
<команда> [<аргументы>]
/
```

Разделитель `/` ставится отдельной строкой после команды — именно после него выполняется введённая команда. Несколько пробелов подряд схлопываются в один.

## Команды

### exit

Завершает работу клиента.

```
exit
/
```

### show

Выводит данные.

```
show one <objectType> <name|id>
/
```

Выводит JSON запрошенного объекта.

```
show all <objectType>
/
```

Выводит список объектов запрошенного типа в виде таблицы.

### upload

Загружает объект конфигурации из JSON-файла в `jarina-config-svc`.

```
upload <objectType> <filePath>
/
```

Результат: HTTP-код ответа сервиса (`http code <код>`).

### download

Скачивает объект конфигурации из `jarina-config-svc` в JSON-файл.

```
download <objectType> <name|id> <filePath>
/
```

Результат: `Saved to file <filePath>`.

### delete

Удаляет объект конфигурации из `jarina-config-svc`.

```
delete <objectType> <name|id>
/
```

Результат: HTTP-код ответа сервиса (`http code <код>`).

### lock

Управление блокировками задач планировщика (`jarina-scheduler-svc`).

Захват задачи в работу:

```
lock new <taskId> <taskExecutorServiceId>
/
```

Результат: JSON объекта `ScheduledTaskLockDTO` (содержит `lockId`, `serviceId`, `lastHeartBeatDateTime`).

Продление блокировки (heartbeat):

```
lock heartbeat <lockId>
/
```

Результат: HTTP-код ответа сервиса.

Освобождение блокировки с указанием результата:

```
lock release <lockId> <status>
/
```

Результат: HTTP-код ответа сервиса.

Возможные значения `<status>` (перечисление `Status.Task`):

`NEW`, `QUEUED`, `RUNNING`, `SUCCESS`, `FAILED`, `CONF_ERROR`

## Типы объектов

| `<objectType>` | Операции | Сервис | Выводимые колонки (show all) |
|---|---|---|---|
| `nameSpace` / `nameSpaces` | show one, show all, upload, download, delete | config | `name`, `description` |
| `datastore` / `datastores` | show one, show all, upload, download, delete | config | `name`, `description`, `driver` |
| `dataset` / `datasets` | show one, show all, upload, download, delete | config | `name`, `description`, `nameSpace` |
| `scenarioactemplate` / `scenarioactemplates` | show one, show all, upload, download, delete | config | `name`, `description` |
| `schedule` / `schedules` | show one, show all, upload, download, delete | config | `name`, `description`, `intervalExpression`, `startDateTime`, `stopDateTime` |
| `taskexecutionservicegroup` / `taskexecutionservicegroups` | show one, show all, upload, download, delete | config | `name`, `description` |
| `scheduledtask` / `scheduledtasks` | show one, show all | scheduler | `scheduleName`, `scheduleTargetDateTime`, `scenarioActName`, `name`, `status`, `executionModule`, `taskExecutionServiceGroupName` |
| `tasklock` / `tasklocks` | show one, show all | scheduler | `lockId`, `serviceId`, `lastHeartBeatDateTime` |

Тип объекта можно указывать как в единственном, так и во множественном числе.

Для операций `upload` / `download` / `delete` доступны только объекты конфигурации (типы, помеченные сервисом config). `scheduledtask` и `tasklock` доступны только для операций `show`.

## Примеры

Просмотр всех namespace:

```
show all namespaces
/
```

Просмотр одного dataset в формате JSON:

```
show one dataset my_dataset
/
```

Загрузка нового расписания из файла:

```
upload schedule /home/user/schedule.json
/
```

Скачивание namespace в файл:

```
download namespace my_ns /home/user/ns.json
/
```

Удаление datastore:

```
delete datastore my_store
/
```

Захват задачи с id 42 сервисом executor-1:

```
lock new 42 executor-1
/
```

Heartbeat по блокировке:

```
lock heartbeat 10
/
```

Освобождение блокировки с результатом SUCCESS:

```
lock release 10 success
/
```

## Обработка ошибок

При вводе неизвестной команды или неизвестной комбинации аргументов выбрасываются исключения:

- `UnknownCommandCombination` — неизвестная корневая команда или неизвестная подкоманда `show` / `lock`;
- `UnknownObjectTypeInCommandCombination` — неизвестный тип объекта;
- `UnApplicableCommandCombination` — неприменимая комбинация аргументов.

Сообщение об ошибке выводится в консоль, приложение продолжает работу.
