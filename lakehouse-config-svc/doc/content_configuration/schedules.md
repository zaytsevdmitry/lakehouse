# Расписание
Абстрактная конфигурация расписания, задает правила конфигурирования, формирует взаимосвязь между датасетом и интервальными процессами его обслуживания 
Интерпретация расписания лежит на исполняющем механизме. В зависимости от применяемого исполняющего механизма могут  меняться детали поведения.


## Особенности устройства
Составной объект - расписание содержит поля конфигурирования, список действий и описание направленного графа порядка действий.
Если действие не указано в графе, тогда считается, что это действие не имеет зависимостей и выполняется вне графа.

## Поля объекта 

 Поле                   | Назначение                                                                                               |
|:-----------------------|:---------------------------------------------------------------------------------------------------------|
| keyName                | Уникальный идентификатов                                                                                 | 
| description            | Описание для документирования                                                                            | 
| startDateTime          | Начало момента времни с которого будет формироваться расписание. Значение обязательно                    |
| stopDateTime           | Момент времни до которого будет формироваться расписание. Если null - рассписание вормируется бесконечно |
| enabled                | Включено - true. После установки false расписание должно игнорироваться исполняющей системой             |
| scenarioActs           | Сценарий действий.                                                                                       |
| scenarioActEdges       | Направленный граф для определения последовательности действий                                            |

## Сценарий действий (scenarioActs)
Сложный составной объект. Связывает датасет с набором задач

### Поля объекта
 Поле                 | Назначение                                                                                        | 
|:---------------------|:--------------------------------------------------------------------------------------------------|
| name                 | Наименование                                                                                      |
| dataSet              | ссылка на  keyName [датасета](datasets.md), который будет обслуживаться действием сценария        |
| [scenarioActTemplate ](scenarioActTemplate.md) | Ссылка на шаблон действия сценария                                                                |
| tasks                | Список задач, при наличии шаблона переопределяет одноименные задачи, остальные добавляет в список |
| dagEdges             | Направленный граф задач, при наличии шаблон , переопределяет и встраивается в шаблон              |



**Пример**
```json
{
  "keyName": "regular",
  "description": "regular schedule for client transactions",
  "intervalExpression": "@daily",
  "startDateTime": "2025-01-01T00:00:00.0+00:00",
  "stopDateTime": null,
  "enabled": true,
  "scenarioActs": [
    {
      "name": "transaction_dds",
      "dataSet": "transaction_dds",
      "scenarioActTemplate": "spark",
      "intervalStart": "{{ adddays(targetDateTime, -1) }}",
      "intervalEnd": "{{ targetDateTime }}",
      "tasks": [
        {
          "name": "ext",
          "taskExecutionServiceGroupName": "default",
          "taskProcessor": "lockedStateTaskProcessor",
          "importance": "critical",
          "description": "Extended task"
        }
      ],
      "dagEdges": [
        {
          "from": "begin",
          "to": "ext"
        }
      ]
    },
    {
      "name": "aggregation_pay_per_client_daily_mart",
      "dataSet": "aggregation_pay_per_client_daily_mart",
      "scenarioActTemplate": "spark",
      "intervalStart": "{{ adddays(targetDateTime, -1) }}",
      "intervalEnd": "{{ targetDateTime }}"
    },
    {
      "name": "aggregation_pay_per_client_total_mart",
      "dataSet": "aggregation_pay_per_client_total_mart",
      "scenarioActTemplate": "spark",
      "intervalStart": "{{ adddays(targetDateTime, -1) }}",
      "intervalEnd": "{{ targetDateTime }}"
    }
  ],
  "scenarioActEdges": [
    {
      "from": "transaction_dds",
      "to": "aggregation_pay_per_client_daily_mart"
    },
    {
      "from": "transaction_dds",
      "to": "aggregation_pay_per_client_total_mart"
    }
  ]
}

```
##  /v1_0/configs/schedules
Список объектов

##  /v1_0/configs/schedules/{keyName}
Манипуляция конкретным объектом по ключу

##  /v1_0/configs/effective/schedules/schedule/{keyName}
Вернет настройки расписания с учетом применения шаблона сценария. Все данные, шаблона и самого расписания будут слиты в одну эффективную конфигурацию

##  /v1_0/configs/effective/schedules/schedule/{keyName}/scenarioActName/{scenarioActName}/taskName/{taskName} 
Вернет конфигурацию конкретной задачи по указанному расписанию и сценарию.

