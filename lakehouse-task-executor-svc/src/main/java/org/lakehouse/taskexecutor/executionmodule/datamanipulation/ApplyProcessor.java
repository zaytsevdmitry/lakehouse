package org.lakehouse.taskexecutor.executionmodule.datamanipulation;

import org.lakehouse.client.api.constant.Status;
import org.lakehouse.taskexecutor.entity.TaskProcessorConfig;
import org.lakehouse.taskexecutor.executionmodule.AbstractTaskProcessor;

public class ApplyProcessor extends AbstractTaskProcessor{

	public ApplyProcessor(TaskProcessorConfig taskProcessorConfig) {
		super(taskProcessorConfig);
	}


	@Override
	public Status.Task runTask() {
		return Status.Task.SUCCESS;
	}

}