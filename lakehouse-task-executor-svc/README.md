# Lakehouse Task Executor Service

The task executor is the service that receives tasks scheduled by the scheduler, locks them, runs them through a task processor (TaskProcessor) and reports the result back to the scheduler.

## Documentation

- [Service overview](doc/readme.md)
- [Service settings](doc/properties.md)
- [Task processors](doc/processors.md)
- [Scaling](doc/scaling.md)