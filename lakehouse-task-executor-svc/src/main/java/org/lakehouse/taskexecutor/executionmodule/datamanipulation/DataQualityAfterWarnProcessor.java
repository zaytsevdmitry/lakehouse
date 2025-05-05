package org.lakehouse.taskexecutor.executionmodule.datamanipulation;


import org.lakehouse.client.api.constant.Status;
import org.lakehouse.taskexecutor.entity.TaskProcessorConfig;
import org.lakehouse.taskexecutor.executionmodule.AbstractTaskProcessor;

public class DataQualityAfterWarnProcessor extends AbstractTaskProcessor{

	public DataQualityAfterWarnProcessor(TaskProcessorConfig taskProcessorConfig) {
		super(taskProcessorConfig);
	}


	@Override
	public Status.Task runTask() {
		return Status.Task.SUCCESS;
	}

}