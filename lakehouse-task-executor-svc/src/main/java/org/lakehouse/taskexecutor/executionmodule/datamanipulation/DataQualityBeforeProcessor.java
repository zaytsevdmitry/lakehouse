package org.lakehouse.taskexecutor.executionmodule.datamanipulation;

import org.lakehouse.common.api.task.processor.entity.TaskProcessorConfigDTO;
import org.lakehouse.common.api.task.processor.exception.TaskFailedException;
import org.lakehouse.taskexecutor.executionmodule.AbstractDefaultTaskProcessor;

public class DataQualityBeforeProcessor extends AbstractDefaultTaskProcessor {

	public DataQualityBeforeProcessor(TaskProcessorConfigDTO taskProcessorConfigDTO) {
		super(taskProcessorConfigDTO);
		// TODO Auto-generated constructor stub
	}


	@Override
	public void runTask() throws TaskFailedException {

	}
}