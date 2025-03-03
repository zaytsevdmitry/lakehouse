package org.lakehouse.taskexecutor.executionmodule.datamanipulation;

import java.util.Map;

import org.lakehouse.client.api.constant.Status;
import org.lakehouse.taskexecutor.entity.TaskProcessorConfig;
import org.lakehouse.taskexecutor.executionmodule.AbstractTaskProcessor;

public class LoadProcessor extends AbstractTaskProcessor{

	public LoadProcessor(TaskProcessorConfig taskProcessorConfig) {
		super(taskProcessorConfig);
		// TODO Auto-generated constructor stub
	}


	@Override
	public Status.Task runTask() {
		return Status.Task.SUCCESS;
	}
}
