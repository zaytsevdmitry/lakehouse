package org.lakehouse.taskexecutor.executionmodule.datamanipulation;

import org.lakehouse.client.api.constant.Status;
import org.lakehouse.taskexecutor.entity.TaskProcessorConfig;
import org.lakehouse.taskexecutor.executionmodule.AbstractTaskProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MergeProcessor extends AbstractTaskProcessor{
	final private Logger logger = LoggerFactory.getLogger(this.getClass());

	public MergeProcessor(TaskProcessorConfig taskProcessorConfig) {
		super(taskProcessorConfig);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Status.Task  runTask() {
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage(), e);
		} catch (Exception e){
		return Status.Task.FAILED;
	}
        return Status.Task.SUCCESS;
	}

}