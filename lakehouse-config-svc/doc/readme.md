# lakehouse-config-svc

Metadata management service - a single storage for all lakehouse configurations. It is the source of truth for metadata on the basis of which the other services (scheduler-svc, task-executor-svc, state-svc) perform data processing.

## Overview

`lakehouse-config-svc` stores and serves lakehouse metadata:

- **Namespaces** - logical separation of environments
- **Drivers** - connection settings for compute clusters
- **Data sources** - connections to external storages (JDBC/Spark)
- **Datasets** - table, column and constraint descriptions
- **Schedules** - data processing periodicity (intervals, scenario acts, tasks)
- **Data quality metrics** - DQ checks
- **Scripts and SQL templates** - query templates with Jinjava substitutions
- **Data lineage** - data provenance relationships
- **TaskExecutionServiceGroups** - task executor groups

Configurations are defined as DTOs, stored in PostgreSQL and exposed via REST API. Schedule changes are published to Kafka (topic `schedule_effective_changes`) so that scheduler-svc builds actual schedule instances.

## Architecture

```
┌───────────────────────┐     REST (CRUD)      ┌───────────────────────────┐
│  Admin / UI / CLI     │ ────────────────────▶│   lakehouse-config-svc    │
└───────────────────────┘                      │   (REST API /v1_0/configs)│
                                               │                           │
┌───────────────────────┐     REST (read)      │  ┌─────────────────────┐  │
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

- **Controller** - REST CRUD endpoints for each metadata type plus compound endpoints for derived objects.
- **Service** - business logic: validation, DTO/entity mapping, merging of template and concrete configurations via `DtoMergeUtils`.
- **Repository (JPA/Hibernate)** - persistence in PostgreSQL.
- **InternalScheduler** - periodic publishing of schedule changes to Kafka.
- Metadata is organized hierarchically (namespace → datasource → dataset → ...); the dependency scheme is described in [content_configuration](content_configuration/content_configuration.md).

## Modules

### lakehouse-config-svc

Spring Boot application that implements the REST API and the metadata storage. Entry point: `org.lakehouse.config.LakehouseConfigApplication`.

### lakehouse-config-rest-client

Java client (`ConfigRestClientApi`/`ConfigRestClientApiImpl`) for accessing `lakehouse-config-svc` from other services (scheduler-svc, task-executor-svc, etc.). It performs typed requests to the `/v1_0/configs/...` endpoints through `RestClientHelper`. The base URL is set by the property `lakehouse.client.rest.config.server.url`.

## API Endpoints

The description of the endpoint structure and metadata configurations is in the [content_configuration](content_configuration/content_configuration.md) section.

## Configuration

Application parameters (datasource, JPA, Kafka schedule publishing settings, health endpoints) are described in [appconf/service_configuration.md](appconf/service_configuration.md).