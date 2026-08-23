# lakehouse-config-svc

Сервис управления метаданными - единое хранилище всех конфигураций lakehouse. Является системой записи (source of truth) для метаданных, на основе которых остальные сервисы (scheduler-svc, task-executor-svc, state-svc) выполняют обработку данных.

## Обзор

`lakehouse-config-svc` хранит и обслуживает метаданные lakehouse:

- **Пространства имен** - логическое разделение окружений
- **Драйверы** - настройки подключения к вычислительным кластерам
- **Источники данных** - подключения к внешним хранилищам (JDBC/Spark)
- **Датасеты** - описание таблиц, колонок, ограничений
- **Расписания** - периодичность обработки данных (интервалы, сценарии актов, задачи)
- **Метрики качества данных** - проверки DQ
- **Скрипты и SQL-шаблоны** - шаблоны запросов с Jinjava-подстановками
- **Линковка данных** - связи происхождения данных (lineage)
- **TaskExecutionServiceGroups** - группы исполнителей задач

Конфигурации задаются в виде DTO, хранятся в PostgreSQL и отдаются через REST API. Изменения расписаний транслируются в Kafka (topic `schedule_effective_changes`), чтобы scheduler-svc строил актуальные инстансы расписаний.

## Архитектура

```
┌───────────────────────┐     REST (CRUD)      ┌───────────────────────────┐
│  Admin / UI / CLI     │ ────────────────────▶│   lakehouse-config-svc    │
└───────────────────────┘                      │   (REST API /v1_0/configs)│
                                               │                           │
┌───────────────────────┐     REST (чтение)    │  ┌─────────────────────┐  │
│  scheduler-svc        │ ────────────────────▶│  │ ConfigService       │  │
│  task-executor-svc    │                      │  │ (CRUD + merge DTO)  │  │
│  state-svc            │                      │  └─────────────────────┘  │
└───────────────────────┘                      │           │               │
                                               │           ▼               │
                                               │  ┌─────────────────────┐  │
                                               │  │ PostgreSQL          │  │
                                               │  │ (schema lakehouse_  │  │
                                               │  │       config)       │  │
                                               │  └─────────────────────┘  │
                                               │           │               │
                                               │           ▼  Kafka        │
                                               │  InternalScheduler        │
                                               │  schedule_effective_      │
                                               │  changes                  │
                                               └───────────────────────────┘
```

- **Controller** - REST-эндпоинты CRUD для каждого типа метаданных + compound-эндпоинты для производных объектов.
- **Service** - бизнес-логика: валидация, приведение DTO к сущностям и обратно, объединение шаблонных и частных конфигураций через `DtoMergeUtils`.
- **Repository (JPA/Hibernate)** - персистентность в PostgreSQL.
- **InternalScheduler** - периодическая отправка изменений расписаний в Kafka.
- Метаданные связаны иерархически (namespace → datasource → dataset → ...), схема зависимостей описана в [content_configuration](content_configuration/content_configuration.md).

## Модули

### lakehouse-config-svc

Spring Boot-приложение, реализующее REST API и хранилище метаданных. Точка входа: `org.lakehouse.config.LakehouseConfigApplication`.

### lakehouse-config-rest-client

Java-клиент (`ConfigRestClientApi`/`ConfigRestClientApiImpl`) для доступа к `lakehouse-config-svc` из других сервисов (scheduler-svc, task-executor-svc и др.). Выполняет типизированные запросы к эндпоинтам `/v1_0/configs/...` через `RestClientHelper`. Базовый URL задается свойством `lakehouse.client.rest.config.server.url`.

## API Endpoints

Описание структуры эндпоинтов и конфигураций метаданных находится в разделе [content_configuration](content_configuration/content_configuration.md).

## Конфигурация

Параметры приложения (datasource, JPA, настройки отправки расписаний в Kafka, health-эндпоинты) описаны в [appconf/service_configuration.md](appconf/service_configuration.md).

## Безопасность

`lakehouse-config-svc` защищен по OAuth 2.0 / OIDC с Keycloak в качестве identity provider (realm `lakehouse`). Spring Security настроен как **OAuth2 resource server**: каждый запрос должен содержать валидный JWT, выпущенный этим realm, иначе сервис возвращает `401`.

### Аутентификация

- **Запросы пользователей** (UI BFF, CLI, прямые вызовы API) - JWT проверяется по `spring.security.oauth2.resourceserver.jwt.issuer-uri`. Роли из claim'а `realm_access.roles` конвертируются в authorities `ROLE_<ИМЯ>` классом `KeycloakRoleConverter` и могут использоваться в `@PreAuthorize`.
- **Межсервисные вызовы** (`BearerTokenClientHttpRequestInterceptor`) - если запрос инициирован фоновой задачей (в `SecurityContext` нет пользовательского JWT), исходящий `RestClient` получает токен `client_credentials` через `OAuth2AuthorizedClientManager` по регистрации `keycloak-internal` (клиент `lakehouse-internal-client`) и добавляет его как `Authorization: Bearer`. При наличии пользовательского JWT он пробрасывается без изменений.
- Безопасность можно полностью отключить свойством `lakehouse.security.enabled=false` (все запросы становятся анонимными).

### Аудит

`AuditLoggingFilter` пишет одну строку на каждый запрос в логгер `AUDIT_LOG` (файл `logs/audit.log`):

```
User ID: <subject>, Username: <preferred_username>, Method: <method>, URI: <uri>, HTTP status: <status>
```

Для токенов, полученных через service account, вместо имени пользователя подставляется значение `lakehouse.security.audit.service-account-name` (по умолчанию `system`).

### Необходимые настройки

| Свойство / env | По умолчанию | Описание |
|---|---|---|
| `KEYCLOAK_ISSUER_URI` | `http://lakehouse-auth-svc:8080/realms/lakehouse` | URL realm'а Keycloak |
| `KEYCLOAK_INTERNAL_CLIENT_SECRET` | `super-secret-internal-key-987654321` | Секрет клиента `lakehouse-internal-client` |
| `lakehouse.security.enabled` | `true` | `false` полностью отключает безопасность |
| `lakehouse.security.audit.service-account-name` | `system` | Имя пользователя в аудите для service account токенов |
| `lakehouse.security.oauth2.internal-client-id` | `lakehouse-internal-client` | Клиент, идентифицирующий service account токены (claim `azp`) |
| `lakehouse.security.oauth2.client-registration-id` | `keycloak-internal` | OAuth2-регистрация, используемая интерцептором |

`spring.security.oauth2.resourceserver.jwt.issuer-uri` и блок `spring.security.oauth2.client` (регистрация `keycloak-internal`) преднастроены в `src/main/resources/application.yml`.

Пути из белого списка (токен не требуется): `/healthz`, `/readyz`, `/actuator/**`, `/v3/api-docs/**`, `/swagger-ui/**`. Пути Swagger активны, только пока включен Swagger; в профиле `prod` он полностью отключается (`springdoc.api-docs.enabled: false`, `springdoc.swagger-ui.enabled: false`).

### Realm Keycloak

В realm `lakehouse` должны быть:

- **`lakehouse-internal-client`** - confidential-клиент с включенными *Service Accounts* (межсервисные вызовы);
- **`lakehouse-ui-client`** - confidential-клиент со включенным *Standard Flow* (вход пользователей через UI BFF);
- роли realm'а `USER` / `ADMIN` (опционально, используются в `@PreAuthorize`).

Эталонный realm для импорта: `demo/compose/conf_infra/security/realms/lakehouse-realm.json`.