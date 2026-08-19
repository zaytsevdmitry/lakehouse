```yaml
server:
  port: 8089
lakehouse:
  client: # External interaction settings
    rest:
      state:
        server:
          url: http://127.0.0.1:8082 # State service
      config:
        server:
          url: http://127.0.0.1:8080 # Configuration service
      scheduler:
        server:
          url: http://127.0.0.1:8081 # Scheduler service
  task-executor:
    service:
      # Intervals for sending heartbeat on the taken task to the scheduler service.
      # If not sent in time, the scheduler service will decide that something is wrong with the task
      # and move it to failed
      heart-beat-initial-delaY-ms: 5000 # delay at startup
      heart-beat-interval-ms: 5000 # interval between sends. Must be more frequent than  lakehouse.scheduler.task.retry.delay-ms in the scheduler service
      max-lock-retries: 5 # attempts to take a lock. The scheduler service may be temporarily unavailable.
      max-lock-retries-duration-ms: 5 # delay between attempts
      # Multiple executors can be in the same group.
      # This identifier will be sent to the scheduler service when locking a task,
      # so that it is possible to find out which specific instance took the task lock.
      # You can specify a pod or host name
      id: first1
    scheduled: # Parameters for receiving tasks
      task:
        kafka:
          consumer:
            concurrency: 1 # number of task consumption threads. 1 means the process will process 1 task at a time sequentially. The functionality is provided by Spring, it has not been tested in detail.
            properties:
              bootstrap.servers: 192.1.193.20:9092
              group.id: default  # Corresponds to the taskExecutionServiceGroupName parameter from the task configuration. if this parameter and the task's taskExecutionServiceGroupName do not match, the task is ignored because another executor group should take it
              auto.offset.reset: earliest
            # The name of the topic where the scheduler service puts tasks passed for execution.
            # The name must match the one in the scheduler service
            topics: scheduled_task_msg
```