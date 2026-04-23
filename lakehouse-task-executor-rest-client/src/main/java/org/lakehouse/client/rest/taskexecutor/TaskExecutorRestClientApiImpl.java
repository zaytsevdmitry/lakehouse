package org.lakehouse.client.rest.taskexecutor;

import org.lakehouse.client.api.constant.Endpoint;
import org.lakehouse.client.api.dto.scheduler.lock.ScheduledTaskLockDTO;
import org.lakehouse.client.rest.RestClientHelper;

public class TaskExecutorRestClientApiImpl implements TaskExecutorRestClientApi {
    private final RestClientHelper restClientHelper;

    public TaskExecutorRestClientApiImpl(RestClientHelper restClientHelper) {
        this.restClientHelper = restClientHelper;
    }

    @Override
    public ScheduledTaskLockDTO getScheduledTaskLockDTO(Long lockId) {
        return restClientHelper.getDtoOne(lockId.toString(), Endpoint.TASK_EXECUTOR_PROCESSOR_GET_BY_LOCK_ID, ScheduledTaskLockDTO.class);
    }
}
