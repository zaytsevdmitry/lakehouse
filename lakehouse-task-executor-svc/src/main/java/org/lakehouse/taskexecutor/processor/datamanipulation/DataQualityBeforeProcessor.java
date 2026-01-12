package org.lakehouse.taskexecutor.processor.datamanipulation;

import org.lakehouse.client.api.dto.task.TaskProcessorConfigDTO;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.taskexecutor.processor.AbstractDefaultTaskProcessor;

public class DataQualityBeforeProcessor extends AbstractDefaultTaskProcessor {

    public DataQualityBeforeProcessor(TaskProcessorConfigDTO taskProcessorConfigDTO) {
        super(taskProcessorConfigDTO);
        // TODO Auto-generated constructor stub
    }


    @Override
    public void runTask() throws TaskFailedException {

    }
}