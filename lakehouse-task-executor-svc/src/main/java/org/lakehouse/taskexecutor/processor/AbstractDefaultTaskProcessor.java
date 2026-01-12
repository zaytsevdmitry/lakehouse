package org.lakehouse.taskexecutor.processor;

import org.lakehouse.client.api.dto.task.TaskProcessorConfigDTO;

public abstract class AbstractDefaultTaskProcessor extends AbstractTaskProcessor {
    public AbstractDefaultTaskProcessor(TaskProcessorConfigDTO taskProcessorConfigDTO) {
        super(taskProcessorConfigDTO);
    }
}
