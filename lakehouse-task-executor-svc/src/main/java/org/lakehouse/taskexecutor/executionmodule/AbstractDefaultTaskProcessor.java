package org.lakehouse.taskexecutor.executionmodule;

import org.lakehouse.common.api.task.processor.entity.TaskProcessorConfigDTO;

public abstract class AbstractDefaultTaskProcessor extends AbstractTaskProcessor{
    public AbstractDefaultTaskProcessor(TaskProcessorConfigDTO taskProcessorConfigDTO) {
        super(taskProcessorConfigDTO);
    }
}
