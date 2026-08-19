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