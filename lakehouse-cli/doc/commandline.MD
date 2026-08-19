# Command-line client (lakehouse-cli)

## Overview

`lakehouse-cli` is an interactive command-line client for managing the lakehouse. It performs operations over configuration metadata (namespaces, datastores, datasets, scenario act templates, schedules, task execution service groups) via the `lakehouse-config-svc` REST API, as well as scheduler operations (viewing scheduled tasks, locking tasks) via the `lakehouse-scheduler-svc` REST API.

The client is built on Spring Boot (`CommandLineClient`, non-web mode) and uses `lakehouse-config-rest-client` and `lakehouse-scheduler-rest-client` to communicate with the services.

## Configuration

Connection parameters are defined in `src/main/resources/application.yml`:

```yaml
lakehouse:
  client:
    rest:
      config:
        server:
          url: http://127.0.0.1:8080
      scheduler:
        server:
          url: http://127.0.0.1:8081
```

| Parameter | Purpose | Default |
|---|---|---|
| `lakehouse.client.rest.config.server.url` | URL of the configuration service (`lakehouse-config-svc`) | `http://127.0.0.1:8080` |
| `lakehouse.client.rest.scheduler.server.url` | URL of the scheduler service (`lakehouse-scheduler-svc`) | `http://127.0.0.1:8081` |

## Running

```bash
java -jar lakehouse-cli-<version>.jar
```

On startup the prompt `Provide the Java Scanner char input:` is printed. Commands are entered interactively; each command is terminated by a line containing the `/` delimiter.

## Command format

```
<command> [<arguments>]
/
```

The `/` delimiter is placed on a separate line after the command — the entered command is executed only after it. Runs of multiple spaces are collapsed into a single space.

## Commands

### exit

Shuts down the client.

```
exit
/
```

### show

Prints requested data.

```
show one <objectType> <name|id>
/
```

Prints the JSON of the requested object.

```
show all <objectType>
/
```

Prints the list of objects of the requested type as a table.

### upload

Uploads a configuration object from a JSON file to `lakehouse-config-svc`.

```
upload <objectType> <filePath>
/
```

Result: the HTTP code of the service response (`http code <code>`).

### download

Downloads a configuration object from `lakehouse-config-svc` to a JSON file.

```
download <objectType> <name|id> <filePath>
/
```

Result: `Saved to file <filePath>`.

### delete

Deletes a configuration object from `lakehouse-config-svc`.

```
delete <objectType> <name|id>
/
```

Result: the HTTP code of the service response (`http code <code>`).

### lock

Manages task locks of the scheduler (`lakehouse-scheduler-svc`).

Acquiring a task:

```
lock new <taskId> <taskExecutorServiceId>
/
```

Result: the JSON of the `ScheduledTaskLockDTO` object (contains `lockId`, `serviceId`, `lastHeartBeatDateTime`).

Renewing a lock (heartbeat):

```
lock heartbeat <lockId>
/
```

Result: the HTTP code of the service response.

Releasing a lock with a result status:

```
lock release <lockId> <status>
/
```

Result: the HTTP code of the service response.

Allowed `<status>` values (the `Status.Task` enum):

`NEW`, `QUEUED`, `RUNNING`, `SUCCESS`, `FAILED`, `CONF_ERROR`

## Object types

| `<objectType>` | Operations | Service | Columns printed (show all) |
|---|---|---|---|
| `nameSpace` / `nameSpaces` | show one, show all, upload, download, delete | config | `name`, `description` |
| `datastore` / `datastores` | show one, show all, upload, download, delete | config | `name`, `description`, `driver` |
| `dataset` / `datasets` | show one, show all, upload, download, delete | config | `name`, `description`, `nameSpace` |
| `scenarioactemplate` / `scenarioactemplates` | show one, show all, upload, download, delete | config | `name`, `description` |
| `schedule` / `schedules` | show one, show all, upload, download, delete | config | `name`, `description`, `intervalExpression`, `startDateTime`, `stopDateTime` |
| `taskexecutionservicegroup` / `taskexecutionservicegroups` | show one, show all, upload, download, delete | config | `name`, `description` |
| `scheduledtask` / `scheduledtasks` | show one, show all | scheduler | `scheduleName`, `scheduleTargetDateTime`, `scenarioActName`, `name`, `status`, `executionModule`, `taskExecutionServiceGroupName` |
| `tasklock` / `tasklocks` | show one, show all | scheduler | `lockId`, `serviceId`, `lastHeartBeatDateTime` |

An object type can be specified in either singular or plural form.

Only configuration objects (types marked with the config service) are available for the `upload` / `download` / `delete` operations. `scheduledtask` and `tasklock` are available only for the `show` operations.

## Examples

Viewing all namespaces:

```
show all namespaces
/
```

Viewing a single dataset as JSON:

```
show one dataset my_dataset
/
```

Uploading a new schedule from a file:

```
upload schedule /home/user/schedule.json
/
```

Downloading a namespace to a file:

```
download namespace my_ns /home/user/ns.json
/
```

Deleting a datastore:

```
delete datastore my_store
/
```

Acquiring task with id 42 by service executor-1:

```
lock new 42 executor-1
/
```

Sending a heartbeat for a lock:

```
lock heartbeat 10
/
```

Releasing a lock with SUCCESS status:

```
lock release 10 success
/
```

## Error handling

Entering an unknown command or an unknown combination of arguments throws exceptions:

- `UnknownCommandCombination` — unknown root command or unknown `show` / `lock` subcommand;
- `UnknownObjectTypeInCommandCombination` — unknown object type;
- `UnApplicableCommandCombination` — inapplicable combination of arguments.

The error message is printed to the console and the application continues running.
