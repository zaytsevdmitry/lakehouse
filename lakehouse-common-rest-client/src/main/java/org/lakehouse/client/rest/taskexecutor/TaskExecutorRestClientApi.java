package org.lakehouse.client.rest.taskexecutor;

import org.lakehouse.client.api.dto.task.TaskProcessorConfigDTO;

public interface TaskExecutorRestClientApi {
    TaskProcessorConfigDTO getTaskProcessorConfigDto(Long lockId);
}
