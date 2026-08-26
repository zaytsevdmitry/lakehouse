# lakehouse-state-svc

Сервис хранения и управления состояниями интервалов датасетов lakehouse. Для каждого датасета ведет покрытие временного ряда интервалами с состояниями (LOCKED/SUCCESS), защищает интервалы от конфликтующих изменений и позволяет находить «дыры» - интервалы, которые еще не обработаны или обработаны неуспешно.

## Обзор

`lakehouse-state-svc` отвечает за:

- **Хранение состояний интервалов** - для каждого датасета (`dataSetKeyName`) хранит записи об интервалах времени с состоянием `LOCKED` или `SUCCESS`.
- **Запись состояния** - при записи нового интервала существующие пересекающиеся интервалы перестраиваются (merge), дубликаты исключаются за счет уникального ограничения `(dataSetKeyName, intervalStartDateTime, intervalEndDateTime)`.
- **Защиту от конфликтов** - если новый `lockSource` не совпадает с уже зафиксированным для незакрытых интервалов (не SUCCESS), запись отклоняется исключением `LockedStateRuntimeException`.
- **Поиск «дыр»** - получение списка интервалов без состояния `SUCCESS` (не обработанных, либо с состоянием `LOCKED`) в заданном окне времени. Служит признаком необходимости запуска задач (используется планировщиком/исполнителями).
- **Вывод состояния** - получение всех состояний датасета в заданном интервале.

## Архитектура

```
┌──────────────────────────┐        ┌──────────────────────────────────────┐
│ lakehouse-scheduler-svc  │  REST  │        lakehouse-state-svc           │
│ task-executor-svc        │ ─────▶ │                                      │
│ (через state-rest-client)│        │  ┌────────────────────────────────┐  │
└──────────────────────────┘        │  │ StateController                │  │
                                    │  │  POST /state/dataset/wrong     │  │
                                    │  │  PUT  /state/dataset           │  │
                                    │  │  GET  /state/dataset           │  │
                                    │  └───────────────┬────────────────┘  │
                                    │                  ▼                   │
                                    │  ┌────────────────────────────────┐  │
                                    │  │ StateService                   │  │
                                    │  │  checkForPossibleChanges       │  │
                                    │  │  save (merge)                  │  │
                                    │  │  getStatesByDataSetAndInterval │  │
                                    │  │  getWrongStateByInterval       │  │
                                    │  └───────────────┬────────────────┘  │
                                    │                  ▼                   │
                                    │  ┌────────────────────────────────┐  │
                                    │  │ StateFactory  (merge,          │  │
                                    │  │  sortStates, leftRightPad,     │  │
                                    │  │  feelGaps) / StateMapper       │  │
                                    │  └───────────────┬────────────────┘  │
                                    │                  ▼                   │
                                    │  ┌────────────────────────────────┐  │
                                    │  │ PostgreSQL                     │  │
                                    │  │ (schema lakehouse_state)       │  │
                                    │  └────────────────────────────────┘  │
                                    └──────────────────────────────────────┘
```

- **StateController** - REST API (см. [restapi.md](restapi.md)): запись состояния, вывод состояний, поиск «дыр».
- **StateService** - бизнес-логика: проверка возможных изменений (защита от конфликтов), сохранение с перестроением интервалов, выборка состояний и «дыр».
- **StateFactory** - алгоритмы работы с интервалами: `merge` (перестройка пересекающихся интервалов), `sortStates`, `leftRightPad` и `feelGaps` (заполнение пробелов на границах и внутри окна), `getForRemove`.
- **StateMapper** - преобразование сущности `DataSetState` в DTO (`DataSetStateDTO`) и обратно.
- **DataSetStateRepository (JPA)** - персистентность, поиск пересечений интервалов (`findIntersection`).

Модель состояний интервалов датасета описана в [state_model/state-models.MD](state_model/state-models.MD).

## Модули

### lakehouse-state-svc

Spring Boot-приложение, реализующее сервис состояний. Точка входа: `org.lakehouse.state.LakehouseStateApplication`. Работает на порту **8082**.

### lakehouse-state-rest-client

Java-клиент (`StateRestClientApi`/`StateRestClientApiImpl`) для доступа к `lakehouse-state-svc` из других сервисов (task-executor-svc, scheduler-svc и др.). Выполняет типизированные запросы к эндпоинтам `/v1_0/state/...` через `RestClientHelper`. Базовый URL задается свойством `lakehouse.client.rest.state.server.url`.

## API Endpoints

Сервис работает на порту **8082**, все эндпоинты начинаются с `/v1_0`:

| Метод | Эндпоинт                          | Назначение                                   |
|:------|:----------------------------------|:---------------------------------------------|
| POST  | `/v1_0/state/dataset/wrong`       | Получение «дыр» - интервалов без статуса SUCCESS в заданном окне |
| PUT   | `/v1_0/state/dataset`             | Запись состояния интервала (с перестройкой пересечений) |
| GET   | `/v1_0/state/dataset`             | Получение состояний датасета в заданном интервале |

Тело запросов - `DataSetIntervalDTO` (`dataSetKeyName`, `intervalStartDateTime`, `intervalEndDateTime`); запись состояния - `DataSetStateDTO` (дополнительно `status` [LOCKED/SUCCESS], `lockSource`); ответ о «дырах» - `DataSetWrongStateResponseDTO` со списком `wrongStates`.

## Конфигурация

Параметры приложения (порт, datasource, JPA, health-эндпоинты) описаны в [appconf/service_configuration.md](appconf/service_configuration.md).

## Безопасность

`lakehouse-state-svc` защищен по OAuth 2.0 / OIDC с Keycloak в качестве identity provider (realm `lakehouse`). Spring Security настроен как **OAuth2 resource server**: каждый запрос должен содержать валидный JWT, выпущенный этим realm, иначе сервис возвращает `401`.

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

Пути из белого списка (токен не требуется): `/healthz`, `/readyz`, `/actuator/**`, `/v3/api-docs/**`, `/swagger-ui/**`.

### Realm Keycloak

В realm `lakehouse` должны быть:

- **`lakehouse-internal-client`** - confidential-клиент с включенными *Service Accounts* (межсервисные вызовы);
- роли realm'а `USER` / `ADMIN` (опционально, используются в `@PreAuthorize`).

Эталонный realm для импорта: `demo/compose/conf_infra/security/realms/lakehouse-realm.json`.