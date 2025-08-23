package org.lakehouse.taskexecutor.executionmodule.datamanipulation;

import org.lakehouse.common.api.task.processor.entity.TaskProcessorConfigDTO;
import org.lakehouse.common.api.task.processor.exception.TaskFailedException;
import org.lakehouse.taskexecutor.executionmodule.AbstractDefaultTaskProcessor;

;

public class ApplyProcessor extends AbstractDefaultTaskProcessor {

	public ApplyProcessor(TaskProcessorConfigDTO taskProcessorConfigDTO) {
		super(taskProcessorConfigDTO);
	}


	@Override
	public void runTask() throws TaskFailedException {

	}

}