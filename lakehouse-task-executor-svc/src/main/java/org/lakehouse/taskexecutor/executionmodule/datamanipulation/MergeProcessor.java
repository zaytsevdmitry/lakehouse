package org.lakehouse.taskexecutor.executionmodule.datamanipulation;

import com.hubspot.jinjava.Jinjava;
import org.lakehouse.taskexecutor.entity.TaskProcessorConfig;
import org.lakehouse.taskexecutor.exception.TaskFailedException;
import org.lakehouse.taskexecutor.executionmodule.AbstractDefaultTaskProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MergeProcessor extends AbstractDefaultTaskProcessor {
	final private Logger logger = LoggerFactory.getLogger(this.getClass());

	public MergeProcessor(TaskProcessorConfig taskProcessorConfig, Jinjava jinjava) {
		super(taskProcessorConfig, jinjava);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void runTask() throws TaskFailedException {

	}

}