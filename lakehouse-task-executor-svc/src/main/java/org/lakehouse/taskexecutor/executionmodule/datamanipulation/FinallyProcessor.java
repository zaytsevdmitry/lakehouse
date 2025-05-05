package org.lakehouse.taskexecutor.executionmodule.datamanipulation;

import org.lakehouse.client.api.constant.Status;
import org.lakehouse.taskexecutor.entity.TaskProcessorConfig;
import org.lakehouse.taskexecutor.executionmodule.AbstractTaskProcessor;

public class FinallyProcessor extends AbstractTaskProcessor{

	public FinallyProcessor(TaskProcessorConfig taskProcessorConfig) {
		super(taskProcessorConfig);
		// TODO Auto-generated constructor stub
	}


	@Override
	public Status.Task runTask() {
		return Status.Task.SUCCESS;
	}

}