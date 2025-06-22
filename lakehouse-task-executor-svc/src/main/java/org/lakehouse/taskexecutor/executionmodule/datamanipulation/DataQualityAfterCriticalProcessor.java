package org.lakehouse.taskexecutor.executionmodule.datamanipulation;


import com.hubspot.jinjava.Jinjava;
import org.lakehouse.taskexecutor.entity.TaskProcessorConfig;
import org.lakehouse.taskexecutor.exception.TaskFailedException;
import org.lakehouse.taskexecutor.executionmodule.AbstractDefaultTaskProcessor;

public class DataQualityAfterCriticalProcessor extends AbstractDefaultTaskProcessor {

	public DataQualityAfterCriticalProcessor(TaskProcessorConfig taskProcessorConfig, Jinjava jinjava) {
		super(taskProcessorConfig,jinjava);
	}


	@Override
	public void runTask()throws TaskFailedException {

	}

}