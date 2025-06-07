package org.lakehouse.taskexecutor.executionmodule.datamanipulation;

import com.hubspot.jinjava.Jinjava;
import org.lakehouse.client.api.constant.Status;
import org.lakehouse.taskexecutor.entity.TaskProcessorConfig;
import org.lakehouse.taskexecutor.executionmodule.AbstractTaskProcessor;

public class FinallyProcessor extends AbstractTaskProcessor{

	public FinallyProcessor(TaskProcessorConfig taskProcessorConfig, Jinjava jinjava) {
		super(taskProcessorConfig, jinjava);
		// TODO Auto-generated constructor stub
	}


	@Override
	public Status.Task runTask() {
		return Status.Task.SUCCESS;
	}

}