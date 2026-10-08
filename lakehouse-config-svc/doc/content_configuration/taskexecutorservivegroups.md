# Task executor service group
This is a special marker for task configuration. Using this marker, an executor can understand that a task is intended for it (the group it belongs to).
It is used as a task routing mechanism. For example, executors can be equipped with different functionality or even written in different languages.
By marking a task and an executor with the same marker, the effect of routing or queue management can be achieved.

## Object fields
| Field | Purpose                       |
|:------|:------------------------------|
| keyName | Unique identifier            | 
| description | Description for documentation| 
| domainKeyName | Domain owning the group. Derived from the repository the file is loaded from and **stamped at synchronization time** (`GitOpsSynchronizer.stampDomainOn(...)`), never taken from the file content. `null` for a group created through the REST API. Not part of the identity: `keyName` stays the sole primary key |
| allowedDomains | Additional domains this group is allowed to serve. Stored in the `task_execution_service_group_domains` collection table (`domain_name` column). Empty means "own domain only" |

## Domains
The group carries the domain in two independent roles:

- **Ownership** — `domainKeyName` says which repository the group was loaded from. Like every
  other managed construct, the domain is not written in the YAML file: it is derived from the
  repository and always wins over anything in the parsed object. The same `keyName` cannot
  exist in two domains, and a REST update that would overwrite a group of one domain with a
  group of another is rejected with `DomainConflictException` → `409 Conflict`.
- **Capability** — `allowedDomains` is the list of domains the executors of this group may
  additionally run tasks for. The domain of the group itself is always implicitly allowed.

`lakehouse-scheduler-svc` enforces the capability when it publishes tasks
(`ScheduleTaskInstanceService.checkDomain(...)`): if the domain of the schedule is not in
`allowedDomains ∪ {domainKeyName}`, the task instance is stored as `CONF_ERROR`, the pending
producer message is dropped and nothing is sent to Kafka. This is the mechanism that keeps a
foreign domain out of an executor group, because the executors themselves are grouped only by
`taskExecutionServiceGroupName` (the Kafka `group.id`) and know nothing about domains. See
[scheduler-svc: Domains](../../../lakehouse-scheduler-svc/doc/readme.md#domains) and
[Domains](domains.md).

**Example**
```json
{
  "name": "spark-cluster",
  "description": "Spark job group",
  "allowedDomains": ["analytics"]
}

```

##  /v1_0/configs/taskexecutionservicegroups
List of objects
##  /v1_0/configs/taskexecutionservicegroups/{keyName}    
Manipulates a specific object by key