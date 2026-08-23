# Сервис веб-интерфейса (lakehouse-ui-svc)

Веб-интерфейс управления lakehouse: единая точка визуализации и администрирования сервисов, каталога данных, расписаний, состояний датасетов и Spark-подписок.

## Обзор

`lakehouse-ui-svc` — сервис, агрегирующий данные всех остальных сервисов lakehouse и предоставляющий единый веб-интерфейс. Он не хранит собственное состояние и не выполняет бизнес-логику lakehouse — он лишь обращается к другим сервисам через их REST-клиенты и отдаёт результат фронтенду.

Сервис состоит из двух частей:

- **бэкенд** — Spring Boot приложение, которое проксирует запросы фронтенда к сервисам lakehouse и отдаёт статический фронтенд;
- **фронтенд** — одностраничное React-приложение (Vite), собираемое в `src/main/resources/static` и раздаваемое тем же сервисом.

Разделы интерфейса:

- **Services** — граф сервисов lakehouse и их статус (`UP`/`DOWN`) по health-check.
- **Catalog** — дерево каталога данных: источники → схемы → датасеты; просмотр датасета (модель/DDL, линковка, ограничения) и источника данных.
- **Schedules** — список запусков расписаний за интервал, DAG запуска расписания.
- **SparkJobs** — список Spark-подписок через `lakehouse-task-proxy-for-spark`: создание, статус, kill, kill all, clear.

## Архитектура

Сервис является тонким слоем агрегации: каждый раздел интерфейса обслуживается своим контроллером, который делегирует работу REST-клиенту соответствующего сервиса lakehouse. Прямых обращений к базам данных сервис не выполняет.

Внешние взаимодействия:

- **lakehouse-config-svc** — каталог данных, линковка, ограничения, модели, заголовки расписаний.
- **lakehouse-scheduler-svc** — запуски расписаний за интервал, DAG запуска.
- **lakehouse-state-svc** — состояния интервалов датасетов.
- **lakehouse-task-proxy-for-spark** — Spark-подписки (создание, статус, kill, clear).

Контроллеры:

```
CatalogController   /api/catalog     — дерево каталога, датасеты, линковка, ограничения, скрипты
ScheduleController  /api/schedules   — запуски расписаний, заголовки, DAG
ServicesController  /api/services    — граф сервисов и их статус
SparkProxyController /api/spark-proxy — Spark-подписки
StateController     /api/states      — состояния интервалов датасетов
```

Статусы сервисов вычисляются `HealthChecker`: HTTP-проверкой по `healthCheckUrl` (тип `http`) либо проверкой открытого TCP-порта (тип `tcp`). Состав сервисов, рёбра и вершины графа задаются конфигурацией `lakehouse.ui.services/edges/vertices`.

Фронтенд собирается Vite (каталог `frontend`), результат сборки кладётся в `src/main/resources/static`. В dev-режиме Vite проксирует `/api` на сервис (`vite.config.js`).

## Модули

### lakehouse-ui-svc

Сам сервис. Содержит:

- точку входа `LakehouseUiApplication`;
- контроллеры (`controller`): Catalog, Schedule, Services, SparkProxy, State;
- сервисы (`service`): `CatalogService`, `ScheduleService`, `ServicesService`, `SparkProxyService`, `StateService`, `HealthChecker`;
- конфигурацию `UiServiceProperties` (список сервисов, граф);
- DTO (`dto`) — представления для фронтенда (`CatalogTreeNodeDTO`, `ConstraintDTO`, `ServiceNodeDTO`, `ScheduleRequestDTO`, `DataSetStateRequestDTO`);
- `GlobalExceptionHandler` — единая обработка ошибок;
- фронтенд (`src/main/resources/frontend`): React + Vite.

Зависит от REST-клиентов: `lakehouse-config-rest-client`, `lakehouse-scheduler-rest-client`, `lakehouse-state-rest-client`, `lakehouse-task-proxy-for-spark-rest-client`.

## API Endpoints

