# Task executor service group
This is a special marker for task configuration. Using this marker, an executor can understand that a task is intended for it (the group it belongs to).
It is used as a task routing mechanism. For example, executors can be equipped with different functionality or even written in different languages.
By marking a task and an executor with the same marker, the effect of routing or queue management can be achieved.

## Object fields
| Field | Purpose                       |
|:------|:------------------------------|
| keyName | Unique identifier            | 
| description | Description for documentation| 

**Example**
```json
{
  "name": "default",
  "description": null
}

```

##  /v1_0/configs/taskexecutionservicegroups
List of objects
##  /v1_0/configs/taskexecutionservicegroups/{keyName}    
Manipulates a specific object by key