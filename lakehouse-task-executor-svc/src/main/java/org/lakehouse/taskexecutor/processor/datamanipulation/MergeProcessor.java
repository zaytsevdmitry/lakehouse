package org.lakehouse.taskexecutor.processor.datamanipulation;

import org.lakehouse.client.api.dto.task.TaskProcessorConfigDTO;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.taskexecutor.processor.AbstractDefaultTaskProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MergeProcessor extends AbstractDefaultTaskProcessor {
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    public MergeProcessor(TaskProcessorConfigDTO taskProcessorConfigDTO) {
        super(taskProcessorConfigDTO);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void runTask() throws TaskFailedException {

    }

}