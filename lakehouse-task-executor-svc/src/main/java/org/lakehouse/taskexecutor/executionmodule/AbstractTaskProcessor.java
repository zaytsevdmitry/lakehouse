package org.lakehouse.taskexecutor.executionmodule;

import org.lakehouse.client.api.dto.task.TaskProcessor;
import org.lakehouse.client.api.dto.task.TaskProcessorConfigDTO;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class AbstractTaskProcessor implements TaskProcessor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final TaskProcessorConfigDTO taskProcessorConfigDTO;

    public AbstractTaskProcessor(
            TaskProcessorConfigDTO taskProcessorConfigDTO) {
        this.taskProcessorConfigDTO = taskProcessorConfigDTO;
    }

    public TaskProcessorConfigDTO getTaskProcessorConfig() {
        return taskProcessorConfigDTO;
    }

    protected void sleep(long ms) throws TaskFailedException {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new TaskFailedException("Sleep failed", e);
        }
    }
}
