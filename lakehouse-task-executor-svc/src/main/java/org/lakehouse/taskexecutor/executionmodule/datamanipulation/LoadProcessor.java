package org.lakehouse.taskexecutor.executionmodule.datamanipulation;


import org.lakehouse.client.api.dto.task.TaskProcessorConfigDTO;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.taskexecutor.executionmodule.AbstractDefaultTaskProcessor;

public class LoadProcessor extends AbstractDefaultTaskProcessor {

	public LoadProcessor(TaskProcessorConfigDTO taskProcessorConfigDTO) {
		super(taskProcessorConfigDTO);
		// TODO Auto-generated constructor stub
	}


	@Override
	public void runTask() throws TaskFailedException {

	}
}
