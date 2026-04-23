package org.lakehouse.taskexecutor.api.processor.body;

import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.exception.TaskFailedException;

public interface ProcessorBody {
    void run(ScheduledTaskDTO scheduledTaskDTO) throws TaskFailedException, TaskConfigurationException;
}

