# REST API

Сервис состояний работает на порту **8082**. Все эндпоинты начинаются с `/v1_0`.

## Состояния датасета

### Получение «дыр» - интервалов без статуса SUCCESS
Метод запроса POST с телом JSON `DataSetIntervalDTO`
```
http://localhost:8082/v1_0/state/dataset/wrong
```
```bash
curl -X POST http://localhost:8082/v1_0/state/dataset/wrong \
     -H "Content-Type: application/json" \
     -d '{"dataSetKeyName":"source", "intervalStartDateTime":"2025-01-01T00:00:00Z", "intervalEndDateTime":"2025-02-01T00:00:00Z"}' |jq
```
Возвращает объект [DataSetWrongStateResponseDTO.java](../../lakehouse-common/src/main/java/org/lakehouse/client/api/dto/state/DataSetWrongStateResponseDTO.java)
со списком `wrongStates` - интервалов, не покрытых статусом `SUCCESS` (не обработанных, либо `LOCKED`). Если в окне
нет ни одного состояния, возвращается один интервал, равный всему запрошенному окну.

### Запись состояния интервала
Метод запроса PUT с телом JSON `DataSetStateDTO`
```
http://localhost:8082/v1_0/state/dataset
```
```bash
curl -X PUT http://localhost:8082/v1_0/state/dataset \
     -H "Content-Type: application/json" \
     -d '{"dataSetKeyName":"source", "intervalStartDateTime":"2025-01-01T00:00:00Z", "intervalEndDateTime":"2025-01-15T00:00:00Z", "status":"SUCCESS", "lockSource":"task-executor-1"}' |jq
```
Возвращаемый объект [DataSetStateDTO.java](../../lakehouse-common/src/main/java/org/lakehouse/client/api/dto/state/DataSetStateDTO.java)
наследует `DataSetIntervalDTO` и добавляет поля:

| Поле          | Назначение                                        |
|:--------------|:--------------------------------------------------|
| dataSetKeyName| Ключ датасета                                     |
| intervalStartDateTime | Нижняя граница интервала времени           |
| intervalEndDateTime   | Верхняя граница интервала времени           |
| status        | Статус интервала: `LOCKED` или `SUCCESS`          |
| lockSource    | Источник блокировки (исполнитель, зафиксировавший интервал) |

Существующие пересекающиеся интервалы перестраиваются: они сжимаются/делятся так, чтобы новый интервал не имел
пересечений. Если для незакрытых (не `SUCCESS`) пересекающихся интервалов зафиксирован другой `lockSource`, запись
отклоняется исключением `LockedStateRuntimeException`.

### Получение состояний датасета в заданном интервале
Метод запроса GET с телом JSON `DataSetIntervalDTO`
```
http://localhost:8082/v1_0/state/dataset
```
```bash
curl -X GET http://localhost:8082/v1_0/state/dataset \
     -H "Content-Type: application/json" \
     -d '{"dataSetKeyName":"source", "intervalStartDateTime":"2025-01-01T00:00:00Z", "intervalEndDateTime":"2025-02-01T00:00:00Z"}' |jq
```
Возвращает список `DataSetStateDTO` всех состояний датасета, пересекающихся с заданным интервалом.