# Работа с конфигурационным репозиторием git-server (config-repo)

Демо-стек подсистемы GitOps/CVS: `lakehouse-config-svc` по расписанию подтягивает
ветку `main` конфигурационного репозитория (`git://git-server:9418/config-repo.git`),
вычисляет разницу с последним успешно применённым коммитом и применяет изменения
в базу данных одной транзакцией. Источником истины для git-потока является
YAML-зеркало `demo/compose/conf_git/` — оно монтируется в контейнер `git-server`,
и при каждом старте контейнера его изменения коммитятся в `main`.

> JSON-файлы в `demo/compose/conf/` используются REST-загрузчиком `load.sh`.
> Для git-потока править нужно YAML-файлы в `demo/compose/conf_git/`. Чтобы оба
> потока не расходились, при необходимости переносите правку и в `conf/`.

## Пример: отключить расписание generateSource

Отключим периодическое расписание `generateSource`, переведя поле `enabled`
из `true` в `false` в файле `schedules/generateSource.yaml`.

### Шаг 1. Отредактируйте YAML

Файл: `demo/compose/conf_git/schedules/generateSource.yaml`.

```yaml
kind: Schedule
keyName: generateSource
...
enabled: true   # → замените на false
...
```

```yaml
enabled: false
```

### Шаг 2. Перезапустите git-server, чтобы изменения попали в main

```bash
docker compose restart git-server
```

При старте `git-server` заново импортирует смонтированную директорию `/conf`
(`conf_git`), зафиксирует только изменения и запушит их в ветку `main`:

```bash
docker compose logs --tail 10 git-server
```

```text
[git-server] Repository /srv/git/config-repo.git already initialized on branch main
[git-server] Importing declarative configuration from /conf
[main <хэш>] Update of lakehouse declarative configuration
[git-server] Starting git daemon on :9418
```

Если изменений нет, появится строка `No configuration changes, nothing to commit`.

### Шаг 3. Дождитесь синхронизации lakehouse-config-svc

Сервис синхронизируется по расписанию `lakehouse.config.cvs.git.sync.interval-ms`
(по умолчанию `30000` мс, задержка первого цикла `10000` мс). Обычно достаточно
подождать 30–45 секунд.

```bash
docker compose logs --since 1m lakehouse-config-svc | grep "applied successfully"
```

```text
o.l.c.cvs.service.GitOpsSynchronizer: Configuration commit <хэш> applied successfully
```

### Шаг 4. Проверьте результат в базе данных

```bash
docker compose exec db-dev psql -U postgresUser -d postgresDB \
  -c "set search_path=lakehouse_config" \
  -c "select key_name, enabled from schedule where key_name='generateSource';"
```

```text
    key_name    | enabled
----------------+---------
 generateSource | f
```

История синхронизации — в таблице `cvs_sync_log`, детализация по объектам — в `cvs_object_log`:

```bash
docker compose exec db-dev psql -U postgresUser -d postgresDB \
  -c "set search_path=lakehouse_config" \
  -c "select id, status, commit_id from cvs_sync_log order by id;" \
  -c "select id, date_time_rec, kind, object_name, file_path, commit_id from cvs_object_log order by id;"
```

## Как вернуть изменение обратно

1. Верните `enabled: true` в `demo/compose/conf_git/schedules/generateSource.yaml`.
2. Перезапустите git-server: `docker compose restart git-server`.
3. Дождитесь следующего цикла синхронизации и проверьте, что в таблице
   `schedule` снова `t`, а в `cvs_sync_log` появился новый `SUCCESS`.

Каждое изменение формирует отдельный коммит поверх существующей истории —
история конфигурации в репозитории сохраняется полностью.

## Что происходит при удалении файла

При удалении YAML-файла из репозитория сервис **не удаляет** соответствующий
конструкт из базы — он лишь сбрасывает флаг `is_cvs_managed` на сущности
(`= false`). Сам объект остаётся в базе, и уже пользователь может удалить его
через REST API (UI).

> Учтите: зеркалирование `conf_git` копирует только наличествующие файлы
> (`cp -a`), поэтому удаление файла из `conf_git` само по себе не убирает его из
> репозитория. Чтобы удалить файл из `main`, удалите коммит напрямую в репозиторий
> (см. «Клонирование репозитория» ниже) или удалите файл из `conf_git` и очистите
> его из ветки `main` коммитом из клона. После удаления и ручной правки дождитесь
> следующего цикла синхронизации.

## Почему REST API отклоняет правку управляемых объектов

Пока на сущности стоит `isCvsManaged=true` (объект загружен из git), любой
`POST`/`PUT`/`DELETE` через REST API отклоняется ответом `409 Conflict`
(`CvsManagedException`). Сначала удалите файл объекта из репозитория, дождитесь
синхронизации (флаг станет `false`), и только после этого меняйте или удаляйте
объект через REST API.

## Справочно: клонирование репозитория

`git daemon` принимает запросы по `git://` и на чтение (`upload-pack`), и на запись
(`receive-pack`), но только внутри сети стека. Клонировать можно, например, так:

```bash
docker run --rm --network compose_lakehouse_net \
  -v "$PWD:/work" --entrypoint sh alpine/git:latest -c \
  "git clone git://git-server:9418/config-repo.git /work/config-repo"
```

Имя сети стека уточните через `docker network ls` (обычно `compose_lakehouse_net`).

> Учтите: при следующем перезапуске `git-server` синхронизирует репозиторий со
> смонтированной директорией `conf_git`, поэтому правки, внесённые напрямую в
> клон, будут перезаписаны. Основной способ внесения изменений — правка
> `demo/compose/conf_git/` и перезапуск `git-server`.

Посмотреть историю коммитов в репозитории, не выходя из контейнера:

```bash
docker compose exec git-server sh -c \
  "git --git-dir=/srv/git/config-repo.git log --oneline --decorate"
```

## Что делать, если коммит применился со статусом FAILED

Если правка не проходит парсинг YAML, валидацию DTO или ограничение базы данных,
коммит фиксируется в `cvs_sync_log` со статусом `FAILED` и повторно не применяется.
В этом случае:

1. Исправьте файл в `demo/compose/conf_git/`.
2. Перезапустите git-server, чтобы создать новый коммит с исправлением:
   `docker compose restart git-server`.
3. Дождитесь цикла синхронизации — исправленный коммит применится как отдельный
   `SUCCESS`.