package org.lakehouse.client.rest.taskexecutor;

import org.lakehouse.common.api.task.processor.entity.TaskProcessorConfigDTO;

public interface TaskExecutorRestClientApi {
    TaskProcessorConfigDTO getTaskProcessorConfigDto(Long lockId);
}
