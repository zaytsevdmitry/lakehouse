# Домены конфигурации (`jarina-domain-api`)

Этот документ — **каноническое описание общей доменной модели** платформы Jarina. Ему
принадлежит модуль `jarina-domain-api`, который потребляют все сервисы, работающие с
доменами конфигурации (`jarina-config-svc`, `jarina-ui-svc`).

В модуле две части:

- **модель настроек доменов** — пакет `org.jarina.domain.settings`: `DomainSettings`
  (`@ConfigurationProperties(prefix = "jarina")`, т.е. пространство имён
  `jarina.domains.<name>.*`), `GitProperties` и `GitCredentialsSettings`. Здесь живут
  разбор, канонизация, упорядочивание и валидация доменных параметров;
- **VCS-слой** — пакет `org.jarina.domain.vcs`: транспортно-независимый интерфейс
  `VcsClient`, его реализация на JGit `GitVcsClient`, учётные данные (`GitCredentials`)
  и `DomainVcsSpec`/`DomainVcsFactory`.

Сервисы только **доставляют сырые свойства `jarina.domains.*`** и вызывают общие функции
`DomainSettings`; никаких правил разрешения они не реализуют заново. Сторонние задачи
сервисов (цикл синхронизации GitOps в `jarina-config-svc`, панель веток / workspace /
конвейер ревью в `jarina-ui-svc`) описаны в
[config-svc: Домены](../../jarina-config-svc/doc-ru/content_configuration/domains.md) и в
[ui-svc](../../jarina-ui-svc/doc-ru/readme.md#домены).

## 1. Что такое домен конфигурации

**Домен** — единица изоляции конфигурации. Домен одновременно является логической областью
метаданных и **Git-репозиторием**: у каждого домена ровно один репозиторий, он забирается
и применяется независимо, а его имя проставляется на каждый загруженный из него конструкт
как `domainKeyName`.

```
jarina.domains  ──▶  DomainSettings.orderedDomains()  ──▶  для каждого домена
                                                                fetch + diff
                                                                apply в одной транзакции
                                                                простановка domainKeyName
                                                                    │
                                                                    ▼
                                         DataSet / DataSource / Driver / Task /
                                         ScenarioActTemplate / Schedule /
                                         QualityMetricsConf / TaskExecutionServiceGroup
                                         (владение = domainKeyName, идентичность = keyName)
```

Следствия этого определения:

- Домен **не входит** в содержимое YAML. Файл конструкта никогда не содержит доменного
  поля: домен определяется репозиторием, из которого загружен файл, и всегда побеждает
  всё, что оказалось в распарсенном DTO.
- Домен — атрибут **владения**, а не часть идентичности. `keyName` остаётся **сквозным** —
  это единственный первичный ключ (`KeyEntityAbstract.keyName`, `@Id`), уникальный во всей
  конфигурации и *не* входящий в составной ключ с `domainKeyName`. Поэтому один и тот же
  `keyName` не может существовать в двух доменах: чтобы описать одну и ту же физическую
  таблицу в двух доменах, нужны два разных `keyName`.
- Домен определяет, какие фильтры и правила изоляции применяются к конструкту (см.
  [§7](#7-правила-изоляции)) и как он попадает в отчётность, но никогда — коллизию с
  другим конструктом.
- **Скрипты общие.** Скрипты (`kind: Script` / `SQLTemplate`) — только контент, они общие
  между доменами по ключу и никогда не штампуются доменом. Домен в строке `SQLTemplate`
  наследуется от `Driver` или `Task`, который на них ссылается.
- Конструкт, созданный через REST API, домена не имеет (`domainKeyName = null`).

Имена доменов канонизируются `DomainSettings.canonicalDomain(...)`: обрезка и нижний
регистр. Два настроенных домена, совпадающих без учёта регистра, — ошибка при старте.

## 2. Дерево доменов, порядок и распространение отказов

Домены образуют дерево — домен может объявить вложенные домены через
`jarina.domains.<name>.domains`, например `platform` ⊃ `processing` ⊃ `analytics`.
Разрешение живёт в `DomainSettings`:

| Функция | Назначение |
|---|---|
| `rootDomains()` | Домены верхнего уровня, отсортированные по `priority` по возрастанию |
| `nestedDomains(parent)` | Вложенные поддомены одного домена, отсортированные по `priority` по возрастанию |
| `orderedDomains()` | Плоский список в порядке зависимостей: **родитель перед детьми**, обход в глубину, каждый уровень по `priority` |
| `domainSettings(name)` | Настройки домена; поиск спускается по **всему** дереву, поэтому вложенные домены находятся и по листовому имени; `null` для неизвестного домена |
| `hasRepository(domain)` / `branch(domain)` / `buildSpec(domain, …)` | Выводимые по-доменные факты, которые используют сервисы |

Порядок: в пределах одного уровня домены сортируются по `priority` **по возрастанию**;
домен без `priority` считается равным `Integer.MAX_VALUE` и поэтому идёт **последним**;
равные приоритеты разрешаются по имени домена.

Вложенность — **конфигурационное** отношение, а не складское. Одни и те же три
репозитория можно объявить соседними или цепочкой — это выбор администратора. Единственное
поведенческое следствие вложенности — **распространение отказа**: если синхронизация домена
не удалась, всё его поддерево пропускается до успеха родителя, потому что дочерний домен
может ссылаться на конструкты родителя.

## 3. Домен активен тогда и только тогда, когда задан `git.repository-url`

У домена без `git.repository-url` (пусто/отсутствует) **нет клиента VCS**:

- `jarina-config-svc` пропускает его на каждом цикле синхронизации (его содержимое, если
  оно есть, не синхронизируется);
- `jarina-ui-svc` падает **при использовании**: любая попытка построить клиент такого
  домена завершается ошибкой `Domain '<name>': no repository URL configured (property:
  jarina.domains.<name>.git.repository-url)`.

**Legacy-фолбэка с одним репозиторием больше нет**: свойства, существовавшие до доменов, и
домен `default`, который раньше синтезировался из одного репозитория, удалены в обоих
сервисах. Объявляйте каждый репозиторий явно в `jarina.domains.<name>.git.repository-url`.

## 4. Пространство имён `jarina.domains.<name>.*`

`DomainSettings` связывает всё дерево; сервисы доставляют сырые свойства без изменений.

```yaml
jarina:
  domains:
    platform:
      priority: 0
      git:
        repository-url: git://git-server:9418/platform.git
        branch: main
        local-clone-path: /tmp/platform
        credentials:                    # по-доменные, никогда не сливаются с другим доменом
          auth-type: ssh                # ssh | token | basic (может быть выведен)
          ssh-private-key-path: /keys/platform_ed25519
      domains:                          # вложенные поддомены
        processing:
          priority: 1
          git:
            repository-url: git://git-server:9418/processing.git
            branch: main
            local-clone-path: /tmp/processing
            credentials:
              auth-type: token
              token: ${PROCESSING_GIT_TOKEN:}
    analytics:
      git:
        repository-url: git://git-server:9418/analytics.git
```

| Свойство | По умолчанию | Назначение |
|---|---|---|
| `jarina.domains.<name>.priority` | `Integer.MAX_VALUE` | Позиция в пределах одного уровня дерева (по возрастанию); без `priority` — в конец |
| `jarina.domains.<name>.git.repository-url` | *(пусто)* | Репозиторий домена: `git://`, `ssh://`, `http(s)://` или локальный путь. Пусто — домен неактивен |
| `jarina.domains.<name>.git.branch` | `main` | Отслеживаемая ветка; `DomainSettings.branch(...)` возвращает `main` для неизвестного домена |
| `jarina.domains.<name>.git.local-clone-path` | *(пусто)* | Локальный клон, принадлежащий сервису; должен быть уникален для каждого домена. `jarina-config-svc` **требует** его (`buildSpec(domain, true)`), `jarina-ui-svc` — нет |
| `jarina.domains.<name>.git.credentials.auth-type` | *(выводится)* | `ssh` \| `token` \| `basic`; см. [§5](#5-по-доменные-учётные-данные) |
| `jarina.domains.<name>.git.credentials.ssh-private-key-path` | *(пусто)* | Приватный ключ OpenSSH для `auth-type: ssh` |
| `jarina.domains.<name>.git.credentials.username` | *(пусто)* | Имя пользователя для `basic` (для `token` — необязательно) |
| `jarina.domains.<name>.git.credentials.token` | *(пусто)* | Токен доступа для `token` |
| `jarina.domains.<name>.git.credentials.password` | *(пусто)* | Пароль для `basic` |
| `jarina.domains.<name>.domains` | - | Вложенные поддомены |

Переменные окружения строятся по relaxed binding из того же пространства имён, например
`JARINA_DOMAINS_PLATFORM_GIT_REPOSITORY_URL`, `JARINA_DOMAINS_PLATFORM_GIT_BRANCH`,
`JARINA_DOMAINS_PLATFORM_GIT_LOCAL_CLONE_PATH`,
`JARINA_DOMAINS_PLATFORM_GIT_CREDENTIALS_SSH_PRIVATE_KEY_PATH` (этот шаблон используется в
поставляемом `application.yml` обоих сервисов).

## 5. По-доменные учётные данные

Учётные данные задаются **по доменам** в `jarina.domains.<name>.git.credentials.*`:

- **глобального/системного аккаунта больше нет**: не существует ни
  `jarina.config.vcs.system-account`, ни `jarina.modeller.vcs-system-account`;
- учётные данные **никогда не сливаются** — ни между доменами, ни с каким-либо блоком по
  умолчанию. Домен либо несёт собственные учётные данные, либо обращается к своему
  репозиторию **анонимно**;
- полностью пустой блок учётных данных всегда валиден и означает анонимный доступ.

`auth-type` вычисляется `GitCredentialsSettings.effectiveAuthType()`:

1. явно заданный `auth-type` имеет приоритет;
2. иначе `ssh`, если задан `ssh-private-key-path`;
3. иначе `token`, если задано что-то из `token` / `username` / `password`;
4. иначе auth-type отсутствует вовсе → анонимный доступ.

Валидация (`GitCredentialsSettings.validate(...)`) выполняется при построении клиента и
формулирует проблему по префиксу свойства:

| Объявленный `auth-type` | Требование |
|---|---|
| `ssh` | обязан быть задан `ssh-private-key-path` |
| `token` | обязан быть задан `token` |
| `basic` | обязаны быть заданы `username` **и** `password` |

Соответствие транспорту (`GitCredentials.forDomain(...)`): `ssh` устанавливает Apache
SSH-сессию для ключа домена (и восстанавливает предыдущую при закрытии, чтобы ключ одного
домена не утекал в другой), `basic` шлёт HTTP Basic, `token` шлёт токен как HTTP-пароль
(пользователь `oauth2`, если `username` не задан).

## 6. Такт планировщика — отдельная забота

Модель доменов ничего не говорит о **том, когда** синхронизируется домен. Такт живёт только
в `jarina-config-svc`, под собственным префиксом (см.
[appconf/service_configuration.md](../../jarina-config-svc/doc-ru/appconf/service_configuration.md)):

| Свойство | По умолчанию | Назначение |
|---|---|---|
| `jarina.config.vcs.sync.enabled` | `false` | Включает бин планировщика VCS |
| `jarina.config.vcs.sync.interval-ms` | `30000` | Период цикла |
| `jarina.config.vcs.sync.initial-delay-ms` | `10000` | Задержка первого цикла после старта |

Цикл **глобальный**, а не посегментный: на каждом обходе домены идут в порядке
`DomainSettings.orderedDomains()`, и каждый репозиторий синхронизируется независимо.

## 7. Правила изоляции

Реализуются `jarina-config-svc` поверх общей модели:

- `Schedule` может ссылаться **только** на датасеты своего домена. Ссылка на датасет другого
  домена — или на созданный вручную датасет, не принадлежащий ни одному домену, —
  проваливает весь коммит с `DataSetDomainConflictException`, потому что применимость
  коммита не должна зависеть от репозитория, которым коммит не управляет.
- REST API отклоняет обновление, которое перезаписало бы конструкт одного домена
  конструктом другого: `DomainConflictException` → `409 Conflict`. Конструкт, у которого
  сохранённый или входящий домен равен `null`, не проверяется.
- Удаление файла из репозитория домена **не удаляет** конструкт: сервис только сбрасывает
  `isVcsManaged`, а удаляет конструкт пользователь через REST API.

## 8. Наблюдаемость

Обе таблицы журналов VCS несут домен, поэтому синхронизацию можно отнести к репозиторию, из
которого она пришла:

| Таблица / эндпоинт | Колонка / параметр |
|---|---|
| `vcs_sync_log` | `domain_key_name` |
| `vcs_object_log` | `domain_key_name` |
| `GET /v1_0/configs/vcs/logs` | `domainKeyName` (необязательный фильтр; обязательны `from`/`to`) |
| `GET /v1_0/configs/vcs/objectlogs` | `domainKeyName` (необязательный фильтр; обязателен `commitId` либо обе границы `from` и `to`) |

`domainKeyName` входит и в REST-представление доменных DTO, и используется `jarina-ui-svc`
как read-only-фильтр. Большинство доменных DTO выставляют `domainKeyName` — `DataSet`,
`DataSource`, `Driver`, `Task`, `ScenarioActTemplate`, `Schedule`, `QualityMetricsConf`,
`TaskExecutionServiceGroup` — и для этих видов домен является атрибутом владения, а не
частью идентичности объекта.

## 9. Где что лежит

| Забота | Владелец |
|---|---|
| Дерево доменов, порядок, канонизация, разбор и валидация `git.*` и `git.credentials.*` | `jarina-domain-api` — `org.jarina.domain.settings` |
| Абстракция `VcsClient`, реализация на JGit, по-доменные учётные данные | `jarina-domain-api` — `org.jarina.domain.vcs` |
| Цикл синхронизации, сбор набора изменений, разбор YAML, простановка `domainKeyName`, правила изоляции, `vcs_sync_log`/`vcs_object_log` | `jarina-config-svc` — [Подсистема VCS для разработчиков](../../jarina-config-svc/doc-ru/vcs/cvs_for_developers.md) |
| Панель веток, workspace'ы `(домен, ветка)`, ревью по доменам, фильтр журналов VCS | `jarina-ui-svc` — [readme](../../jarina-ui-svc/doc-ru/readme.md#домены) |
| Проверка домена при публикации (`TaskExecutionServiceGroup.allowedDomains`) | `jarina-scheduler-svc` |

Дополнительно:
[config-svc: Домены](../../jarina-config-svc/doc-ru/content_configuration/domains.md),
[Руководство пользователя git-расширения](../../jarina-config-svc/doc-ru/vcs/git_extension_user_guide.md),
[ui-svc: Домены](../../jarina-ui-svc/doc-ru/readme.md#домены),
[English version](../../jarina-domain-api/doc/readme.md).
