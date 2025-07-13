package org.lakehouse.taskexecutor.executionmodule.datamanipulation;

import com.hubspot.jinjava.Jinjava;
import org.lakehouse.taskexecutor.entity.TaskProcessorConfig;
import org.lakehouse.taskexecutor.exception.TaskFailedException;
import org.lakehouse.taskexecutor.executionmodule.AbstractDefaultTaskProcessor;

public class DataQualityBeforeProcessor extends AbstractDefaultTaskProcessor {

	public DataQualityBeforeProcessor(TaskProcessorConfig taskProcessorConfig, Jinjava jinjava) {
		super(taskProcessorConfig, jinjava);
		// TODO Auto-generated constructor stub
	}


	@Override
	public void runTask() throws TaskFailedException {

	}
}