| Метод | Путь | Описание |
|---|---|---|
| GET | `/api/catalog/tree` | Дерево каталога: источники → схемы → датасеты |
| GET | `/api/catalog/dataset/{keyName}` | Датасет по ключевому имени |
| GET | `/api/catalog/dataset/{keyName}/lineage` | Линковка датасета |
| GET | `/api/catalog/dataset/{keyName}/constraints` | Ограничения датасета |
| GET | `/api/catalog/script/{key}` | SQL-скрипт по ключу |
| GET | `/api/catalog/dataset/{keyName}/model-script` | Модель (DDL) датасета |
| GET | `/api/catalog/datasource/{keyName}` | Источник данных по ключевому имени |
| POST | `/api/schedules` | Запуски расписаний за интервал (`fromDate`, `toDate`, `names`) |
| GET | `/api/schedules/headers` | Заголовки расписаний |
| GET | `/api/schedules/dag/{id}` | DAG запуска расписания по id |
| GET | `/api/services` | Список сервисов со статусами |
| GET | `/api/services/edges` | Рёбра графа сервисов |
| GET | `/api/services/vertices` | Вершины графа сервисов |
| GET | `/api/spark-proxy/submissions` | Список подписок (`limit`, `lastId`, `id`, `status`, `dateFrom`, `dateTo`) |
| GET | `/api/spark-proxy/submissions/{id}/spark-properties` | Spark-свойства подписки |
| POST | `/api/spark-proxy/submissions` | Создание подписки |
| GET | `/api/spark-proxy/submissions/status/{submissionId}` | Статус подписки |
| POST | `/api/spark-proxy/submissions/kill/{submissionId}` | Убить подписку |
| POST | `/api/spark-proxy/submissions/killall` | Убить все подписки |
| POST | `/api/spark-proxy/submissions/clear` | Очистить завершённые подписки |
| POST | `/api/states` | Состояния интервалов датасета (`dataSetKeyName`, `fromDate`, `toDate`) |

## Конфигурация

Основные параметры (`src/main/resources/application.yml`):

```yaml
server:
  port: 8084
lakehouse:
  client:
    rest:
      config:
        server:
          url: http://localhost:8080
      state:
        server:
          url: http://localhost:8082
      scheduler:
        server:
          url: http://localhost:8081
      task-proxy-for-spark:
        server:
          url: http://localhost:8099
  ui:
    health-check-timeout-ms: 3000
    services:
      - name: lakehouse-config-svc
        url: http://localhost:8080
        health-check-url: http://localhost:8080/healthz
      - name: postgres-db
        url: http://localhost:5432
        health-check-url: localhost:5432
        check-type: tcp
    vertices:
      config-svc: lakehouse-config-svc
      ...
    edges:
      config-svc:
        - state-svc
        - scheduler-svc
```

| Параметр | Описание |
|---|---|
| `server.port` | Порт сервиса |
| `lakehouse.client.rest.config.server.url` | URL `lakehouse-config-svc` |
| `lakehouse.client.rest.state.server.url` | URL `lakehouse-state-svc` |
| `lakehouse.client.rest.scheduler.server.url` | URL `lakehouse-scheduler-svc` |
| `lakehouse.client.rest.task-proxy-for-spark.server.url` | URL `lakehouse-task-proxy-for-spark` |
| `lakehouse.ui.health-check-timeout-ms` | Таймаут проверки доступности сервиса |
| `lakehouse.ui.services[].name` | Имя сервиса в интерфейсе |
| `lakehouse.ui.services[].url` | URL сервиса |
| `lakehouse.ui.services[].health-check-url` | URL health-check (по умолчанию = `url`) |
| `lakehouse.ui.services[].check-type` | Тип проверки: `http` (по умолчанию) или `tcp` |
| `lakehouse.ui.vertices` | Вершины графа: ключ → имя сервиса |
| `lakehouse.ui.edges` | Рёбра графа: ключ вершины → список вершин-приёмников |

## Безопасность

