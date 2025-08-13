package org.lakehouse.taskexecutor.executionmodule;
import org.lakehouse.common.api.task.processor.entity.TaskProcessor;
import org.lakehouse.common.api.task.processor.entity.TaskProcessorConfigDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public abstract class AbstractTaskProcessor implements TaskProcessor {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private final TaskProcessorConfigDTO taskProcessorConfigDTO;
	public AbstractTaskProcessor(
            TaskProcessorConfigDTO taskProcessorConfigDTO) {
		this.taskProcessorConfigDTO = taskProcessorConfigDTO;
    }

	public TaskProcessorConfigDTO getTaskProcessorConfig() {
		return taskProcessorConfigDTO;
	}

}
