package org.lakehouse.taskexecutor.executionmodule;
import org.lakehouse.taskexecutor.entity.TaskProcessor;
import org.lakehouse.taskexecutor.entity.TaskProcessorConfig;

import java.util.List;
import java.util.Map;
public abstract class AbstractTaskProcessor implements TaskProcessor {

	private final TaskProcessorConfig taskProcessorConfig;
	public AbstractTaskProcessor(
			TaskProcessorConfig taskProcessorConfig) {
		this.taskProcessorConfig = taskProcessorConfig;
    }

	public TaskProcessorConfig getTaskProcessorConfig() {
		return taskProcessorConfig;
	}
}
