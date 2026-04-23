package org.lakehouse.client.rest.taskexecutor;


import org.lakehouse.client.api.dto.scheduler.lock.ScheduledTaskLockDTO;

public interface TaskExecutorRestClientApi {
    ScheduledTaskLockDTO getScheduledTaskLockDTO(Long lockId);
}
