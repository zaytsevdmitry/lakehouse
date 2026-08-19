# Schedule
Abstract schedule configuration, defines configuration rules and forms the relationship between a dataset and the interval processes maintaining it.
Interpretation of the schedule lies with the executing engine. Depending on the executing engine used, behavior details may vary.


## Design features
A compound object - the schedule contains configuration fields, a list of actions and a description of the directed graph of the action order.
If an action is not listed in the graph, it is considered to have no dependencies and runs outside the graph.

## Object fields 

| Field                | Purpose                                                                                               |
|:---------------------|:------------------------------------------------------------------------------------------------------|
| keyName              | Unique identifier                                                                                     | 
| description          | Description for documentation                                                                        | 
| intervalExpression   | Expression defining the schedule periodicity (e.g. @daily)                                            |
| startDateTime        | Start of the time from which the schedule will be formed. The value is mandatory                      |
| stopDateTime         | Time up to which the schedule will be formed. If null - the schedule is formed indefinitely           |
| enabled              | Enabled - true. After setting false the schedule must be ignored by the executing system              |
| scenarioActs         | Scenario actions.                                                                                    |
| scenarioActEdges     | Directed graph to define the order of actions                                                        |

## Scenario acts (scenarioActs)
A complex compound object. Links a dataset with a set of tasks

### Object fields
| Field                | Purpose                                                                                        | 
|:---------------------|:-----------------------------------------------------------------------------------------------|
| name                 | Name                                                                                           |
| dataSet              | Reference to the keyName of the [dataset](datasets.md) that will be maintained by the scenario act |
| [scenarioActTemplate](scenarioActTemplate.md) | Reference to the scenario act template |
| tasks                | List of tasks; when a template is present, overrides same-named tasks, adds the rest to the list |
| dagEdges             | Directed task graph; when a template is present, overrides and embeds into the template         |



**Example**
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
List of objects

##  /v1_0/configs/schedules/headers
List of objects in a short form (schedule headers)

##  /v1_0/configs/schedules/{keyName}
Manipulates a specific object by key

##  /v1_0/configs/effective/schedules/fromdt/{dt}
Returns schedule settings changed since the specified point in time {dt}

##  /v1_0/configs/effective/schedules/schedule/{keyName}
Returns the schedule settings with the scenario template applied. All data of the template and the schedule itself are merged into a single effective configuration

##  /v1_0/configs/effective/schedules/schedule/{keyName}/scenarioActName/{scenarioActName}/taskName/{taskName}
Returns the configuration of a specific task for the specified schedule and scenario act.