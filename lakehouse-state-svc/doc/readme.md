# lakehouse-state-svc

The lakehouse service for storing and managing dataset interval states. For each dataset it maintains a coverage of the time series with intervals and their states (LOCKED/SUCCESS), protects intervals from conflicting changes, and makes it possible to find "gaps" - intervals that have not been processed yet or have been processed unsuccessfully.

## Overview

`lakehouse-state-svc` is responsible for:

- **Storing interval states** - for each dataset (`dataSetKeyName`) it stores records about time intervals with the state `LOCKED` or `SUCCESS`.
- **Writing a state** - when a new interval is written, existing intersecting intervals are rebuilt (merge); duplicates are prevented by the unique constraint `(dataSetKeyName, intervalStartDateTime, intervalEndDateTime)`.
- **Conflict protection** - if a new `lockSource` does not match the one already recorded for unclosed (non-SUCCESS) intervals, the write is rejected with a `LockedStateRuntimeException`.
- **Finding "gaps"** - retrieving the list of intervals without the `SUCCESS` state (not processed, or with the `LOCKED` state) within a given time window. It serves as a signal that tasks need to be launched (used by the scheduler/executors).
- **State output** - retrieving all dataset states within a given interval.

## Architecture

```
┌──────────────────────────┐        ┌──────────────────────────────────────┐
│ lakehouse-scheduler-svc  │  REST  │        lakehouse-state-svc           │
│ task-executor-svc        │ ─────▶ │                                      │
│ (via state-rest-client)  │        │  ┌────────────────────────────────┐  │
└──────────────────────────┘        │  │ StateController               │  │
                                    │  │  POST /state/dataset/wrong    │  │
                                    │  │  PUT  /state/dataset          │  │
                                    │  │  GET  /state/dataset          │  │
                                    │  └───────────────┬────────────────┘  │
                                    │                  ▼                   │
                                    │  ┌────────────────────────────────┐  │
                                    │  │ StateService                  │  │
                                    │  │  checkForPossibleChanges      │  │
                                    │  │  save (merge)                 │  │
                                    │  │  getStatesByDataSetAndInterval│  │
                                    │  │  getWrongStateByInterval      │  │
                                    │  └───────────────┬────────────────┘  │
                                    │                  ▼                   │
                                    │  ┌────────────────────────────────┐  │
                                    │  │ StateFactory  (merge,          │  │
                                    │  │  sortStates, leftRightPad,     │  │
                                    │  │  feelGaps) / StateMapper       │  │
                                    │  └───────────────┬────────────────┘  │
                                    │                  ▼                   │
                                    │  ┌────────────────────────────────┐  │
                                    │  │ PostgreSQL                    │  │
                                    │  │ (schema lakehouse_state)      │  │
                                    │  └────────────────────────────────┘  │
                                    └──────────────────────────────────────┘
```

- **StateController** - REST API (see [restapi.md](restapi.md)): writing a state, retrieving states, finding "gaps".
- **StateService** - business logic: checking for possible changes (conflict protection), saving with interval rebuilding, retrieving states and "gaps".
- **StateFactory** - interval algorithms: `merge` (rebuilding intersecting intervals), `sortStates`, `leftRightPad` and `feelGaps` (filling gaps at the borders and inside the window), `getForRemove`.
- **StateMapper** - converting the `DataSetState` entity to DTO (`DataSetStateDTO`) and back.
- **DataSetStateRepository (JPA)** - persistence, finding interval intersections (`findIntersection`).

The dataset interval state model is described in [state_model/state-models.MD](state_model/state-models.MD).

## Modules

### lakehouse-state-svc

Spring Boot application implementing the state service. Entry point: `org.lakehouse.state.LakehouseStateApplication`. Runs on port **8082**.

### lakehouse-state-rest-client

Java client (`StateRestClientApi`/`StateRestClientApiImpl`) for accessing `lakehouse-state-svc` from other services (task-executor-svc, scheduler-svc and others). Performs typed requests to the `/v1_0/state/...` endpoints via `RestClientHelper`. The base URL is set by the `lakehouse.client.rest.state.server.url` property.

## API Endpoints

The service runs on port **8082**; all endpoints start with `/v1_0`:

| Method | Endpoint                        | Purpose                                       |
|:-------|:--------------------------------|:----------------------------------------------|
| POST   | `/v1_0/state/dataset/wrong`     | Retrieving "gaps" - intervals without the SUCCESS status in a given window |
| PUT    | `/v1_0/state/dataset`           | Writing an interval state (rebuilding intersections) |
| GET    | `/v1_0/state/dataset`           | Retrieving dataset states within a given interval |

Request bodies - `DataSetIntervalDTO` (`dataSetKeyName`, `intervalStartDateTime`, `intervalEndDateTime`); writing a state - `DataSetStateDTO` (additionally `status` [LOCKED/SUCCESS], `lockSource`); the "gaps" response - `DataSetWrongStateResponseDTO` with the `wrongStates` list.

## Configuration

Application parameters (port, datasource, JPA, health endpoints) are described in [appconf/service_configuration.md](appconf/service_configuration.md).