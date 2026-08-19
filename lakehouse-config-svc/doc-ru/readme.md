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