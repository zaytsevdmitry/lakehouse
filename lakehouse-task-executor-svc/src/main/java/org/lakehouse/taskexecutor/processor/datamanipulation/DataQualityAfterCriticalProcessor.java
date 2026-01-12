package org.lakehouse.taskexecutor.processor.datamanipulation;


import org.lakehouse.client.api.dto.task.TaskProcessorConfigDTO;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.taskexecutor.processor.AbstractDefaultTaskProcessor;

public class DataQualityAfterCriticalProcessor extends AbstractDefaultTaskProcessor {

    public DataQualityAfterCriticalProcessor(TaskProcessorConfigDTO taskProcessorConfigDTO) {
        super(taskProcessorConfigDTO);
    }


    @Override
    public void runTask() throws TaskFailedException {

    }

}