UI BFF аутентифицирует пользователей через Keycloak (realm `lakehouse`) по OAuth 2.0 **authorization code flow** (`oauth2Login()`). После успешного входа Spring Security выдает фронтенду защищенную сессионную cookie `JSESSIONID` (`HttpOnly`; в профиле `prod` также `Secure`). Запросы, изменяющие состояние, защищены от CSRF: токен передается фронтенду через cookie `XSRF-TOKEN` (доступную JS) и должен возвращаться в заголовке `X-XSRF-TOKEN`.

Пути из белого списка (вход не требуется): `/healthz`, `/readyz`, `/actuator/**`, `/favicon.ico`. Все остальные запросы требуют аутентифицированной сессии; неаутентифицированные запросы браузера перенаправляются на страницу входа Keycloak, после входа пользователь возвращается на `/` (`defaultSuccessUrl`).

### Необходимые настройки

| Свойство / env | По умолчанию | Описание |
|---|---|---|
| `KEYCLOAK_ISSUER_URI` | `http://lakehouse-auth-svc:8080/realms/lakehouse` | URL realm'а; из него строятся эндпоинты auth/token/userinfo/certs |
| `KEYCLOAK_UI_CLIENT_SECRET` | `super-secret-bff-key-1234567890` | Секрет клиента `lakehouse-ui-client` |
| `LAKEHOUSE_UI_REDIRECT_URI` | `{baseUrl}/login/oauth2/code/{registrationId}` | OAuth2 redirect URI BFF |
| `server.servlet.session.cookie.name` / `.http-only` | `JSESSIONID` / `true` | Имя сессионной cookie и флаг HttpOnly |
| `server.servlet.session.cookie.secure` | `false` (`true` в профиле `prod`) | Установите `true`, если UI работает по HTTPS |

### Настройка учетных записей и ролей в Keycloak

1. **Разверните Keycloak.** В demo-окружении compose поднимает Keycloak 26.0 с админ-консолью на `http://localhost:8085` (учетные данные из `KEYCLOAK_ADMIN`/`KEYCLOAK_ADMIN_PASSWORD`, по умолчанию `admin`/`admin_local_password`) и импортирует эталонный realm из `demo/compose/conf_infra/security/realms/lakehouse-realm.json`. В production используйте постоянную БД и смените все пароли/секреты по умолчанию.
2. **Роли realm'а.** В realm `lakehouse` определены две роли realm'а:
   - `USER` - обычный пользователь экосистемы;
   - `ADMIN` - администратор с полным доступом.

   Роли попадают к сервисам в claim JWT `realm_access.roles` и преобразуются там в authorities `ROLE_USER`/`ROLE_ADMIN` (`KeycloakRoleConverter`).
3. **Клиент `lakehouse-ui-client`.** Confidential-клиент (*Standard Flow Enabled*, *Direct Access Grants* выключен), используемый данным BFF. Проверьте, что:
   - *Valid redirect URIs* содержат внешне видимый адрес UI: по умолчанию `http://localhost:8080/*` и `http://localhost:8080/login/oauth2/code/keycloak`;
   - *Web Origins* содержит origin UI (`http://localhost:8080`);
   - при развертывании на другом хосте/порту добавьте соответствующие redirect URI и web origin и задайте `LAKEHOUSE_UI_REDIRECT_URI`.
4. **Создание пользователей.** Админ-консоль → realm `lakehouse` → *Users* → *Add user*: заполните username/email/имя, затем *Credentials* → задайте пароль (выключите *Temporary*, чтобы пароль был постоянным).
5. **Назначение ролей.** *Users* → выберите пользователя → *Role mapping* → фильтр *Filter by realm roles* → назначьте `USER` и/или `ADMIN` кнопкой *Assign*.
6. **Service account.** Confidential-клиент `lakehouse-internal-client` (*Service Accounts Enabled*) используется backend-сервисами для межсервисных вызовов; его секрет должен совпадать со значением `KEYCLOAK_INTERNAL_CLIENT_SECRET` на каждом сервисе.

После настройки откройте UI - первый запрос перенаправит на страницу входа Keycloak; войти смогут только пользователи с учетной записью в realm `lakehouse`.
