package org.lakehouse.taskexecutor.executionmodule.datamanipulation;

import org.lakehouse.common.api.task.processor.entity.TaskProcessorConfigDTO;
import org.lakehouse.common.api.task.processor.exception.TaskFailedException;
import org.lakehouse.taskexecutor.executionmodule.AbstractDefaultTaskProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MergeProcessor extends AbstractDefaultTaskProcessor {
	final private Logger logger = LoggerFactory.getLogger(this.getClass());

	public MergeProcessor(TaskProcessorConfigDTO taskProcessorConfigDTO) {
		super(taskProcessorConfigDTO);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void runTask() throws TaskFailedException {

	}

}