# REST API

The state service runs on port **8082**. All endpoints start with `/v1_0`.

## Dataset states

### Get "gaps" - intervals without the SUCCESS status
POST request with a JSON body `DataSetIntervalDTO`
```
http://localhost:8082/v1_0/state/dataset/wrong
```
```bash
curl -X POST http://localhost:8082/v1_0/state/dataset/wrong \
     -H "Content-Type: application/json" \
     -d '{"dataSetKeyName":"source", "intervalStartDateTime":"2025-01-01T00:00:00Z", "intervalEndDateTime":"2025-02-01T00:00:00Z"}' |jq
```
Returns the object [DataSetWrongStateResponseDTO.java](../../lakehouse-common/src/main/java/org/lakehouse/client/api/dto/state/DataSetWrongStateResponseDTO.java)
with the `wrongStates` list - intervals not covered by the `SUCCESS` status (not processed, or `LOCKED`). If the window
contains no states at all, a single interval equal to the whole requested window is returned.

### Write an interval state
PUT request with a JSON body `DataSetStateDTO`
```
http://localhost:8082/v1_0/state/dataset
```
```bash
curl -X PUT http://localhost:8082/v1_0/state/dataset \
     -H "Content-Type: application/json" \
     -d '{"dataSetKeyName":"source", "intervalStartDateTime":"2025-01-01T00:00:00Z", "intervalEndDateTime":"2025-01-15T00:00:00Z", "status":"SUCCESS", "lockSource":"task-executor-1"}' |jq
```
The returned object [DataSetStateDTO.java](../../lakehouse-common/src/main/java/org/lakehouse/client/api/dto/state/DataSetStateDTO.java)
extends `DataSetIntervalDTO` and adds the fields:

| Field          | Purpose                                        |
|:---------------|:-----------------------------------------------|
| dataSetKeyName | Dataset key                                     |
| intervalStartDateTime | Lower bound of the time interval        |
| intervalEndDateTime   | Upper bound of the time interval        |
| status         | Interval status: `LOCKED` or `SUCCESS`          |
| lockSource     | Lock source (the executor that has locked the interval) |

Existing intersecting intervals are rebuilt: they are shrunk/split so that the new interval has no intersections.
If a different `lockSource` is recorded for unclosed (non-`SUCCESS`) intersecting intervals, the write is rejected
with a `LockedStateRuntimeException`.

### Get dataset states within a given interval
GET request with a JSON body `DataSetIntervalDTO`
```
http://localhost:8082/v1_0/state/dataset
```
```bash
curl -X GET http://localhost:8082/v1_0/state/dataset \
     -H "Content-Type: application/json" \
     -d '{"dataSetKeyName":"source", "intervalStartDateTime":"2025-01-01T00:00:00Z", "intervalEndDateTime":"2025-02-01T00:00:00Z"}' |jq
```
Returns a list of `DataSetStateDTO` of all dataset states intersecting the given interval.