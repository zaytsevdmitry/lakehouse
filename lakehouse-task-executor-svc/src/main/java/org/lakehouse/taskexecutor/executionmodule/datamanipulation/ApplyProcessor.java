package org.lakehouse.taskexecutor.executionmodule.datamanipulation;

import com.hubspot.jinjava.Jinjava;
import org.lakehouse.client.api.constant.Status;
import org.lakehouse.taskexecutor.entity.TaskProcessorConfig;
import org.lakehouse.taskexecutor.executionmodule.AbstractTaskProcessor;

public class ApplyProcessor extends AbstractTaskProcessor{

	public ApplyProcessor(TaskProcessorConfig taskProcessorConfig, Jinjava  jinjava) {
		super(taskProcessorConfig, jinjava);
	}


	@Override
	public Status.Task runTask() {
		return Status.Task.SUCCESS;
	}

}