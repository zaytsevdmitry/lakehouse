# Домен

Домен конфигурации — единица изоляции `lakehouse-config-svc`. Домен одновременно является
логической областью метаданных и **Git-репозиторием**: каждый домен владеет собственным
репозиторием, забирается и применяется независимо, а его имя проставляется на каждый
загруженный из него конструкт как `domainKeyName` (см. [GitOps: декларативная конфигурация
из Git-репозиториев](../readme.md#gitops-декларативная-конфигурация-из-git-репозиториев-vcs)).

Домен **не входит** в содержимое YAML. Файл конструкта никогда не содержит доменного
поля: домен определяется репозиторием, из которого загружен файл, и всегда побеждает всё,
что оказалось в распарсенном DTO.

## Дерево доменов и порядок применения

Домены образуют дерево: домен может объявить вложенные домены через
`lakehouse.config.vcs.domains.<name>.domains`, например `platform` ⊃ `processing` ⊃
`analytics`. Дерево обходится в глубину (`LakehouseVCSProperties.orderedDomains()`), поэтому
родитель всегда применяется раньше детей. В пределах одного уровня домены сортируются по
`priority` по возрастанию; домены без `priority` считаются равными `Integer.MAX_VALUE` и
идут последними, а равные приоритеты разрешаются по имени домена.

Вложенность - это **конфигурационная**, а не складская. Одни и те же три репозитория
можно объявить соседними или цепочкой: это выбор администратора. Единственное поведенческое
следствие вложенности - распространение ошибок: если синхронизация домена не удалась, всё его
поддерево пропускается до успеха родителя, потому что дочерний домен может ссылаться на
конструкты родителя.

Домен без `git.repository-url` пропускается целиком (его содержимое, если оно есть, не
синхронизируется).

## Принадлежность домену

- `keyName` остаётся **сквозным**: это единственный первичный ключ
  (`KeyEntityAbstract.keyName`, `@Id`), уникальный во всей конфигурации и *не* входящий в
  составной ключ с `domainKeyName`. Поэтому один и тот же `keyName` не может существовать в
  двух доменах - чтобы описать одну и ту же физическую таблицу в двух доменах, нужны два
  разных `keyName`.
- Домен - это атрибут **владения**, а не часть идентичности: он определяет, из какого
  репозитория пришёл конструкт, какие фильтры и правила изоляции к нему применяются
  (`Schedule` может ссылаться только на датасеты своего домена) и как он попадёт в
  отчётность, но никогда - коллизию с другим конструктом.
- Домен проставляется при синхронизации в `GitOpsSynchronizer.stampDomainOn(...)` для всех
  управляемых видов, кроме `Script`. Скрипты (`SQLTemplate`) — только контент, они общие
  между доменами по ключу; домен в строке `SQLTemplate` наследуется от `Driver` или `Task`,
  который на них ссылается (`SQLTemplateEntitySpecifier.domainOf()`).
- Конструкт, созданный через REST API, домена не имеет (`domainKeyName = null`).

## Правила изоляции домена

- `Schedule` может ссылаться **только** на датасеты своего домена. Ссылка на датасет другого
  домена - или на созданный вручную датасет, не принадлежащий ни одному домену, - проваливает
  весь коммит с `DataSetDomainConflictException`
  (`GitOpsSynchronizer.checkScheduleDatasetDomains()`), потому что применимость коммита не должна
  зависеть от репозитория, которым коммит не управляет.
- REST API отклоняет обновление, которое перезаписало бы конструкт одного домена конструктом
  другого: `DomainConflictException` → `409 Conflict`. Проверка выполняется в
  `rejectDomainConflict(...)` сервисов `DataSet`, `DataSource`, `Driver`, `Task`,
  `ScenarioActTemplate`, `Schedule`, `QualityMetricsConf` и `TaskExecutionServiceGroup`.
  Конструкт, у которого сохранённый или входящий домен равен `null`, не проверяется.
- Удаление файла из репозитория домена не удаляет конструкт: сервис только сбрасывает
  `isVcsManaged`, а удалить конструкт должен пользователь через REST API. См.
  [флаг VCS-управления](../readme.md#флаг-управления-vcs).

## Настройка

Всё связано префиксом `lakehouse.config.vcs` в `LakehouseVCSProperties`:

```yaml
lakehouse:
  config:
    vcs:
      domains:
        platform:
          priority: 0
          git:
            repository-url: git://git-server:9418/platform.git
            branch: main
            local-clone-path: /tmp/platform
          domains:                    # вложенные поддомены
            processing:
              priority: 0
              git:
                repository-url: git://git-server:9418/processing.git
                branch: main
                local-clone-path: /tmp/processing
```

| Свойство | По умолчанию | Назначение |
|---|---|---|
| `lakehouse.config.vcs.domains.<name>.git.repository-url` | *(пусто)* | Репозиторий домена: `git://`, `ssh://`, `http(s)://` или локальный путь. Пусто — домен пропускается |
| `lakehouse.config.vcs.domains.<name>.git.branch` | `main` | Отслеживаемая ветка |
| `lakehouse.config.vcs.domains.<name>.git.local-clone-path` | *(пусто)* | Локальный клон, принадлежащий сервису. Должен быть уникален для каждого домена |
| `lakehouse.config.vcs.domains.<name>.git.private-key-path` | *(пусто)* | Приватный SSH-ключ, только для URL вида `ssh://` |
| `lakehouse.config.vcs.domains.<name>.priority` | `Integer.MAX_VALUE` | Позиция в пределах одного уровня дерева (по возрастанию) |
| `lakehouse.config.vcs.domains.<name>.domains` | - | Вложенные поддомены |

Сам планировщик глобален, а не посегментный: `lakehouse.config.vcs.git.sync.enabled`,
`...sync.interval-ms` и `...sync.initial-delay-ms` (см.
[appconf/service_configuration.md](../appconf/service_configuration.md)).
Переменные окружения домена: `LAKEHOUSE_CONFIG_GIT_<NAME>_URL`, `..._<NAME>_BRANCH`,
`..._<NAME>_CLONE_PATH`, `..._<NAME>_PRIVATE_KEY_PATH`.

### Legacy-конфигурация с одним репозиторием

Свойства, существовавшие до доменов, - `lakehouse.config.vcs.git.repository-url` / `.branch`
/ `.local-clone-path` / `.private-key-path` - продолжают работать. Если `domains` **пуст** и
`git.repository-url` не пуст, этот единственный репозиторий публикуется как один домен с
именем `default` (`LakehouseVCSProperties.rootDomains()`). Как только в `domains` есть хотя бы
одна запись, legacy-блок игнорируется - дополнительного домена `default` не возникает.

## Наблюдаемость

Обе таблицы журналов VCS несут домен, поэтому синхронизацию можно отнести к репозиторию, из
которого она пришла:

| Таблица / endpoint | Колонка / параметр |
|---|---|
| `vcs_sync_log` | `domain_key_name` |
| `vcs_object_log` | `domain_key_name` |
| `GET /v1_0/configs/vcs/logs` | `domainKeyName` (необязательный фильтр; обязательны `from`/`to`, необязательны `status`, `commitId`) |
| `GET /v1_0/configs/vcs/objectlogs` | `domainKeyName` (необязательный фильтр; обязателен `commitId` либо обе границы `from` и `to`) |

## Поля объекта

Большинство DTO конфигурации выставляют `domainKeyName` в REST-представлении - `DataSet`,
`DataSource`, `Driver`, `Task`, `ScenarioActTemplate`, `Schedule`, `QualityMetricsConf`,
`TaskExecutionServiceGroup` - и для этих видов домен является атрибутом владения, а не
частью идентичности объекта.
То же поле доходит до остальных сервисов: `lakehouse-ui-svc` использует его как
read-only-фильтр журналов VCS, а `lakehouse-scheduler-svc` сравнивает его с доменами,
разрешёнными в `TaskExecutionServiceGroup`.
