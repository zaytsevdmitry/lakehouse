package org.lakehouse.client.rest.taskexecutor;

import org.lakehouse.client.api.constant.Endpoint;
import org.lakehouse.client.rest.RestClientHelper;
import org.lakehouse.common.api.task.processor.entity.TaskProcessorConfigDTO;

public class TaskExecutorRestClientApiImpl implements TaskExecutorRestClientApi{
    private final RestClientHelper restClientHelper;

    public TaskExecutorRestClientApiImpl(RestClientHelper restClientHelper) {
        this.restClientHelper = restClientHelper;
    }

    @Override
    public TaskProcessorConfigDTO getTaskProcessorConfigDto(Long lockId) {
        return restClientHelper.getDtoOne(lockId.toString(), Endpoint.TASK_EXECUTOR_PROCESSOR_GET_BY_LOCK_ID, TaskProcessorConfigDTO.class);
    }
}
